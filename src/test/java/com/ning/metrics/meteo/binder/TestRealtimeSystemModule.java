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

package com.ning.metrics.meteo.binder;

import com.ning.metrics.meteo.publishers.DummyPublisherConfig;
import com.ning.metrics.meteo.publishers.PublisherConfig;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestRealtimeSystemModule
{
    private StatementsConfig statementsConfig;

    @BeforeTest
    public void setUp()
    {
        // Build the following configuration
        //     {
        //         "publishers": [
        //             {
        //                 "name": "OpenTSDB",
        //                 "type": "com.ning.metrics.meteo.publishers.OpenTSDBListener",
        //                 "host": "opentsdb.company.com",
        //                 "port": 4242
        //             }
        //         ],
        //
        //         "streams": [
        //             {
        //                 "name": "TPs of visit",
        //
        //                 "sql": [
        //                     "select tp90 from visit output last every 1 second"
        //                 ],
        //
        //                 "routes": [
        //                     {
        //                         "name": "OpenTSDB",
        //                         "filters": [ "predict", "tp90" ],
        //                         "timeAttr": "visit_date"
        //                     }
        //                 ]
        //             }
        //         ]
        //     }

        statementsConfig = new StatementsConfig();

        DummyPublisherConfig globalPublisherConfig = new DummyPublisherConfig();
        globalPublisherConfig.setName("OpenTSDB");
        globalPublisherConfig.setType("com.ning.metrics.meteo.publishers.OpenTSDBListener");
        globalPublisherConfig.setHost("opentsdb.company.com");
        globalPublisherConfig.setPort(4242);
        statementsConfig.setPublishers(Arrays.asList((PublisherConfig) globalPublisherConfig));

        StreamConfig streamConfig = new StreamConfig();
        streamConfig.setName("TPs of Visit");
        streamConfig.setSql(Arrays.asList("select tp90 from visit output last every 1 second"));
        HashMap<String, Object> localPublisherConfig = new HashMap<String, Object>();
        localPublisherConfig.put("name", "OpenTSDB");
        localPublisherConfig.put("filters", (Arrays.asList("predict", "tp90")));
        localPublisherConfig.put("timeAttribute", "visit_date");
        streamConfig.setRoutes(Arrays.asList(localPublisherConfig));
        statementsConfig.setStatementConfigs(Arrays.asList(streamConfig));
    }

    @Test
    public void testInstantiateListener() throws Exception
    {
        List<StreamConfig> statementsConfigs = RealtimeSystemModule.mergeRoutesAndGlobalPublishers(statementsConfig);
        StreamConfig streamConfig = statementsConfigs.get(0);
        PublisherConfig publisherConfig = streamConfig.getPublishers().get(0);
        assertTrue(publisherConfig instanceof DummyPublisherConfig);

        DummyPublisherConfig config = (DummyPublisherConfig) publisherConfig;
        assertEquals(config.getName(), "OpenTSDB");
        assertEquals(config.getType(), "com.ning.metrics.meteo.publishers.OpenTSDBListener");
        assertEquals(config.getHost(), "opentsdb.company.com");
        assertEquals((int) config.getPort(), 4242);
        assertEquals(config.getType(), "com.ning.metrics.meteo.publishers.OpenTSDBListener");
        assertEquals(config.getFilters().get(0).toString(), "predict");
        assertEquals(config.getFilters().get(1).toString(), "tp90");
        assertEquals(config.getTimeAttribute(), "visit_date");
    }
}
