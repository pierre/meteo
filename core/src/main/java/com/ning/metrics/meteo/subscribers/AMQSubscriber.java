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


import com.espertech.esper.client.EPServiceProvider;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import java.util.concurrent.atomic.AtomicBoolean;

class AMQSubscriber implements Subscriber
{
    private final static Logger log = Logger.getLogger(AMQSubscriber.class);

    private Connection connection;
    private Session session;
    private MessageConsumer consumer;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private final AMQSubscriberConfig amqConfig;
    private final MessageListener listener;

    public AMQSubscriber(AMQSubscriberConfig amqConfig, EPServiceProvider epService)
    {
        this.amqConfig = amqConfig;
        this.listener = new TopicListener(amqConfig.getEventOutputName(), epService);
    }

    @Override
    public void subscribe()
    {
        failSafeConnect(amqConfig.getInitialBackoffTime(), amqConfig.getMaxBackoffTime());
    }

    private void failSafeConnect(final int initialBackoffTime, final int maxBackoffTime)
    {
        int backoffTime = initialBackoffTime;

        // Disconnect if we have a broken connection
        unsubscribe();

        // Try to open
        try {
            log.info("Attempting to connect to ActiveMQ");
            connect();
            return;
        }
        catch (JMSException e) {
            log.warn("Unable to connect to ActiveMQ. Will retry in " + backoffTime + " ms", e);
        }

        // Failed :( Let's try again
        unsubscribe();

        try {
            Thread.sleep(backoffTime);
        }
        catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }

        backoffTime = backoffTime * 2;
        if (backoffTime > maxBackoffTime) {
            backoffTime = maxBackoffTime;
        }

        failSafeConnect(backoffTime, maxBackoffTime);
    }

    private void connect() throws JMSException
    {
        String url = String.format("%s://%s:%d", amqConfig.getProtocol(), amqConfig.getHost(), amqConfig.getPort());
        log.info("Connecting to: " + url);

        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(amqConfig.getUsername(), amqConfig.getPassword(), url);
        connection = connectionFactory.createConnection();
        connection.start();

        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        consumer = session.createConsumer(session.createTopic(amqConfig.getTopic()));
        consumer.setMessageListener(listener);

        log.info("Connected!");
    }


    @Override
    public void unsubscribe()
    {
        try {
            closed.set(true);

            if (consumer != null) {
                consumer.close();
                consumer = null;
            }
            if (session != null) {
                session.close();
                session = null;
            }
            if (connection != null) {
                connection.close();
                connection = null;
            }
        }
        catch (JMSException ignored) {
        }
    }
}
