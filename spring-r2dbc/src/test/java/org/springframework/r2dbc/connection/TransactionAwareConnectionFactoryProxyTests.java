/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.connection;

import java.util.concurrent.atomic.AtomicReference;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Wrapped;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.transaction.reactive.TransactionalOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TransactionAwareConnectionFactoryProxy}.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 */
class TransactionAwareConnectionFactoryProxyTests {

	ConnectionFactory connectionFactoryMock = mock();

	Connection connectionMock1 = mock();

	Connection connectionMock2 = mock();

	Connection connectionMock3 = mock();

	R2dbcTransactionManager tm;


	@BeforeEach
	@SuppressWarnings({"rawtypes", "unchecked"})
	void before() {
		when(connectionFactoryMock.create()).thenReturn((Mono) Mono.just(connectionMock1),
				(Mono) Mono.just(connectionMock2), (Mono) Mono.just(connectionMock3));
		tm = new R2dbcTransactionManager(connectionFactoryMock);
	}


	@Test
	void createShouldWrapConnection() {
		new TransactionAwareConnectionFactoryProxy(connectionFactoryMock).create()
				.as(StepVerifier::create)
				.consumeNextWith(connection -> assertThat(connection).isInstanceOf(Wrapped.class))
				.verifyComplete();
	}

	@Test
	void unwrapShouldReturnTargetConnection() {
		new TransactionAwareConnectionFactoryProxy(connectionFactoryMock).create()
				.map(Wrapped.class::cast).as(StepVerifier::create)
				.consumeNextWith(wrapped -> assertThat(wrapped.unwrap()).isEqualTo(connectionMock1))
				.verifyComplete();
	}

	@Test
	void unwrapShouldReturnTargetConnectionEvenWhenClosed() {
		when(connectionMock1.close()).thenReturn(Mono.empty());

		new TransactionAwareConnectionFactoryProxy(connectionFactoryMock).create()
				.map(Connection.class::cast).flatMap(
						connection -> Mono.from(connection.close()).then(Mono.just(connection))).as(
								StepVerifier::create)
				.consumeNextWith(wrapped -> assertThat(((Wrapped<?>) wrapped).unwrap()).isEqualTo(connectionMock1))
				.verifyComplete();
	}

	@Test
	void getTargetConnectionShouldReturnTargetConnection() {
		new TransactionAwareConnectionFactoryProxy(connectionFactoryMock).create()
				.as(StepVerifier::create)
				.consumeNextWith(connection ->
						assertThat(ConnectionFactoryUtils.getTargetConnection(connection))
								.isSameAs(connectionMock1))
				.verifyComplete();
	}

	@Test
	void getMetadataShouldThrowsErrorEvenWhenClosed() {
		when(connectionMock1.close()).thenReturn(Mono.empty());

		new TransactionAwareConnectionFactoryProxy(connectionFactoryMock).create()
				.map(Connection.class::cast).flatMap(
						connection -> Mono.from(connection.close())
								.then(Mono.just(connection))).as(StepVerifier::create)
				.consumeNextWith(connection -> assertThatIllegalStateException().isThrownBy(
						connection::getMetadata)).verifyComplete();
	}

	@Test
	void hashCodeShouldReturnProxyHash() {
		new TransactionAwareConnectionFactoryProxy(connectionFactoryMock).create()
				.map(Connection.class::cast).as(StepVerifier::create)
				.consumeNextWith(connection -> assertThat(connection.hashCode()).isEqualTo(
						System.identityHashCode(connection))).verifyComplete();
	}

	@Test
	void equalsShouldCompareCorrectly() {
		new TransactionAwareConnectionFactoryProxy(connectionFactoryMock).create()
				.map(Connection.class::cast).as(StepVerifier::create)
				.consumeNextWith(connection -> {
					assertThat(connection).isEqualTo(connection);
					assertThat(connection).isNotEqualTo(connectionMock1);
				}).verifyComplete();
	}

	@Test
	void shouldEmitBoundConnection() {
		when(connectionMock1.beginTransaction(ArgumentMatchers.any())).thenReturn(Mono.empty());
		when(connectionMock1.commitTransaction()).thenReturn(Mono.empty());
		when(connectionMock1.close()).thenReturn(Mono.empty());

		TransactionalOperator rxtx = TransactionalOperator.create(tm);
		AtomicReference<Connection> transactionalConnection = new AtomicReference<>();

		TransactionAwareConnectionFactoryProxy proxyCf = new TransactionAwareConnectionFactoryProxy(
				connectionFactoryMock);

		ConnectionFactoryUtils.getConnection(connectionFactoryMock)
				.doOnNext(transactionalConnection::set).flatMap(connection -> proxyCf.create()
				.doOnNext(wrappedConnection -> assertThat(((Wrapped<?>) wrappedConnection).unwrap()).isSameAs(connection)))
				.flatMapMany(Connection::close)
				.as(rxtx::transactional)
				.as(StepVerifier::create)
				.verifyComplete();

		verify(connectionFactoryMock, times(1)).create();
		verify(connectionMock1, times(1)).close();
		verifyNoInteractions(connectionMock2);
		verifyNoInteractions(connectionMock3);
	}

}
