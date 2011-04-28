/*
 * Copyright 2010-2011 Ning, Inc.
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


import com.espertech.esper.client.EPServiceProvider;
import org.apache.log4j.Logger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class JMXSubscriber implements Subscriber
{
    private final static Logger log = Logger.getLogger(JMXSubscriber.class);

    private final JMXSubscriberConfig jmxConfig;
    private JMXConnector jmxConn;
    private MBeanServerConnection mbeanConn;
    private ObjectName mbeanName;
    private String[] attrNames;
    private EPServiceProvider esperSink;
    private AtomicBoolean closed = new AtomicBoolean(true);
    private ScheduledFuture<?> worker;

    public JMXSubscriber(JMXSubscriberConfig jmxConfig, EPServiceProvider esperSink)
    {
        this.jmxConfig = jmxConfig;
        this.esperSink = esperSink;
    }

    private void connect() throws IOException
    {
        String url = String.format("service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi", jmxConfig.getHost(), jmxConfig.getPort());

        log.info("Connecting to: " + url);

        try {
            JMXServiceURL urlObj = new JMXServiceURL(url);

            jmxConn = JMXConnectorFactory.connect(urlObj);
            mbeanConn = jmxConn.getMBeanServerConnection();
            mbeanName = new ObjectName(jmxConfig.getQuery());
            attrNames = jmxConfig.getAttributes();
            closed.set(false);
        }
        catch (Exception ex) {
            if (jmxConn != null) {
                try {
                    jmxConn.close();
                    jmxConn = null;
                }
                catch (IOException innerEx) {
                    // ignored
                }
            }
            if (ex instanceof IOException) {
                throw (IOException) ex;
            }
            else {
                throw new RuntimeException(ex);
            }
        }
        log.info("Connected!");
    }

    @Override
    public void subscribe()
    {
        try {
            connect();
        }
        catch (IOException e) {
            return; // ignored
        }

        this.worker = Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(new Runnable()
        {
            @Override
            public void run()
            {
                final AttributeList attrList;
                try {
                    attrList = mbeanConn.getAttributes(mbeanName, attrNames);
                    final LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();

                    log.debug(String.format("Found %d data points", attrList.size()));

                    for (Object attrObj : attrList) {
                        Attribute attr = (Attribute) attrObj;

                        data.put(attr.getName(), attr.getValue());
                    }
                    log.debug("Received a message, yay!\n" + data);
                    esperSink.getEPRuntime().sendEvent(data, jmxConfig.getEventOutputName());
                }
                catch (InstanceNotFoundException ex) {
                    log.error("Could not fetch from JMX", ex);
                }
                catch (ReflectionException ex) {
                    log.error("Could not fetch from JMX", ex);
                }
                catch (IOException ex) {
                    log.error("Could not fetch from JMX", ex);
                }
            }
        }, 0, jmxConfig.getPollIntervalInMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void unsubscribe()
    {
        worker.cancel(true);

        try {
            if (jmxConn != null) {
                jmxConn.close();
                jmxConn = null;
            }
        }
        catch (IOException ignored) {
        }
    }
}
