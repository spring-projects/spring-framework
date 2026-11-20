/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.jdbc;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.transaction.TransactionAssert.assertThatTransaction;

/**
 * Integration tests that ensure that <em>primary</em> data sources are
 * supported.
 *
 * @author Sam Brannen
 * @since 4.3
 * @see org.springframework.test.context.transaction.manager.PrimaryTransactionManagerTests
 */
@SpringJUnitConfig
@DirtiesContext
class PrimaryDataSourceTests {

	@Configuration
	static class Config {

		@Primary
		@Bean
		DataSource primaryDataSource() {
			// @formatter:off
			return new EmbeddedDatabaseBuilder()
					.generateUniqueName(true)
					.addScript("classpath:/org/springframework/test/context/jdbc/schema.sql")
					.build();
			// @formatter:on
		}

		@Bean
		DataSource additionalDataSource() {
			return new EmbeddedDatabaseBuilder().generateUniqueName(true).build();
		}

	}


	private JdbcTemplate jdbcTemplate;


	@Autowired
	void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Test
	@Sql("data.sql")
	void dataSourceTest() {
		assertThatTransaction().isNotActive();
		assertThat(JdbcTestUtils.countRowsInTable(this.jdbcTemplate, "user")).as("Number of rows in the 'user' table.").isEqualTo(1);
	}

}
