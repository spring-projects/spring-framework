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

package org.springframework.r2dbc.connection.init;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Mono;

import org.springframework.dao.DataAccessException;
import org.springframework.r2dbc.connection.ConnectionFactoryUtils;
import org.springframework.util.Assert;

/**
 * Utility methods for executing a {@link DatabasePopulator}.
 *
 * @author Mark Paluch
 * @since 5.3
 */
public abstract class DatabasePopulatorUtils {

	// utility constructor
	private DatabasePopulatorUtils() {}


	/**
	 * Execute the given {@link DatabasePopulator} against the given {@link ConnectionFactory}.
	 * @param populator the {@link DatabasePopulator} to execute
	 * @param connectionFactory the {@link ConnectionFactory} to execute against
	 * @return {@link Mono} that initiates {@link DatabasePopulator#populate(Connection)}
	 * and is notified upon completion
	 */
	public static Mono<Void> execute(DatabasePopulator populator, ConnectionFactory connectionFactory)
			throws DataAccessException {
		Assert.notNull(populator, "DatabasePopulator must not be null");
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		return Mono.usingWhen(ConnectionFactoryUtils.getConnection(connectionFactory), //
				populator::populate, //
				connection -> ConnectionFactoryUtils.releaseConnection(connection, connectionFactory), //
				(connection, err) -> ConnectionFactoryUtils.releaseConnection(connection, connectionFactory),
				connection -> ConnectionFactoryUtils.releaseConnection(connection, connectionFactory))
				.onErrorMap(ex -> !(ex instanceof ScriptException),
						ex -> new UncategorizedScriptException("Failed to execute database script", ex));
	}

}
