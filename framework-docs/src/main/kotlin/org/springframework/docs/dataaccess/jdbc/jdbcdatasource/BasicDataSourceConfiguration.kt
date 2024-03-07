package org.springframework.docs.dataaccess.jdbc.jdbcdatasource

import org.apache.commons.dbcp2.BasicDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BasicDataSourceConfiguration {

	// tag::dataSourceBean[]
	@Bean(destroyMethod = "close")
	fun dataSource(): BasicDataSource {
		val dataSource = BasicDataSource()
		dataSource.driverClassName = "org.hsqldb.jdbcDriver"
		dataSource.url = "jdbc:hsqldb:hsql://localhost:"
		dataSource.username = "sa"
		dataSource.password = ""
		return dataSource
	}
	// end::dataSourceBean[]
}
