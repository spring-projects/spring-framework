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

package org.springframework.r2dbc.core.binding;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link BindMarkersFactoryResolver}.
 *
 * @author Mark Paluch
 */
class BindMarkersFactoryResolverUnitTests {

	@Test
	void shouldReturnBindMarkersFactoryForOracle() {

		BindMarkers bindMarkers = BindMarkersFactoryResolver
				.resolve(new MockConnectionFactory("Oracle Database")).create();

		assertThat(bindMarkers.next("foo").getPlaceholder()).isEqualTo(":P0_foo");
	}

	static class MockConnectionFactory implements ConnectionFactory {

		private final String driverName;

		MockConnectionFactory(String driverName) {
			this.driverName = driverName;
		}

		@Override
		public Publisher<? extends Connection> create() {
			throw new UnsupportedOperationException();
		}

		@Override
		public ConnectionFactoryMetadata getMetadata() {
			return () -> driverName;
		}

	}

}
