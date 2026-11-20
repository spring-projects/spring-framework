/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
