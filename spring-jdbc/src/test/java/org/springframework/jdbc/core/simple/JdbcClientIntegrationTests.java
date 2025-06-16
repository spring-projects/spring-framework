/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.jdbc.core.simple;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.H2;

/**
 * Integration tests for {@link JdbcClient} using an embedded H2 database.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 6.1
 * @see JdbcClientIndexedParameterTests
 * @see JdbcClientNamedParameterTests
 */
class JdbcClientIntegrationTests {

	private static final String INSERT_WITH_JDBC_PARAMS =
			"INSERT INTO users (first_name, last_name) VALUES(?, ?)";

	private static final String INSERT_WITH_NAMED_PARAMS =
			"INSERT INTO users (first_name, last_name) VALUES(:firstName, :lastName)";


	private final EmbeddedDatabase embeddedDatabase =
			new EmbeddedDatabaseBuilder(new ClassRelativeResourceLoader(DatabasePopulator.class))
				.generateUniqueName(true)
				.setType(H2)
				.addScripts("users-schema.sql", "users-data.sql")
				.build();

	private final JdbcClient jdbcClient = JdbcClient.create(this.embeddedDatabase);


	@BeforeEach
	void checkDatabase() {
		assertNumUsers(1);
	}

	@AfterEach
	void shutdownDatabase() {
		this.embeddedDatabase.shutdown();
	}


	@Test
	void updateWithGeneratedKeys() {
		int expectedId = 2;
		String firstName = "Jane";
		String lastName = "Smith";

		KeyHolder generatedKeyHolder = new GeneratedKeyHolder();

		int rowsAffected = this.jdbcClient.sql(INSERT_WITH_JDBC_PARAMS)
				.params(firstName, lastName)
				.update(generatedKeyHolder);

		assertThat(rowsAffected).isEqualTo(1);
		assertThat(generatedKeyHolder.getKey()).isEqualTo(expectedId);
		assertNumUsers(2);
		assertUser(expectedId, firstName, lastName);
	}

	@Test
	void updateWithGeneratedKeysAndKeyColumnNames() {
		int expectedId = 2;
		String firstName = "Jane";
		String lastName = "Smith";

		KeyHolder generatedKeyHolder = new GeneratedKeyHolder();

		int rowsAffected = this.jdbcClient.sql(INSERT_WITH_JDBC_PARAMS)
				.params(firstName, lastName)
				.update(generatedKeyHolder, "id");

		assertThat(rowsAffected).isEqualTo(1);
		assertThat(generatedKeyHolder.getKey()).isEqualTo(expectedId);
		assertNumUsers(2);
		assertUser(expectedId, firstName, lastName);
	}

	@Test
	void updateWithGeneratedKeysUsingNamedParameters() {
		int expectedId = 2;
		String firstName = "Jane";
		String lastName = "Smith";

		KeyHolder generatedKeyHolder = new GeneratedKeyHolder();

		int rowsAffected = this.jdbcClient.sql(INSERT_WITH_NAMED_PARAMS)
				.param("firstName", firstName)
				.param("lastName", lastName)
				.update(generatedKeyHolder);

		assertThat(rowsAffected).isEqualTo(1);
		assertThat(generatedKeyHolder.getKey()).isEqualTo(expectedId);
		assertNumUsers(2);
		assertUser(expectedId, firstName, lastName);
	}

	@Test
	void updateWithGeneratedKeysAndKeyColumnNamesUsingNamedParameters() {
		int expectedId = 2;
		String firstName = "Jane";
		String lastName = "Smith";

		KeyHolder generatedKeyHolder = new GeneratedKeyHolder();

		int rowsAffected = this.jdbcClient.sql(INSERT_WITH_NAMED_PARAMS)
				.param("firstName", firstName)
				.param("lastName", lastName)
				.update(generatedKeyHolder, "id");

		assertThat(rowsAffected).isEqualTo(1);
		assertThat(generatedKeyHolder.getKey()).isEqualTo(expectedId);
		assertNumUsers(2);
		assertUser(expectedId, firstName, lastName);
	}


	@Nested  // gh-34768
	class ReusedNamedParameterTests {

		private static final String QUERY1 = """
				select * from users
					where
						first_name in ('Bogus', :name) or
						last_name in (:name, 'Bogus')
					order by last_name
				""";

		private static final String QUERY2 = """
				select * from users
					where
						first_name in (:names) or
						last_name in (:names)
					order by last_name
				""";


		@BeforeEach
		void insertTestUsers() {
			jdbcClient.sql(INSERT_WITH_JDBC_PARAMS).params("John", "John").update();
			jdbcClient.sql(INSERT_WITH_JDBC_PARAMS).params("John", "Smith").update();
			jdbcClient.sql(INSERT_WITH_JDBC_PARAMS).params("Smith", "Smith").update();
			assertNumUsers(4);
		}

		@Test
		void selectWithReusedNamedParameter() {
			List<User> users = jdbcClient.sql(QUERY1)
					.param("name", "John")
					.query(User.class)
					.list();

			assertResults(users);
		}

		@Test
		void selectWithReusedNamedParameterFromBeanProperties() {
			List<User> users = jdbcClient.sql(QUERY1)
					.paramSource(new Name("John"))
					.query(User.class)
					.list();

			assertResults(users);
		}

		@Test
		void selectWithReusedNamedParameterList() {
			List<User> users = jdbcClient.sql(QUERY2)
					.param("names", List.of("John", "Bogus"))
					.query(User.class)
					.list();

			assertResults(users);
		}

		@Test
		void selectWithReusedNamedParameterListFromBeanProperties() {
			List<User> users = jdbcClient.sql(QUERY2)
					.paramSource(new Names(List.of("John", "Bogus")))
					.query(User.class)
					.list();

			assertResults(users);
		}


		private static void assertResults(List<User> users) {
			assertThat(users).containsExactly(new User(2, "John", "John"), new User(3, "John", "Smith"));
		}

		record Name(String name) {}

		record Names(List<String> names) {}

	}


	private void assertNumUsers(long count) {
		long numUsers = this.jdbcClient.sql("select count(id) from users").query(Long.class).single();
		assertThat(numUsers).isEqualTo(count);
	}

	private void assertUser(long id, String firstName, String lastName) {
		User user = this.jdbcClient.sql("select * from users where id = ?").param(id).query(User.class).single();
		assertThat(user).isEqualTo(new User(id, firstName, lastName));
	}


	record User(long id, String firstName, String lastName) {}

}
