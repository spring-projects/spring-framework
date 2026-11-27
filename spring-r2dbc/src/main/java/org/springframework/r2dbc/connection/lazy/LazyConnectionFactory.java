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
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.Wrapped;
import reactor.core.publisher.Mono;

/**
 * A {@link ConnectionFactory} implementation that returns lazy {@link Connection} proxies.
 *
 * <p>This factory returns a proxy {@link Connection} and only acquires a physical connection when a
 * database interaction is performed.
 *
 * @author Somil Jain
 * @since 6.2
 * @see LazyConnection
 */
public final class LazyConnectionFactory
		implements ConnectionFactory, Wrapped<ConnectionFactory> {

	private final ConnectionFactory targetFactory;

	public LazyConnectionFactory(ConnectionFactory targetFactory) {
		this.targetFactory = targetFactory;
	}

	/**
	 * Return a new {@link LazyConnection} that lazily obtains a physical connection.
	 * <p>
	 * The returned connection does not acquire a physical connection until an
	 * operation requiring database interaction is invoked.
	 *
	 * @return a {@code Mono} emitting the lazy connection
	 */
	@Override
	public Mono<Connection> create() {
		return Mono.fromSupplier(() -> new LazyConnection(this.targetFactory));
	}

	/**
	 * Return metadata from the underlying {@link ConnectionFactory}.
	 * <p>
	 * Accessing metadata does not trigger physical connection creation.
	 */
	@Override
	public ConnectionFactoryMetadata getMetadata() {
		return this.targetFactory.getMetadata();
	}

	@Override
	public ConnectionFactory unwrap() {
		return this.targetFactory;
	}
}
