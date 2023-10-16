/*
 * Copyright 2002-2023 the original author or authors.
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
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.transaction.TransactionAssert.assertThatTransaction;

/**
 * Exact copy of {@link InferredDataSourceSqlScriptsTests}, except that test
 * methods are transactional.
 *
 * @author Sam Brannen
 * @since 4.1
 * @see InferredDataSourceSqlScriptsTests
 */
@SpringJUnitConfig
@DirtiesContext
class InferredDataSourceTransactionalSqlScriptsTests {

	@Autowired
	DataSource dataSource1;

	@Autowired
	DataSource dataSource2;


	@Test
	@Transactional("txMgr1")
	@Sql(scripts = "data-add-dogbert.sql", config = @SqlConfig(transactionManager = "txMgr1"))
	void database1() {
		assertThatTransaction().isActive();
		assertUsers(new JdbcTemplate(dataSource1), "Dilbert", "Dogbert");
	}

	@Test
	@Transactional("txMgr2")
	@Sql(scripts = "data-add-catbert.sql", config = @SqlConfig(transactionManager = "txMgr2"))
	void database2() {
		assertThatTransaction().isActive();
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
			return new EmbeddedDatabaseBuilder()
					.generateUniqueName(true)
					.addScript("classpath:/org/springframework/test/context/jdbc/schema.sql")
					.addScript("classpath:/org/springframework/test/context/jdbc/data.sql")
					.build();
		}

		@Bean
		DataSource dataSource2() {
			return new EmbeddedDatabaseBuilder()
					.generateUniqueName(true)
					.addScript("classpath:/org/springframework/test/context/jdbc/schema.sql")
					.addScript("classpath:/org/springframework/test/context/jdbc/data.sql")
					.build();
		}
	}

}
