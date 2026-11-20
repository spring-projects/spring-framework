/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.dataaccess.transaction.declarative.transactiondeclarativeannotations

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

// tag::snippet[]
@Configuration
@EnableTransactionManagement
class AppConfig {

	@Bean
	fun fooService(): FooService {
		return DefaultFooService()
	}

	@Bean
	fun txManager(dataSource: DataSource): PlatformTransactionManager {
		return DataSourceTransactionManager(dataSource)
	}
}
// end::snippet[]
