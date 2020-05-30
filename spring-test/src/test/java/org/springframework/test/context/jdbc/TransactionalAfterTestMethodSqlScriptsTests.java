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
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
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
