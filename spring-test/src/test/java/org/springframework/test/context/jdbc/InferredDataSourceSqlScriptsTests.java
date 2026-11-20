/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.jdbc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.transaction.TransactionAssert.assertThatTransaction;

/**
 * Integration tests for {@link Sql @Sql} that verify support for inferring
 * {@link DataSource}s from {@link PlatformTransactionManager}s.
 *
 * @author Sam Brannen
 * @since 4.1
 * @see InferredDataSourceTransactionalSqlScriptsTests
 */
@SpringJUnitConfig
@DirtiesContext
class InferredDataSourceSqlScriptsTests {

	@Autowired
	DataSource dataSource1;

	@Autowired
	DataSource dataSource2;


	@Test
	@Sql(scripts = "data-add-dogbert.sql", config = @SqlConfig(transactionManager = "txMgr1"))
	void database1() {
		assertThatTransaction().isNotActive();
		assertUsers(new JdbcTemplate(dataSource1), "Dilbert", "Dogbert");
	}

	@Test
	@Sql(scripts = "data-add-catbert.sql", config = @SqlConfig(transactionManager = "txMgr2"))
	void database2() {
		assertThatTransaction().isNotActive();
		assertUsers(new JdbcTemplate(dataSource2), "Dilbert", "Catbert");
	}

	private void assertUsers(JdbcTemplate jdbcTemplate, String... users) {
		List<String> expected = Arrays.asList(users);
		Collections.sort(expected);
		List<String> actual = jdbcTemplate.queryForList("select name from user", String.class);
		Collections.sort(actual);
		assertThat(actual).as("Users in database;").isEqualTo(expected);
	}


	@Configuration
	static class Config {

		@Bean
		PlatformTransactionManager txMgr1() {
			return new DataSourceTransactionManager(dataSource1());
		}

		@Bean
		PlatformTransactionManager txMgr2() {
			return new DataSourceTransactionManager(dataSource2());
		}

		@Bean
		DataSource dataSource1() {
			return new EmbeddedDatabaseBuilder()//
					.setName("database1")//
					.addScript("classpath:/org/springframework/test/context/jdbc/schema.sql")//
					.addScript("classpath:/org/springframework/test/context/jdbc/data.sql")//
					.build();
		}

		@Bean
		DataSource dataSource2() {
			return new EmbeddedDatabaseBuilder()//
					.setName("database2")//
					.addScript("classpath:/org/springframework/test/context/jdbc/schema.sql")//
					.addScript("classpath:/org/springframework/test/context/jdbc/data.sql")//
					.build();
		}
	}

}
