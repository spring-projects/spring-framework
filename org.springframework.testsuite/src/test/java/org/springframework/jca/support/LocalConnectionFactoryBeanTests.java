/*
 * Copyright 2002-2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jca.support;

import junit.framework.TestCase;
import org.easymock.MockControl;
import org.springframework.test.AssertThrows;

import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnectionFactory;

/**
 * Unit tests for the {@link LocalConnectionFactoryBean} class.
 *
 * @author Rick Evans
 */
public final class LocalConnectionFactoryBeanTests extends TestCase {

    public void testManagedConnectionFactoryIsRequired() throws Exception {
        new AssertThrows(IllegalArgumentException.class) {
            public void test() throws Exception {
                new LocalConnectionFactoryBean().afterPropertiesSet();
            }
        }.runTest();
    }

    public void testIsSingleton() throws Exception {
        LocalConnectionFactoryBean factory = new LocalConnectionFactoryBean();
        assertTrue(factory.isSingleton());
    }

    public void testGetObjectTypeIsNullIfConnectionFactoryHasNotBeenConfigured() throws Exception {
        LocalConnectionFactoryBean factory = new LocalConnectionFactoryBean();
        assertNull(factory.getObjectType());
    }

    public void testCreatesVanillaConnectionFactoryIfNoConnectionManagerHasBeenConfigured() throws Exception {
        
        final Object CONNECTION_FACTORY = new Object();
        
        MockControl mockManagedConnectionFactory = MockControl.createControl(ManagedConnectionFactory.class);
        ManagedConnectionFactory managedConnectionFactory = (ManagedConnectionFactory) mockManagedConnectionFactory.getMock();

        managedConnectionFactory.createConnectionFactory();
        mockManagedConnectionFactory.setReturnValue(CONNECTION_FACTORY);
        mockManagedConnectionFactory.replay();

        LocalConnectionFactoryBean factory = new LocalConnectionFactoryBean();
        factory.setManagedConnectionFactory(managedConnectionFactory);
        factory.afterPropertiesSet();
        assertEquals(CONNECTION_FACTORY, factory.getObject());

        mockManagedConnectionFactory.verify();
    }

    public void testCreatesManagedConnectionFactoryIfAConnectionManagerHasBeenConfigured() throws Exception {
        MockControl mockManagedConnectionFactory = MockControl.createControl(ManagedConnectionFactory.class);
        ManagedConnectionFactory managedConnectionFactory = (ManagedConnectionFactory) mockManagedConnectionFactory.getMock();

        MockControl mockConnectionManager = MockControl.createControl(ConnectionManager.class);
        ConnectionManager connectionManager = (ConnectionManager) mockConnectionManager.getMock();

        managedConnectionFactory.createConnectionFactory(connectionManager);
        mockManagedConnectionFactory.setReturnValue(null);

        mockConnectionManager.replay();
        mockManagedConnectionFactory.replay();

        LocalConnectionFactoryBean factory = new LocalConnectionFactoryBean();
        factory.setManagedConnectionFactory(managedConnectionFactory);
        factory.setConnectionManager(connectionManager);
        factory.afterPropertiesSet();

        mockManagedConnectionFactory.verify();
        mockConnectionManager.verify();
    }

}
