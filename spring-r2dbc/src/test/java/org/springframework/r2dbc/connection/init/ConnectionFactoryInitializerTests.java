/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.connection.init;

import java.util.concurrent.atomic.AtomicBoolean;

import io.r2dbc.spi.test.MockConnection;
import io.r2dbc.spi.test.MockConnectionFactory;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ConnectionFactoryInitializer}.
 *
 * @author Mark Paluch
 */
class ConnectionFactoryInitializerTests {

	AtomicBoolean called = new AtomicBoolean();

	DatabasePopulator populator = mock();

	MockConnection connection = MockConnection.builder().build();

	MockConnectionFactory connectionFactory = MockConnectionFactory.builder().connection(connection).build();


	@Test
	void shouldInitializeConnectionFactory() {
		when(populator.populate(connectionFactory)).thenReturn(
				Mono.<Void> empty().doOnSubscribe(subscription -> called.set(true)));

		ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
		initializer.setConnectionFactory(connectionFactory);
		initializer.setDatabasePopulator(populator);

		initializer.afterPropertiesSet();

		assertThat(called).isTrue();
	}

	@Test
	void shouldCleanConnectionFactory() {
		when(populator.populate(connectionFactory)).thenReturn(
				Mono.<Void> empty().doOnSubscribe(subscription -> called.set(true)));

		ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
		initializer.setConnectionFactory(connectionFactory);
		initializer.setDatabaseCleaner(populator);

		initializer.destroy();

		assertThat(called).isTrue();
	}

}
