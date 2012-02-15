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

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.ning.metrics.meteo.publishers.PublisherConfig;
import com.ning.metrics.meteo.publishers.PublishersCompiler;
import com.ning.metrics.meteo.publishers.ResourceListener;
import com.ning.metrics.meteo.subscribers.SubscriberConfig;
import com.ning.metrics.meteo.subscribers.SubscribersCompiler;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.skife.config.ConfigurationObjectFactory;

import javax.management.MBeanServer;

public class RealtimeSystemModule implements Module
{
    private static ObjectMapper mapper = new ObjectMapper();

    /**
     * Contributes bindings and other configurations for this module to {@code binder}.
     * <p/>
     * <p><strong>Do not invoke this method directly</strong> to install submodules. Instead use
     * {@link com.google.inject.Binder#install(com.google.inject.Module)}, which ensures that {@link com.google.inject.Provides provider methods} are
     * discovered.
     */
    @Override
    public void configure(Binder binder)
    {
        binder.bind(MBeanServer.class).toInstance(ManagementFactory.getPlatformMBeanServer());

        // Jetty/Jersey stuff
        binder.bind(JacksonJsonProvider.class).asEagerSingleton();

        // Main configuration file
        RealtimeSystemConfig config = new ConfigurationObjectFactory(System.getProperties()).build(RealtimeSystemConfig.class);
        binder.bind(RealtimeSystemConfig.class).toInstance(config);

        // Configure Esper
        Configuration configuration = new Configuration();
        if (!config.getEsperConfigurationFile().equals("")) {
            configuration.configure(new File(config.getEsperConfigurationFile()));
        }
        binder.bind(EPServiceProvider.class).toInstance(EPServiceProviderManager.getDefaultProvider(configuration));

        // Parse the main streams files
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        binder.bind(ObjectMapper.class).toInstance(mapper);

        StatementsConfig statementsConfig;
        try {
            statementsConfig = mapper.readValue(new File(config.getConfigurationFile()), StatementsConfig.class);
        } catch (IOException e) {
            throw new RuntimeException("Unable to parse the main configuration file", e);
        }

        if (statementsConfig != null) {
            // Configure the subscribers (started in main)
            binder.bind(new TypeLiteral<List<SubscriberConfig>>()
            {
            }).toInstance(statementsConfig.getSubscribers());
            binder.bind(SubscribersCompiler.class).asEagerSingleton();

            // Configure the streams in the Esper engine
            List<StreamConfig> streams = mergeRoutesAndGlobalPublishers(statementsConfig);
            binder.bind(new TypeLiteral<List<StreamConfig>>()
            {
            }).toInstance(streams);
            binder.bind(PublishersCompiler.class).asEagerSingleton();
        }
    }

    static List<StreamConfig> mergeRoutesAndGlobalPublishers(StatementsConfig statementsConfig)
    {
        HashMap<String, PublisherConfig> globalPublishersConfigs = new HashMap<String, PublisherConfig>();
        for (PublisherConfig globalPublisherConfig : statementsConfig.getPublishers()) {
            globalPublishersConfigs.put(globalPublisherConfig.getName(), globalPublisherConfig);
        }

        // Retrieve the streams for the Esper engine.
        // The streams implicitly map to the correct subscriber (the eventOutputName maps to the field
        // in the esper queries).
        // For the publishers though, we need to bind them manually and merge them

        // For each route, find the corresponding publisher
        for (StreamConfig streamConfig : statementsConfig.getStatements()) {
            List<PublisherConfig> newRoutes = new ArrayList<PublisherConfig>();

            for (HashMap<String, Object> overrides : streamConfig.getRoutes()) {
                String routeName = (String) overrides.get("name");
                PublisherConfig associatedGlobalPublisherConfig = globalPublishersConfigs.get(routeName);
                if (associatedGlobalPublisherConfig != null) {
                    Map<String, Object> base = mapper.convertValue(associatedGlobalPublisherConfig, Map.class);
                    for (String key : overrides.keySet()) {
                        if (overrides.get(key) != null) {
                            base.put(key, overrides.get(key));
                        }
                    }

                    newRoutes.add(mapper.convertValue(base, associatedGlobalPublisherConfig.getClass()));
                }
            }

            streamConfig.setPublishers(newRoutes);
        }
        return statementsConfig.getStatements();
    }
}
