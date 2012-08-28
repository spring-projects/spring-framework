/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * A builder that provides a convenient API for constructing an embedded database.
 *
 * <p>Usage example:
 * <pre>
 * EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
 * EmbeddedDatabase db = builder.setType(H2).addScript("schema.sql").addScript("data.sql").build();
 * db.shutdown();
 * </pre>
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Dave Syer
 * @author Sam Brannen
 * @since 3.0
 */
public class EmbeddedDatabaseBuilder {

	private final EmbeddedDatabaseFactory databaseFactory;

	private final ResourceDatabasePopulator databasePopulator;

	private final ResourceLoader resourceLoader;

	/**
	 * Create a new embedded database builder.
	 */
	public EmbeddedDatabaseBuilder() {
		this(new DefaultResourceLoader());
	}

	/**
	 * Create a new embedded database builder with the given ResourceLoader.
	 * @param resourceLoader the ResourceLoader to delegate to
	 */
	public EmbeddedDatabaseBuilder(ResourceLoader resourceLoader) {
		this.databaseFactory = new EmbeddedDatabaseFactory();
		this.databasePopulator = new ResourceDatabasePopulator();
		this.databaseFactory.setDatabasePopulator(this.databasePopulator);
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Set the name of the embedded database.
	 * <p>Defaults to "testdb" if not called.
	 * @param databaseName the database name
	 * @return this, to facilitate method chaining
	 */
	public EmbeddedDatabaseBuilder setName(String databaseName) {
		this.databaseFactory.setDatabaseName(databaseName);
		return this;
	}

	/**
	 * Set the type of embedded database.
	 * <p>Defaults to HSQL if not called.
	 * @param databaseType the database type
	 * @return this, to facilitate method chaining
	 */
	public EmbeddedDatabaseBuilder setType(EmbeddedDatabaseType databaseType) {
		this.databaseFactory.setDatabaseType(databaseType);
		return this;
	}

	/**
	 * Add a SQL script to execute to populate the database.
	 * @param sqlResource the sql resource location
	 * @return this, to facilitate method chaining
	 */
	public EmbeddedDatabaseBuilder addScript(String sqlResource) {
		this.databasePopulator.addScript(this.resourceLoader.getResource(sqlResource));
		return this;
	}

	/**
	 * Add default scripts to execute to populate the database.
	 * <p>The default scripts are <code>schema.sql</code> to create the db
	 * schema and <code>data.sql</code> to populate the db with data.
	 * @return this, to facilitate method chaining
	 */
	public EmbeddedDatabaseBuilder addDefaultScripts() {
		addScript("schema.sql");
		addScript("data.sql");
		return this;
	}
	
	/**
	 * Build the embedded database.
	 * @return the embedded database
	 */
	public EmbeddedDatabase build() {
		return this.databaseFactory.getDatabase();
	}

}
