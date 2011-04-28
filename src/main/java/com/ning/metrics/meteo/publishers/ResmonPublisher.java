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

package com.ning.metrics.meteo.publishers;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class ResmonPublisher
{
    private final Map<String, Object> currentMetrics = new ConcurrentHashMap<String, Object>();

    public ResmonPublisher() throws Exception
    {
        Handler handler = new AbstractHandler()
        {
            public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
                throws IOException, ServletException
            {

            }

            /**
             * Handle a request.
             *
             * @param target      The target of the request - either a URI or a name.
             * @param baseRequest The original unwrapped request object.
             * @param request     The request either as the {@link org.eclipse.jetty.server.Request}
             *                    object or a wrapper of that request. The {@link org.eclipse.jetty.server.HttpConnection#getCurrentConnection()}
             *                    method can be used access the Request object if required.
             * @param response    The response as the {@link org.eclipse.jetty.server.Response}
             *                    object or a wrapper of that request. The {@link org.eclipse.jetty.server.HttpConnection#getCurrentConnection()}
             *                    method can be used access the Response object if required.
             * @throws java.io.IOException
             * @throws javax.servlet.ServletException
             */
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                response.setContentType("text/xml; encoding=UTF-8");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println(writeResmon());
                ((Request) request).setHandled(true);
            }
        };

        Server server = new Server(8083);
        server.setHandler(handler);
        server.start();
    }

    private String writeResmon()
    {
        StringBuilder builder = new StringBuilder();
        builder.setLength(0);
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        builder.append("<?xml-stylesheet type=\"text/xsl\" href=\"resmon.xsl\"?>\n");
        builder.append("<ResmonResults>\n");
        builder.append(String.format("\t<ResmonResult module=\"%s\" service=\"%s\">\n", "amq", "graphing"));
        builder.append(String.format("\t\t<last_runtime_seconds>%d</last_runtime_seconds>\n", 0)); // TODO
        builder.append(String.format("\t\t<last_update>%d</last_update>\n", System.currentTimeMillis()));

        // n for float
        for (String metric : currentMetrics.keySet()) {
            builder.append(String.format("\t\t<metric name=\"%s\" type=\"%s\">%s</metric>\n", metric, 'n', currentMetrics.get(metric)));
        }
        builder.append(String.format("\t\t<state>%s</state>\n\t</ResmonResult>\n", "OK"));


        builder.append("</ResmonResults>\n");

        return builder.toString();
    }

    public void send(String metric, Object value)
    {
        currentMetrics.put(metric, value);
    }
}
