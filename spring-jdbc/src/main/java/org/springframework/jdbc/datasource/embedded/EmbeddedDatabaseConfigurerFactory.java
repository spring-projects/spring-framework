/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.jdbc.datasource.embedded;

import org.springframework.util.Assert;

/**
 * Maps well-known {@linkplain EmbeddedDatabaseType embedded database types}
 * to {@link EmbeddedDatabaseConfigurer} strategies.
 *
 * @author Keith Donald
 * @author Oliver Gierke
 * @author Sam Brannen
 * @since 3.0
 */
final class EmbeddedDatabaseConfigurerFactory {

	private EmbeddedDatabaseConfigurerFactory() {
	}


	/**
	 * Return a configurer instance for the given embedded database type.
	 * @param type the embedded database type (HSQL, H2 or Derby)
	 * @return the configurer instance
	 * @throws IllegalStateException if the driver for the specified database type is not available
	 */
	public static EmbeddedDatabaseConfigurer getConfigurer(EmbeddedDatabaseType type) throws IllegalStateException {
		Assert.notNull(type, "EmbeddedDatabaseType is required");
		try {
			return switch (type) {
				case HSQL -> HsqlEmbeddedDatabaseConfigurer.getInstance();
				case H2 -> H2EmbeddedDatabaseConfigurer.getInstance();
				case DERBY -> DerbyEmbeddedDatabaseConfigurer.getInstance();
				default -> throw new UnsupportedOperationException("Embedded database type [" + type + "] is not supported");
			};
		}
		catch (ClassNotFoundException | NoClassDefFoundError ex) {
			throw new IllegalStateException("Driver for test database type [" + type + "] is not available", ex);
		}
	}

}
