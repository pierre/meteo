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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import org.apache.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

class FileListener extends EsperListener implements UpdateListener
{
    private final static Logger log = Logger.getLogger(FileListener.class);

    private final FilePublisherConfig config;

    private PrintWriter writer = null;

    public FileListener(FilePublisherConfig config)
    {
        this.config = config;
        try {
            if (config.getPath() == null) {
                throw new IOException("File path not specified!");
            }

            writer = new PrintWriter(new FileWriter(config.getPath()));
        }
        catch (IOException e) {
            log.warn(String.format("Unable to write to %s, file publisher disabled", config.getPath()));
        }
    }

    @Override
    public void update(EventBean[] newEvents, EventBean[] oldEvents)
    {
        if (newEvents != null && writer != null) {
            StringBuilder builder = new StringBuilder();
            for (EventBean newEvent : newEvents) {
                String typeName = getEventName(newEvent);

                builder.setLength(0);
                for (String attribute : newEvent.getEventType().getPropertyNames()) {
                    String fullName = (typeName == null ? attribute : typeName + "." + attribute);

                    if (config.isIncluded(fullName)) {
                        Object value = newEvent.get(attribute);

                        builder.append(getEventMillis(newEvent, config.getTimeAttribute()));
                        builder.append(config.getSeparator());
                        builder.append(fullName);
                        builder.append(config.getSeparator());
                        if (value == null) {
                            builder.append("null");
                        }
                        else if (value instanceof String) {
                            builder.append('\"');
                            builder.append(value);
                            builder.append('\"');
                        }
                        else {
                            builder.append(value);
                        }
                        builder.append("\n");
                    }
                }

                if (builder.length() > 0) {
                    writer.print(builder.toString());
                    writer.flush();
                }
            }
        }
    }
}
