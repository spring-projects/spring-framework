/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.connection.lookup;

import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link BeanFactoryConnectionFactoryLookup}.
 *
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
class BeanFactoryConnectionFactoryLookupTests {

	private static final String CONNECTION_FACTORY_BEAN_NAME = "connectionFactory";

	@Mock
	BeanFactory beanFactory;


	@Test
	void shouldLookupConnectionFactory() {
		DummyConnectionFactory expectedConnectionFactory = new DummyConnectionFactory();
		when(beanFactory.getBean(CONNECTION_FACTORY_BEAN_NAME, ConnectionFactory.class))
				.thenReturn(expectedConnectionFactory);

		BeanFactoryConnectionFactoryLookup lookup = new BeanFactoryConnectionFactoryLookup();
		lookup.setBeanFactory(beanFactory);

		ConnectionFactory connectionFactory = lookup.getConnectionFactory(CONNECTION_FACTORY_BEAN_NAME);
		assertThat(connectionFactory).isNotNull();
		assertThat(connectionFactory).isSameAs(expectedConnectionFactory);
	}

	@Test
	void shouldLookupWhereBeanFactoryYieldsNonConnectionFactoryType() {
		BeanFactory beanFactory = mock();
		when(beanFactory.getBean(CONNECTION_FACTORY_BEAN_NAME, ConnectionFactory.class))
				.thenThrow(new BeanNotOfRequiredTypeException(
						CONNECTION_FACTORY_BEAN_NAME, ConnectionFactory.class, String.class));

		BeanFactoryConnectionFactoryLookup lookup = new BeanFactoryConnectionFactoryLookup(beanFactory);
		assertThatExceptionOfType(ConnectionFactoryLookupFailureException.class)
				.isThrownBy(() -> lookup.getConnectionFactory(CONNECTION_FACTORY_BEAN_NAME));
	}

	@Test
	void shouldLookupWhereBeanFactoryHasNotBeenSupplied() {
		BeanFactoryConnectionFactoryLookup lookup = new BeanFactoryConnectionFactoryLookup();

		assertThatThrownBy(() -> lookup.getConnectionFactory(CONNECTION_FACTORY_BEAN_NAME))
				.isInstanceOf(IllegalStateException.class);
	}

}
