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

import com.espertech.esper.client.UpdateListener;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class TestPublishersCompiler
{
    private DebugPublisherConfig publisherConfig;

    @BeforeTest
    public void setUp()
    {
        publisherConfig = new DebugPublisherConfig();
        publisherConfig.setName("Debug");
        publisherConfig.setType("com.ning.metrics.meteo.publishers.DebugListener");
    }

    @Test(groups = "fast")
    public void testInstantiateListener() throws Exception
    {
        final UpdateListener listener = PublishersCompiler.instantiateUpdateListener(publisherConfig);
        assertTrue(listener instanceof DebugListener);
    }
}
