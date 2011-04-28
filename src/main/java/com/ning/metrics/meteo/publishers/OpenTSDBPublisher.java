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

package com.ning.metrics.meteo.publishers;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

class OpenTSDBPublisher
{
    private static final Logger log = Logger.getLogger(OpenTSDBPublisher.class);

    private OpenTSDBPublisherConfig config;
    private Socket socket;
    private PrintStream ps;

    public OpenTSDBPublisher(OpenTSDBPublisherConfig config)
    {
        this.config = config;
    }

    public void send(String metric, long timeInMS, Object value)
    {
        try {
            String m = String.format("put %s %d %s host=unknown", metric, timeInMS / 1000, value);
            ps.println(m);

            log.debug(String.format("Sent to OpenTSDB: %s", m));
        }
        catch (Exception e) {
            log.warn("Dropping event - unable to send data to OpenTSDB", e);
            failSafeConnect();
        }
    }

    public void connect() throws IOException
    {
        socket = new Socket(config.getHost(), config.getPort());
        ps = new PrintStream(socket.getOutputStream());
    }

    public void disconnect() throws IOException
    {
        if (ps != null) {
            ps.close();
        }
        if (socket != null) {
            socket.close();
        }
    }

    public void failSafeConnect()
    {
        failSafeConnect(1000, 10000);
    }

    public void failSafeConnect(final int initialBackoffTime, final int maxBackoffTime)
    {
        int backoffTime = initialBackoffTime;

        // Disconnect if we have a broken connection
        try {
            disconnect();
        }
        catch (IOException e) {
            log.debug("Exception trying to close an already broken connection", e);
        }

        // Try to open
        try {
            log.info("Attempting to connect to OpenTSDB");
            connect();
            return;
        }
        catch (IOException e) {
            log.warn("Unable to connect to OpenTSDB. Will retry in " + backoffTime + " ms", e);
        }

        // Failed :( Let's try again
        try {
            disconnect();
        }
        catch (IOException e) {
            log.debug("Exception trying to close an already broken connection", e);
        }

        try {
            Thread.sleep(backoffTime);
        }
        catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }

        backoffTime = backoffTime * 2;
        if (backoffTime > maxBackoffTime) {
            backoffTime = maxBackoffTime;
        }

        failSafeConnect(backoffTime, maxBackoffTime);
    }
}
