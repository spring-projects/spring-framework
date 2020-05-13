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

import io.r2dbc.spi.ConnectionFactory;

/**
 * Strategy interface for looking up {@link ConnectionFactory} by name.
 *
 * @author Mark Paluch
 * @since 5.3
 */
@FunctionalInterface
public interface ConnectionFactoryLookup {

	/**
	 * Retrieve the {@link ConnectionFactory} identified by the given name.
	 * @param connectionFactoryName the name of the {@link ConnectionFactory}
	 * @return the {@link ConnectionFactory} (never {@code null})
	 * @throws ConnectionFactoryLookupFailureException if the lookup failed
	 */
	ConnectionFactory getConnectionFactory(String connectionFactoryName) throws ConnectionFactoryLookupFailureException;

}
