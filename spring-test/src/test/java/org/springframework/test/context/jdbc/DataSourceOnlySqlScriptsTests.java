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
