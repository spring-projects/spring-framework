/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.context.configuration.interfaces;

import org.junit.Test;

import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import static org.junit.Assert.*;

/**
 * @author Sam Brannen
 * @since 4.3
 */
public class SqlConfigInterfaceTests extends AbstractTransactionalJUnit4SpringContextTests
		implements SqlConfigTestInterface {

	@Test
	@Sql(scripts = "/org/springframework/test/context/jdbc/schema.sql", //
			config = @SqlConfig(separator = ";"))
	@Sql("/org/springframework/test/context/jdbc/data-add-users-with-custom-script-syntax.sql")
	public void methodLevelScripts() {
		assertNumUsers(3);
	}

	protected void assertNumUsers(int expected) {
		assertEquals("Number of rows in the 'user' table.", expected, countRowsInTable("user"));
	}

}
