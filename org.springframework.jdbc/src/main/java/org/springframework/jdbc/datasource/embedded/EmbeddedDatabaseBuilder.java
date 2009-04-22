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

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * A builder that provides a fluent API for constructing an embedded database.
 * Usage example:
 * <pre>
 * EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
 * EmbeddedDatabase db = builder.script("schema.sql").script("test-data.sql").build();
 * db.shutdown();
 * </pre>
 * @author Keith Donald
 */
public class EmbeddedDatabaseBuilder {

	private EmbeddedDatabaseFactory databaseFactory;
	
	private ResourceDatabasePopulator databasePopulator;

	private ResourceLoader resourceLoader;
	
	/**
	 * Creates a new embedded database builder.
	 */
	public EmbeddedDatabaseBuilder() {
		init(new DefaultResourceLoader());
	}

	/**
	 * Sets the name of the embedded database
	 * Defaults to 'testdb' if not called.
	 * @param databaseName the database name
	 * @return this, for fluent call chaining
	 */
	public EmbeddedDatabaseBuilder name(String databaseName) {
		databaseFactory.setDatabaseName(databaseName);
		return this;
	}

	/**
	 * Sets the type of embedded database.
	 * Defaults to HSQL if not called.
	 * @param databaseType the database type
	 * @return this, for fluent call chaining
	 */
	public EmbeddedDatabaseBuilder type(EmbeddedDatabaseType databaseType) {
		databaseFactory.setDatabaseType(databaseType);
		return this;
	}
	
	/**
	 * Adds a SQL script to execute to populate the database.
	 * @param sqlResource the sql resource location
	 * @return this, for fluent call chaining
	 */
	public EmbeddedDatabaseBuilder script(String sqlResource) {
		databasePopulator.addScript(resourceLoader.getResource(sqlResource));
		return this;
	}

	/**
	 * Build the embedded database.
	 * @return the embedded database
	 */
	public EmbeddedDatabase build() {
		return databaseFactory.getDatabase();
	}
	
	/**
	 * Factory method that creates a EmbeddedDatabaseBuilder that loads SQL resources relative to the provided class.
	 * @param clazz the class to load relative to
	 * @return the embedded database builder
	 */
	public static EmbeddedDatabaseBuilder relativeTo(final Class<?> clazz) {
		ResourceLoader loader = new ResourceLoader() {
			public ClassLoader getClassLoader() {
				return getClass().getClassLoader();
			}

			public Resource getResource(String location) {
				return new ClassPathResource(location, clazz);
			}			
		};
		return new EmbeddedDatabaseBuilder(loader);
	}
	
	/**
	 * Factory method that builds a default EmbeddedDatabase instance.
	 * The default instance is HSQL with a schema created from classpath:schema.sql and test-data loaded from classpath:test-data.sql.
	 * @return an embedded database
	 */
	public static EmbeddedDatabase buildDefault() {
		return new EmbeddedDatabaseBuilder().script("schema.sql").script("test-data.sql").build();
	}
	
	private EmbeddedDatabaseBuilder(ResourceLoader loader) {
		init(loader);
	}
	
	private void init(ResourceLoader loader) {
		databaseFactory = new EmbeddedDatabaseFactory();
		databasePopulator = new ResourceDatabasePopulator();
		databaseFactory.setDatabasePopulator(databasePopulator);		
		resourceLoader = loader;
	}

}
