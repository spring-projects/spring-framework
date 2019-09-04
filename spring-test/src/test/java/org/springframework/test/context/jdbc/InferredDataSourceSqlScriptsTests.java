/*
 * Copyright 2002-2014 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.Assert.*;
import static org.springframework.test.transaction.TransactionTestUtils.*;

/**
 * Integration tests for {@link Sql @Sql} that verify support for inferring
 * {@link DataSource}s from {@link PlatformTransactionManager}s.
 *
 * @author Sam Brannen
 * @since 4.1
 * @see InferredDataSourceTransactionalSqlScriptsTests
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
public class InferredDataSourceSqlScriptsTests {

	@Autowired
	private DataSource dataSource1;

	@Autowired
	private DataSource dataSource2;


	@Test
	@Sql(scripts = "data-add-dogbert.sql", config = @SqlConfig(transactionManager = "txMgr1"))
	public void database1() {
		assertInTransaction(false);
		assertUsers(new JdbcTemplate(dataSource1), "Dilbert", "Dogbert");
	}

	@Test
	@Sql(scripts = "data-add-catbert.sql", config = @SqlConfig(transactionManager = "txMgr2"))
	public void database2() {
		assertInTransaction(false);
		assertUsers(new JdbcTemplate(dataSource2), "Dilbert", "Catbert");
	}

	private void assertUsers(JdbcTemplate jdbcTemplate, String... users) {
		List<String> expected = Arrays.asList(users);
		Collections.sort(expected);
		List<String> actual = jdbcTemplate.queryForList("select name from user", String.class);
		Collections.sort(actual);
		assertEquals("Users in database;", expected, actual);
	}


	@Configuration
	static class Config {

		@Bean
		public PlatformTransactionManager txMgr1() {
			return new DataSourceTransactionManager(dataSource1());
		}

		@Bean
		public PlatformTransactionManager txMgr2() {
			return new DataSourceTransactionManager(dataSource2());
		}

		@Bean
		public DataSource dataSource1() {
			return new EmbeddedDatabaseBuilder()//
			.setName("database1")//
			.addScript("classpath:/org/springframework/test/context/jdbc/schema.sql")//
			.addScript("classpath:/org/springframework/test/context/jdbc/data.sql")//
			.build();
		}

		@Bean
		public DataSource dataSource2() {
			return new EmbeddedDatabaseBuilder()//
			.setName("database2")//
			.addScript("classpath:/org/springframework/test/context/jdbc/schema.sql")//
			.addScript("classpath:/org/springframework/test/context/jdbc/data.sql")//
			.build();
		}
	}

}
