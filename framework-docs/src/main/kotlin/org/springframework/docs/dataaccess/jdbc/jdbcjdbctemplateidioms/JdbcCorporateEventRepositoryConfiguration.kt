/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.dataaccess.jdbc.jdbcjdbctemplateidioms

import org.apache.commons.dbcp2.BasicDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

// tag::snippet[]
@Configuration
@ComponentScan("org.example.jdbc")
class JdbcCorporateEventRepositoryConfiguration {

	@Bean(destroyMethod = "close")
	fun dataSource() = BasicDataSource().apply {
		driverClassName = "org.hsqldb.jdbcDriver"
		url = "jdbc:hsqldb:hsql://localhost:"
		username = "sa"
		password = ""
	}

}
// end::snippet[]
