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
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Transactional integration tests for {@link Sql @Sql} support with
 * inlined SQL {@link Sql#statements statements}.
 *
 * @author Sam Brannen
 * @since 4.2
 * @see TransactionalSqlScriptsTests
 */
@SpringJUnitConfig(EmptyDatabaseConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql(
	scripts    = "schema.sql",
	statements = "INSERT INTO user VALUES('Dilbert')"
)
@DirtiesContext
class TransactionalInlinedStatementsSqlScriptsTests extends AbstractTransactionalTests {

	@Test
	@Order(1)
	void classLevelScripts() {
		assertNumUsers(1);
	}

	@Test
	@Sql(statements = "DROP TABLE user IF EXISTS")
	@Sql("schema.sql")
	@Sql(statements = "INSERT INTO user VALUES ('Dilbert'), ('Dogbert'), ('Catbert')")
	@Order(2)
	void methodLevelScripts() {
		assertNumUsers(3);
	}

}
