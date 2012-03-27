/*
 * Copyright 2010-2012 Ning, Inc.
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
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.io.IOException;
import java.util.Map;

class TopicListener implements MessageListener
{
    private final Logger log = LoggerFactory.getLogger(TopicListener.class);

    private final String esperTopicKey;
    private final ObjectMapper mapper;
    private final EPServiceProvider esperSink;

    public TopicListener(String esperTopicKey, EPServiceProvider esperSink)
    {
        this.esperTopicKey = esperTopicKey;
        this.esperSink = esperSink;

        mapper = new ObjectMapper();
        // also: feed may or may not quote field names, so:
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    }

    @Override
    public void onMessage(Message message)
    {
        if (message instanceof TextMessage) {
            TextMessage txtMsg = (TextMessage) message;
            String txt = null;

            try {
                txt = txtMsg.getText();
                log.debug("Received a message, yay!\n" + txt);
                Map event = mapper.readValue(txt, Map.class);
                esperSink.getEPRuntime().sendEvent(event, esperTopicKey);
            }
            catch (JMSException ex) {
                log.warn("Got an error from the message queue", ex);
            }
            catch (ClassCastException ex) {
                log.info("Received message that I couldn't parse: " + txt, ex);
            }
            catch (JsonMappingException ex) {
                log.info("Received message that I couldn't parse: " + txt, ex);
            }
            catch (JsonParseException ex) {
                log.info("Received message that I couldn't parse: " + txt, ex);
            }
            catch (IOException ex) {
                log.warn("Got an error from the message queue", ex);
            }
        }
        else if (message instanceof BytesMessage) {
            final BytesMessage byteMessage = (BytesMessage) message;
            long llen;
            try {
                llen = byteMessage.getBodyLength();
            }
            catch (JMSException e) {
                log.warn("Unable to get message length", e);
                return;
            }

            if (llen > Integer.MAX_VALUE) { // should never occur but...
                log.error("Ridiculously huge message payload, above 32-bit length");
            }
            else {
                final int len = (int) llen;
                final byte[] data = new byte[len];
                final int readLen;
                try {
                    readLen = byteMessage.readBytes(data);
                }
                catch (JMSException e) {
                    log.warn("Unable to get message bytes", e);
                    return;
                }

                if (readLen < len) {
                    log.error("Failed to read byte message contents; read {}, was trying to read {}", readLen, data.length);
                }
                else {
                    final Map event;
                    try {
                        event = mapper.readValue(data, Map.class);
                        esperSink.getEPRuntime().sendEvent(event, esperTopicKey);
                    }
                    catch (IOException e) {
                        log.error("Failed to convert message to Esper Event", readLen, data.length);
                    }
                }
            }
        }
        else {
            log.error("Unexpected message type '{}' from AMQ broker: must skip", message.getClass().getName());
        }
    }
}
