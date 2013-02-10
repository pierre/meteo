/*
 * Copyright 2010-2013 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.metrics.meteo.subscribers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.xml.XMLSerializer;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.espertech.esper.client.EPServiceProvider;
import com.google.inject.Inject;

/**
 * A Subscriber which reads 1K of JSON as the event.
 * 
 * The JSON is converted to an XML DOM Node guessing at the types.
 * 
 * To configure this in Esper, using something like this:
 * &lt;event-type name="UDPEvents"&gt;
 *   &lt;xml-dom root-element-name="UDPEvents" /&gt;
 * &lt;/event-type&gt;
 * 
 * To configure this in meteo use something like this:
 * {
 *   "name": "UDP JSON",
 *   "type": "com.ning.metrics.meteo.subscribers.UdpJsonSubscriber",
 *   "@class": "com.ning.metrics.meteo.subscribers.UdpJsonSubscriberConfig",
 *   "port": 5678,
 *   "eventOutputName": "UDPEvents",
 *   "enabled": true
 * }
 * 
 * @author William Speirs <bill.speirs@gmail.com>
 */
class UdpJsonSubscriber implements Subscriber
{
    private final Logger log = Logger.getLogger(UdpJsonSubscriber.class);

    private final EPServiceProvider esperSink;
    private final UdpJsonSubscriberConfig config;
    
    private final DatagramSocket socket;
    private final DatagramPacket packet;
    
    private Thread acceptThread;
    private ExecutorService handlerPool;
    boolean running = false;

    @Inject
    public UdpJsonSubscriber(UdpJsonSubscriberConfig config, EPServiceProvider esperSink) throws SocketException, UnknownHostException {
        this.config = config;
        this.esperSink = esperSink;
        
        this.socket = new DatagramSocket(config.getPort(), InetAddress.getByName("0.0.0.0"));
        this.packet = new DatagramPacket(new byte[config.getPacketSize()], config.getPacketSize());
        
        log.info("Created UDP socket on port " + config.getPort() + " with packet size " + config.getPacketSize());
    }

    @Override
    public void subscribe() {
        // set our status to running
        running = true;
        
        // create the handler pool
        handlerPool = Executors.newCachedThreadPool();

        // create our accepter thread
        acceptThread = new Thread() {
          @Override
          public void run() {
              while(running) {
                  try {
                      log.debug("Waiting on packet");
                      socket.receive(packet);
                      log.debug("Got packet: " + packet.getLength());
                  } catch (IOException e) {
                      log.error("Error receiving packet: " + e.getMessage());
                  }
                  
                  // create and submit the worker to the thread pool
                  handlerPool.submit(new PacketHandler(packet.getData()));
              }
          }
        };
        
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    @Override
    public void unsubscribe() {
        log.info("Unsubscribing...");
        // stop the processing loop
        running = false;
        
        // interrupt the thread if it's waiting
        acceptThread.interrupt();
        
        try {
            acceptThread.join();
        } catch (InterruptedException e) {
        }
        
        // shutdown the handler thread pool
        handlerPool.shutdownNow();
    }

    protected class PacketHandler implements Runnable {
        private final byte[] packetData;
        
        public PacketHandler(byte[] packetData) {
            this.packetData = packetData;
        }

        @Override
        public void run() {
            log.debug("Running handler...");
            final XMLSerializer serializer = new XMLSerializer(); 
            JSONObject json = null;
            
            try {
                json = (JSONObject)JSONSerializer.toJSON(new String(packetData));
            } catch(ClassCastException e) {
                log.error("Error converting packet to JSON: " + e.getMessage());
                return;
            } catch(Exception e) {
                log.error("Got exception: " + e.getMessage());
                return;
            }
            
            // check for a timestamp, and add if not already there
            if(!json.has("timestamp")) {
                json.put("timestamp", new Date().getTime());
            }

            // set the root to the event output name
            serializer.setRootName(config.getEventOutputName());
            
            final String xml = serializer.write(json);
            final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

            try {
                final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                final Document document = dBuilder.parse(new ByteArrayInputStream(xml.getBytes()));
                
                esperSink.getEPRuntime().sendEvent(document);
                log.debug("JSON event submitted: " + json);
            } catch (ParserConfigurationException e) {
                log.error("Error with parser configuration: " + e.getMessage());
                return;
            } catch (SAXException e) {
                log.error("SAX Exception: " + e.getMessage());
                return;
            } catch (IOException e) {
                log.error("IO Exception: " + e.getMessage());
                e.printStackTrace();
                return;
            }
        }
    }
}
