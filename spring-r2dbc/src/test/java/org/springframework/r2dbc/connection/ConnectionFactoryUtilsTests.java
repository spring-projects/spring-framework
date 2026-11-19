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

package org.springframework.r2dbc.connection;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.R2dbcBadGrammarException;
import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
import io.r2dbc.spi.R2dbcException;
import io.r2dbc.spi.R2dbcNonTransientResourceException;
import io.r2dbc.spi.R2dbcPermissionDeniedException;
import io.r2dbc.spi.R2dbcRollbackException;
import io.r2dbc.spi.R2dbcTimeoutException;
import io.r2dbc.spi.R2dbcTransientResourceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.r2dbc.BadSqlGrammarException;
import org.springframework.r2dbc.UncategorizedR2dbcException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ConnectionFactoryUtils}.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 */
class ConnectionFactoryUtilsTests {

	@Test
	void explicitlyReleasedConnectionIsReusedAndClosedOnTransactionCompletion() {
		AtomicInteger transactionConnectionCloses = new AtomicInteger();
		TransactionalOperator operator = transactionalOperator(transactionConnectionCloses);
		Connection connection = mock();
		AtomicInteger connectionCloses = closeCounter(connection);
		ConnectionFactory connectionFactory = mock();
		when(connectionFactory.create()).thenAnswer(invocation -> Mono.just(connection));

		Mono<Connection> useAndRelease = ConnectionFactoryUtils.getConnection(connectionFactory)
				.flatMap(con -> ConnectionFactoryUtils.releaseConnection(con, connectionFactory).thenReturn(con));

		StepVerifier.create(useAndRelease.then(useAndRelease).as(operator::transactional))
				.expectNext(connection)
				.verifyComplete();
		assertThat(connectionCloses).hasValue(1);
		assertThat(transactionConnectionCloses).hasValue(1);
		verify(connectionFactory).create();
	}

	@Test
	void connectionReleasedBeforeSuspensionIsClosedAndReplacedAfterResume() {
		AtomicInteger transactionConnectionCloses = new AtomicInteger();
		TransactionalOperator operator = transactionalOperator(transactionConnectionCloses);
		TransactionalOperator requiresNewOperator = transactionalOperator(
				transactionConnectionCloses, TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		Connection firstConnection = mock();
		Connection secondConnection = mock();
		AtomicInteger firstConnectionCloses = closeCounter(firstConnection);
		AtomicInteger secondConnectionCloses = closeCounter(secondConnection);
		ConnectionFactory connectionFactory = connectionFactory(firstConnection, secondConnection);

		Mono<Connection> useAndRelease = ConnectionFactoryUtils.getConnection(connectionFactory)
				.flatMap(con -> ConnectionFactoryUtils.releaseConnection(con, connectionFactory).thenReturn(con));
		Mono<Void> suspension = Mono.<Void>empty().as(requiresNewOperator::transactional);

		StepVerifier.create(useAndRelease.then(suspension).then(useAndRelease).as(operator::transactional))
				.expectNext(secondConnection)
				.verifyComplete();
		assertThat(firstConnectionCloses).hasValue(1);
		assertThat(secondConnectionCloses).hasValue(1);
	}

	private static TransactionalOperator transactionalOperator(AtomicInteger connectionCloses) {
		return transactionalOperator(connectionCloses, TransactionDefinition.PROPAGATION_REQUIRED);
	}

	private static TransactionalOperator transactionalOperator(AtomicInteger connectionCloses, int propagationBehavior) {
		Connection connection = mock();
		when(connection.beginTransaction(any(io.r2dbc.spi.TransactionDefinition.class))).thenReturn(Mono.empty());
		when(connection.commitTransaction()).thenReturn(Mono.empty());
		when(connection.rollbackTransaction()).thenReturn(Mono.empty());
		when(connection.close()).thenReturn(Mono.fromRunnable(connectionCloses::incrementAndGet));
		return TransactionalOperator.create(new R2dbcTransactionManager(connectionFactory(connection)),
				new DefaultTransactionDefinition(propagationBehavior));
	}

	private static ConnectionFactory connectionFactory(Connection... connections) {
		ConnectionFactory connectionFactory = mock();
		AtomicInteger index = new AtomicInteger();
		when(connectionFactory.create()).thenAnswer(invocation -> Mono.just(connections[index.getAndIncrement()]));
		return connectionFactory;
	}

	private static AtomicInteger closeCounter(Connection connection) {
		AtomicInteger closeCalls = new AtomicInteger();
		when(connection.close()).thenReturn(Mono.fromRunnable(closeCalls::incrementAndGet));
		return closeCalls;
	}

	@Test
	void shouldTranslateTransientResourceException() {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new R2dbcTransientResourceException(""));
		assertThat(exception).isExactlyInstanceOf(TransientDataAccessResourceException.class);
	}

	@Test
	void shouldTranslateRollbackException() {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new R2dbcRollbackException());
		assertThat(exception).isExactlyInstanceOf(PessimisticLockingFailureException.class);

		exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new R2dbcRollbackException("reason", "40001"));
		assertThat(exception).isExactlyInstanceOf(CannotAcquireLockException.class);
	}

	@Test
	void shouldTranslateTimeoutException() {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new R2dbcTimeoutException());
		assertThat(exception).isExactlyInstanceOf(QueryTimeoutException.class);
	}

	@Test
	void shouldNotTranslateUnknownExceptions() {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new MyTransientException());
		assertThat(exception).isExactlyInstanceOf(UncategorizedR2dbcException.class);
	}

	@Test
	void shouldTranslateNonTransientResourceException() {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new R2dbcNonTransientResourceException());
		assertThat(exception).isExactlyInstanceOf(DataAccessResourceFailureException.class);
	}

	@Test
	void shouldTranslateIntegrityViolationException() {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new R2dbcDataIntegrityViolationException());
		assertThat(exception).isExactlyInstanceOf(DataIntegrityViolationException.class);
	}

	static final List<Arguments> duplicateKeyErrorCodes = List.of(
			arguments("Oracle", "23505", 0),
			arguments("Oracle", "23000", 1),
			arguments("SAP HANA", "23000", 301),
			arguments("MySQL/MariaDB", "23000", 1062),
			arguments("MS SQL Server", "23000", 2601),
			arguments("MS SQL Server", "23000", 2627),
			arguments("Informix", "23000", -239),
			arguments("Informix", "23000", -268)
		);

	@ParameterizedTest
	@FieldSource("duplicateKeyErrorCodes")
	void shouldTranslateIntegrityViolationExceptionToDuplicateKeyException(String db, String sqlState, int errorCode) {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new R2dbcDataIntegrityViolationException("reason", sqlState, errorCode));
		assertThat(exception).as(db).isExactlyInstanceOf(DuplicateKeyException.class);
	}

	@Test
	void shouldTranslatePermissionDeniedException() {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new R2dbcPermissionDeniedException());
		assertThat(exception).isExactlyInstanceOf(PermissionDeniedDataAccessException.class);
	}

	@Test
	void shouldTranslateBadSqlGrammarException() {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new R2dbcBadGrammarException());
		assertThat(exception).isExactlyInstanceOf(BadSqlGrammarException.class);
	}

	@Test
	void messageGeneration() {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("TASK",
				"SOME-SQL", new R2dbcTransientResourceException("MESSAGE"));
		assertThat(exception)
				.isExactlyInstanceOf(TransientDataAccessResourceException.class)
				.hasMessage("TASK; SQL [SOME-SQL]; MESSAGE");
	}

	@Test
	void messageGenerationNullSQL() {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("TASK", null,
				new R2dbcTransientResourceException("MESSAGE"));
		assertThat(exception)
				.isExactlyInstanceOf(TransientDataAccessResourceException.class)
				.hasMessage("TASK; MESSAGE");
	}

	@Test
	void messageGenerationNullMessage() {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("TASK",
				"SOME-SQL", new R2dbcTransientResourceException());
		assertThat(exception)
				.isExactlyInstanceOf(TransientDataAccessResourceException.class)
				.hasMessage("TASK; SQL [SOME-SQL]; null");
	}


	@SuppressWarnings("serial")
	private static class MyTransientException extends R2dbcException {
	}

}
