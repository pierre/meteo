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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import org.apache.commons.mail.HtmlEmail;
import org.apache.log4j.Logger;

import javax.mail.internet.InternetAddress;
import java.util.Arrays;

class AlertListener extends EsperListener implements UpdateListener
{
    private final static Logger log = Logger.getLogger(AlertListener.class);
    private final AlertPublisherConfig config;

    public AlertListener(AlertPublisherConfig config)
    {
        this.config = config;
    }

    @Override
    public void update(EventBean[] newEvents, EventBean[] oldEvents)
    {
        // TODO register esper callback which sends email -> how to specify what to look for in the event ?

        if (newEvents != null) {
            for (EventBean newEvent : newEvents) {
                Double result1 = (Double) newEvent.get("result1");

                if (result1 != null) {
                    createAndSendAlertEmail(String.format("Boo ! Result " + result1));
                }
            }
        }
    }

    private void createAndSendAlertEmail(String body)
    {
        try {
            log.info(String.format("Sending alert email to [%s]: %s", config.getRecipients(), body));

            HtmlEmail email = new HtmlEmail();

            email.setTextMsg(body);
            email.setFrom("esper-is-awesome@example.com");
            email.setTo(Arrays.asList(new InternetAddress(config.getRecipients())));
            email.setHostName(config.getHost());
            email.setSmtpPort(config.getPort());
            email.send();
        }
        catch (Exception ex) {
            log.warn("Could not create or send email", ex);
        }
    }
}
