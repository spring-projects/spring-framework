/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.docs.dataaccess.jdbc.jdbcjdbctemplateidioms;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// tag::snippet[]
@Repository
public class JdbcCorporateEventRepository implements CorporateEventRepository {

	private JdbcTemplate jdbcTemplate;

	// Implicitly autowire the DataSource constructor parameter
	public JdbcCorporateEventRepository(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	// JDBC-backed implementations of the methods on the CorporateEventRepository follow...
}
// end::snippet[]
