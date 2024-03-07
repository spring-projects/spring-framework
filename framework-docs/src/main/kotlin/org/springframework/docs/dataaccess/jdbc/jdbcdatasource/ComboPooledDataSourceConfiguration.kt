package org.springframework.docs.dataaccess.jdbc.jdbcdatasource

import com.mchange.v2.c3p0.ComboPooledDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class ComboPooledDataSourceConfiguration {

	// tag::dataSourceBean[]
	@Bean(destroyMethod = "close")
	fun dataSource(): ComboPooledDataSource {
		val dataSource = ComboPooledDataSource()
		dataSource.driverClass = "org.hsqldb.jdbcDriver"
		dataSource.jdbcUrl = "jdbc:hsqldb:hsql://localhost:"
		dataSource.user = "sa"
		dataSource.password = ""
		return dataSource
	}
	// end::dataSourceBean[]

}
