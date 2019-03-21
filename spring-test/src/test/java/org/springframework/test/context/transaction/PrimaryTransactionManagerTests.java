/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.test.context.transaction;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.transaction.TransactionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;

/**
 * Integration tests that ensure that <em>primary</em> transaction managers
 * are supported.
 *
 * @author Sam Brannen
 * @since 4.3
 * @see org.springframework.test.context.jdbc.PrimaryDataSourceTests
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
public class PrimaryTransactionManagerTests {

	private JdbcTemplate jdbcTemplate;


	@Autowired
	public void setDataSource(DataSource dataSource1) {
		this.jdbcTemplate = new JdbcTemplate(dataSource1);
	}


	@BeforeTransaction
	public void beforeTransaction() {
		assertNumUsers(0);
	}

	@AfterTransaction
	public void afterTransaction() {
		assertNumUsers(0);
	}

	@Test
	@Transactional
	public void transactionalTest() {
		TransactionTestUtils.assertInTransaction(true);

		ClassPathResource resource = new ClassPathResource("/org/springframework/test/context/jdbc/data.sql");
		new ResourceDatabasePopulator(resource).execute(jdbcTemplate.getDataSource());

		assertNumUsers(1);
	}

	private void assertNumUsers(int expected) {
		assertEquals("Number of rows in the 'user' table", expected,
				JdbcTestUtils.countRowsInTable(this.jdbcTemplate, "user"));
	}


	@Configuration
	static class Config {

		@Primary
		@Bean
		public PlatformTransactionManager primaryTransactionManager() {
			return new DataSourceTransactionManager(dataSource1());
		}

		@Bean
		public PlatformTransactionManager additionalTransactionManager() {
			return new DataSourceTransactionManager(dataSource2());
		}

		@Bean
		public DataSource dataSource1() {
			return new EmbeddedDatabaseBuilder()
					.generateUniqueName(true)
					.addScript("classpath:/org/springframework/test/context/jdbc/schema.sql")
					.build();
		}

		@Bean
		public DataSource dataSource2() {
			return new EmbeddedDatabaseBuilder().generateUniqueName(true).build();
		}
	}

}
