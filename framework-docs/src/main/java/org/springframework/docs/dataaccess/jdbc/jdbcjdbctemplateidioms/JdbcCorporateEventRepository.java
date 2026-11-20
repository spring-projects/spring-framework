/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
