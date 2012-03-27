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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.UpdateListener;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.ning.metrics.meteo.binder.StreamConfig;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PublishersCompiler
{
    private static final Logger log = Logger.getLogger(PublishersCompiler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    }

    // Mapping publisher class -> configuration (written once at start time)
    private final Map<String, PublisherConfig> publisherConfigs = new LinkedHashMap<String, PublisherConfig>();

    // Mapping publisher class -> instance (updated as we add streams)
    private final Map<String, UpdateListener> publisherInstances = new LinkedHashMap<String, UpdateListener>();

    // Mapping streams name -> streamConfig
    private final Map<String, StreamConfig> streamConfigs = new LinkedHashMap<String, StreamConfig>();

    private final EPServiceProvider epService;

    @Inject
    public PublishersCompiler(final List<PublisherConfig> publisherConfigs, final List<StreamConfig> streamConfigs, final EPServiceProvider epService)
    {
        this.epService = epService;

        for (final PublisherConfig globalPublisherConfig : publisherConfigs) {
            this.publisherConfigs.put(globalPublisherConfig.getName(), globalPublisherConfig);
        }

        try {
            for (final StreamConfig stream : streamConfigs) {
                addStream(stream);
            }
        }
        catch (Exception ex) {
            log.error("Could not instantiate the publishers", ex);
        }
    }

    /**
     * Expose the publisher for the StreamResource endpoint
     *
     * @return the mapping of publisher classes and instances
     */
    public Map<String, UpdateListener> getPublisherInstances()
    {
        return publisherInstances;
    }

    public Map<String, PublisherConfig> getPublisherConfigs()
    {
        return publisherConfigs;
    }

    public Map<String, StreamConfig> getStreamConfigs()
    {
        return streamConfigs;
    }

    /**
     * Add a new stream to the Esper engine. This will:
     * <ul>
     * <li>Connect the stream to the right publisher</li>
     * <li>Instantiate the publisher</li>
     * <li>Add the stream to the Esper engine</li>
     * </ul>
     *
     * @param streamConfig stream configuration file
     * @throws ClassNotFoundException    if the publisher class cannot be found
     * @throws InstantiationException    if the publisher cannot be instantiated
     * @throws IllegalAccessException    if the publisher cannot be instantiated
     * @throws InvocationTargetException if the publisher cannot be instantiated
     */
    public void addStream(final StreamConfig streamConfig) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException
    {
        // Connect the stream to its publisher(s)
        configurePublishersForStream(streamConfig);

        // Instantiate its publisher(s)
        final LinkedHashMap<String, UpdateListener> publishers = new LinkedHashMap<String, UpdateListener>();
        for (final PublisherConfig route : streamConfig.getPublishers()) {
            final UpdateListener updateListener = instantiateUpdateListener(route);
            publishers.put(route.getType(), updateListener);
            publisherInstances.put(streamConfig.getName(), updateListener);
        }

        // Add the stream in the Esper engine
        for (final String sqlStatement : streamConfig.getSql()) {
            final EPStatement epl = epService.getEPAdministrator().createEPL(sqlStatement);
            for (final String publisherType : publishers.keySet()) {
                log.info(String.format("Added publisher [%-50s] to [%s]", publisherType, sqlStatement));
                epl.addListener(publishers.get(publisherType));
            }
            epl.start();
        }
    }

    /**
     * Connect a given stream to its publisher. Its subscriber(s) is automatically mapped (the eventOutputName maps to the field
     * in the Esper queries).
     *
     * @param streamConfig The stream configuration object
     */
    @VisibleForTesting
    void configurePublishersForStream(final StreamConfig streamConfig)
    {
        final List<PublisherConfig> newRoutes = new ArrayList<PublisherConfig>();

        for (final HashMap<String, Object> overrides : streamConfig.getRoutes()) {
            final String routeName = (String) overrides.get("name");
            final PublisherConfig associatedGlobalPublisherConfig = publisherConfigs.get(routeName);
            if (associatedGlobalPublisherConfig != null) {
                final Map<String, Object> base = mapper.convertValue(associatedGlobalPublisherConfig, new TypeReference<Map<String, Object>>()
                {
                });
                for (final String key : overrides.keySet()) {
                    if (overrides.get(key) != null) {
                        base.put(key, overrides.get(key));
                    }
                }

                newRoutes.add(mapper.convertValue(base, associatedGlobalPublisherConfig.getClass()));
            }
        }

        streamConfig.setPublishers(newRoutes);
        streamConfigs.put(streamConfig.getName(), streamConfig);
    }

    // We need a better way to do that, see http://jira.codehaus.org/browse/JACKSON-453
    @VisibleForTesting
    static UpdateListener instantiateUpdateListener(final PublisherConfig publisherConfig)
        throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException
    {
        final String listenerType = publisherConfig.getType();
        final Class listenerClass = Class.forName(listenerType);
        Constructor<?> defaultConstructor = null;
        Constructor<?> configConstructor = null;

        for (final Constructor<?> constructor : listenerClass.getConstructors()) {
            if (constructor.getParameterTypes() == null || constructor.getParameterTypes().length == 0) {
                defaultConstructor = constructor;
            }
            else if (constructor.getParameterTypes().length == 1) {
                configConstructor = constructor;
            }
        }

        final UpdateListener listener;

        if (configConstructor != null) {
            final Class listenerConfigClass = configConstructor.getParameterTypes()[0];
            listener = (UpdateListener) configConstructor.newInstance(listenerConfigClass.cast(publisherConfig));
        }
        else if (defaultConstructor != null) {
            listener = (UpdateListener) defaultConstructor.newInstance();
        }
        else {
            throw new IllegalArgumentException("Can't find a suitable constructor in subscribers class " + listenerClass.getName());
        }

        return listener;
    }
}
