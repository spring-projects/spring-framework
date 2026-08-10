/*
 * Copyright 2002-present the original author or authors.
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

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Transactional integration tests for {@link Sql @Sql} support when the
 * {@link DataSource} is wrapped in a {@link TransactionAwareDataSourceProxy}.
 *
 * @author Sam Brannen
 * @since 6.2.18
 */
@SpringJUnitConfig
@DirtiesContext
class SqlScriptsAndTransactionAwareDataSourceProxyTests extends AbstractTransactionalTests {

	@Test
	@Sql("data-add-catbert.sql")
	void onlyCatbertIsPresent() {
		assertUsers("Catbert");
	}

	@Test
	@Sql("data-add-dogbert.sql")
	void onlyDogbertIsPresent() {
		assertUsers("Dogbert");
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		JdbcTemplate jdbcTemplate(DataSource dataSource) {
			return new JdbcTemplate(dataSource);
		}

		@Bean
		PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}

		@Bean
		DataSource dataSource() {
			DataSource dataSource = new EmbeddedDatabaseBuilder()
					.generateUniqueName(true)
					.addScript("classpath:/org/springframework/test/context/jdbc/schema.sql")
					.build();
			return new TransactionAwareDataSourceProxy(dataSource);
		}
	}

}
