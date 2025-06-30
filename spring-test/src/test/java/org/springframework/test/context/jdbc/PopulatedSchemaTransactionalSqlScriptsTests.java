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

import org.junit.jupiter.api.Test;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;

/**
 * Transactional integration tests that verify rollback semantics for
 * {@link Sql @Sql} support.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@SpringJUnitConfig(PopulatedSchemaDatabaseConfig.class)
@DirtiesContext
class PopulatedSchemaTransactionalSqlScriptsTests extends AbstractTransactionalTests {

	@BeforeTransaction
	@AfterTransaction
	void verifyPreAndPostTransactionDatabaseState() {
		assertNumUsers(0);
	}

	@Test
	@SqlGroup(@Sql("data-add-dogbert.sql"))
	void methodLevelScripts() {
		assertNumUsers(1);
	}

}
