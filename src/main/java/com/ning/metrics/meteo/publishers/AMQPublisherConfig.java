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

package com.ning.metrics.meteo.publishers;

class AMQPublisherConfig extends PublisherConfig
{
    private String uri;
    private String prefix;
    private Integer messagesTTLMilliseconds;
    private Boolean useAsyncSend;
    private Boolean useBytesMessage;

    public String getUri()
    {
        return uri;
    }

    public void setUri(final String uri)
    {
        this.uri = uri;
    }

    public String getPrefix()
    {
        return prefix;
    }

    public void setPrefix(final String prefix)
    {
        this.prefix = prefix;
    }

    public long getMessagesTTLMilliseconds()
    {
        return messagesTTLMilliseconds;
    }

    public void setMessagesTTLMilliseconds(final Integer messagesTTLMilliseconds)
    {
        this.messagesTTLMilliseconds = messagesTTLMilliseconds;
    }

    public Boolean getUseAsyncSend()
    {
        return useAsyncSend;
    }

    public void setUseAsyncSend(final Boolean useAsyncSend)
    {
        this.useAsyncSend = useAsyncSend;
    }

    public Boolean getUseBytesMessage()
    {
        return useBytesMessage;
    }

    public void setUseBytesMessage(final Boolean useBytesMessage)
    {
        this.useBytesMessage = useBytesMessage;
    }
}
