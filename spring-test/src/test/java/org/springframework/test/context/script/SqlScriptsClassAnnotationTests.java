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

import org.junit.Test;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;

import static org.junit.Assert.*;

/**
 * Unit test to verify class level {@link SqlScripts} annotation.
 *
 * @author Tadaya Tsuyukubo
 */
@ContextConfiguration("/org/springframework/test/context/script/SqlScripts-SingleDataSource-context.xml")
@TestExecutionListeners(SqlScriptsTestExecutionListener.class)
@SqlScripts("db-add-bar.sql")
public class SqlScriptsClassAnnotationTests extends AbstractTransactionalJUnit4SpringContextTests {

	@Test
	public void testClassAnnotation() {
		assertEquals("expected foo and bar", 2, getPersonCount());
	}

	@Test
	@SqlScripts("db-add-baz.sql")
	public void testMethodAnnotationOverride() {
		assertEquals("expected foo and baz", 2, getPersonCount());
		int result = simpleJdbcTemplate.queryForInt("SELECT COUNT(*) FROM person WHERE name = 'baz'");
		assertEquals("expected baz instead of bar", 1, result);
	}

	private int getPersonCount() {
		return SimpleJdbcTestUtils.countRowsInTable(simpleJdbcTemplate, "person");
	}

}
