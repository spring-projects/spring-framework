/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.dataaccess.jdbc.jdbcjdbctemplateidioms;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

// tag::snippet[]
public class JdbcCorporateEventDao implements CorporateEventDao {

	private final JdbcTemplate jdbcTemplate;

	public JdbcCorporateEventDao(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	// JDBC-backed implementations of the methods on the CorporateEventDao follow...
}
// end::snippet[]