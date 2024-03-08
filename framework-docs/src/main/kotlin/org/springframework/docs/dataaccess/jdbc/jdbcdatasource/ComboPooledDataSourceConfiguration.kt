package org.springframework.docs.dataaccess.jdbc.jdbcdatasource

import com.mchange.v2.c3p0.ComboPooledDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class ComboPooledDataSourceConfiguration {

	// tag::snippet[]
	@Bean(destroyMethod = "close")
	fun dataSource() = ComboPooledDataSource().apply {
		driverClass = "org.hsqldb.jdbcDriver"
		jdbcUrl = "jdbc:hsqldb:hsql://localhost:"
		user = "sa"
		password = ""
	}
	// end::snippet[]

}
