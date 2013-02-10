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

/**
 * Configuration class for UdpJsonSubscriber.
 * 
 * @author William Speirs <bill.speirs@gmail.com>
 */
class UdpJsonSubscriberConfig extends SubscriberConfig
{
    private int port;
    private int packetSize;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPacketSize() {
        return packetSize == 0 ? 1024 : packetSize;
    }

    public void setPacketSize(int packetSize) {
        this.packetSize = packetSize;
    }

}
