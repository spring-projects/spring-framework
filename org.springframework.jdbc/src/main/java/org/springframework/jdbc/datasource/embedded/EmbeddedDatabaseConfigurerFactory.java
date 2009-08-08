/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
 * Maps well-known {@link EmbeddedDatabaseType embedded database types} to
 * {@link EmbeddedDatabaseConfigurer} strategies.
 *
 * @author Keith Donald
 * @author Oliver Gierke
 * @since 3.0
 */
final class EmbeddedDatabaseConfigurerFactory {

	public static EmbeddedDatabaseConfigurer getConfigurer(EmbeddedDatabaseType type) throws IllegalStateException {
		Assert.notNull(type, "EmbeddedDatabaseType is required");
		try {
			switch (type) {
				case HSQL:
					return HsqlEmbeddedDatabaseConfigurer.getInstance();
				case H2:
					return H2EmbeddedDatabaseConfigurer.getInstance();
				case DERBY:
					return DerbyEmbeddedDatabaseConfigurer.getInstance();
				default:
					throw new UnsupportedOperationException("Other embedded database types not yet supported");
			}
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Driver for test database type [" + type +
					"] is not available in the classpath", ex);
		}
	}

	private EmbeddedDatabaseConfigurerFactory() {
	}

}
