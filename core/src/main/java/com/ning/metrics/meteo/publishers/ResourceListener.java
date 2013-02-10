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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ResourceListener implements UpdateListener
{
    private final ResourcePublisherConfig config;
    private final Map<String, Cache<Object, Object>> samplesCache = Maps.newConcurrentMap();
    private final Object mapLock = new Object();

    public ResourceListener(ResourcePublisherConfig config)
    {
        this.config = config;
    }

    @Override
    public void update(EventBean[] newEvents, EventBean[] oldEvents)
    {
        if (newEvents != null) {
            for (EventBean newEvent : newEvents) {
                for (String attribute : newEvent.getEventType().getPropertyNames()) {
                    add(attribute, new DateTime(DateTimeZone.UTC), newEvent.get(attribute));
                }
            }
        }
    }

    public Map<String, Cache<Object, Object>> getSamplesCache()
    {
        return samplesCache;
    }

    private void add(String attribute, DateTime dateTime, Object sample)
    {
        Cache<Object, Object> samplesForType = samplesCache.get(attribute);

        // Build the samples cache for this type if it doesn't exist
        if (samplesForType == null) {
            synchronized (mapLock) {
                samplesForType = samplesCache.get(attribute);
                if (samplesForType == null) {
                    samplesForType = CacheBuilder.newBuilder()
                        .maximumSize(config.getCacheMaxSize())
                        .expireAfterWrite(config.getCacheExpirySeconds(), TimeUnit.SECONDS)
                        .build();
                    samplesCache.put(attribute, samplesForType);
                }
            }
        }

        samplesForType.put(dateTime, sample);
    }
}
