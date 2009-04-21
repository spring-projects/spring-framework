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
package org.springframework.test.jdbc;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * Populates a test DataSource from schema and test-data SQL defined in external resources.
 * By default, looks for a schema.sql file and test-data.sql resource in the root of the classpath.
 * 
 * May be configured.
 * Call {@link #setSchemaLocation(Resource)} to configure the location of the database schema file.
 * Call {@link #setTestDataLocation(Resource)} to configure the location of the test data file.
 * Call {@link #setSqlScriptEncoding(String)} to set the encoding for the schema and test data SQL.
 */
public class ResourceTestDatabasePopulator implements TestDatabasePopulator {

	private Resource schemaLocation = new ClassPathResource("schema.sql");

	private Resource testDataLocation = new ClassPathResource("test-data.sql");

	private String sqlScriptEncoding;
	
	/**
	 * Sets the location of .sql file containing the database schema to create.
	 * @param schemaLocation the path to the db schema definition
	 */
	public void setSchemaLocation(Resource schemaLocation) {
		this.schemaLocation = schemaLocation;
	}

	/**
	 * Sets the location of the .sql file containing the test data to load.
	 * @param testDataLocation the path to the db test data file
	 */
	public void setTestDataLocation(Resource testDataLocation) {
		this.testDataLocation = testDataLocation;
	}
	
	/**
	 * Specify the encoding for SQL scripts, if different from the platform encoding.
	 */
	public void setSqlScriptEncoding(String sqlScriptEncoding) {
		this.sqlScriptEncoding = sqlScriptEncoding;
	}

	public void populate(JdbcTemplate template) {
		createDatabaseSchema(template);
		insertTestData(template);
	}

	// create the application's database schema (tables, indexes, etc.)
	private void createDatabaseSchema(JdbcTemplate template) {
		// TODO SimpleJdbcTemplate is unnecessary now with Java5+ - make similar method available on JdbcTestUtils?
		SimpleJdbcTestUtils.executeSqlScript(new SimpleJdbcTemplate(template), new EncodedResource(schemaLocation, sqlScriptEncoding), false);
	}

	// populate the tables with test data
	private void insertTestData(JdbcTemplate template) {
		// TODO SimpleJdbcTemplate is unnecessary now with Java5+ - make similar method available on JdbcTestUtils?
		SimpleJdbcTestUtils.executeSqlScript(new SimpleJdbcTemplate(template), new EncodedResource(testDataLocation, sqlScriptEncoding), false);
	}

}