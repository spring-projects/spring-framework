/*
 * Copyright 2002-present the original author or authors.
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

import java.sql.Types;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.H2;

/**
 * Integration tests for {@link JdbcClient} using an embedded H2 database.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Jiri Krokviak
 * @author Yanming Zhou
 * @since 6.1
 * @see JdbcClientIndexedParameterTests
 * @see JdbcClientNamedParameterTests
 */
class JdbcClientIntegrationTests {

	private static final String INSERT_WITH_JDBC_PARAMS =
			"INSERT INTO users (first_name, last_name) VALUES(?, ?)";

	private static final String INSERT_WITH_NAMED_PARAMS =
			"INSERT INTO users (first_name, last_name) VALUES(:firstName, :lastName)";

	private static final String MIXED_BATCH_ENTRIES_MESSAGE =
			"Configure either named or indexed parameters for all batch entries, not both";


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
		int expectedId = 1;
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
		int expectedId = 1;
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
		int expectedId = 1;
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
		int expectedId = 1;
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

	@Test
	void batchUpdateWithIndexedParameterList() {
		int[] rowsAffected = this.jdbcClient.sql(INSERT_WITH_JDBC_PARAMS)
				.batch()
					.entry(List.of("Jane", "Smith"))
					.entry(List.of("John", "Doe"))
				.update();

		assertThat(rowsAffected).containsExactly(1, 1);
		assertNumUsers(3);
		assertUser(1, "Jane", "Smith");
		assertUser(2, "John", "Doe");
	}

	@Test
	void batchUpdateWithIndividualIndexedParameters() {
		int[] rowsAffected = this.jdbcClient.sql(INSERT_WITH_JDBC_PARAMS)
				.batch()
					.entry(entry -> entry.param("Jane").param("Smith"))
					.entry(entry -> entry.param("John").param("Doe"))
				.update();

		assertThat(rowsAffected).containsExactly(1, 1);
		assertNumUsers(3);
		assertUser(1, "Jane", "Smith");
		assertUser(2, "John", "Doe");
	}

	@Test
	void batchUpdateWithJdbcIndexParameters() {
		int[] rowsAffected = this.jdbcClient.sql(INSERT_WITH_JDBC_PARAMS)
				.batch()
					.entry(entry -> entry.param(2, "Smith").param(1, "Jane"))
					.entry(entry -> entry.param(2, "Doe").param(1, "John"))
				.update();

		assertThat(rowsAffected).containsExactly(1, 1);
		assertNumUsers(3);
		assertUser(1, "Jane", "Smith");
		assertUser(2, "John", "Doe");
	}

	@Test
	void batchUpdateWithJdbcIndexParametersAndSqlType() {
		int[] rowsAffected = this.jdbcClient.sql(INSERT_WITH_JDBC_PARAMS)
				.batch()
					.entry(entry -> entry.param(2, "Smith", Types.VARCHAR).param(1, "Jane", Types.VARCHAR))
					.entry(entry -> entry.param(2, "Doe", Types.VARCHAR).param(1, "John", Types.VARCHAR))
				.update();

		assertThat(rowsAffected).containsExactly(1, 1);
		assertNumUsers(3);
		assertUser(1, "Jane", "Smith");
		assertUser(2, "John", "Doe");
	}

	@Test
	void batchUpdateWithNamedParameters() {
		int[] rowsAffected = this.jdbcClient.sql(INSERT_WITH_NAMED_PARAMS)
				.batch()
					.entry(entry -> entry.param("firstName", "Jane").param("lastName", "Smith"))
					.entry(entry -> entry.param("firstName", "John").param("lastName", "Doe"))
				.update();

		assertThat(rowsAffected).containsExactly(1, 1);
		assertNumUsers(3);
		assertUser(1, "Jane", "Smith");
		assertUser(2, "John", "Doe");
	}

	@Test
	void batchUpdateWithNamedParametersAndSqlType() {
		int[] rowsAffected = this.jdbcClient.sql(INSERT_WITH_NAMED_PARAMS)
				.batch()
					.entry(entry -> entry.param("firstName", "Jane", Types.VARCHAR).param("lastName", "Smith", Types.VARCHAR))
					.entry(entry -> entry.param("firstName", "John", Types.VARCHAR).param("lastName", "Doe", Types.VARCHAR))
				.update();

		assertThat(rowsAffected).containsExactly(1, 1);
		assertNumUsers(3);
		assertUser(1, "Jane", "Smith");
		assertUser(2, "John", "Doe");
	}

	@Test
	void batchUpdateWithNamedParameterMap() {
		int[] rowsAffected = this.jdbcClient.sql(INSERT_WITH_NAMED_PARAMS)
				.batch()
					.entry(Map.of("firstName", "Jane", "lastName", "Smith"))
					.entry(Map.of("firstName", "John", "lastName", "Doe"))
				.update();

		assertThat(rowsAffected).containsExactly(1, 1);
		assertNumUsers(3);
		assertUser(1, "Jane", "Smith");
		assertUser(2, "John", "Doe");
	}

	@Test
	void batchUpdateWithParameterObject() {
		int[] rowsAffected = this.jdbcClient.sql(INSERT_WITH_NAMED_PARAMS)
				.batch()
					.entry(entry -> entry.paramSource(new NewUser("Jane", "Smith")))
					.entry(entry -> entry.paramSource(new NewUser("John", "Doe")))
				.update();

		assertThat(rowsAffected).containsExactly(1, 1);
		assertNumUsers(3);
		assertUser(1, "Jane", "Smith");
		assertUser(2, "John", "Doe");
	}

	@Test
	void batchUpdateWithParameterSource() {
		int[] rowsAffected = this.jdbcClient.sql(INSERT_WITH_NAMED_PARAMS)
				.batch()
					.entry(entry -> entry.paramSource(new MapSqlParameterSource("firstName", "Jane").addValue("lastName", "Smith")))
					.entry(entry -> entry.paramSource(new MapSqlParameterSource("firstName", "John").addValue("lastName", "Doe")))
				.update();

		assertThat(rowsAffected).containsExactly(1, 1);
		assertNumUsers(3);
		assertUser(1, "Jane", "Smith");
		assertUser(2, "John", "Doe");
	}

	@Test
	void batchUpdateWithListOfParameterObjects() {
		List<NewUser> users = List.of(new NewUser("Jane", "Smith"), new NewUser("John", "Doe"));

		int[] rowsAffected = this.jdbcClient.sql(INSERT_WITH_NAMED_PARAMS).batch().entries(users).update();

		assertThat(rowsAffected).containsExactly(1, 1);
		assertNumUsers(3);
		assertUser(1, "Jane", "Smith");
		assertUser(2, "John", "Doe");
	}

	@Test
	void batchUpdateWithVarargsParameterMaps() {
		int[] rowsAffected = this.jdbcClient.sql(INSERT_WITH_NAMED_PARAMS)
				.batch()
					.entries(
						Map.of("firstName", "Jane", "lastName", "Smith"),
						Map.of("firstName", "John", "lastName", "Doe"))
				.update();

		assertThat(rowsAffected).containsExactly(1, 1);
		assertNumUsers(3);
		assertUser(1, "Jane", "Smith");
		assertUser(2, "John", "Doe");
	}

	@Test
	void batchUpdateWithVarargsParameterSources() {
		int[] rowsAffected = this.jdbcClient.sql(INSERT_WITH_NAMED_PARAMS)
				.batch()
					.entries(
						new MapSqlParameterSource("firstName", "Jane").addValue("lastName", "Smith"),
						new MapSqlParameterSource("firstName", "John").addValue("lastName", "Doe"))
				.update();

		assertThat(rowsAffected).containsExactly(1, 1);
		assertNumUsers(3);
		assertUser(1, "Jane", "Smith");
		assertUser(2, "John", "Doe");
	}

	@Test
	void batchUpdateWithCombinedEntries() {
		int[] rowsAffected = this.jdbcClient.sql(INSERT_WITH_NAMED_PARAMS)
				.batch()
					.entries(List.of(new NewUser("Jane", "Smith"), new NewUser("John", "Doe")))
					.entry(entry -> entry.param("firstName", "Jack").param("lastName", "Jones"))
					.entry(Map.of("firstName", "Jill", "lastName", "Brown"))
				.update();

		assertThat(rowsAffected).containsExactly(1, 1, 1, 1);
		assertNumUsers(5);
		assertUser(1, "Jane", "Smith");
		assertUser(2, "John", "Doe");
		assertUser(3, "Jack", "Jones");
		assertUser(4, "Jill", "Brown");
	}

	@Test
	void emptyBatchUpdate() {
		int[] rowsAffected = this.jdbcClient.sql(INSERT_WITH_NAMED_PARAMS).batch().update();

		assertThat(rowsAffected).isEmpty();
		assertNumUsers(1);
	}

	@Test
	void emptyEntriesBatchUpdate() {
		int[] rowsAffected = this.jdbcClient.sql(INSERT_WITH_NAMED_PARAMS).batch().entries(List.of()).update();

		assertThat(rowsAffected).isEmpty();
		assertNumUsers(1);
	}

	@Test
	void batchUpdateRejectsEntryWithoutParameters() {
		String message = "Configure at least one parameter for each batch entry";

		assertThatIllegalStateException()
				.isThrownBy(() -> this.jdbcClient.sql(INSERT_WITH_NAMED_PARAMS).batch().entry(entry -> {}))
				.withMessage(message);
		assertThatIllegalStateException()
				.isThrownBy(() -> this.jdbcClient.sql(INSERT_WITH_JDBC_PARAMS).batch().entry(List.of()))
				.withMessage(message);
		assertThatIllegalStateException()
				.isThrownBy(() -> this.jdbcClient.sql(INSERT_WITH_NAMED_PARAMS).batch().entry(Map.of()))
				.withMessage(message);
	}

	@Test
	void batchUpdateRejectsMixedParametersWithinEntry() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.jdbcClient.sql(INSERT_WITH_JDBC_PARAMS)
						.batch().entry(entry -> entry.param("Jane").param("lastName", "Smith")))
				.withMessage("Configure either named or indexed parameters, not both");
	}

	@Test
	void batchUpdateRejectsIndexedParametersWithParameterSourceWithinEntry() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.jdbcClient.sql(INSERT_WITH_JDBC_PARAMS)
						.batch().entry(entry -> entry.param("Jane").paramSource(new NewUser("Jane", "Smith"))))
				.withMessage("Configure either named or indexed parameters, not both");
	}

	@Test
	void batchUpdateRejectsIndividualNamedParametersWithParameterSource() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.jdbcClient.sql(INSERT_WITH_NAMED_PARAMS)
						.batch()
							.entry(entry -> entry.param("firstName", "Jane")
									.paramSource(new MapSqlParameterSource("lastName", "Smith"))))
				.withMessage("Configure either individual named parameters or a SqlParameterSource, not both");
	}

	@Test
	void batchUpdateRejectsIndexedEntryAfterNamedEntry() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.jdbcClient.sql(INSERT_WITH_NAMED_PARAMS)
						.batch()
							.entry(Map.of("firstName", "Jane", "lastName", "Smith"))
							.entry(List.of("John", "Doe")))
				.withMessage(MIXED_BATCH_ENTRIES_MESSAGE);
	}

	@Test
	void batchUpdateRejectsNamedEntryAfterIndexedEntry() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.jdbcClient.sql(INSERT_WITH_JDBC_PARAMS)
						.batch()
							.entry(List.of("Jane", "Smith"))
							.entry(Map.of("firstName", "John", "lastName", "Doe")))
				.withMessage(MIXED_BATCH_ENTRIES_MESSAGE);
	}

	@Test
	void batchUpdateRejectsEntriesAfterIndexedEntry() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.jdbcClient.sql(INSERT_WITH_JDBC_PARAMS)
						.batch()
							.entry(List.of("Jane", "Smith"))
							.entries(new NewUser("John", "Doe")))
				.withMessage(MIXED_BATCH_ENTRIES_MESSAGE);
	}

	@Test
	void batchUpdateWithNamedParametersAndGeneratedKeys() {
		KeyHolder generatedKeyHolder = new GeneratedKeyHolder();
		int[] rowsAffected = this.jdbcClient.sql(INSERT_WITH_NAMED_PARAMS)
				.batch()
					.entry(Map.of("firstName", "Jane", "lastName", "Smith"))
					.entry(Map.of("firstName", "John", "lastName", "Doe"))
				.update(generatedKeyHolder);

		assertThat(rowsAffected).containsExactly(1, 1);
		assertNumUsers(3);
		assertUser(1, "Jane", "Smith");
		assertUser(2, "John", "Doe");
		assertThat(generatedKeyHolder.getKeyList()).containsExactly(Map.of("ID", 1), Map.of("ID", 2));
	}

	@Test
	void batchUpdateWithIndexedParametersAndGeneratedKeys() {
		KeyHolder generatedKeyHolder = new GeneratedKeyHolder();
		int[] rowsAffected = this.jdbcClient.sql(INSERT_WITH_JDBC_PARAMS)
				.batch()
					.entry(List.of("Jane", "Smith"))
					.entry(List.of("John", "Doe"))
				.update(generatedKeyHolder);

		assertThat(rowsAffected).containsExactly(1, 1);
		assertNumUsers(3);
		assertUser(1, "Jane", "Smith");
		assertUser(2, "John", "Doe");
		assertThat(generatedKeyHolder.getKeyList()).containsExactly(Map.of("ID", 1), Map.of("ID", 2));
	}

	@Test
	void batchUpdateWithNamedParametersAndGeneratedKeysAndKeyColumnNames() {
		KeyHolder generatedKeyHolder = new GeneratedKeyHolder();
		int[] rowsAffected = this.jdbcClient.sql(INSERT_WITH_NAMED_PARAMS)
				.batch()
					.entry(entry -> entry.param("firstName", "Jane").param("lastName", "Smith"))
					.entry(entry -> entry.param("firstName", "John").param("lastName", "Doe"))
				.update(generatedKeyHolder, "id");

		assertThat(rowsAffected).containsExactly(1, 1);
		assertNumUsers(3);
		assertUser(1, "Jane", "Smith");
		assertUser(2, "John", "Doe");
		assertThat(generatedKeyHolder.getKeyList()).containsExactly(Map.of("ID", 1), Map.of("ID", 2));
	}

	@Test
	void batchUpdateWithParameterObjectsAndGeneratedKeys() {
		KeyHolder generatedKeyHolder = new GeneratedKeyHolder();
		int[] rowsAffected = this.jdbcClient.sql(INSERT_WITH_NAMED_PARAMS)
				.batch().entries(new NewUser("Jane", "Smith"), new NewUser("John", "Doe"))
				.update(generatedKeyHolder);

		assertThat(rowsAffected).containsExactly(1, 1);
		assertNumUsers(3);
		assertUser(1, "Jane", "Smith");
		assertUser(2, "John", "Doe");
		assertThat(generatedKeyHolder.getKeyList()).containsExactly(Map.of("ID", 1), Map.of("ID", 2));
	}

	@Test
	void batchUpdateWithIndexedParametersAndGeneratedKeysAndKeyColumnNames() {
		KeyHolder generatedKeyHolder = new GeneratedKeyHolder();
		int[] rowsAffected = this.jdbcClient.sql(INSERT_WITH_JDBC_PARAMS)
				.batch()
					.entry(entry -> entry.param("Jane").param("Smith"))
					.entry(entry -> entry.param("John").param("Doe"))
				.update(generatedKeyHolder, "id");

		assertThat(rowsAffected).containsExactly(1, 1);
		assertNumUsers(3);
		assertUser(1, "Jane", "Smith");
		assertUser(2, "John", "Doe");
		assertThat(generatedKeyHolder.getKeyList()).containsExactly(Map.of("ID", 1), Map.of("ID", 2));
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
		void selectWithReusedNamedParameterAndMaxRows() {
			List<User> users = jdbcClient.sql(QUERY1)
					.withFetchSize(1)
					.withMaxRows(1)
					.withQueryTimeout(1)
					.param("name", "John")
					.query(User.class)
					.list();

			assertSingleResult(users);
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

		@Test
		void selectWithReusedNamedParameterListAndMaxRows() {
			List<User> users = jdbcClient.sql(QUERY2)
					.withFetchSize(1)
					.withMaxRows(1)
					.withQueryTimeout(1)
					.paramSource(new Names(List.of("John", "Bogus")))
					.query(User.class)
					.list();

			assertSingleResult(users);
		}

		private static void assertResults(List<User> users) {
			assertThat(users).containsExactly(new User(1, "John", "John"), new User(2, "John", "Smith"));
		}

		private static void assertSingleResult(List<User> users) {
			assertThat(users).containsExactly(new User(1, "John", "John"));
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

	record NewUser(String firstName, String lastName) {}

}
