/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.dataaccess.jdbc.jdbcjdbctemplateidioms

import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

// tag::snippet[]
class JdbcCorporateEventDao(dataSource: DataSource): CorporateEventDao {

	private val jdbcTemplate = JdbcTemplate(dataSource)

	// JDBC-backed implementations of the methods on the CorporateEventDao follow...
}
// end::snippet[]