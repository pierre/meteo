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

package com.ning.metrics.meteo.subscribers;

import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderIsolated;
import com.espertech.esper.client.EPServiceStateListener;
import com.espertech.esper.client.EPStatementStateListener;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.naming.Context;

import static org.testng.Assert.assertTrue;

public class TestSubscribersCompiler
{
    private JMXSubscriberConfig subscriberConfig;

    class MockEPServiceProvider implements EPServiceProvider
    {

        /**
         * Returns a class instance of EPRuntime.
         *
         * @return an instance of EPRuntime
         */
        @Override
        public EPRuntime getEPRuntime()
        {
            return null;
        }

        /**
         * Returns a class instance of EPAdministrator.
         *
         * @return an instance of EPAdministrator
         */
        @Override
        public EPAdministrator getEPAdministrator()
        {
            return null;
        }

        /**
         * Provides naming context for public named objects.
         * <p/>
         * An extension point designed for use by input and output adapters as well as
         * other extension services.
         *
         * @return naming context providing name-to-object bindings
         */
        @Override
        public Context getContext()
        {
            return null;
        }

        /**
         * Frees any resources associated with this engine instance, and leaves the engine instance
         * ready for further use.
         * <p/>
         * Retains the existing configuration of the engine instance but forgets any runtime configuration changes.
         * <p/>
         * Stops and destroys any existing statement resources such as filters, patterns, expressions, views.
         */
        @Override
        public void initialize()
        {
        }

        /**
         * Returns the provider URI, or "default" if this is the default provider.
         *
         * @return provider URI
         */
        @Override
        public String getURI()
        {
            return null;
        }

        /**
         * Destroys the service.
         * <p/>
         * Releases any resources held by the service. The service enteres a state in
         * which operations provided by administrative and runtime interfaces originiated by the service
         * are not guaranteed to operate properly.
         * <p/>
         * Removes the service URI from the known URIs. Allows configuration to change for the instance.
         */
        @Override
        public void destroy()
        {
        }

        /**
         * Returns true if the service is in destroyed state, or false if not.
         *
         * @return indicator whether the service has been destroyed
         */
        @Override
        public boolean isDestroyed()
        {
            return false;
        }

        /**
         * Add a listener to service provider state changes that receives a before-destroy event.
         * The listener collection applies set-semantics.
         *
         * @param listener to add
         */
        @Override
        public void addServiceStateListener(EPServiceStateListener listener)
        {
        }

        /**
         * Removate a listener to service provider state changes.
         *
         * @param listener to remove
         * @return true to indicate the listener was removed, or fals
         */
        @Override
        public boolean removeServiceStateListener(EPServiceStateListener listener)
        {
            return false;
        }

        /**
         * Remove all listeners to service provider state changes.
         */
        @Override
        public void removeAllServiceStateListeners()
        {
        }

        /**
         * Add a listener to statement state changes that receives statement-level events.
         * The listener collection applies set-semantics.
         *
         * @param listener to add
         */
        @Override
        public void addStatementStateListener(EPStatementStateListener listener)
        {
        }

        /**
         * Removate a listener to statement state changes.
         *
         * @param listener to remove
         * @return true to indicate the listener was removed, or fals
         */
        @Override
        public boolean removeStatementStateListener(EPStatementStateListener listener)
        {
            return false;
        }

        /**
         * Remove all listeners to statement state changes.
         */
        @Override
        public void removeAllStatementStateListeners()
        {
        }

        /**
         * Returns the isolated service provider for that name,
         * creating an isolated service if the name is a new name, or
         * returning an existing isolated service for an existing name.
         *
         * @param name to return isolated service for
         * @return isolated service
         */
        @Override
        public EPServiceProviderIsolated getEPServiceIsolated(String name)
        {
            return null;
        }

        /**
         * Returns the names of isolated service providers currently allocated.
         *
         * @return isolated service provider names
         */
        @Override
        public String[] getEPServiceIsolatedNames()
        {
            return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    @BeforeTest
    public void setUp()
    {
        subscriberConfig = new JMXSubscriberConfig();
        subscriberConfig.setName("JMX");
        subscriberConfig.setType("com.ning.metrics.meteo.subscribers.JMXSubscriber");
        subscriberConfig.setHost("127.0.0.1");
        subscriberConfig.setQuery("memory.*");
        subscriberConfig.setAttributes("memory,cpu");
        subscriberConfig.setEventOutputName("mem");
    }

    @Test
    public void testInstantiateSubscriber() throws Exception
    {
        Subscriber subscriber = SubscribersCompiler.instantiateSubscriber(new MockEPServiceProvider(), subscriberConfig);
        assertTrue(subscriber instanceof JMXSubscriber);
    }
}
