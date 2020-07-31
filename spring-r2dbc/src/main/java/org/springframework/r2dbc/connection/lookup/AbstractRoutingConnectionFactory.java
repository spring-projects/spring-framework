/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.r2dbc.connection.lookup;

import java.util.HashMap;
import java.util.Map;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Abstract {@link ConnectionFactory} implementation that routes
 * {@link #create()} calls to one of various target
 * {@link ConnectionFactory factories} based on a lookup key.
 * The latter is typically (but not necessarily) determined from some
 * subscriber context.
 *
 * <p> Allows to configure a {@link #setDefaultTargetConnectionFactory(Object)
 * default ConnectionFactory} as fallback.
 *
 * <p> Calls to {@link #getMetadata()} are routed to the
 * {@link #setDefaultTargetConnectionFactory(Object) default ConnectionFactory}
 * if configured.
 *
 * @author Mark Paluch
 * @author Jens Schauder
 * @since 5.3
 * @see #setTargetConnectionFactories
 * @see #setDefaultTargetConnectionFactory
 * @see #determineCurrentLookupKey()
 */
public abstract class AbstractRoutingConnectionFactory implements ConnectionFactory, InitializingBean {

	private static final Object FALLBACK_MARKER = new Object();


	@Nullable
	private Map<?, ?> targetConnectionFactories;

	@Nullable
	private Object defaultTargetConnectionFactory;

	private boolean lenientFallback = true;

	private ConnectionFactoryLookup connectionFactoryLookup = new MapConnectionFactoryLookup();

	@Nullable
	private Map<Object, ConnectionFactory> resolvedConnectionFactories;

	@Nullable
	private ConnectionFactory resolvedDefaultConnectionFactory;


	/**
	 * Specify the map of target {@link ConnectionFactory ConnectionFactories},
	 * with the lookup key as key. The mapped value can either be a corresponding
	 * {@link ConnectionFactory} instance or a connection factory name String (to be
	 * resolved via a {@link #setConnectionFactoryLookup ConnectionFactoryLookup}).
	 *
	 * <p>The key can be of arbitrary type; this class implements the generic lookup
	 * process only. The concrete key representation will be handled by
	 * {@link #resolveSpecifiedLookupKey(Object)} and {@link #determineCurrentLookupKey()}.
	 */
	public void setTargetConnectionFactories(Map<?, ?> targetConnectionFactories) {
		this.targetConnectionFactories = targetConnectionFactories;
	}

	/**
	 * Specify the default target {@link ConnectionFactory}, if any.
	 *
	 * <p>The mapped value can either be a corresponding {@link ConnectionFactory}
	 * instance or a connection factory name {@link String} (to be resolved via a
	 * {@link #setConnectionFactoryLookup ConnectionFactoryLookup}).
	 *
	 * <p>This {@link ConnectionFactory} will be used as target if none of the keyed
	 * {@link #setTargetConnectionFactories targetConnectionFactories} match the
	 * {@link #determineCurrentLookupKey() current lookup key}.
	 */
	public void setDefaultTargetConnectionFactory(Object defaultTargetConnectionFactory) {
		this.defaultTargetConnectionFactory = defaultTargetConnectionFactory;
	}

	/**
	 * Specify whether to apply a lenient fallback to the default {@link ConnectionFactory}
	 * if no specific {@link ConnectionFactory} could be found for the current lookup key.
	 *
	 * <p>Default is {@code true}, accepting lookup keys without a corresponding entry
	 * in the target {@link ConnectionFactory} map - simply falling back to the default
	 * {@link ConnectionFactory} in that case.
	 *
	 * <p>Switch this flag to {@code false} if you would prefer the fallback to only
	 * apply when no lookup key was emitted. Lookup keys without a {@link ConnectionFactory}
	 * entry will then lead to an {@link IllegalStateException}.
	 * @see #setTargetConnectionFactories
	 * @see #setDefaultTargetConnectionFactory
	 * @see #determineCurrentLookupKey()
	 */
	public void setLenientFallback(boolean lenientFallback) {
		this.lenientFallback = lenientFallback;
	}

	/**
	 * Set the {@link ConnectionFactoryLookup} implementation to use for resolving
	 * connection factory name Strings in the {@link #setTargetConnectionFactories
	 * targetConnectionFactories} map.
	 */
	public void setConnectionFactoryLookup(ConnectionFactoryLookup connectionFactoryLookup) {
		Assert.notNull(connectionFactoryLookup, "ConnectionFactoryLookup must not be null");
		this.connectionFactoryLookup = connectionFactoryLookup;
	}

	@Override
	public void afterPropertiesSet() {

		Assert.notNull(this.targetConnectionFactories, "Property 'targetConnectionFactories' must not be null");

		this.resolvedConnectionFactories = new HashMap<>(this.targetConnectionFactories.size());
		this.targetConnectionFactories.forEach((key, value) -> {
			Object lookupKey = resolveSpecifiedLookupKey(key);
			ConnectionFactory connectionFactory = resolveSpecifiedConnectionFactory(value);
			this.resolvedConnectionFactories.put(lookupKey, connectionFactory);
		});

		if (this.defaultTargetConnectionFactory != null) {
			this.resolvedDefaultConnectionFactory = resolveSpecifiedConnectionFactory(this.defaultTargetConnectionFactory);
		}
	}

	/**
	 * Resolve the given lookup key object, as specified in the
	 * {@link #setTargetConnectionFactories targetConnectionFactories} map,
	 * into the actual lookup key to be used for matching with the
	 * {@link #determineCurrentLookupKey() current lookup key}.
	 * <p>The default implementation simply returns the given key as-is.
	 * @param lookupKey the lookup key object as specified by the user
	 * @return the lookup key as needed for matching.
	 */
	protected Object resolveSpecifiedLookupKey(Object lookupKey) {
		return lookupKey;
	}

	/**
	 * Resolve the specified connection factory object into a
	 * {@link ConnectionFactory} instance.
	 * <p>The default implementation handles {@link ConnectionFactory} instances
	 * and connection factory names (to be resolved via a
	 * {@link #setConnectionFactoryLookup ConnectionFactoryLookup}).
	 * @param connectionFactory the connection factory value object as specified in the
	 * {@link #setTargetConnectionFactories targetConnectionFactories} map
	 * @return the resolved {@link ConnectionFactory} (never {@code null})
	 * @throws IllegalArgumentException in case of an unsupported value type
	 */
	protected ConnectionFactory resolveSpecifiedConnectionFactory(Object connectionFactory)
			throws IllegalArgumentException {
		if (connectionFactory instanceof ConnectionFactory) {
			return (ConnectionFactory) connectionFactory;
		}
		else if (connectionFactory instanceof String) {
			return this.connectionFactoryLookup.getConnectionFactory((String) connectionFactory);
		}
		else {
			throw new IllegalArgumentException(
					"Illegal connection factory value - only 'io.r2dbc.spi.ConnectionFactory' and 'String' supported: "
							+ connectionFactory);
		}
	}

	@Override
	public Mono<Connection> create() {
		return determineTargetConnectionFactory() //
				.map(ConnectionFactory::create) //
				.flatMap(Mono::from);
	}

	@Override
	public ConnectionFactoryMetadata getMetadata() {
		if (this.resolvedDefaultConnectionFactory != null) {
			return this.resolvedDefaultConnectionFactory.getMetadata();
		}
		throw new UnsupportedOperationException(
				"No default ConnectionFactory configured to retrieve ConnectionFactoryMetadata");
	}

	/**
	 * Retrieve the current target {@link ConnectionFactory}. Determines the
	 * {@link #determineCurrentLookupKey() current lookup key}, performs a lookup
	 * in the {@link #setTargetConnectionFactories targetConnectionFactories} map,
	 * falls back to the specified {@link #setDefaultTargetConnectionFactory default
	 * target ConnectionFactory} if necessary.
	 * @return {@link Mono} emitting the current {@link ConnectionFactory} as
	 * per {@link #determineCurrentLookupKey()}
	 * @see #determineCurrentLookupKey()
	 */
	protected Mono<ConnectionFactory> determineTargetConnectionFactory() {
		Assert.state(this.resolvedConnectionFactories != null, "ConnectionFactory router not initialized");

		Mono<Object> lookupKey = determineCurrentLookupKey().defaultIfEmpty(FALLBACK_MARKER);

		return lookupKey.handle((key, sink) -> {
			ConnectionFactory connectionFactory = this.resolvedConnectionFactories.get(key);
			if (connectionFactory == null && (key == FALLBACK_MARKER || this.lenientFallback)) {
				connectionFactory = this.resolvedDefaultConnectionFactory;
			}
			if (connectionFactory == null) {
				sink.error(new IllegalStateException(String.format(
						"Cannot determine target ConnectionFactory for lookup key '%s'", key == FALLBACK_MARKER ? null : key)));
				return;
			}
			sink.next(connectionFactory);
		});
	}

	/**
	 * Determine the current lookup key. This will typically be implemented to check a
	 * subscriber context. Allows for arbitrary keys. The returned key needs to match the
	 * stored lookup key type, as resolved by the {@link #resolveSpecifiedLookupKey} method.
	 *
	 * @return {@link Mono} emitting the lookup key. May complete without emitting a value
	 * if no lookup key available
	 */
	protected abstract Mono<Object> determineCurrentLookupKey();

}
