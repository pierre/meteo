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

package com.ning.metrics.meteo.binder;

import com.ning.metrics.meteo.publishers.PublisherConfig;
import com.ning.metrics.meteo.subscribers.SubscriberConfig;

import java.util.List;

public class StatementsConfig
{
    public List<PublisherConfig> publishers;
    public List<SubscriberConfig> subscribers;
    public List<StreamConfig> streams;

    public List<PublisherConfig> getPublishers()
    {
        return publishers;
    }

    public void setPublishers(List<PublisherConfig> publishers)
    {
        this.publishers = publishers;
    }

    public List<SubscriberConfig> getSubscribers()
    {
        return subscribers;
    }

    public void setSubscribers(List<SubscriberConfig> subscribers)
    {
        this.subscribers = subscribers;
    }

    public List<StreamConfig> getStatements()
    {
        return streams;
    }

    public void setStatementConfigs(List<StreamConfig> statementsConfigs)
    {
        this.streams = statementsConfigs;
    }
}
