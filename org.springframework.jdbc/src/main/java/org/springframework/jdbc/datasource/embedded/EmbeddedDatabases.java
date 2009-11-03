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

import org.springframework.core.io.ClassRelativeResourceLoader;

/**
 * Convenience factory for constructing commonly used EmbeddedDatabase configurations.
 * @author Keith Donald
 */
public final class EmbeddedDatabases {

	private EmbeddedDatabases() {
		
	}
	
	/**
	 * Factory method that creates a default EmbeddedDatabase instance.
	 * <p>The default instance is HQL populated with a schema loaded from <code>classpath:schema.sql</code> and data loaded from <code>classpath:data.sql</code>.
	 * @return an embedded database
	 */
	public static EmbeddedDatabase createDefault() {
		return buildDefault(new EmbeddedDatabaseBuilder());
	}

	/**
	 * Factory method that creates a default HSQL EmbeddedDatabase instance.
	 * <p>The default instance is HQL populated with a schema loaded from <code>schema.sql</code> and data loaded from <code>data.sql</code>, where
	 * each .sql file location is relative to the specified class.
	 * @param clazz the class to load .sql resources relative to
	 * @return an embedded database
	 */
	public static EmbeddedDatabase createDefault(Class<?> clazz) {
		return buildDefault(new EmbeddedDatabaseBuilder(new ClassRelativeResourceLoader(clazz)));
	}

	// internal helpers
	
	private static EmbeddedDatabase buildDefault(EmbeddedDatabaseBuilder builder) {
		return builder.addScript("schema.sql").addScript("data.sql").build();		
	}

}
