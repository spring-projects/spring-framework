/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.context.script;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.junit.Test;

import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;

import static org.junit.Assert.*;

/**
 * Unit test to verify {@link SqlScripts} in multiple datasource.
 *
 * @author Tadaya Tsuyukubo
 */
@ContextConfiguration("/org/springframework/test/context/script/SqlScripts-MultipleDataSource-context.xml")
@TestExecutionListeners(SqlScriptsTestExecutionListener.class)
public class SqlScriptsMultiDatasourceTests extends AbstractJUnit4SpringContextTests {

	@Resource
	private DataSource dataSourceA;

	@Resource
	private DataSource dataSourceB;

	@Test
	@SqlScripts(value = "db-add-bar.sql", dataSource = "dataSourceB")
	public void testSpecificDatasource() {
		assertEquals("expected only foo in datasourceA", 1, getPersonCount(dataSourceA));
		assertEquals("expected foo and bar in datasourceB", 2, getPersonCount(dataSourceB));
	}

	@SuppressWarnings("deprecation")
	private int getPersonCount(DataSource dataSource) {
		return SimpleJdbcTestUtils.countRowsInTable(new SimpleJdbcTemplate(dataSource), "person");
	}

}
