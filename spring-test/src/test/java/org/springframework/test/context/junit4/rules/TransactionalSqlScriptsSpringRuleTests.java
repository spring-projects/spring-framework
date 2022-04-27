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

package org.springframework.test.context.junit4.rules;

import java.util.concurrent.TimeUnit;

import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.EmptyDatabaseConfig;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This class is a JUnit 4 based copy of
 * {@link org.springframework.test.context.jdbc.TransactionalSqlScriptsTests}
 * that has been modified to use {@link SpringClassRule} and {@link SpringMethodRule}.
 *
 * @author Sam Brannen
 * @since 4.2
 */
@RunWith(JUnit4.class)
@ContextConfiguration(classes = EmptyDatabaseConfig.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Sql({ "../../jdbc/schema.sql", "../../jdbc/data.sql" })
@DirtiesContext
public class TransactionalSqlScriptsSpringRuleTests {

	@ClassRule
	public static final SpringClassRule springClassRule = new SpringClassRule();

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();

	@Rule
	public Timeout timeout = Timeout.builder().withTimeout(10, TimeUnit.SECONDS).build();

	@Autowired
	JdbcTemplate jdbcTemplate;


	@Test
	public void classLevelScripts() {
		assertNumUsers(1);
	}

	@Test
	@Sql({ "../../jdbc/drop-schema.sql", "../../jdbc/schema.sql", "../../jdbc/data.sql", "../../jdbc/data-add-dogbert.sql" })
	public void methodLevelScripts() {
		assertNumUsers(2);
	}

	private void assertNumUsers(int expected) {
		assertThat(countRowsInTable("user")).as("Number of rows in the 'user' table.").isEqualTo(expected);
	}

	private int countRowsInTable(String tableName) {
		return JdbcTestUtils.countRowsInTable(this.jdbcTemplate, tableName);
	}

}
