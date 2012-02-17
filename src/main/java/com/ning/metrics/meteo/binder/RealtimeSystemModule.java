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

package com.ning.metrics.meteo.binder;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.ning.metrics.meteo.publishers.PublisherConfig;
import com.ning.metrics.meteo.publishers.PublishersCompiler;
import com.ning.metrics.meteo.subscribers.SubscriberConfig;
import com.ning.metrics.meteo.subscribers.SubscribersCompiler;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.skife.config.ConfigurationObjectFactory;

import javax.management.MBeanServer;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;

public class RealtimeSystemModule implements Module
{
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    }

    /**
     * Contributes bindings and other configurations for this module to {@code binder}.
     * <p/>
     * <p><strong>Do not invoke this method directly</strong> to install submodules. Instead use
     * {@link com.google.inject.Binder#install(com.google.inject.Module)}, which ensures that {@link com.google.inject.Provides provider methods} are
     * discovered.
     */
    @Override
    public void configure(final Binder binder)
    {
        // JMX
        binder.bind(MBeanServer.class).toInstance(ManagementFactory.getPlatformMBeanServer());

        // Jetty/Jersey stuff
        binder.bind(JacksonJsonProvider.class).asEagerSingleton();

        // Main configuration file
        final RealtimeSystemConfig config = new ConfigurationObjectFactory(System.getProperties()).build(RealtimeSystemConfig.class);
        binder.bind(RealtimeSystemConfig.class).toInstance(config);

        // Configure Esper
        final Configuration configuration = new Configuration();
        if (!config.getEsperConfigurationFile().equals("")) {
            configuration.configure(new File(config.getEsperConfigurationFile()));
        }
        binder.bind(EPServiceProvider.class).toInstance(EPServiceProviderManager.getDefaultProvider(configuration));

        // Configure the routes
        configureFromFile(binder, config.getConfigurationFile());
    }

    /**
     * Configure the compiler from a file
     *
     * @param binder          Guice binder
     * @param statementConfig the main configuration file (with subscribers, publishers and streams)
     */
    private void configureFromFile(final Binder binder, final String statementConfig)
    {
        final StatementsConfig statementsConfig;
        try {
            statementsConfig = mapper.readValue(new File(statementConfig), StatementsConfig.class);
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to parse the main configuration file", e);
        }

        // Configure the subscribers (started in main)
        binder.bind(new TypeLiteral<List<SubscriberConfig>>()
        {
        }).toInstance(statementsConfig.getSubscribers());
        binder.bind(SubscribersCompiler.class).asEagerSingleton();

        // Configure the original streams (in the Esper engine)
        binder.bind(new TypeLiteral<List<PublisherConfig>>()
        {
        }).toInstance(statementsConfig.getPublishers());
        binder.bind(new TypeLiteral<List<StreamConfig>>()
        {
        }).toInstance(statementsConfig.getStatements());
        binder.bind(PublishersCompiler.class).asEagerSingleton();
    }
}
