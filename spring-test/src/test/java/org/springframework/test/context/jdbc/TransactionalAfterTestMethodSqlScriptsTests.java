/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.jdbc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.transaction.AfterTransaction;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

/**
 * Transactional integration tests for {@link Sql @Sql} that verify proper
 * support for {@link ExecutionPhase#AFTER_TEST_METHOD}.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@SpringJUnitConfig(EmptyDatabaseConfig.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
@DirtiesContext
class TransactionalAfterTestMethodSqlScriptsTests extends AbstractTransactionalTests {

	String testName;


	@BeforeEach
	void trackTestName(TestInfo testInfo) {
		this.testName = testInfo.getTestMethod().get().getName();
	}

	@AfterTransaction
	void afterTransaction() {
		if ("test01".equals(testName)) {
			// Should throw a BadSqlGrammarException after test01, assuming 'drop-schema.sql' was executed
			assertThatExceptionOfType(BadSqlGrammarException.class).isThrownBy(() -> assertNumUsers(99));
		}
	}

	@Test
	@SqlGroup({
		@Sql({ "schema.sql", "data.sql" }),
		@Sql(scripts = "drop-schema.sql", executionPhase = AFTER_TEST_METHOD)
	})
	// test## is required for @TestMethodOrder.
	void test01() {
		assertNumUsers(1);
	}

	@Test
	@Sql({ "schema.sql", "data.sql", "data-add-dogbert.sql" })
	// test## is required for @TestMethodOrder.
	void test02() {
		assertNumUsers(2);
	}

}
