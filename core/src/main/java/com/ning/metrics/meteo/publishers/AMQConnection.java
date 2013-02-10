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

package com.ning.metrics.meteo.publishers;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;
import java.util.concurrent.atomic.AtomicBoolean;

public class AMQConnection
{
    private static final Logger logger = Logger.getLogger(AMQConnection.class);

    private ActiveMQConnectionFactory connectionFactory = null;
    private final Object connectionMonitor = new Object();
    private TopicConnection connection = null;
    private final AtomicBoolean useBytesMessage;

    public AMQConnection(final AMQPublisherConfig baseConfig)
    {
        useBytesMessage = new AtomicBoolean(baseConfig.getUseBytesMessage());

        final String uri = baseConfig.getUri();
        if (uri != null) {
            this.connectionFactory = new ActiveMQConnectionFactory(uri);
            this.connectionFactory.setUseAsyncSend(baseConfig.getUseAsyncSend());
        }
    }

    public void reconnect()
    {
        final long startTime = System.currentTimeMillis();

        if (connectionFactory == null) {
            logger.warn("Asked to reconnect to AMQ but no connectionFactory was configured!");
            return;
        }

        synchronized (connectionMonitor) {
            close();
            int numTries = 0;
            int pauseInMs = 100;
            boolean connected = false;
            while (!connected) {
                numTries++;
                try {
                    connection = connectionFactory.createTopicConnection();
                    connection.start();
                    connected = true;
                }
                catch (JMSException ex) {
                    logger.warn("Got error while trying to connect to activemq");
                    try {
                        Thread.sleep((long) pauseInMs);
                    }
                    catch (InterruptedException innerEx) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    if (numTries < 10) {
                        pauseInMs += pauseInMs;
                    }
                }
            }
        }

        final long secondsToReconnect = (System.currentTimeMillis() - startTime) / 1000;
        logger.info(String.format("Reconnected to AMQ in %d seconds", secondsToReconnect));
    }

    public AMQSession getSessionFor(final String type, final AMQPublisherConfig config)
    {
        return new AMQSession(config, this, type, useBytesMessage);
    }

    TopicSession createTopicSession()
    {
        TopicSession result = null;

        while (result == null) {
            try {
                result = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            }
            catch (JMSException ex) {
                reconnect();
            }
        }
        return result;
    }

    public void close()
    {
        synchronized (connectionMonitor) {
            if (connection != null) {
                try {
                    connection.close();
                }
                catch (JMSException ex) {
                    logger.error("Error while closing the connection to ActiveMQ", ex);
                }
                connection = null;
            }
        }
    }
}
