/*
 * Copyright 2002-2019 the original author or authors.
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

import org.junit.jupiter.api.Test;

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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.transaction.TransactionAssert.assertThatTransaction;

/**
 * Integration tests that ensure that <em>primary</em> transaction managers
 * are supported.
 *
 * @author Sam Brannen
 * @since 4.3
 * @see org.springframework.test.context.jdbc.PrimaryDataSourceTests
 */
@SpringJUnitConfig
@DirtiesContext
final class PrimaryTransactionManagerTests {

	private JdbcTemplate jdbcTemplate;


	@Autowired
	void setDataSource(DataSource dataSource1) {
		this.jdbcTemplate = new JdbcTemplate(dataSource1);
	}

	@BeforeTransaction
	void beforeTransaction() {
		assertNumUsers(0);
	}

	@AfterTransaction
	void afterTransaction() {
		assertNumUsers(0);
	}

	@Test
	@Transactional
	void transactionalTest() {
		assertThatTransaction().isActive();

		ClassPathResource resource = new ClassPathResource("/org/springframework/test/context/jdbc/data.sql");
		new ResourceDatabasePopulator(resource).execute(jdbcTemplate.getDataSource());

		assertNumUsers(1);
	}

	private void assertNumUsers(int expected) {
		assertThat(JdbcTestUtils.countRowsInTable(this.jdbcTemplate, "user")).as("Number of rows in the 'user' table").isEqualTo(expected);
	}


	@Configuration
	@EnableTransactionManagement  // SPR-17137: should not break trying to proxy the final test class
	static class Config {

		@Primary
		@Bean
		PlatformTransactionManager primaryTransactionManager() {
			return new DataSourceTransactionManager(dataSource1());
		}

		@Bean
		PlatformTransactionManager additionalTransactionManager() {
			return new DataSourceTransactionManager(dataSource2());
		}

		@Bean
		DataSource dataSource1() {
			return new EmbeddedDatabaseBuilder()
					.generateUniqueName(true)
					.addScript("classpath:/org/springframework/test/context/jdbc/schema.sql")
					.build();
		}

		@Bean
		DataSource dataSource2() {
			return new EmbeddedDatabaseBuilder().generateUniqueName(true).build();
		}
	}

}
