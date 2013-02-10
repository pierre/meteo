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

package com.ning.metrics.meteo.binder;

import com.ning.metrics.meteo.publishers.PublisherConfig;
import org.codehaus.jackson.annotate.JsonIgnore;

import java.util.HashMap;
import java.util.List;

public class StreamConfig
{
    public String name;
    public List<String> sql;
    public List<HashMap<String, Object>> routes;
    @JsonIgnore
    private List<PublisherConfig> publishers;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public List<String> getSql()
    {
        return sql;
    }

    public void setSql(List<String> sql)
    {
        this.sql = sql;
    }

    public List<HashMap<String, Object>> getRoutes()
    {
        return routes;
    }

    public void setRoutes(List<HashMap<String, Object>> routes)
    {
        this.routes = routes;
    }

    // Merge of global publishers and local overrides from routes
    public void setPublishers(List<PublisherConfig> newRoutes)
    {
        this.publishers = newRoutes;
    }

    public List<PublisherConfig> getPublishers()
    {
        return publishers;
    }
}
