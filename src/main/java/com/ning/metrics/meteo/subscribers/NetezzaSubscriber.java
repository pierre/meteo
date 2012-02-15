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

import com.espertech.esper.client.EPServiceProvider;
import com.google.inject.Inject;
import com.google.inject.internal.ImmutableList;
import org.apache.log4j.Logger;
import org.netezza.datasource.NzDatasource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;

class NetezzaSubscriber implements Subscriber
{
    private final Logger log = Logger.getLogger(NetezzaSubscriber.class);

    private final EPServiceProvider esperSink;
    private final NetezzaSubscriberConfig subscriberConfig;

    private final static LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();

    @Inject
    public NetezzaSubscriber(NetezzaSubscriberConfig subscriberConfig, EPServiceProvider esperSink)
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
        NzDatasource dataSource = new NzDatasource();

        dataSource.setHost(subscriberConfig.getHost());
        dataSource.setDatabase(subscriberConfig.getDatabase());
        dataSource.setUser(subscriberConfig.getUsername());
        dataSource.setPassword(subscriberConfig.getPassword());

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement();

            resultSet = statement.executeQuery(subscriberConfig.getSqlQuery());
            ImmutableList.Builder<LinkedHashMap<String, Object>> builder = new ImmutableList.Builder<LinkedHashMap<String, Object>>();


            while (resultSet.next()) {
                map.clear();

                for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                    map.put(resultSet.getMetaData().getColumnName(i), resultSet.getObject(i));
                }

                builder.add(map);
            }

            return builder.build();
        }
        catch (Throwable e) {
            log.error("Error retrieving query", e);
        }
        finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
            }
            catch (Exception e) {
                log.error("Error closing resultSet.", e);
            }
            try {
                if (statement != null) {
                    statement.close();
                }
            }
            catch (Exception e) {
                log.error("Error closing statement.", e);
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            }
            catch (Exception e) {
                log.error("Error closing connection.", e);
            }
        }

        return null;
    }
}
