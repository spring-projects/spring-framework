/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	void managedConnectionFactoryIsRequired() {
		assertThatIllegalArgumentException().isThrownBy(
				new LocalConnectionFactoryBean()::afterPropertiesSet);
	}

	@Test
	void isSingleton() {
		LocalConnectionFactoryBean factory = new LocalConnectionFactoryBean();
		assertThat(factory.isSingleton()).isTrue();
	}

	@Test
	void getObjectTypeIsNullIfConnectionFactoryHasNotBeenConfigured() {
		LocalConnectionFactoryBean factory = new LocalConnectionFactoryBean();
		assertThat(factory.getObjectType()).isNull();
	}

	@Test
	void createsVanillaConnectionFactoryIfNoConnectionManagerHasBeenConfigured() throws Exception {
		final Object CONNECTION_FACTORY = new Object();
		ManagedConnectionFactory managedConnectionFactory = mock();
		given(managedConnectionFactory.createConnectionFactory()).willReturn(CONNECTION_FACTORY);
		LocalConnectionFactoryBean factory = new LocalConnectionFactoryBean();
		factory.setManagedConnectionFactory(managedConnectionFactory);
		factory.afterPropertiesSet();
		assertThat(factory.getObject()).isEqualTo(CONNECTION_FACTORY);
	}

	@Test
	void createsManagedConnectionFactoryIfAConnectionManagerHasBeenConfigured() throws Exception {
		ManagedConnectionFactory managedConnectionFactory = mock();
		ConnectionManager connectionManager = mock();
		LocalConnectionFactoryBean factory = new LocalConnectionFactoryBean();
		factory.setManagedConnectionFactory(managedConnectionFactory);
		factory.setConnectionManager(connectionManager);
		factory.afterPropertiesSet();
		verify(managedConnectionFactory).createConnectionFactory(connectionManager);
	}

}
