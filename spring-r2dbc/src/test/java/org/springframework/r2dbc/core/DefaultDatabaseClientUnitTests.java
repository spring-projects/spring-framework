/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Parameters;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import io.r2dbc.spi.test.MockColumnMetadata;
import io.r2dbc.spi.test.MockResult;
import io.r2dbc.spi.test.MockRow;
import io.r2dbc.spi.test.MockRowMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.binding.BindMarkersFactory;
import org.springframework.r2dbc.core.binding.BindTarget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.doReturn;
import static org.mockito.BDDMockito.inOrder;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.verifyNoInteractions;
import static org.mockito.BDDMockito.verifyNoMoreInteractions;
import static org.mockito.BDDMockito.when;

/**
 * Unit tests for {@link DefaultDatabaseClient}.
 *
 * @author Mark Paluch
 * @author Ferdinand Jacobs
 * @author Jens Schauder
 * @author Simon Baslé
 */
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultDatabaseClientUnitTests {

	@Mock
	private Connection connection;

	private DatabaseClient.Builder databaseClientBuilder;


	@BeforeEach
	@SuppressWarnings({"rawtypes", "unchecked"})
	void before() {
		ConnectionFactory connectionFactory = mock();

		when(connectionFactory.create()).thenReturn((Publisher) Mono.just(connection));
		when(connection.close()).thenReturn(Mono.empty());

		databaseClientBuilder = DatabaseClient.builder().connectionFactory(
				connectionFactory).bindMarkers(BindMarkersFactory.indexed("$", 1));
	}


	@Test
	void connectionFactoryIsExposed() {
		ConnectionFactory connectionFactory = mock();
		DatabaseClient databaseClient = DatabaseClient.builder()
				.connectionFactory(connectionFactory)
				.bindMarkers(BindMarkersFactory.anonymous("?")).build();
		assertThat(databaseClient.getConnectionFactory()).isSameAs(connectionFactory);
	}

	@Test
	void shouldCloseConnectionOnlyOnce() {
		DefaultDatabaseClient databaseClient = (DefaultDatabaseClient) databaseClientBuilder.build();
		Flux<Object> flux = databaseClient.inConnectionMany(connection -> Flux.empty());

		flux.subscribe(new CoreSubscriber<Object>() {

			Subscription subscription;

			@Override
			public void onSubscribe(Subscription s) {
				s.request(1);
				subscription = s;
			}

			@Override
			public void onNext(Object o) {
			}

			@Override
			public void onError(Throwable t) {
			}

			@Override
			public void onComplete() {
				subscription.cancel();
			}
		});

		verify(connection, times(1)).close();
	}

	@Test
	void executeShouldBindNullValues() {
		Statement statement = mockStatementFor("SELECT * FROM table WHERE key = $1");

		DatabaseClient databaseClient = databaseClientBuilder.namedParameters(false).build();

		databaseClient.sql("SELECT * FROM table WHERE key = $1").bindNull(0,
				String.class).then().as(StepVerifier::create).verifyComplete();

		verify(statement).bind(0, Parameters.in(String.class));

		databaseClient.sql("SELECT * FROM table WHERE key = $1").bindNull("$1",
				String.class).then().as(StepVerifier::create).verifyComplete();

		verify(statement).bind("$1", Parameters.in(String.class));
	}

	@Test
	@SuppressWarnings("deprecation")
	void executeShouldBindSettableValues() {
		Statement statement = mockStatementFor("SELECT * FROM table WHERE key = $1");
		DatabaseClient databaseClient = databaseClientBuilder.namedParameters(false).build();

		databaseClient.sql("SELECT * FROM table WHERE key = $1").bind(0,
				Parameter.empty(String.class)).then().as(
						StepVerifier::create).verifyComplete();

		verify(statement).bind(0, Parameters.in(String.class));

		databaseClient.sql("SELECT * FROM table WHERE key = $1").bind("$1",
				Parameter.empty(String.class)).then().as(
						StepVerifier::create).verifyComplete();

		verify(statement).bind("$1", Parameters.in(String.class));
	}

	@Test
	void executeShouldBindNamedNullValues() {
		Statement statement = mockStatementFor("SELECT * FROM table WHERE key = $1");
		DatabaseClient databaseClient = databaseClientBuilder.build();

		databaseClient.sql("SELECT * FROM table WHERE key = :key").bindNull("key",
				String.class).then().as(StepVerifier::create).verifyComplete();

		verify(statement).bind(0, Parameters.in(String.class));
	}

	@Test
	void executeShouldBindNamedValuesFromIndexes() {
		Statement statement = mockStatementFor(
				"SELECT id, name, manual FROM legoset WHERE name IN ($1, $2, $3)");

		DatabaseClient databaseClient = databaseClientBuilder.build();

		databaseClient.sql(
				"SELECT id, name, manual FROM legoset WHERE name IN (:name)").bind(0,
						Arrays.asList("unknown", "dunno", "other")).then().as(
								StepVerifier::create).verifyComplete();

		verify(statement).bind(0, "unknown");
		verify(statement).bind(1, "dunno");
		verify(statement).bind(2, "other");
		verify(statement).execute();
		verifyNoMoreInteractions(statement);
	}

	@Test
	@SuppressWarnings("deprecation")
	void executeShouldBindValues() {
		Statement statement = mockStatementFor("SELECT * FROM table WHERE key = $1");
		DatabaseClient databaseClient = databaseClientBuilder.build();

		databaseClient.sql("SELECT * FROM table WHERE key = $1").bind(0,
				Parameter.from("foo")).then().as(StepVerifier::create).verifyComplete();

		verify(statement).bind(0, Parameters.in("foo"));

		databaseClient.sql("SELECT * FROM table WHERE key = $1").bind("$1",
				"foo").then().as(StepVerifier::create).verifyComplete();

		verify(statement).bind("$1", Parameters.in("foo"));
	}

	@Test
	void executeShouldBindNamedValuesByIndex() {
		Statement statement = mockStatementFor("SELECT * FROM table WHERE key = $1");
		DatabaseClient databaseClient = databaseClientBuilder.build();

		databaseClient.sql("SELECT * FROM table WHERE key = :key").bind("key",
				"foo").then().as(StepVerifier::create).verifyComplete();

		verify(statement).bind(0, Parameters.in("foo"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void rowsUpdatedShouldEmitSingleValue() {
		Result result = mock();
		when(result.getRowsUpdated()).thenReturn(Mono.empty(), Mono.just(2L), Flux.just(1L, 2L, 3L));
		mockStatementFor("DROP TABLE tab;", result);

		DatabaseClient databaseClient = databaseClientBuilder.build();

		databaseClient.sql("DROP TABLE tab;").fetch().rowsUpdated().as(
				StepVerifier::create).expectNextCount(1).verifyComplete();

		databaseClient.sql("DROP TABLE tab;").fetch().rowsUpdated().as(
				StepVerifier::create).expectNextCount(1).verifyComplete();

		databaseClient.sql("DROP TABLE tab;").fetch().rowsUpdated().as(
				StepVerifier::create).expectNextCount(1).verifyComplete();
	}

	@Test
	void selectShouldEmitFirstValue() {
		MockRowMetadata metadata = MockRowMetadata.builder().columnMetadata(
				MockColumnMetadata.builder().name("name").javaType(String.class).build()).build();

		MockResult result = MockResult.builder().row(
					MockRow.builder().identified(0, Object.class, "Walter").metadata(metadata).build(),
					MockRow.builder().identified(0, Object.class, "White").metadata(metadata).build()
				).build();

		mockStatementFor("SELECT * FROM person", result);

		DatabaseClient databaseClient = databaseClientBuilder.build();

		databaseClient.sql("SELECT * FROM person").map(row -> row.get(0))
				.first()
				.as(StepVerifier::create)
				.expectNext("Walter")
				.verifyComplete();
	}

	@Test
	void selectShouldEmitAllValues() {
		MockRowMetadata metadata = MockRowMetadata.builder().columnMetadata(
				MockColumnMetadata.builder().name("name").javaType(String.class).build()).build();

		MockResult result = MockResult.builder().row(
					MockRow.builder().identified(0, Object.class, "Walter").metadata(metadata).build(),
					MockRow.builder().identified(0, Object.class, "White").metadata(metadata).build()
				).build();

		mockStatementFor("SELECT * FROM person", result);

		DatabaseClient databaseClient = databaseClientBuilder.build();

		databaseClient.sql("SELECT * FROM person").map(row -> row.get(0))
				.all()
				.as(StepVerifier::create)
				.expectNext("Walter")
				.expectNext("White")
				.verifyComplete();
	}

	@Test
	void selectOneShouldFailWithException() {
		MockRowMetadata metadata = MockRowMetadata.builder().columnMetadata(
				MockColumnMetadata.builder().name("name").javaType(String.class).build()).build();

		MockResult result = MockResult.builder().row(
					MockRow.builder().identified(0, Object.class, "Walter").metadata(metadata).build(),
					MockRow.builder().identified(0, Object.class, "White").metadata(metadata).build()
				).build();

		mockStatementFor("SELECT * FROM person", result);

		DatabaseClient databaseClient = databaseClientBuilder.build();

		databaseClient.sql("SELECT * FROM person").map(row -> row.get(0))
				.one()
				.as(StepVerifier::create)
				.verifyError(IncorrectResultSizeDataAccessException.class);
	}

	@Test
	void shouldApplyExecuteFunction() {
		Statement statement = mockStatement();
		MockResult result = mockSingleColumnResult(
				MockRow.builder().identified(0, Object.class, "Walter"));

		DatabaseClient databaseClient = databaseClientBuilder.executeFunction(
				stmnt -> Mono.just(result)).build();

		databaseClient.sql("SELECT").fetch().all().as(
				StepVerifier::create).expectNextCount(1).verifyComplete();

		verifyNoInteractions(statement);
	}

	@Test
	void shouldApplyPreparedOperation() {
		MockResult result = mockSingleColumnResult(
				MockRow.builder().identified(0, Object.class, "Walter"));
		Statement statement = mockStatementFor("SELECT * FROM person", result);

		DatabaseClient databaseClient = databaseClientBuilder.build();

		databaseClient.sql(new PreparedOperation<String>() {

			@Override
			public String toQuery() {
				return "SELECT * FROM person";
			}

			@Override
			public String getSource() {
				return "SELECT";
			}

			@Override
			public void bindTo(BindTarget target) {
				target.bind("index", "value");
			}
		}).fetch().all().as(
				StepVerifier::create).expectNextCount(1).verifyComplete();

		verify(statement).bind("index", "value");
	}

	@Test
	void shouldApplyStatementFilterFunctions() {
		MockResult result = MockResult.builder().build();
		Statement statement = mockStatement(result);
		DatabaseClient databaseClient = databaseClientBuilder.build();

		databaseClient.sql("SELECT").filter(
				(s, next) -> next.execute(s.returnGeneratedValues("foo"))).filter(
						(s, next) -> next.execute(
								s.returnGeneratedValues("bar"))).fetch().all().as(
										StepVerifier::create).verifyComplete();

		InOrder inOrder = inOrder(statement);
		inOrder.verify(statement).returnGeneratedValues("foo");
		inOrder.verify(statement).returnGeneratedValues("bar");
		inOrder.verify(statement).execute();
		inOrder.verifyNoMoreInteractions();
	}

	@Test
	void shouldApplySimpleStatementFilterFunctions() {
		MockResult result = mockSingleColumnEmptyResult();
		Statement statement = mockStatement(result);
		DatabaseClient databaseClient = databaseClientBuilder.build();

		databaseClient.sql("SELECT").filter(
				s -> s.returnGeneratedValues("foo")).filter(
						s -> s.returnGeneratedValues("bar")).fetch().all().as(
								StepVerifier::create).verifyComplete();

		InOrder inOrder = inOrder(statement);
		inOrder.verify(statement).returnGeneratedValues("foo");
		inOrder.verify(statement).returnGeneratedValues("bar");
		inOrder.verify(statement).execute();
		inOrder.verifyNoMoreInteractions();
	}

	@Test
	void sqlSupplierInvocationIsDeferredUntilSubscription() {
		// We'll have either 2 or 3 rows, depending on the subscription and the generated SQL
		MockRowMetadata metadata = MockRowMetadata.builder().columnMetadata(
				MockColumnMetadata.builder().name("id").javaType(Integer.class).build()).build();
		final MockRow row1 = MockRow.builder().metadata(metadata)
				.identified("id", Integer.class, 1).build();
		final MockRow row2 = MockRow.builder().metadata(metadata)
				.identified("id", Integer.class, 2).build();
		final MockRow row3 = MockRow.builder().metadata(metadata)
				.identified("id", Integer.class, 3).build();
		// Set up 2 mock statements
		mockStatementFor("SELECT id FROM test WHERE id < '3'", MockResult.builder()
				.row(row1, row2).build());
		mockStatementFor("SELECT id FROM test WHERE id < '4'", MockResult.builder()
				.row(row1, row2, row3).build());
		// Create the client
		DatabaseClient databaseClient = this.databaseClientBuilder.build();

		AtomicInteger invoked = new AtomicInteger();
		// Assemble a publisher, but don't subscribe yet
		Mono<List<Integer>> operation = databaseClient
				.sql(() -> {
					int idMax = 2 + invoked.incrementAndGet();
					return String.format("SELECT id FROM test WHERE id < '%s'", idMax);
				})
				.map(r -> r.get("id", Integer.class))
				.all()
				.collectList();

		assertThat(invoked).as("invoked (before subscription)").hasValue(0);

		List<Integer> rows = operation.block();
		assertThat(invoked).as("invoked (after 1st subscription)").hasValue(1);
		assertThat(rows).containsExactly(1, 2);

		rows = operation.block();
		assertThat(invoked).as("invoked (after 2nd subscription)").hasValue(2);
		assertThat(rows).containsExactly(1, 2, 3);
	}


	private Statement mockStatement() {
		return mockStatementFor(null, null);
	}

	private Statement mockStatement(Result result) {
		return mockStatementFor(null, result);
	}

	private Statement mockStatementFor(String sql) {
		return mockStatementFor(sql, null);
	}

	private Statement mockStatementFor(@Nullable String sql, @Nullable Result result) {
		Statement statement = mock();
		when(connection.createStatement(sql == null ? anyString() : eq(sql))).thenReturn(statement);
		when(statement.returnGeneratedValues(anyString())).thenReturn(statement);
		when(statement.returnGeneratedValues()).thenReturn(statement);
		doReturn(result == null ? Mono.empty() : Flux.just(result)).when(statement).execute();
		return statement;
	}

	private MockResult mockSingleColumnEmptyResult() {
		return mockSingleColumnResult(null);
	}

	/**
	 * Mocks a {@link Result} with a single column "name" and a single row if a non-null
	 * row is provided.
	 */
	private MockResult mockSingleColumnResult(@Nullable MockRow.Builder row) {
		MockResult.Builder resultBuilder = MockResult.builder();
		if (row != null) {
			MockRowMetadata metadata = MockRowMetadata.builder().columnMetadata(
					MockColumnMetadata.builder().name("name").javaType(String.class).build()).build();
			resultBuilder = resultBuilder.row(row.metadata(metadata).build());
		}
		return resultBuilder.build();
	}

}
