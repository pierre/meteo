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

class AMQSubscriberConfig extends SubscriberConfig
{
    public String protocol;
    public String host;
    public int port;
    public String username;
    public String password;
    public String topic;
    public int initialBackoffTime = 1000;
    public int maxBackoffTime = 30 * 1000;

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public String getProtocol()
    {
        return protocol;
    }

    public void setProtocol(String protocol)
    {
        this.protocol = protocol;
    }

    public String getTopic()
    {
        return topic;
    }

    public void setTopic(String topic)
    {
        this.topic = topic;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public int getInitialBackoffTime()
    {
        return initialBackoffTime;
    }

    public void setInitialBackoffTime(int initialBackoffTime)
    {
        this.initialBackoffTime = initialBackoffTime;
    }

    public int getMaxBackoffTime()
    {
        return maxBackoffTime;
    }

    public void setMaxBackoffTime(int maxBackoffTime)
    {
        this.maxBackoffTime = maxBackoffTime;
    }
}
