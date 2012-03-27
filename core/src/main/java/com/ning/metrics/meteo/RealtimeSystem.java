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

package com.ning.metrics.meteo;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.ServletModule;
import com.ning.metrics.meteo.binder.RealtimeSystemModule;
import com.ning.metrics.meteo.server.JettyServer;
import com.ning.metrics.meteo.subscribers.SubscribersCompiler;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import org.eclipse.jetty.servlet.DefaultServlet;

import java.util.HashMap;

public class RealtimeSystem
{
    public static void main(final String[] args) throws Exception
    {
        final Injector injector = Guice.createInjector(
            new RealtimeSystemModule(),
            new ServletModule()
            {
                @Override
                protected void configureServlets()
                {
                    // Static files
                    bind(DefaultServlet.class).asEagerSingleton();
                    serve("/media/*").with(DefaultServlet.class);

                    serve("*").with(GuiceContainer.class, new HashMap<String, String>()
                    {
                        {
                            put(PackagesResourceConfig.PROPERTY_PACKAGES, "com.ning.metrics.meteo.server.resources");
                        }
                    });
                }
            }
        );

        final SubscribersCompiler subscribersCompiler = injector.getInstance(SubscribersCompiler.class);
        subscribersCompiler.startAll();

        final JettyServer jetty = injector.getInstance(JettyServer.class);
        jetty.start(injector);

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                subscribersCompiler.stopAll();
                jetty.stop();
            }
        });
    }
}
