/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.dataaccess.jdbc.jdbcdatasource

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DriverManagerDataSource

@Configuration
class DriverManagerDataSourceConfiguration {

	// tag::snippet[]
	@Bean
	fun dataSource() = DriverManagerDataSource().apply {
		setDriverClassName("org.hsqldb.jdbcDriver")
		url = "jdbc:hsqldb:hsql://localhost:"
		username = "sa"
		password = ""
	}
	// end::snippet[]

}
