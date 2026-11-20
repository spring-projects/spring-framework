/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.transaction;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Shared configuration for tests that need an embedded database preloaded
 * with the schema for the 'person' table.
 *
 * @author Sam Brannen
 * @since 4.2
 */
@Configuration
class EmbeddedPersonDatabaseTestsConfig {

	@Bean
	PlatformTransactionManager txMgr() {
		return new DataSourceTransactionManager(dataSource());
	}

	@Bean
	DataSource dataSource() {
		return new EmbeddedDatabaseBuilder()
				.generateUniqueName(true)
				.addScript("classpath:/org/springframework/test/jdbc/schema.sql")
				.build();
	}

}
