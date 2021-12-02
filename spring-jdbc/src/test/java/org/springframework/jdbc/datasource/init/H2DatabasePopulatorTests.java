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

package org.springframework.jdbc.datasource.init;

import java.sql.Connection;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.embedded.AutoCommitDisabledH2EmbeddedDatabaseConfigurer;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0.3
 */
class H2DatabasePopulatorTests extends AbstractDatabasePopulatorTests {

	@Override
	protected EmbeddedDatabaseType getEmbeddedDatabaseType() {
		return EmbeddedDatabaseType.H2;
	}

	/**
	 * https://jira.spring.io/browse/SPR-15896
	 *
	 * @since 5.0
	 */
	@Test
	void scriptWithH2Alias() throws Exception {
		databasePopulator.addScript(usersSchema());
		databasePopulator.addScript(resource("db-test-data-h2-alias.sql"));
		// Set statement separator to double newline so that ";" is not
		// considered a statement separator within the source code of the
		// aliased function 'REVERSE'.
		databasePopulator.setSeparator("\n\n");
		DatabasePopulatorUtils.execute(databasePopulator, db);
		String sql = "select REVERSE(first_name) from users where last_name='Brannen'";
		assertThat(jdbcTemplate.queryForObject(sql, String.class)).isEqualTo("maS");
	}

	/**
	 * https://github.com/spring-projects/spring-framework/issues/27008
	 *
	 * @since 5.3.11
	 */
	@Test
	void automaticallyCommitsIfAutoCommitIsDisabled() throws Exception {
		EmbeddedDatabase database = null;
		try {
			EmbeddedDatabaseFactory databaseFactory = new EmbeddedDatabaseFactory();
			databaseFactory.setDatabaseConfigurer(new AutoCommitDisabledH2EmbeddedDatabaseConfigurer());
			database = databaseFactory.getDatabase();

			assertAutoCommitDisabledPreconditions(database);

			// Set up schema
			databasePopulator.setScripts(usersSchema());
			DatabasePopulatorUtils.execute(databasePopulator, database);
			assertThat(selectFirstNames(database)).isEmpty();

			// Insert data
			databasePopulator.setScripts(resource("users-data.sql"));
			DatabasePopulatorUtils.execute(databasePopulator, database);
			assertThat(selectFirstNames(database)).containsExactly("Sam");
		}
		finally {
			if (database != null) {
				database.shutdown();
			}
		}
	}

	/**
	 * DatabasePopulatorUtils.execute() will obtain a new Connection, so we're
	 * really just testing the configuration of the database here.
	 */
	private void assertAutoCommitDisabledPreconditions(DataSource dataSource) throws Exception {
		Connection connection = DataSourceUtils.getConnection(dataSource);
		assertThat(connection.getAutoCommit()).as("auto-commit").isFalse();
		assertThat(DataSourceUtils.isConnectionTransactional(connection, dataSource)).as("transactional").isFalse();
		connection.close();
	}

	private List<String> selectFirstNames(DataSource dataSource) {
		return new JdbcTemplate(dataSource).queryForList("select first_name from users", String.class);
	}

}
