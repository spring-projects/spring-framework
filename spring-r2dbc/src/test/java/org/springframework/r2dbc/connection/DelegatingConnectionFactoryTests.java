/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.connection;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DelegatingConnectionFactory}.
 *
 * @author Mark Paluch
 */
class DelegatingConnectionFactoryTests {

	ConnectionFactory delegate = mock();

	Connection connectionMock = mock();

	DelegatingConnectionFactory connectionFactory = new ExampleConnectionFactory(delegate);


	@Test
	@SuppressWarnings({"rawtypes", "unchecked"})
	void shouldDelegateGetConnection() {
		Mono<Connection> connectionMono = Mono.just(connectionMock);
		when(delegate.create()).thenReturn((Mono) connectionMono);

		assertThat(connectionFactory.create()).isSameAs(connectionMono);
	}

	@Test
	void shouldDelegateUnwrapWithoutImplementing() {
		assertThat(connectionFactory.unwrap()).isSameAs(delegate);
	}


	static class ExampleConnectionFactory extends DelegatingConnectionFactory {

		ExampleConnectionFactory(ConnectionFactory targetConnectionFactory) {
			super(targetConnectionFactory);
		}
	}

}
