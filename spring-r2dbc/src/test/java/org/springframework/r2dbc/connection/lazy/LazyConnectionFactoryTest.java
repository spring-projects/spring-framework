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

package org.springframework.r2dbc.connection.lazy;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Statement;
import io.r2dbc.spi.ValidationDepth;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

/**
 * Tests for {@link LazyConnectionFactory}.
 *
 * @author Somil Jain
 * @since 6.2
 */
class LazyConnectionFactoryTest {

	@Test
	void emptyTransactionDoesNotCreatePhysicalConnection() {

		ConnectionFactory targetFactory = mock(ConnectionFactory.class);
		when(targetFactory.getMetadata()).thenReturn(() -> "mock-db");

		LazyConnectionFactory lazyFactory = new LazyConnectionFactory(targetFactory);

		Mono<Void> flow = lazyFactory.create()
				.flatMap(conn ->
						Mono.from(conn.beginTransaction())
								.then(Mono.from(conn.commitTransaction()))
								.then(Mono.from(conn.close()))
				);

		StepVerifier.create(flow)
				.verifyComplete();

		verify(targetFactory, never()).create();
	}

	@Test
	void connectionIsCreatedWhenStatementIsExecuted() {

		ConnectionFactory targetFactory = mock(ConnectionFactory.class);
		Connection physicalConnection = mock(Connection.class);
		Statement statement = mock(Statement.class);

		when(targetFactory.getMetadata()).thenReturn(() -> "mock-db");
		doReturn(Mono.just(physicalConnection))
				.when(targetFactory)
				.create();

		when(physicalConnection.beginTransaction()).thenReturn(Mono.empty());
		when(physicalConnection.commitTransaction()).thenReturn(Mono.empty());
		when(physicalConnection.close()).thenReturn(Mono.empty());

		when(physicalConnection.createStatement("SELECT 1")).thenReturn(statement);
		when(statement.execute()).thenReturn(Flux.empty());

		LazyConnectionFactory lazyFactory = new LazyConnectionFactory(targetFactory);

		Mono<Void> flow = lazyFactory.create()
				.flatMap(conn ->
						Mono.from(conn.beginTransaction())
								.then(Flux.from(conn.createStatement("SELECT 1").execute()).then())
								.then(Mono.from(conn.commitTransaction()))
								.then(Mono.from(conn.close()))
				);

		StepVerifier.create(flow)
				.verifyComplete();

		verify(targetFactory, times(1)).create();
		verify(physicalConnection).beginTransaction();
		verify(physicalConnection).createStatement("SELECT 1");
		verify(physicalConnection).commitTransaction();
		verify(physicalConnection).close();
	}

	@Test
	void validateDoesNotTriggerConnectionCreation() {

		ConnectionFactory targetFactory = mock(ConnectionFactory.class);
		when(targetFactory.getMetadata()).thenReturn(() -> "mock-db");

		LazyConnectionFactory lazyFactory = new LazyConnectionFactory(targetFactory);

		Mono<Boolean> validate = lazyFactory.create()
				.flatMap(conn -> Mono.from(conn.validate(ValidationDepth.LOCAL)));

		StepVerifier.create(validate)
				.expectNext(true)
				.verifyComplete();

		verify(targetFactory, never()).create();
	}
}
