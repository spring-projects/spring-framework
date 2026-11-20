/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.transaction;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.test.annotation.Rollback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.transaction.TransactionAssert.assertThatTransaction;

/**
 * Extension of {@link DefaultRollbackTrueRollbackAnnotationTransactionalTests}
 * which tests method-level <em>rollback override</em> behavior via the
 * {@link Rollback @Rollback} annotation.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see Rollback
 */
class RollbackOverrideDefaultRollbackTrueTransactionalTests
		extends DefaultRollbackTrueRollbackAnnotationTransactionalTests {

	@BeforeEach
	@Override
	void verifyInitialTestData() {
		clearPersonTable(jdbcTemplate);
		assertThat(addPerson(jdbcTemplate, BOB)).as("Adding bob").isEqualTo(1);
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the initial number of rows in the person table.").isEqualTo(1);
	}

	@Test
	@Rollback(false)
	@Override
	void modifyTestDataWithinTransaction() {
		assertThatTransaction().isActive();
		assertThat(addPerson(jdbcTemplate, JANE)).as("Adding jane").isEqualTo(1);
		assertThat(addPerson(jdbcTemplate, SUE)).as("Adding sue").isEqualTo(1);
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table within a transaction.").isEqualTo(3);
	}

	@AfterAll
	@Override
	void verifyFinalTestData() {
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the final number of rows in the person table after all tests.").isEqualTo(3);
	}

}
