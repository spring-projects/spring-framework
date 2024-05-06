/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.docs.dataaccess.jdbc.jdbcjdbctemplateidioms

import org.apache.commons.dbcp2.BasicDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class JdbcCorporateEventDaoConfiguration {

	// tag::snippet[]
	@Bean
	fun corporateEventDao(dataSource: DataSource) = JdbcCorporateEventDao(dataSource)

	@Bean(destroyMethod = "close")
	fun dataSource() = BasicDataSource().apply {
		driverClassName = "org.hsqldb.jdbcDriver"
		url = "jdbc:hsqldb:hsql://localhost:"
		username = "sa"
		password = ""
	}
	// end::snippet[]

}
