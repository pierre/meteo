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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.event.map.MapEventType;

public class EsperListener
{
    /**
     * Given an Event from the Esper core engine, get its name.
     * This return null for UUID-based names (select statements), the specified name otherwise (named streams, e.g. insert)
     *
     * @param newEvent Event to extract the name from
     * @return event name if human-readable, null otherwise
     */
    static String getEventName(EventBean newEvent)
    {
        String typeName = newEvent.getEventType().getName();

        // For automatically generated event names (uuid, most likely from a select statement), the public name
        // is null.
        if (newEvent.getEventType() instanceof MapEventType) {
            typeName = ((MapEventType) newEvent.getEventType()).getMetadata().getPublicName();
        }

        return typeName;
    }


    /**
     * Given an Event form the Esper core engine, get its timestamp
     *
     * @param newEvent            Event to extract its timestamp from
     * @param configTimeAttribute time attribute in the publisher configuration, can be null
     * @return the event timestamp
     */
    public static Long getEventMillis(EventBean newEvent, String configTimeAttribute)
    {
        Long timeInMs = null;

        if (configTimeAttribute != null) {
            EventPropertyDescriptor desc = newEvent.getEventType().getPropertyDescriptor(configTimeAttribute);

            if (desc != null) {
                Object value = newEvent.get(configTimeAttribute);

                if (value != null) {
                    timeInMs = Long.parseLong(value.toString());
                }
            }
        }

        if (timeInMs == null) {
            timeInMs = System.currentTimeMillis();
        }

        return timeInMs;
    }
}
