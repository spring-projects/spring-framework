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

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnectionFactory;

import org.junit.Test;

/**
 * Unit tests for the {@link LocalConnectionFactoryBean} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public final class LocalConnectionFactoryBeanTests {

	@Test(expected=IllegalArgumentException.class)
	public void testManagedConnectionFactoryIsRequired() throws Exception {
		new LocalConnectionFactoryBean().afterPropertiesSet();
	}

	@Test
	public void testIsSingleton() throws Exception {
		LocalConnectionFactoryBean factory = new LocalConnectionFactoryBean();
		assertTrue(factory.isSingleton());
	}

	@Test
	public void testGetObjectTypeIsNullIfConnectionFactoryHasNotBeenConfigured() throws Exception {
		LocalConnectionFactoryBean factory = new LocalConnectionFactoryBean();
		assertNull(factory.getObjectType());
	}

	@Test
	public void testCreatesVanillaConnectionFactoryIfNoConnectionManagerHasBeenConfigured() throws Exception {

		final Object CONNECTION_FACTORY = new Object();

		ManagedConnectionFactory managedConnectionFactory = createMock(ManagedConnectionFactory.class);

		expect(managedConnectionFactory.createConnectionFactory()).andReturn(CONNECTION_FACTORY);
		replay(managedConnectionFactory);

		LocalConnectionFactoryBean factory = new LocalConnectionFactoryBean();
		factory.setManagedConnectionFactory(managedConnectionFactory);
		factory.afterPropertiesSet();
		assertEquals(CONNECTION_FACTORY, factory.getObject());

		verify(managedConnectionFactory);
	}

	@Test
	public void testCreatesManagedConnectionFactoryIfAConnectionManagerHasBeenConfigured() throws Exception {
		ManagedConnectionFactory managedConnectionFactory = createMock(ManagedConnectionFactory.class);

		ConnectionManager connectionManager = createMock(ConnectionManager.class);

		expect(managedConnectionFactory.createConnectionFactory(connectionManager)).andReturn(null);

		replay(connectionManager, managedConnectionFactory);

		LocalConnectionFactoryBean factory = new LocalConnectionFactoryBean();
		factory.setManagedConnectionFactory(managedConnectionFactory);
		factory.setConnectionManager(connectionManager);
		factory.afterPropertiesSet();

		verify(connectionManager, managedConnectionFactory);
	}

}
