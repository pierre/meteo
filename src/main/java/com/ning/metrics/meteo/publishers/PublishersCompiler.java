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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.UpdateListener;
import com.google.inject.Inject;
import com.ning.metrics.meteo.binder.StreamConfig;
import org.apache.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;

public class PublishersCompiler
{
    private static final Logger log = Logger.getLogger(PublishersCompiler.class);

    @Inject
    public PublishersCompiler(List<StreamConfig> streams, EPServiceProvider epService)
    {
        try {
            for (StreamConfig stream : streams) {
                LinkedHashMap<String, UpdateListener> publishers = new LinkedHashMap<String, UpdateListener>();
                for (PublisherConfig route : stream.getPublishers()) {
                    publishers.put(route.getType(), instantiateUpdateListener(route));
                }

                for (String sqlStatement : stream.getSql()) {
                    EPStatement epl = epService.getEPAdministrator().createEPL(sqlStatement);
                    for (String publisherType : publishers.keySet()) {
                        log.info(String.format("Added publisher [%-50s] to [%s]", publisherType, sqlStatement));
                        epl.addListener(publishers.get(publisherType));
                    }

                }
            }
        }
        catch (Exception ex) {
            log.error("Could not instantiate the publishers", ex);
        }
    }

    // We need a better way to do that, see http://jira.codehaus.org/browse/JACKSON-453
    static UpdateListener instantiateUpdateListener(PublisherConfig publisherConfig)
        throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException
    {
        String listenerType = publisherConfig.getType();
        Class listenerClass = Class.forName(listenerType);
        Constructor<?> defaultConstructor = null;
        Constructor<?> configConstructor = null;

        for (Constructor<?> constructor : listenerClass.getConstructors()) {
            if (constructor.getParameterTypes() == null || constructor.getParameterTypes().length == 0) {
                defaultConstructor = constructor;
            }
            else if (constructor.getParameterTypes().length == 1) {
                configConstructor = constructor;
            }
        }

        UpdateListener listener;

        if (configConstructor != null) {
            Class listenerConfigClass = configConstructor.getParameterTypes()[0];
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
