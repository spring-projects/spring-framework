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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.r2dbc.spi.ConnectionFactory;

import org.springframework.util.Assert;

/**
 * Simple {@link ConnectionFactoryLookup} implementation that relies
 * on a map for doing lookups.
 *
 * <p>Useful for testing environments or applications that need to match
 * arbitrary {@link String} names to target {@link ConnectionFactory} objects.
 *
 * @author Mark Paluch
 * @author Jens Schauder
 * @since 5.3
 */
public class MapConnectionFactoryLookup implements ConnectionFactoryLookup {

	private final Map<String, ConnectionFactory> connectionFactories = new HashMap<>();


	/**
	 * Create a new instance of the {@link MapConnectionFactoryLookup} class.
	 */
	public MapConnectionFactoryLookup() {}

	/**
	 * Create a new instance of the {@link MapConnectionFactoryLookup} class.
	 * @param connectionFactories the {@link Map} of {@link ConnectionFactory}.
	 * The keys are {@link String Strings}, the values are actual {@link ConnectionFactory} instances.
	 */
	public MapConnectionFactoryLookup(Map<String, ConnectionFactory> connectionFactories) {
		setConnectionFactories(connectionFactories);
	}

	/**
	 * Create a new instance of the {@link MapConnectionFactoryLookup} class.
	 * @param connectionFactoryName the name under which the supplied {@link ConnectionFactory} is to be added
	 * @param connectionFactory the {@link ConnectionFactory} to be added
	 */
	public MapConnectionFactoryLookup(String connectionFactoryName, ConnectionFactory connectionFactory) {
		addConnectionFactory(connectionFactoryName, connectionFactory);
	}


	/**
	 * Set the {@link Map} of {@link ConnectionFactory ConnectionFactories}.
	 * The keys are {@link String Strings}, the values are actual {@link ConnectionFactory} instances.
	 * <p>If the supplied {@link Map} is {@code null}, then this method call effectively has no effect.
	 * @param connectionFactories said {@link Map} of {@link ConnectionFactory connectionFactories}
	 */
	public void setConnectionFactories(Map<String, ConnectionFactory> connectionFactories) {
		Assert.notNull(connectionFactories, "ConnectionFactories must not be null");
		this.connectionFactories.putAll(connectionFactories);
	}

	/**
	 * Get the {@link Map} of {@link ConnectionFactory ConnectionFactories} maintained by this object.
	 * <p>The returned {@link Map} is {@link Collections#unmodifiableMap(Map) unmodifiable}.
	 * @return {@link Map} of {@link ConnectionFactory connectionFactory} (never {@code null})
	 */
	public Map<String, ConnectionFactory> getConnectionFactories() {
		return Collections.unmodifiableMap(this.connectionFactories);
	}

	/**
	 * Add the supplied {@link ConnectionFactory} to the map of
	 * {@link ConnectionFactory ConnectionFactory} instances maintained by this object.
	 * @param connectionFactoryName the name under which the supplied {@link ConnectionFactory} is to be added
	 * @param connectionFactory the {@link ConnectionFactory} to be so added
	 */
	public void addConnectionFactory(String connectionFactoryName, ConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactoryName, "ConnectionFactory name must not be null");
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		this.connectionFactories.put(connectionFactoryName, connectionFactory);
	}

	@Override
	public ConnectionFactory getConnectionFactory(String connectionFactoryName)
			throws ConnectionFactoryLookupFailureException {

		Assert.notNull(connectionFactoryName, "ConnectionFactory name must not be null");
		return this.connectionFactories.computeIfAbsent(connectionFactoryName, key -> {
			throw new ConnectionFactoryLookupFailureException(
					"No ConnectionFactory with name '" + connectionFactoryName + "' registered");
		});
	}

}
