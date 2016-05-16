/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.jdbc.core.simple;

import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.Assert.*;

/**
 * @author Florent Paillard
 * @author Sanghyuk Jung
 */
public class SimpleJdbcUpdateIntegrationTests {
	private static final String INSERT_SQL =
		"INSERT INTO dummy_table(key_1, key_2, a_string, an_int, a_bool)\n"
		+ "VALUES(?, ?, ?, ?, ?)";

	private static final String SELECT_SQL =
		"SELECT key_1, key_2, a_string, an_int, a_bool\n"
		+ "FROM dummy_table\n"
		+ "WHERE key_1 = ? AND key_2 = ?";

	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;

	@Before
	public void createTestTable() {
		dataSource = new DriverManagerDataSource("jdbc:hsqldb:mem:testdb");
		jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.update("CREATE TABLE dummy_table (key_1 VARCHAR(50), key_2 INT, a_string VARCHAR(50), an_int INTEGER, a_bool BIT) ");
	}

	@After
	public void dropTestTable() {
		jdbcTemplate.update("DROP TABLE dummy_table");
	}

	@Test
	public void updateWithSqlParameterSource() {
		String key1 = "spring";
		int key2 = 1;
		jdbcTemplate.update(INSERT_SQL, key1, key2, "original", 0, false);

		SimpleJdbcUpdate simpleJdbcUpdate = new SimpleJdbcUpdate(dataSource)
			.withTableName("dummy_table")
			.updatingColumns("a_string", "an_int", "a_bool")
			.restrictingColumns("key_1", "key_2");

		SqlParameterSource updatingValues = new MapSqlParameterSource()
			.addValue("a_string", "updated")
			.addValue("an_int", 1)
			.addValue("a_bool", true);

		SqlParameterSource restrictingValues = new MapSqlParameterSource()
			.addValue("key_1", key1)
			.addValue("key_2", key2);

		int affected = simpleJdbcUpdate.execute(updatingValues, restrictingValues);
		assertEquals(1, affected);

		Map<String, Object> row = jdbcTemplate.queryForMap(SELECT_SQL, key1, key2);
		assertEquals(key1, row.get("key_1"));
		assertEquals(key2, row.get("key_2"));
		assertEquals("updated", row.get("a_string"));
		assertEquals(1, row.get("an_int"));
		assertEquals(true, row.get("a_bool"));
	}

	@Test
	public void updateWithMap() {
		String key1 = "spring";
		int key2 = 1;
		jdbcTemplate.update(INSERT_SQL, key1, key2, "original", 0, false);

		SimpleJdbcUpdate simpleJdbcUpdate = new SimpleJdbcUpdate(dataSource)
			.withTableName("dummy_table")
			.updatingColumns("a_string", "an_int", "a_bool")
			.restrictingColumns("key_1", "key_2");

		Map<String, Object> updatingValues = new HashMap<>();
		updatingValues.put("a_string", "updated");
		updatingValues.put("an_int", 1);
		updatingValues.put("a_bool", true);

		Map<String, Object> restrictingValues = new HashMap<>();
		restrictingValues.put("key_1", key1);
		restrictingValues.put("key_2", key2);

		int affected = simpleJdbcUpdate.execute(updatingValues, restrictingValues);
		assertEquals(1, affected);

		Map<String, Object> expectedRow = new HashMap<>(updatingValues);
		expectedRow.putAll(restrictingValues);
		Map<String, Object> row = jdbcTemplate.queryForMap(SELECT_SQL, key1, key2);
		assertEquals(expectedRow, row);
	}

	@Test
	public void updateWithColumnNamesFromMetaData() {
		String key1 = "spring";
		int key2 = 1;
		jdbcTemplate.update(INSERT_SQL, key1, key2, "original", 0, false);

		SimpleJdbcUpdate simpleJdbcUpdate = new SimpleJdbcUpdate(dataSource)
			.withTableName("dummy_table")
			// does not specify updating columns
			.restrictingColumns("key_1", "key_2");

		Map<String, Object> columnsValues = new HashMap<>();
		columnsValues.put("key_1", key1);
		columnsValues.put("key_2", key2);
		columnsValues.put("a_string", "updated");
		columnsValues.put("an_int", 1);
		columnsValues.put("a_bool", true);

		int affected = simpleJdbcUpdate.execute(columnsValues, columnsValues);
		assertEquals(1, affected);
	}
}
