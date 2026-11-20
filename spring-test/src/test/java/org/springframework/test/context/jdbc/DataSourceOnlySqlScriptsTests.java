/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.jdbc;

import javax.sql.DataSource;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.transaction.TransactionAssert.assertThatTransaction;

/**
 * Integration tests for {@link Sql @Sql} support with only a {@link DataSource}
 * present in the context (i.e., no transaction manager).
 *
 * @author Sam Brannen
 * @since 4.1
 */
@SpringJUnitConfig
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql({ "schema.sql", "data.sql" })
@DirtiesContext
class DataSourceOnlySqlScriptsTests {

	private JdbcTemplate jdbcTemplate;


	@Autowired
	void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Test
	@Order(1)
	void classLevelScripts() {
		assertThatTransaction().isNotActive();
		assertNumUsers(1);
	}

	@Test
	@Sql({ "drop-schema.sql", "schema.sql", "data.sql", "data-add-dogbert.sql" })
	@Order(2)
	void methodLevelScripts() {
		assertThatTransaction().isNotActive();
		assertNumUsers(2);
	}

	protected void assertNumUsers(int expected) {
		assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "user")).as(
			"Number of rows in the 'user' table.").isEqualTo(expected);
	}


	@Configuration
	static class Config {

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()
					.generateUniqueName(true)
					.build();
		}
	}

}
