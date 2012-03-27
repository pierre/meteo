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

package com.ning.metrics.meteo.server;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Takes java.util.logging and redirects it into log4j.
 */
public class SetupJULBridge implements ServletContextListener
{
    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SetupJULBridge.class);

    @Override
    public void contextInitialized(final ServletContextEvent event)
    {
        // we first remove the default handler(s)
        final Logger rootLogger = LogManager.getLogManager().getLogger("");
        final Handler[] handlers = rootLogger.getHandlers();

        if (!ArrayUtils.isEmpty(handlers)) {
            for (final Handler handler : handlers) {
                rootLogger.removeHandler(handler);
            }
        }
        // and then we let jul-to-sfl4j do its magic so that jersey messages go to sfl4j (and thus log4j)
        SLF4JBridgeHandler.install();

        log.info("Assimilated java.util Logging");
    }

    @Override
    public void contextDestroyed(final ServletContextEvent event)
    {
        SLF4JBridgeHandler.uninstall();
    }
}