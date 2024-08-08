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

package org.springframework.jdbc.datasource.embedded;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import org.springframework.jdbc.datasource.init.DatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EmbeddedDatabaseFactory}.
 *
 * @author Keith Donald
 * @author Stephane Nicoll
 */
class EmbeddedDatabaseFactoryTests {

	private final EmbeddedDatabaseFactory factory = new EmbeddedDatabaseFactory();


	@Test
	void testGetDataSource() {
		StubDatabasePopulator populator = new StubDatabasePopulator();
		factory.setDatabasePopulator(populator);
		EmbeddedDatabase db = factory.getDatabase();
		assertThat(populator.populateCalled).isTrue();
		db.shutdown();
	}

	@Test
	void customizeConfigurerWithAnotherDatabaseName() throws SQLException {
		this.factory.setDatabaseName("original-db-mame");
		this.factory.setDatabaseConfigurer(EmbeddedDatabaseConfigurers.customizeConfigurer(
				EmbeddedDatabaseType.H2, defaultConfigurer ->
						new EmbeddedDatabaseConfigurerDelegate(defaultConfigurer) {
							@Override
							public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
								super.configureConnectionProperties(properties, "custom-db-name");
							}
						}));
		EmbeddedDatabase db = this.factory.getDatabase();
		try (Connection connection = db.getConnection()) {
			assertThat(connection.getMetaData().getURL()).contains("custom-db-name")
					.doesNotContain("original-db-mame");
		}
		db.shutdown();
	}

	@Test
	void customizeConfigurerWithCustomizedUrl() throws SQLException {
		this.factory.setDatabaseName("original-db-mame");
		this.factory.setDatabaseConfigurer(EmbeddedDatabaseConfigurers.customizeConfigurer(
				EmbeddedDatabaseType.H2, defaultConfigurer ->
						new EmbeddedDatabaseConfigurerDelegate(defaultConfigurer) {
							@Override
							public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
								super.configureConnectionProperties(properties, databaseName);
								properties.setUrl("jdbc:h2:mem:custom-db-name;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false;MODE=MariaDB");
							}
						}));
		EmbeddedDatabase db = this.factory.getDatabase();
		try (Connection connection = db.getConnection()) {
			assertThat(connection.getMetaData().getURL()).contains("custom-db-name")
					.doesNotContain("original-db-mame");
		}
		db.shutdown();
	}


	private static class StubDatabasePopulator implements DatabasePopulator {

		private boolean populateCalled;

		@Override
		public void populate(Connection connection) {
			this.populateCalled = true;
		}
	}

}
