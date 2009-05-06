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
 * @author Keith Donald
 */
final class EmbeddedDatabaseConfigurerFactory {

	private EmbeddedDatabaseConfigurerFactory() {		
	}
	
	public static EmbeddedDatabaseConfigurer getConfigurer(EmbeddedDatabaseType type) throws IllegalStateException {
		Assert.notNull(type, "The EmbeddedDatabaseType is required");
		try {
			if (type == EmbeddedDatabaseType.HSQL) {
				return HsqlEmbeddedDatabaseConfigurer.getInstance();
			} else {
				throw new UnsupportedOperationException("Other embedded database types not yet supported");
			}
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("Drivers for test database type [" + type
					+ "] are not available in the classpath", e);
		}
	}

}
