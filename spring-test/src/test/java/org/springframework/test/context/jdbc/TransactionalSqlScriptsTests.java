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

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Transactional integration tests for {@link Sql @Sql} support.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@SpringJUnitConfig(EmptyDatabaseConfig.class)
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
@Sql({ "schema.sql", "data.sql" })
@DirtiesContext
class TransactionalSqlScriptsTests extends AbstractTransactionalTests {

	@Test
	void classLevelScripts() {
		assertNumUsers(1);
	}

	@Test
	@Sql({ "recreate-schema.sql", "data.sql", "data-add-dogbert.sql" })
	void methodLevelScripts() {
		assertNumUsers(2);
	}

}
