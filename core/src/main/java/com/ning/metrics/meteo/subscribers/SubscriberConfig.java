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

import org.codehaus.jackson.annotate.JsonTypeInfo;

@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS)
public class SubscriberConfig
{
    public boolean enabled = true;

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * Event name in the Esper engine
     */
    public String eventOutputName;

    /**
     * Subscriber class name
     */
    public String type;

    /**
     * Name used in routes
     */
    public String name;

    public String getEventOutputName()
    {
        return eventOutputName;
    }

    public void setEventOutputName(String eventOutputName)
    {
        this.eventOutputName = eventOutputName;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
