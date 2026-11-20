/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.dataaccess.jdbc.jdbcembeddeddatabase

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType

@Configuration
class JdbcEmbeddedDatabaseConfiguration {

	// tag::snippet[]
	@Bean
	fun dataSource() = EmbeddedDatabaseBuilder()
		.generateUniqueName(true)
		.setType(EmbeddedDatabaseType.H2)
		.addScripts("schema.sql", "test-data.sql")
		.build()
	// end::snippet[]

}
