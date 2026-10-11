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

import io.r2dbc.h2.H2Connection;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.IsolationLevel;
import io.r2dbc.spi.R2dbcNonTransientResourceException;
import io.r2dbc.spi.Wrapped;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SingleConnectionFactory}.
 *
 * @author Mark Paluch
 */
class SingleConnectionFactoryTests {

	@Test
	void shouldAllocateSameConnection() {
		SingleConnectionFactory factory = new SingleConnectionFactory("r2dbc:h2:mem:///foo", false);

		Mono<? extends Connection> cf1 = factory.create();
		Mono<? extends Connection> cf2 = factory.create();

		Connection c1 = cf1.block();
		Connection c2 = cf2.block();
		assertThat(c1).isSameAs(c2);

		factory.destroy();
	}

	@Test
	void shouldApplyAutoCommit() {
		SingleConnectionFactory factory = new SingleConnectionFactory("r2dbc:h2:mem:///foo", false);
		factory.setAutoCommit(false);

		factory.create().as(StepVerifier::create)
				.consumeNextWith(actual -> assertThat(actual.isAutoCommit()).isFalse())
				.verifyComplete();

		factory.setAutoCommit(true);
		factory.create().as(StepVerifier::create)
				.consumeNextWith(actual -> assertThat(actual.isAutoCommit()).isTrue())
				.verifyComplete();

		factory.destroy();
	}

	@Test
	@SuppressWarnings("rawtypes")
	void shouldSuppressClose() {
		SingleConnectionFactory factory = new SingleConnectionFactory("r2dbc:h2:mem:///foo", true);
		Connection connection = factory.create().block();

		StepVerifier.create(connection.close()).verifyComplete();
		assertThat(connection).isInstanceOf(Wrapped.class);
		assertThat(((Wrapped) connection).unwrap()).isInstanceOf(H2Connection.class);

		StepVerifier.create(
				connection.setTransactionIsolationLevel(IsolationLevel.READ_COMMITTED))
				.verifyComplete();

		factory.destroy();
	}

	@Test
	void shouldNotSuppressClose() {
		SingleConnectionFactory factory = new SingleConnectionFactory("r2dbc:h2:mem:///foo", false);
		Connection connection = factory.create().block();

		StepVerifier.create(connection.close()).verifyComplete();
		StepVerifier.create(connection.setTransactionIsolationLevel(IsolationLevel.READ_COMMITTED))
			.verifyError(R2dbcNonTransientResourceException.class);

		factory.destroy();
	}

	@Test
	void resetConnectionShouldObtainNewConnectionOnNextAccess() {
		SingleConnectionFactory factory = new SingleConnectionFactory("r2dbc:h2:mem:///foo", false);
		Connection connection1 = factory.create().block();

		factory.resetConnection().as(StepVerifier::create).verifyComplete();
		Connection connection2 = factory.create().block();

		assertThat(connection2).isNotSameAs(connection1);
		StepVerifier.create(connection2.setTransactionIsolationLevel(IsolationLevel.READ_COMMITTED))
				.verifyComplete();

		factory.destroy();
	}

	@Test
	void resetConnectionShouldApplyToConnectionMonoObtainedBeforeReset() {
		SingleConnectionFactory factory = new SingleConnectionFactory("r2dbc:h2:mem:///foo", false);
		Mono<? extends Connection> obtainedBeforeReset = factory.create();
		Connection connection1 = factory.create().block();

		factory.resetConnection().as(StepVerifier::create).verifyComplete();
		Connection connection2 = factory.create().block();

		assertThat(connection2).isNotSameAs(connection1);
		assertThat(obtainedBeforeReset.block()).isSameAs(connection2);
		assertThat(factory.create().block()).isSameAs(connection2);

		factory.destroy();
	}

	@Test
	void resetConnectionShouldApplyOnResubscription() {
		SingleConnectionFactory factory = new SingleConnectionFactory("r2dbc:h2:mem:///foo", false);
		Mono<? extends Connection> connectionMono = factory.create();
		Connection connection1 = connectionMono.block();

		factory.resetConnection().as(StepVerifier::create).verifyComplete();
		Connection connection2 = connectionMono.block();

		assertThat(connection2).isNotSameAs(connection1);
		StepVerifier.create(connection2.setTransactionIsolationLevel(IsolationLevel.READ_COMMITTED))
				.verifyComplete();

		factory.destroy();
	}

	@Test
	void failedConnectionAttemptShouldNotBeCached() {
		ConnectionFactory targetFactory = mock();
		Connection connectionMock = mock();
		doReturn(Mono.error(new R2dbcNonTransientResourceException("Database not available")))
				.doReturn(Mono.just(connectionMock))
				.when(targetFactory).create();

		SingleConnectionFactory factory = new SingleConnectionFactory(targetFactory);

		factory.create().as(StepVerifier::create)
				.verifyError(R2dbcNonTransientResourceException.class);
		factory.create().as(StepVerifier::create)
				.consumeNextWith(actual -> assertThat(actual).isSameAs(connectionMock))
				.verifyComplete();
		factory.create().as(StepVerifier::create)
				.consumeNextWith(actual -> assertThat(actual).isSameAs(connectionMock))
				.verifyComplete();

		verify(targetFactory, times(2)).create();
	}

	@Test
	void releaseConnectionShouldNotCloseConnection() {
		Connection connectionMock = mock();
		ConnectionFactoryMetadata metadata = mock();

		SingleConnectionFactory factory = new SingleConnectionFactory(connectionMock, metadata, true);
		Connection connection = factory.create().block();

		ConnectionFactoryUtils.releaseConnection(connection, factory)
				.as(StepVerifier::create)
				.verifyComplete();

		verify(connectionMock, never()).close();
	}

	@Test
	void releaseConnectionShouldCloseUnrelatedConnection() {
		Connection connectionMock = mock();
		Connection otherConnection = mock();
		ConnectionFactoryMetadata metadata = mock();
		when(otherConnection.close()).thenReturn(Mono.empty());

		SingleConnectionFactory factory = new SingleConnectionFactory(connectionMock, metadata, false);
		factory.create().as(StepVerifier::create).expectNextCount(1).verifyComplete();

		ConnectionFactoryUtils.releaseConnection(otherConnection, factory)
				.as(StepVerifier::create)
				.verifyComplete();

		verify(otherConnection).close();
	}

}
