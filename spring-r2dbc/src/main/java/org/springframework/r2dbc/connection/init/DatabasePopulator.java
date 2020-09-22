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
 * Strategy used to populate, initialize, or clean up a database.
 *
 * @author Mark Paluch
 * @since 5.3
 * @see ResourceDatabasePopulator
 * @see ConnectionFactoryInitializer
 */
@FunctionalInterface
public interface DatabasePopulator {

	/**
	 * Populate, initialize, or clean up the database using the
	 * provided R2DBC {@link Connection}.
	 * @param connection the R2DBC connection to use to populate the db;
	 * already configured and ready to use, must not be {@code null}
	 * @return {@link Mono} that initiates script execution and is
	 * notified upon completion
	 * @throws ScriptException in all other error cases
	 */
	Mono<Void> populate(Connection connection) throws ScriptException;

	/**
	 * Execute the given {@link DatabasePopulator} against the given {@link ConnectionFactory}.
	 * @param connectionFactory the {@link ConnectionFactory} to execute against
	 * @return {@link Mono} that initiates {@link DatabasePopulator#populate(Connection)}
	 * and is notified upon completion
	 */
	default Mono<Void> populate(ConnectionFactory connectionFactory) throws DataAccessException {
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		return Mono.usingWhen(ConnectionFactoryUtils.getConnection(connectionFactory), //
				this::populate, //
				connection -> ConnectionFactoryUtils.releaseConnection(connection, connectionFactory), //
				(connection, err) -> ConnectionFactoryUtils.releaseConnection(connection, connectionFactory),
				connection -> ConnectionFactoryUtils.releaseConnection(connection, connectionFactory))
				.onErrorMap(ex -> !(ex instanceof ScriptException),
						ex -> new UncategorizedScriptException("Failed to execute database script", ex));
	}

}
