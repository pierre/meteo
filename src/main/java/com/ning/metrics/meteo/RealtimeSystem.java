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

package com.ning.metrics.meteo;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.ning.metrics.meteo.binder.RealtimeSystemModule;
import com.ning.metrics.meteo.subscribers.SubscribersCompiler;

import java.io.IOException;

public class RealtimeSystem
{
    public static void main(String[] args) throws IOException
    {
        Injector injector = Guice.createInjector(new RealtimeSystemModule());

        final SubscribersCompiler subscribersCompiler = injector.getInstance(SubscribersCompiler.class);

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                subscribersCompiler.stopAll();
            }
        });

        subscribersCompiler.startAll();
    }
}
