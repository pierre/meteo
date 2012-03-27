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

package com.ning.metrics.meteo.publishers;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class AMQPublisher
{
    private static final Logger log = Logger.getLogger(AMQPublisher.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, AMQSession> sessionsPerType = new HashMap<String, AMQSession>();
    private final Object queueMapMonitor = new Object();
    private final AMQPublisherConfig config;
    private final AMQConnection connection;

    public AMQPublisher(final AMQPublisherConfig config)
    {
        this.config = config;
        connection = new AMQConnection(config);
    }

    public void send(final String metric, final Map<String, Object> value)
    {
        try {
            publish(metric, value);
        }
        catch (IOException e) {
            log.warn("Error sending event to AMQ: " + e.getLocalizedMessage());
        }
    }

    private void publish(final String eventType, final Map<String, Object> event) throws IOException
    {
        AMQSession session = sessionsPerType.get(eventType);

        if (session == null) {
            synchronized (queueMapMonitor) {
                session = sessionsPerType.get(eventType);
                if (session == null) {
                    session = connection.getSessionFor(eventType, config);
                    sessionsPerType.put(eventType, session);
                }
            }
        }

        session.send(mapper.writeValueAsBytes(event));
    }
}
