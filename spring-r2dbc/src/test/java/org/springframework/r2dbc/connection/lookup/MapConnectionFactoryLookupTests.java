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

package org.springframework.r2dbc.connection.lookup;

import java.util.HashMap;
import java.util.Map;

import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link MapConnectionFactoryLookup}.
 *
 * @author Mark Paluch
 */
class MapConnectionFactoryLookupTests {

	private static final String CONNECTION_FACTORY_NAME = "connectionFactory";


	@Test
	void getConnectionFactoriesReturnsUnmodifiableMap() {
		MapConnectionFactoryLookup lookup = new MapConnectionFactoryLookup();
		Map<String, ConnectionFactory> connectionFactories = lookup.getConnectionFactories();

		assertThatThrownBy(() -> connectionFactories.put("", new DummyConnectionFactory()))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void shouldLookupConnectionFactory() {
		Map<String, ConnectionFactory> connectionFactories = new HashMap<>();
		DummyConnectionFactory expectedConnectionFactory = new DummyConnectionFactory();
		connectionFactories.put(CONNECTION_FACTORY_NAME, expectedConnectionFactory);

		MapConnectionFactoryLookup lookup = new MapConnectionFactoryLookup();
		lookup.setConnectionFactories(connectionFactories);

		assertThat(lookup.getConnectionFactory(CONNECTION_FACTORY_NAME))
				.isNotNull().isSameAs(expectedConnectionFactory);
	}

	@Test
	void addingConnectionFactoryPermitsOverride() {
		Map<String, ConnectionFactory> connectionFactories = new HashMap<>();
		DummyConnectionFactory overriddenConnectionFactory = new DummyConnectionFactory();
		DummyConnectionFactory expectedConnectionFactory = new DummyConnectionFactory();
		connectionFactories.put(CONNECTION_FACTORY_NAME, overriddenConnectionFactory);

		MapConnectionFactoryLookup lookup = new MapConnectionFactoryLookup();
		lookup.setConnectionFactories(connectionFactories);
		lookup.addConnectionFactory(CONNECTION_FACTORY_NAME, expectedConnectionFactory);

		assertThat(lookup.getConnectionFactory(CONNECTION_FACTORY_NAME))
				.isNotNull().isSameAs(expectedConnectionFactory);
	}

	@Test
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void getConnectionFactoryWhereSuppliedMapHasNonConnectionFactoryTypeUnderSpecifiedKey() {
		Map connectionFactories = new HashMap<>();
		connectionFactories.put(CONNECTION_FACTORY_NAME, new Object());
		MapConnectionFactoryLookup lookup = new MapConnectionFactoryLookup(connectionFactories);

		assertThatThrownBy(() -> lookup.getConnectionFactory(CONNECTION_FACTORY_NAME))
				.isInstanceOf(ClassCastException.class);
	}

	@Test
	void getConnectionFactoryWhereSuppliedMapHasNoEntryForSpecifiedKey() {
		MapConnectionFactoryLookup lookup = new MapConnectionFactoryLookup();

		assertThatThrownBy(
				() -> lookup.getConnectionFactory(CONNECTION_FACTORY_NAME)).isInstanceOf(
						ConnectionFactoryLookupFailureException.class);
	}

}
