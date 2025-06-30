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

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.Wrapped;
import reactor.core.publisher.Mono;

import org.springframework.util.Assert;

/**
 * R2DBC {@link ConnectionFactory} implementation that delegates
 * all calls to a given target {@link ConnectionFactory}.
 *
 * <p>This class is meant to be subclassed, with subclasses overriding
 * only those methods (such as {@link #create()}) that should not simply
 * delegate to the target {@link ConnectionFactory}.
 *
 * @author Mark Paluch
 * @since 5.3
 * @see #create()
 */
public class DelegatingConnectionFactory implements ConnectionFactory, Wrapped<ConnectionFactory> {

	private final ConnectionFactory targetConnectionFactory;


	/**
	 * Create a new DelegatingConnectionFactory.
	 * @param targetConnectionFactory the target ConnectionFactory
	 */
	public DelegatingConnectionFactory(ConnectionFactory targetConnectionFactory) {
		Assert.notNull(targetConnectionFactory, "ConnectionFactory must not be null");
		this.targetConnectionFactory = targetConnectionFactory;
	}


	/**
	 * Return the target ConnectionFactory that this ConnectionFactory delegates to.
	 */
	public ConnectionFactory getTargetConnectionFactory() {
		return this.targetConnectionFactory;
	}

	@Override
	public Mono<? extends Connection> create() {
		return Mono.from(this.targetConnectionFactory.create());
	}

	@Override
	public ConnectionFactoryMetadata getMetadata() {
		return this.targetConnectionFactory.getMetadata();
	}

	@Override
	public ConnectionFactory unwrap() {
		return this.targetConnectionFactory;
	}

}
