/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.transaction;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.Timeout;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.transaction.TransactionAssert.assertThatTransaction;

/**
 * Integration test which verifies proper transactional behavior when the default
 * rollback flag is explicitly set to {@code true} via {@link Rollback @Rollback}.
 *
 * <p>Also tests configuration of the transaction manager qualifier configured
 * via {@link Transactional @Transactional}.
 *
 * @author Sam Brannen
 * @since 4.2
 * @see Rollback
 * @see Transactional#transactionManager
 * @see DefaultRollbackFalseRollbackAnnotationTransactionalTests
 */
@SpringJUnitConfig(classes = EmbeddedPersonDatabaseTestsConfig.class, inheritLocations = false)
@Transactional("txMgr")
@Rollback(true)
@TestInstance(Lifecycle.PER_CLASS)
class DefaultRollbackTrueRollbackAnnotationTransactionalTests extends AbstractTransactionalSpringTests {

	private static int originalNumRows;

	JdbcTemplate jdbcTemplate;


	@Autowired
	void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}


	@BeforeEach
	void verifyInitialTestData() {
		originalNumRows = clearPersonTable(jdbcTemplate);
		assertThat(addPerson(jdbcTemplate, BOB)).as("Adding bob").isEqualTo(1);
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the initial number of rows in the person table.").isEqualTo(1);
	}

	@Test
	@Timeout(1)
	void modifyTestDataWithinTransaction() {
		assertThatTransaction().isActive();
		assertThat(addPerson(jdbcTemplate, JANE)).as("Adding jane").isEqualTo(1);
		assertThat(addPerson(jdbcTemplate, SUE)).as("Adding sue").isEqualTo(1);
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table within a transaction.").isEqualTo(3);
	}

	@AfterAll
	void verifyFinalTestData() {
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the final number of rows in the person table after all tests.").isEqualTo(originalNumRows);
	}

}
