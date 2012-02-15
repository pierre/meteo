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

import org.skife.config.Config;
import org.skife.config.Default;

public interface RealtimeSystemConfig
{
    @Config("rt.server.ip")
    @Default("127.0.0.1")
    String getLocalIp();

    @Config("rt.server.port")
    @Default("8080")
    int getLocalPort();

    @Config("rt.jetty.stats")
    @Default("true")
    boolean isJettyStatsOn();

    /**
     * @return main configuration file
     */
    @Config(value = "rt.configFile")
    @Default("rt_conf.json")
    String getConfigurationFile();

    /**
     * @return configuration file for the Esper engine
     */
    @Config(value = "rt.esper.configFile")
    @Default("esper_conf.xml")
    String getEsperConfigurationFile();
}
