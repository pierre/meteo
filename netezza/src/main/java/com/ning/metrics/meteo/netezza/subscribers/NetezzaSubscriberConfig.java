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

package com.ning.metrics.meteo.netezza.subscribers;

import com.ning.metrics.meteo.subscribers.SubscriberConfig;

public class NetezzaSubscriberConfig extends SubscriberConfig
{
    public String sqlQuery;
    public String host;
    public int port = 5480;
    public String username;
    public String password;
    public String database;

    public String getHost()
    {
        return host;
    }

    public void setHost(final String host)
    {
        this.host = host;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(final String password)
    {
        this.password = password;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(final int port)
    {
        this.port = port;
    }

    public String getSqlQuery()
    {
        return sqlQuery;
    }

    public void setSqlQuery(final String sqlQuery)
    {
        this.sqlQuery = sqlQuery;
    }

    public String getDatabase()
    {
        return database;
    }

    public void setDatabase(final String database)
    {
        this.database = database;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(final String username)
    {
        this.username = username;
    }
}
