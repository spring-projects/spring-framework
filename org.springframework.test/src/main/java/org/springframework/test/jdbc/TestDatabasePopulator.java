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

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Strategy for populating a test database with test data.
 * 
 * @see ResourceTestDatabasePopulator
 */
public interface TestDatabasePopulator {
	
	/**
	 * Populate the test database using the JDBC-based data access template provided.
	 * @param template the data access template to use to populate the db; already configured and ready to use
	 * @throws DataAccessException if an unrecoverable data access exception occurs during database population
	 */
	void populate(JdbcTemplate template);
}