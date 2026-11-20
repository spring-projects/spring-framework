/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.jdbc;

import org.junit.jupiter.api.Test;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.SqlConfig.TransactionMode;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;

/**
 * Transactional integration tests that verify commit semantics for
 * {@link SqlConfig#transactionMode} and {@link TransactionMode#ISOLATED}.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@SpringJUnitConfig(PopulatedSchemaDatabaseConfig.class)
@DirtiesContext
class IsolatedTransactionModeSqlScriptsTests extends AbstractTransactionalTests {

	@BeforeTransaction
	void beforeTransaction() {
		assertNumUsers(0);
	}

	@Test
	@SqlGroup(@Sql(scripts = "data-add-dogbert.sql", config = @SqlConfig(transactionMode = TransactionMode.ISOLATED)))
	void methodLevelScripts() {
		assertNumUsers(1);
	}

	@AfterTransaction
	void afterTransaction() {
		assertNumUsers(1);
	}

}
