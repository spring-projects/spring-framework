/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.jdbc.datasource.init;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;



/**
 * Abstract base class for integration tests involving database initialization.
 *
 * @author Sam Brannen
 * @since 4.0.3
 */
public abstract class AbstractDatabaseInitializationTests {

	private final ClassRelativeResourceLoader resourceLoader = new ClassRelativeResourceLoader(getClass());

	EmbeddedDatabase db;

	JdbcTemplate jdbcTemplate;


	@BeforeEach
	public void setUp() {
		db = new EmbeddedDatabaseBuilder().setType(getEmbeddedDatabaseType()).build();
		jdbcTemplate = new JdbcTemplate(db);
	}

	@AfterEach
	public void shutDown() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
			TransactionSynchronizationManager.unbindResource(db);
		}
		db.shutdown();
	}

	abstract EmbeddedDatabaseType getEmbeddedDatabaseType();

	Resource resource(String path) {
		return resourceLoader.getResource(path);
	}

	Resource defaultSchema() {
		return resource("db-schema.sql");
	}

	Resource usersSchema() {
		return resource("users-schema.sql");
	}

	void assertUsersDatabaseCreated(String... lastNames) {
		for (String lastName : lastNames) {
			String sql = "select count(0) from users where last_name = ?";
			Integer result = jdbcTemplate.queryForObject(sql, Integer.class, lastName);
			assertThat(result).as("user with last name [" + lastName + "]").isEqualTo(1);
		}
	}

}
