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

package org.springframework.test.context.jdbc;

import org.junit.Test;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.context.jdbc.Sql.MergeMode.OVERRIDE;

/**
 * Transactional integration tests for {@link Sql @Sql} that verify proper
 * overriding support for class-level and method-level declarations.
 *
 * @author Dmitry Semukhin
 * @author Sam Brannen
 * @since 5.2
 */
@ContextConfiguration(classes = EmptyDatabaseConfig.class)
@Sql({ "schema.sql", "data-add-catbert.sql" })
@DirtiesContext
public class SqlMethodOverrideTests extends AbstractTransactionalJUnit4SpringContextTests {

	@Test
	@Sql(
		scripts = { "schema.sql", "data.sql", "data-add-dogbert.sql", "data-add-catbert.sql" },
		mergeMode = OVERRIDE
	)
	public void methodLevelSqlScriptsOverrideClassLevelScripts() {
		assertNumUsers(3);
	}

	protected void assertNumUsers(int expected) {
		assertEquals("Number of rows in the 'user' table.", expected, countRowsInTable("user"));
	}

}
