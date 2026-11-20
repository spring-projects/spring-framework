/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
abstract class AbstractDatabaseInitializationTests {

	private final ClassRelativeResourceLoader resourceLoader = new ClassRelativeResourceLoader(getClass());

	EmbeddedDatabase db;

	JdbcTemplate jdbcTemplate;


	@BeforeEach
	void setUp() {
		db = new EmbeddedDatabaseBuilder().setType(getEmbeddedDatabaseType()).build();
		jdbcTemplate = new JdbcTemplate(db);
	}

	@AfterEach
	void shutDown() {
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
