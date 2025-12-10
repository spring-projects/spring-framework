package org.springframework.docs.dataaccess.jdbc.jdbcdatasource

import org.apache.commons.dbcp2.BasicDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BasicDataSourceConfiguration {

	// tag::snippet[]
	@Bean(destroyMethod = "close")
	fun dataSource() = BasicDataSource().apply {
		driverClassName = "org.hsqldb.jdbcDriver"
		url = "jdbc:hsqldb:hsql://localhost:"
		username = "sa"
		password = ""
	}
	// end::snippet[]
}
