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

package com.ning.metrics.meteo.subscribers;

class JMXSubscriberConfig extends SubscriberConfig
{
    private String host;
    private int port = 8989;
    private String query;
    public String[] attributes;
    private long pollIntervalInMillis = 1000;

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public String getQuery()
    {
        return query;
    }

    public void setQuery(String query)
    {
        this.query = query;
    }

    public String[] getAttributes()
    {
        return attributes;
    }

    public void setAttributes(String attributeList)
    {
        this.attributes = (attributeList == null ? new String[0] : attributeList.split("\\s*,\\s*"));
    }

    public long getPollIntervalInMillis()
    {
        return pollIntervalInMillis;
    }

    public void setPollIntervalInMillis(long pollIntervalInMillis)
    {
        this.pollIntervalInMillis = pollIntervalInMillis;
    }
}
