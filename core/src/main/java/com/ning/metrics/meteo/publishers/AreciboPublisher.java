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

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

class AreciboPublisher
{
    private static final Logger log = Logger.getLogger(AreciboPublisher.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final AreciboPublisherConfig config;
    private final AsyncHttpClient asyncSender;

    public static final String HEADER_CONTENT_TYPE = "Content-type";
    public static final String CONTENT_TYPE = "application/json";
    public static final String API_PATH = "/xn/rest/1.0/event";

    public AreciboPublisher(AreciboPublisherConfig config)
    {
        this.config = config;

        AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
        builder.setMaximumConnectionsPerHost(-1);
        asyncSender = new AsyncHttpClient(builder.build());
    }

    public void send(String metric, Map<String, Object> value)
    {
        try {
            publish(metric, convertToRealtimeEvent(metric, value));
        }
        catch (IOException e) {
            log.warn("Error sending event to Arecibo: " + e.getLocalizedMessage());
        }
    }

    private Map<String, Object> convertToRealtimeEvent(String eventType, Map<String, Object> attributes)
    {
        attributes.put("sourceUUID", UUID.randomUUID());
        attributes.put("timestamp", System.currentTimeMillis());
        attributes.put("eventType", eventType);

        return attributes;
    }

    private void publish(String eventType, final Map<String, Object> event) throws IOException
    {
        asyncSender.preparePost(String.format("http://%s:%d%s", config.getHost(), config.getPort(), API_PATH))
            .addHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE)
            .setBody(mapper.writeValueAsBytes(event))
            .execute(new AsyncCompletionHandler<Integer>()
            {
                /**
                 * Invoked once the HTTP response has been fully read.
                 *
                 * @param response The {@link com.ning.http.client.Response}
                 * @return Type of the value that will be returned by the associated {@link java.util.concurrent.Future}
                 */
                @Override
                public Integer onCompleted(Response response) throws Exception
                {
                    if (response.getStatusCode() >= 300) {
                        log.warn("Unexpected response from Arecibo: " + response.getStatusText());
                    }
                    return response.getStatusCode();
                }

                @Override
                public void onThrowable(Throwable t)
                {
                    log.warn(t);
                }
            });
    }
}
