/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.jdbc;

import javax.sql.DataSource;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.junit.Assert.*;

/**
 * Integration tests which verify that scripts executed via {@link Sql @Sql}
 * will persist between non-transactional test methods.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ContextConfiguration(classes = EmptyDatabaseConfig.class)
@Sql({ "schema.sql", "data.sql" })
@DirtiesContext
public class NonTransactionalSqlScriptsTests {

	protected JdbcTemplate jdbcTemplate;


	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Test
	// test##_ prefix is required for @FixMethodOrder.
	public void test01_classLevelScripts() {
		assertNumUsers(1);
	}

	@Test
	@Sql("data-add-dogbert.sql")
	// test##_ prefix is required for @FixMethodOrder.
	public void test02_methodLevelScripts() {
		assertNumUsers(2);
	}

	protected void assertNumUsers(int expected) {
		assertEquals("Number of rows in the 'user' table.", expected,
			JdbcTestUtils.countRowsInTable(jdbcTemplate, "user"));
	}

}
