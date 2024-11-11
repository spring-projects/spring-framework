/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jca.support;

import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ManagedConnectionFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link LocalConnectionFactoryBean}.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
class LocalConnectionFactoryBeanTests {

	@Test
	void testManagedConnectionFactoryIsRequired() {
		assertThatIllegalArgumentException().isThrownBy(
				new LocalConnectionFactoryBean()::afterPropertiesSet);
	}

	@Test
	void testIsSingleton() {
		LocalConnectionFactoryBean factory = new LocalConnectionFactoryBean();
		assertThat(factory.isSingleton()).isTrue();
	}

	@Test
	void testGetObjectTypeIsNullIfConnectionFactoryHasNotBeenConfigured() {
		LocalConnectionFactoryBean factory = new LocalConnectionFactoryBean();
		assertThat(factory.getObjectType()).isNull();
	}

	@Test
	void testCreatesVanillaConnectionFactoryIfNoConnectionManagerHasBeenConfigured() throws Exception {
		final Object CONNECTION_FACTORY = new Object();
		ManagedConnectionFactory managedConnectionFactory = mock();
		given(managedConnectionFactory.createConnectionFactory()).willReturn(CONNECTION_FACTORY);
		LocalConnectionFactoryBean factory = new LocalConnectionFactoryBean();
		factory.setManagedConnectionFactory(managedConnectionFactory);
		factory.afterPropertiesSet();
		assertThat(factory.getObject()).isEqualTo(CONNECTION_FACTORY);
	}

	@Test
	void testCreatesManagedConnectionFactoryIfAConnectionManagerHasBeenConfigured() throws Exception {
		ManagedConnectionFactory managedConnectionFactory = mock();
		ConnectionManager connectionManager = mock();
		LocalConnectionFactoryBean factory = new LocalConnectionFactoryBean();
		factory.setManagedConnectionFactory(managedConnectionFactory);
		factory.setConnectionManager(connectionManager);
		factory.afterPropertiesSet();
		verify(managedConnectionFactory).createConnectionFactory(connectionManager);
	}

}
