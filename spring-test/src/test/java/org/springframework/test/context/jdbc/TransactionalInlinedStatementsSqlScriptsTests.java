/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	scripts = "schema.sql",
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
