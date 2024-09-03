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

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for transactional integration tests for {@link DatabaseClient}.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 */
abstract class AbstractTransactionalDatabaseClientIntegrationTests  {

	private ConnectionFactory connectionFactory;

	AnnotationConfigApplicationContext context;

	DatabaseClient databaseClient;

	R2dbcTransactionManager transactionManager;

	TransactionalOperator rxtx;


	@BeforeEach
	public void before() {
		connectionFactory = createConnectionFactory();

		context = new AnnotationConfigApplicationContext();
		context.getBeanFactory().registerResolvableDependency(ConnectionFactory.class, connectionFactory);
		context.register(Config.class);
		context.refresh();

		Mono.from(connectionFactory.create())
				.flatMapMany(connection -> Flux.from(connection.createStatement("DROP TABLE legoset").execute())
						.flatMap(Result::getRowsUpdated)
						.onErrorResume(e -> Mono.empty())
						.thenMany(connection.createStatement(getCreateTableStatement()).execute())
						.flatMap(Result::getRowsUpdated).thenMany(connection.close())).as(StepVerifier::create).verifyComplete();

		databaseClient = DatabaseClient.create(connectionFactory);
		transactionManager = new R2dbcTransactionManager(connectionFactory);
		rxtx = TransactionalOperator.create(transactionManager);
	}

	@AfterEach
	public void tearDown() {
		context.close();
	}


	/**
	 * Create a {@link ConnectionFactory} to be used in this test.
	 * @return the {@link ConnectionFactory} to be used in this test.
	 */
	protected abstract ConnectionFactory createConnectionFactory();

	/**
	 * Return the CREATE TABLE statement for table {@code legoset} with the following three columns:
	 * <ul>
	 * <li>id integer (primary key), not null</li>
	 * <li>name varchar(255), nullable</li>
	 * <li>manual integer, nullable</li>
	 * </ul>
	 *
	 * @return the CREATE TABLE statement for table {@code legoset} with three columns.
	 */
	protected abstract String getCreateTableStatement();

	/**
	 * Get a parameterized {@code INSERT INTO legoset} statement setting id, name, and manual values.
	 */
	protected String getInsertIntoLegosetStatement() {
		return "INSERT INTO legoset (id, name, manual) VALUES(:id, :name, :manual)";
	}


	@Test
	public void executeInsertInTransaction() {
		Flux<Long> longFlux = databaseClient
				.sql(getInsertIntoLegosetStatement())
				.bind(0, 42055)
				.bind(1, "SCHAUFELRADBAGGER")
				.bindNull(2, Integer.class)
				.fetch().rowsUpdated().flux().as(rxtx::transactional);

		longFlux.as(StepVerifier::create)
				.expectNext(1L)
				.verifyComplete();

		databaseClient
				.sql("SELECT id FROM legoset")
				.fetch()
				.first()
				.as(StepVerifier::create)
				.assertNext(actual -> assertThat(actual).hasEntrySatisfying("id", numberOf(42055)))
				.verifyComplete();
	}

	@Test
	public void shouldRollbackTransaction() {
		Mono<Object> integerFlux = databaseClient.sql(getInsertIntoLegosetStatement())
				.bind(0, 42055)
				.bind(1, "SCHAUFELRADBAGGER")
				.bindNull(2, Integer.class)
				.fetch().rowsUpdated()
				.then(Mono.error(new IllegalStateException("failed")))
				.as(rxtx::transactional);

		integerFlux.as(StepVerifier::create)
				.expectError(IllegalStateException.class)
				.verify();

		databaseClient
				.sql("SELECT id FROM legoset")
				.fetch()
				.first()
				.as(StepVerifier::create)
				.verifyComplete();
	}

	@Test
	public void shouldRollbackTransactionUsingTransactionalOperator() {
		DatabaseClient databaseClient = DatabaseClient.create(connectionFactory);

		TransactionalOperator transactionalOperator = TransactionalOperator
				.create(new R2dbcTransactionManager(connectionFactory), new DefaultTransactionDefinition());

		Flux<Integer> integerFlux = databaseClient.sql(getInsertIntoLegosetStatement())
				.bind(0, 42055)
				.bind(1, "SCHAUFELRADBAGGER")
				.bindNull(2, Integer.class)
				.fetch().rowsUpdated()
				.thenMany(Mono.fromSupplier(() -> {
					throw new IllegalStateException("failed");
				}));

		integerFlux.as(transactionalOperator::transactional)
				.as(StepVerifier::create)
				.expectError(IllegalStateException.class)
				.verify();

		databaseClient
				.sql("SELECT id FROM legoset")
				.fetch()
				.first()
				.as(StepVerifier::create)
				.verifyComplete();
	}

	private Condition<? super Object> numberOf(int expected) {
		return new Condition<>(object -> object instanceof Number num &&
				num.intValue() == expected, "Number %d", expected);
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Autowired GenericApplicationContext context;

		@Bean
		ReactiveTransactionManager txMgr(ConnectionFactory connectionFactory) {
			return new R2dbcTransactionManager(connectionFactory);
		}

		@Bean
		TransactionalOperator transactionalOperator(ReactiveTransactionManager transactionManager) {
			return TransactionalOperator.create(transactionManager);
		}
	}

}
