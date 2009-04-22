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
 * EmbeddedDatabase db = builder.schema("schema.sql").testData("test-data.sql").build();
 * db.shutdown();
 * </pre>
 * 
 * TODO - should we replace schema/testdata with more general 'script' method?
 * 
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
	 * @param databaseType the embedded database type
	 * @return this, for fluent call chaining
	 */
	public EmbeddedDatabaseBuilder name(String databaseName) {
		databaseFactory.setDatabaseName(databaseName);
		return this;
	}

	/**
	 * Sets the type of embedded database.
	 * Defaults to HSQL if not called.
	 * @param databaseType the embedded database type
	 * @return this, for fluent call chaining
	 */
	public EmbeddedDatabaseBuilder type(EmbeddedDatabaseType databaseType) {
		databaseFactory.setDatabaseType(databaseType);
		return this;
	}
	
	/**
	 * Sets the location of the schema SQL to run to create the database structure.
	 * Defaults to classpath:schema.sql if not called.
	 * @param sqlResource the sql resource location
	 * @return this, for fluent call chaining
	 */
	public EmbeddedDatabaseBuilder schema(String sqlResource) {
		databasePopulator.setSchemaLocation(resourceLoader.getResource(sqlResource));
		return this;
	}

	/**
	 * Sets the location of the schema SQL to run to create the database structure.
	 * Defaults to classpath:test-data.sql if not called
	 * @param sqlResource the sql resource location
	 * @return this, for fluent call chaining
	 */
	public EmbeddedDatabaseBuilder testData(String sqlResource) {
		databasePopulator.setTestDataLocation(resourceLoader.getResource(sqlResource));
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
	 * Factory method that creates a embedded database builder that loads resources relative to the provided class.
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
	 * The default is HSQL with a schema created from classpath:schema.sql and test-data loaded from classpatH:test-data.sql.
	 * @return an embedded database
	 */
	public static EmbeddedDatabase buildDefault() {
		return new EmbeddedDatabaseBuilder().build();
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
