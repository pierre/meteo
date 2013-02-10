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

package com.ning.metrics.meteo.server;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import com.ning.metrics.meteo.binder.RealtimeSystemConfig;
import org.apache.log4j.Logger;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;

import javax.management.MBeanServer;
import javax.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.EventListener;

public class JettyServer
{
    private static final Logger log = Logger.getLogger(JettyServer.class);

    private final RealtimeSystemConfig config;
    private final MBeanServer mbeanServer;
    private Server server;

    @Inject
    public JettyServer(final RealtimeSystemConfig config, final MBeanServer mbeanServer)
    {
        this.config = config;
        this.mbeanServer = mbeanServer;
    }

    public void start(final Injector injector) throws Exception
    {
        final long startTime = System.currentTimeMillis();

        server = new Server();

        // Setup JMX
        final MBeanContainer mbContainer = new MBeanContainer(mbeanServer);
        server.getContainer().addEventListener(mbContainer);
        server.addBean(mbContainer);
        mbContainer.addBean(Log.getLog());

        final Connector connector = new SelectChannelConnector();
        connector.setStatsOn(config.isJettyStatsOn());
        connector.setHost(config.getLocalIp());
        connector.setPort(config.getLocalPort());
        server.addConnector(connector);

        server.setStopAtShutdown(true);

        final ServletContextHandler context = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addEventListener(new GuiceServletContextListener()
        {
            @Override
            protected Injector getInjector()
            {
                return injector;
            }
        });

        // Jersey insists on using java.util.logging (JUL)
        final EventListener listener = new SetupJULBridge();
        context.addEventListener(listener);

        // Make sure Guice filter all requests
        final FilterHolder filterHolder = new FilterHolder(GuiceFilter.class);
        context.addFilter(filterHolder, "/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC));

        // Backend servlet for Guice - never used
        final ServletHolder sh = new ServletHolder(DefaultServlet.class);
        context.addServlet(sh, "/*");

        server.start();

        final long secondsToStart = (System.currentTimeMillis() - startTime) / 1000;
        log.info(String.format("Jetty server started in %d:%02d", secondsToStart / 60, secondsToStart % 60));
    }

    public void stop()
    {
        try {
            server.stop();
        }
        catch (Exception e) {
            log.warn("Got exception trying to stop Jetty", e);
        }
    }
}
