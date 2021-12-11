/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.r2dbc.connection.init;

import java.util.concurrent.atomic.AtomicBoolean;

import io.r2dbc.spi.test.MockConnection;
import io.r2dbc.spi.test.MockConnectionFactory;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.when;

/**
 * Unit tests for {@link ConnectionFactoryInitializer}.
 *
 * @author Mark Paluch
 */
class ConnectionFactoryInitializerUnitTests {

	AtomicBoolean called = new AtomicBoolean();

	DatabasePopulator populator = mock(DatabasePopulator.class);

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
