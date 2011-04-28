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

package com.ning.metrics.meteo.subscribers;

import com.espertech.esper.client.EPServiceProvider;
import com.google.inject.Inject;
import org.apache.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class SubscribersCompiler
{
    private final ArrayList<Subscriber> subscribers = new ArrayList<Subscriber>();
    private static final Logger log = Logger.getLogger(SubscribersCompiler.class);

    @Inject
    public SubscribersCompiler(List<SubscriberConfig> subscribersConfigs, EPServiceProvider epService)
        throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException
    {
        for (SubscriberConfig subscriberConfig : subscribersConfigs) {
            final Subscriber subscriber = instantiateSubscriber(epService, subscriberConfig);
            if (subscriber != null) {
                subscribers.add(subscriber);
            }
        }
    }

    static Subscriber instantiateSubscriber(EPServiceProvider epService, SubscriberConfig subscriberConfig)
        throws InstantiationException, IllegalAccessException, InvocationTargetException
    {
        // The Esper runtime engine requires event names
        if (subscriberConfig.getEventOutputName() == null) {
            throw new IllegalStateException("Esper topic key not specified!");
        }

        Class subscriberKlass;

        try {
            // Find the subscriber class
            String subscriberKlassName = subscriberConfig.getType();
            subscriberKlass = Class.forName(subscriberKlassName);
        }
        catch (ClassNotFoundException e) {
            throw new IllegalStateException("Class not found for: " + subscriberConfig.getType());
        }

        // Find the subscriber constructor
        // We expect subscriber to have a constructor with 2 arguments (SubscriberConfig, EPServiceProvider)
        Constructor<?> constructor = null;
        Class subscriberConfigKlass = null;
        for (Constructor<?> cstr : subscriberKlass.getConstructors()) {
            if (cstr.getParameterTypes().length == 2) {
                constructor = cstr;
                subscriberConfigKlass = cstr.getParameterTypes()[0];
            }
        }

        if (constructor != null && subscriberConfigKlass != null) {
            // Make sure to downcast configuration objects
            if (subscriberConfig.isEnabled()) {
                return (Subscriber) constructor.newInstance(subscriberConfigKlass.cast(subscriberConfig), epService);
            }
            else {
                log.info("Skipping disabled subscriber: " + subscriberConfig.getName());
                return null;
            }
        }
        else {
            throw new IllegalArgumentException("Can't find a suitable constructor in subscribers class " + subscriberKlass);
        }
    }

    // Unit test hook

    public ArrayList<Subscriber> getSubscribers()
    {
        return subscribers;
    }

    /**
     * Make all created subscribers start pushing data into the Esper runtime engine
     */
    public void startAll()
    {
        try {
            for (Subscriber subscriber : subscribers) {
                subscriber.subscribe();
            }
        }
        catch (Exception ex) {
            log.error("Unable to start subscriber", ex);
        }
    }

    /**
     * Stop all subscribers
     */
    public void stopAll()
    {
        try {
            for (Subscriber subscriber : subscribers) {
                subscriber.unsubscribe();
            }
        }
        catch (Exception ignored) {
        }
    }
}
