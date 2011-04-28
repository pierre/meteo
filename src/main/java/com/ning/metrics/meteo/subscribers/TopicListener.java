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
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.io.IOException;
import java.util.Map;

class TopicListener implements MessageListener
{
    private final Logger log = Logger.getLogger(TopicListener.class);

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
    }
}
