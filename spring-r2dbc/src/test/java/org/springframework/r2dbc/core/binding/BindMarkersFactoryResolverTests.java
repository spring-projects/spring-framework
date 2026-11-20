/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.core.binding;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BindMarkersFactoryResolver}.
 *
 * @author Mark Paluch
 */
class BindMarkersFactoryResolverTests {

	@Test
	void shouldReturnBindMarkersFactoryForH2() {
		BindMarkers bindMarkers = BindMarkersFactoryResolver
				.resolve(new MockConnectionFactory("H2")).create();

		assertThat(bindMarkers.next().getPlaceholder()).isEqualTo("$1");
	}

	@Test
	void shouldReturnBindMarkersFactoryForMariaDB() {
		BindMarkers bindMarkers = BindMarkersFactoryResolver
				.resolve(new MockConnectionFactory("MariaDB")).create();

		assertThat(bindMarkers.next().getPlaceholder()).isEqualTo("?");
	}

	@Test
	void shouldReturnBindMarkersFactoryForMicrosoftSQLServer() {
		BindMarkers bindMarkers = BindMarkersFactoryResolver
				.resolve(new MockConnectionFactory("Microsoft SQL Server")).create();

		assertThat(bindMarkers.next("foo").getPlaceholder()).isEqualTo("@P0_foo");
	}

	@Test
	void shouldReturnBindMarkersFactoryForMySQL() {
		BindMarkers bindMarkers = BindMarkersFactoryResolver
				.resolve(new MockConnectionFactory("MySQL")).create();

		assertThat(bindMarkers.next().getPlaceholder()).isEqualTo("?");
	}

	@Test
	void shouldReturnBindMarkersFactoryForOracle() {
		BindMarkers bindMarkers = BindMarkersFactoryResolver
				.resolve(new MockConnectionFactory("Oracle Database")).create();

		assertThat(bindMarkers.next("foo").getPlaceholder()).isEqualTo(":P0_foo");
	}

	@Test
	void shouldReturnBindMarkersFactoryForPostgreSQL() {
		BindMarkers bindMarkers = BindMarkersFactoryResolver
				.resolve(new MockConnectionFactory("PostgreSQL")).create();

		assertThat(bindMarkers.next().getPlaceholder()).isEqualTo("$1");
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
