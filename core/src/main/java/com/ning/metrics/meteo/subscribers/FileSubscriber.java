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

package com.ning.metrics.meteo.subscribers;

import com.espertech.esper.client.EPServiceProvider;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

class FileSubscriber implements Subscriber
{
    private final Logger log = Logger.getLogger(FileSubscriber.class);

    private final EPServiceProvider esperSink;
    private final FileSubscriberConfig subscriberConfig;

    private final static LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();

    @Inject
    public FileSubscriber(FileSubscriberConfig subscriberConfig, EPServiceProvider esperSink)
    {
        this.subscriberConfig = subscriberConfig;
        this.esperSink = esperSink;
    }

    @Override
    public void subscribe()
    {
        List<LinkedHashMap<String, Object>> dataPoints = getDataPoints();
        log.info(String.format("Found %d data points", dataPoints.size()));
        for (LinkedHashMap<String, Object> s : dataPoints) {
            try {
                log.debug("Received a message, yay!\n" + s);
                esperSink.getEPRuntime().sendEvent(s, subscriberConfig.getEventOutputName());
            }
            catch (ClassCastException ex) {
                log.info("Received message that I couldn't parse: " + s, ex);
            }
        }
    }

    @Override
    public void unsubscribe()
    {
        // Do nothing
    }

    private ImmutableList<LinkedHashMap<String, Object>> getDataPoints()
    {
        ImmutableList.Builder<LinkedHashMap<String, Object>> builder = new ImmutableList.Builder<LinkedHashMap<String, Object>>();

        try {
            for (String line : (List<String>) IOUtils.readLines(new FileReader(subscriberConfig.getFilePath()))) {
                if (line.trim().length() > 0) {
                    map.clear();
                    String[] items = line.split(subscriberConfig.getSeparator());
                    long dateTime = new DateTime(items[0], DateTimeZone.forID("UTC")).getMillis();
                    map.put("timestamp", dateTime);
                    for (int j = 1; j < items.length; j++) {
                        double value = Double.valueOf(items[j]);
                        map.put(subscriberConfig.getAttributes()[j-1], value);
                    }
                    builder.add(new LinkedHashMap(map));
                }
            }

            return builder.build();
        }
        catch (IOException e) {
            log.error("Unable to read file: " + subscriberConfig.getFilePath());
            return null;
        }
    }
}
