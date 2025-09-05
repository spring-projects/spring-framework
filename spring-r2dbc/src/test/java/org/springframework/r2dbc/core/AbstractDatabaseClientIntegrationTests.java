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

package org.springframework.r2dbc.core;

import java.util.List;
import java.util.Map;

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Parameters;
import io.r2dbc.spi.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DatabaseClient}.
 *
 * @author Mark Paluch
 * @author Mingyuan Wu
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
abstract class AbstractDatabaseClientIntegrationTests {

	private ConnectionFactory connectionFactory;


	@BeforeEach
	public void before() {
		connectionFactory = createConnectionFactory();

		Mono.from(connectionFactory.create())
				.flatMapMany(connection -> Flux.from(connection.createStatement("DROP TABLE legoset").execute())
						.flatMap(Result::getRowsUpdated)
						.onErrorComplete()
						.thenMany(connection.createStatement(getCreateTableStatement()).execute())
						.flatMap(Result::getRowsUpdated).thenMany(connection.close())).as(StepVerifier::create)
				.verifyComplete();
	}

	/**
	 * Create a {@link ConnectionFactory} to be used in this test.
	 * @return the {@link ConnectionFactory} to be used in this test
	 */
	protected abstract ConnectionFactory createConnectionFactory();

	/**
	 * Return the CREATE TABLE statement for table {@code legoset} with the following
	 * three columns:
	 * <ul>
	 * <li>id integer (primary key), not null</li>
	 * <li>name varchar(255), nullable</li>
	 * <li>manual integer, nullable</li>
	 * </ul>
	 * @return the CREATE TABLE statement for table {@code legoset} with three columns.
	 */
	protected abstract String getCreateTableStatement();


	@Test
	void executeInsert() {
		DatabaseClient databaseClient = DatabaseClient.create(connectionFactory);

		databaseClient.sql("INSERT INTO legoset (id, name, manual) VALUES(:id, :name, :manual)")
				.bind("id", 42055)
				.bind("name", "SCHAUFELRADBAGGER")
				.bindNull("manual", Integer.class)
				.fetch().rowsUpdated()
				.as(StepVerifier::create)
				.expectNext(1L)
				.verifyComplete();

		databaseClient.sql("SELECT id FROM legoset")
				.map(row -> row.get("id"))
				.first()
				.as(StepVerifier::create)
				.assertNext(actual -> assertThat(actual).isInstanceOf(Number.class).isEqualTo(42055))
				.verifyComplete();
	}

	@Test
	void executeInsertWithList() {
		DatabaseClient databaseClient = DatabaseClient.create(connectionFactory);

		databaseClient.sql("INSERT INTO legoset (id, name, manual) VALUES(:id, :name, :manual)")
				.bindValues(List.of(42055, Parameters.in("SCHAUFELRADBAGGER"), Parameters.in(Integer.class)))
				.fetch().rowsUpdated()
				.as(StepVerifier::create)
				.expectNext(1L)
				.verifyComplete();

		databaseClient.sql("SELECT id FROM legoset")
				.mapValue(Integer.class)
				.first()
				.as(StepVerifier::create)
				.assertNext(actual -> assertThat(actual).isEqualTo(42055))
				.verifyComplete();
	}

	@Test
	void executeInsertWithMap() {
		DatabaseClient databaseClient = DatabaseClient.create(connectionFactory);

		databaseClient.sql("INSERT INTO legoset (id, name, manual) VALUES(:id, :name, :manual)")
				.bindValues(Map.of(
						"id", 42055,
						"name", Parameters.in("SCHAUFELRADBAGGER"),
						"manual", Parameters.in(Integer.class)))
				.fetch().rowsUpdated()
				.as(StepVerifier::create)
				.expectNext(1L)
				.verifyComplete();

		databaseClient.sql("SELECT id FROM legoset")
				.mapValue(Integer.class)
				.first()
				.as(StepVerifier::create)
				.assertNext(actual -> assertThat(actual).isEqualTo(42055))
				.verifyComplete();
	}

	@Test
	void executeInsertWithRecords() {
		DatabaseClient databaseClient = DatabaseClient.create(connectionFactory);

		databaseClient.sql("INSERT INTO legoset (id, name, manual) VALUES(:id, :name, :manual)")
				.bindProperties(new ParameterRecord(42055, "SCHAUFELRADBAGGER", null))
				.fetch().rowsUpdated()
				.as(StepVerifier::create)
				.expectNext(1L)
				.verifyComplete();

		databaseClient.sql("SELECT id FROM legoset")
				.mapProperties(ResultRecord.class)
				.first()
				.as(StepVerifier::create)
				.assertNext(actual -> assertThat(actual.id()).isEqualTo(42055))
				.verifyComplete();
	}

	@Test
	void shouldTranslateDuplicateKeyException() {
		DatabaseClient databaseClient = DatabaseClient.create(connectionFactory);

		executeInsert();

		databaseClient.sql(
				"INSERT INTO legoset (id, name, manual) VALUES(:id, :name, :manual)")
				.bind("id", 42055)
				.bind("name", "SCHAUFELRADBAGGER")
				.bindNull("manual", Integer.class)
				.fetch().rowsUpdated()
				.as(StepVerifier::create)
				.expectErrorSatisfies(exception -> assertThat(exception)
						.isInstanceOf(DataIntegrityViolationException.class)
						.hasMessageContaining("execute; SQL [INSERT INTO legoset"))
				.verify();
	}

	@Test
	void executeDeferred() {
		DatabaseClient databaseClient = DatabaseClient.create(connectionFactory);

		databaseClient.sql(() -> "INSERT INTO legoset (id, name, manual) VALUES(:id, :name, :manual)")
				.bind("id", 42055)
				.bind("name", "SCHAUFELRADBAGGER")
				.bindNull("manual", Integer.class)
				.fetch().rowsUpdated()
				.as(StepVerifier::create)
				.expectNext(1L)
				.verifyComplete();

		databaseClient.sql("SELECT id FROM legoset")
				.map(row -> row.get("id")).first()
				.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete();
	}

	@Test
	void shouldEmitGeneratedKey() {
		DatabaseClient databaseClient = DatabaseClient.create(connectionFactory);

		databaseClient.sql("INSERT INTO legoset ( name, manual) VALUES(:name, :manual)")
				.bind("name","SCHAUFELRADBAGGER")
				.bindNull("manual", Integer.class)
				.filter(statement -> statement.returnGeneratedValues("id"))
				.map(row -> (Number) row.get("id"))
				.first()
				.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete();
	}


	@Nested
	class ReusedNamedParameterTests {

		@Test  // gh-34768
		void executeInsertWithReusedNamedParameter() {
			DatabaseClient databaseClient = DatabaseClient.create(connectionFactory);

			Lego lego = new Lego(1, 42, "Star Wars", 42);

			// ":number" is reused.
			databaseClient.sql("INSERT INTO legoset (id, version, name, manual) VALUES(:id, :number, :name, :number)")
					.bind("id", lego.id)
					.bind("name", lego.name)
					.bind("number", lego.version)
					.fetch().rowsUpdated()
					.as(StepVerifier::create)
					.expectNext(1L)
					.verifyComplete();

			databaseClient.sql("SELECT * FROM legoset")
					.mapProperties(Lego.class)
					.first()
					.as(StepVerifier::create)
					.assertNext(actual -> assertThat(actual).isEqualTo(lego))
					.verifyComplete();
		}

		@Test  // gh-34768
		void executeSelectWithReusedNamedParameterList() {
			DatabaseClient databaseClient = DatabaseClient.create(connectionFactory);

			String insertSql = "INSERT INTO legoset (id, version, name, manual) VALUES(:id, :version, :name, :manual)";
			// ":numbers" is reused.
			String selectSql = "SELECT * FROM legoset WHERE version IN (:numbers) OR manual IN (:numbers)";
			Lego lego = new Lego(1, 42, "Star Wars", 99);

			databaseClient.sql(insertSql)
					.bind("id", lego.id)
					.bind("version", lego.version)
					.bind("name", lego.name)
					.bind("manual", lego.manual)
					.fetch().rowsUpdated()
					.as(StepVerifier::create)
					.expectNext(1L)
					.verifyComplete();

			databaseClient.sql(selectSql)
					// match version
					.bind("numbers", List.of(2, 3, lego.version, 4))
					.mapProperties(Lego.class)
					.first()
					.as(StepVerifier::create)
					.assertNext(actual -> assertThat(actual).isEqualTo(lego))
					.verifyComplete();

			databaseClient.sql(selectSql)
					// match manual
					.bind("numbers", List.of(2, 3, lego.manual, 4))
					.mapProperties(Lego.class)
					.first()
					.as(StepVerifier::create)
					.assertNext(actual -> assertThat(actual).isEqualTo(lego))
					.verifyComplete();
		}

		@Test  // gh-34768
		void executeSelectWithReusedNamedParameterListFromBeanProperties() {
			DatabaseClient databaseClient = DatabaseClient.create(connectionFactory);

			String insertSql = "INSERT INTO legoset (id, version, name, manual) VALUES(:id, :version, :name, :manual)";
			// ":numbers" is reused.
			String selectSql = "SELECT * FROM legoset WHERE version IN (:numbers) OR manual IN (:numbers)";
			Lego lego = new Lego(1, 42, "Star Wars", 99);

			databaseClient.sql(insertSql)
					.bind("id", lego.id)
					.bind("version", lego.version)
					.bind("name", lego.name)
					.bind("manual", lego.manual)
					.fetch().rowsUpdated()
					.as(StepVerifier::create)
					.expectNext(1L)
					.verifyComplete();

			databaseClient.sql(selectSql)
					// match version
					.bindProperties(new LegoRequest(List.of(lego.version)))
					.mapProperties(Lego.class)
					.first()
					.as(StepVerifier::create)
					.assertNext(actual -> assertThat(actual).isEqualTo(lego))
					.verifyComplete();

			databaseClient.sql(selectSql)
					// match manual
					.bindProperties(new LegoRequest(List.of(lego.manual)))
					.mapProperties(Lego.class)
					.first()
					.as(StepVerifier::create)
					.assertNext(actual -> assertThat(actual).isEqualTo(lego))
					.verifyComplete();
		}


		record Lego(int id, Integer version, String name, Integer manual) {
		}

		static class LegoRequest {

			private final List<Integer> numbers;

			LegoRequest(List<Integer> numbers) {
				this.numbers = numbers;
			}

			public List<Integer> getNumbers() {
				return numbers;
			}
		}

	}


	record ParameterRecord(int id, String name, Integer manual) {
	}

	record ResultRecord(int id) {
	}

}
