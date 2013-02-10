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

import com.ning.metrics.meteo.binder.StatementsConfig;
import com.ning.metrics.meteo.binder.StreamConfig;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestPublishersCompilerStreamConfig
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
        //                         "timeAttribute": "visit_date"
        //                     }
        //                 ]
        //             }
        //         ]
        //     }

        statementsConfig = new StatementsConfig();

        final DummyPublisherConfig globalPublisherConfig = new DummyPublisherConfig();
        globalPublisherConfig.setName("OpenTSDB");
        globalPublisherConfig.setType("com.ning.metrics.meteo.publishers.OpenTSDBListener");
        globalPublisherConfig.setHost("opentsdb.company.com");
        globalPublisherConfig.setPort(4242);
        statementsConfig.setPublishers(Arrays.asList((PublisherConfig) globalPublisherConfig));

        final StreamConfig streamConfig = new StreamConfig();
        streamConfig.setName("TPs of Visit");
        streamConfig.setSql(Arrays.asList("select tp90 from visit output last every 1 second"));
        final HashMap<String, Object> localPublisherConfig = new HashMap<String, Object>();
        localPublisherConfig.put("name", "OpenTSDB");
        localPublisherConfig.put("filters", (Arrays.asList("predict", "tp90")));
        localPublisherConfig.put("timeAttribute", "visit_date");
        streamConfig.setRoutes(Arrays.<HashMap<String, Object>>asList(localPublisherConfig));
        statementsConfig.setStatementConfigs(Arrays.asList(streamConfig));
    }

    @Test(groups = "fast")
    public void testInstantiateListener() throws Exception
    {
        final PublishersCompiler compiler = new PublishersCompiler(statementsConfig.getPublishers(), new ArrayList<StreamConfig>(), null);
        final StreamConfig streamConfig = statementsConfig.getStatements().get(0);
        compiler.configurePublishersForStream(streamConfig);

        final String streamName = streamConfig.getName();
        final PublisherConfig publisherConfig = compiler.getStreamConfigs().get(streamName).getPublishers().get(0);
        assertTrue(publisherConfig instanceof DummyPublisherConfig);

        final DummyPublisherConfig config = (DummyPublisherConfig) publisherConfig;
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
