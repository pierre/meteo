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

package com.ning.metrics.meteo.esper;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TPAggregatorTest
{
    @Test
    public void test40thPercentile() throws Exception
    {
        TPAggregator tp = new TPAggregator();
        Object[] values = {40, null};

        values[1] = 15;
        tp.enter(values);

        values[1] = 20;
        tp.enter(values);

        values[1] = 35;
        tp.enter(values);

        values[1] = 40;
        tp.enter(values);

        values[1] = 50;
        tp.enter(values);

        Assert.assertEquals(tp.getValue(), 29.0);
    }

    @Test
    public void test90thPercentile() throws Exception
    {
        TPAggregator tp = new TPAggregator();
        Object[] values = {90, null};

        for (int i = 0; i <= 100; i++) {
            values[1] = i;
            tp.enter(values);
        }

        Assert.assertEquals(tp.getValue(), 90.0);
    }
}
