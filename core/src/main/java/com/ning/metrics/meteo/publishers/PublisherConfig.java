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

import org.codehaus.jackson.annotate.JsonTypeInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class PublisherConfig
{
    private String name;
    private String type;
    private List<Pattern> filters = new ArrayList<Pattern>();
    private String timeAttribute = "timestamp";

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public List<Pattern> getFilters()
    {
        return Collections.unmodifiableList(filters);
    }

    public void setFilters(List<String> filters)
    {
        this.filters.clear();
        for (String filter : filters) {
            this.filters.add(Pattern.compile(filter));
        }
    }

    public boolean isIncluded(String str)
    {
        if (filters.isEmpty()) {
            return true;
        }
        for (Pattern pattern : filters) {
            if (pattern.matcher(str).matches()) {
                return true;
            }
        }
        return false;
    }

    public String getTimeAttribute()
    {
        return timeAttribute;
    }

    public void setTimeAttribute(String timeAttribute)
    {
        this.timeAttribute = timeAttribute;
    }
}
