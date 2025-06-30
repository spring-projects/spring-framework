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

package org.springframework.test.context.testng.transaction.programmatic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.testng.IHookCallBack;
import org.testng.ITestResult;
import org.testng.annotations.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.transaction.TransactionAssert.assertThatTransaction;

/**
 * This class is a copy of the JUnit-based
 * {@link org.springframework.test.context.transaction.programmatic.ProgrammaticTxMgmtTests}
 * class that has been modified to run with TestNG.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@ContextConfiguration
class ProgrammaticTxMgmtTestNGTests extends AbstractTransactionalTestNGSpringContextTests {

	private String methodName;


	@Override
	public void run(IHookCallBack callBack, ITestResult testResult) {
		this.methodName = testResult.getMethod().getMethodName();
		super.run(callBack, testResult);
	}

	@BeforeTransaction
	void beforeTransaction() {
		deleteFromTables("user");
		executeSqlScript("classpath:/org/springframework/test/context/jdbc/data.sql", false);
	}

	@AfterTransaction
	void afterTransaction() {
		switch (this.methodName) {
			case "commitTxAndStartNewTx", "commitTxButDoNotStartNewTx" -> assertUsers("Dogbert");
			case "rollbackTxAndStartNewTx", "rollbackTxButDoNotStartNewTx", "startTxWithExistingTransaction" ->
					assertUsers("Dilbert");
			case "rollbackTxAndStartNewTxWithDefaultCommitSemantics" -> assertUsers("Dilbert", "Dogbert");
			default -> fail("missing 'after transaction' assertion for test method: " + this.methodName);
		}
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void isActiveWithNonExistentTransactionContext() {
		assertThat(TestTransaction.isActive()).isFalse();
	}

	@Test(expectedExceptions = IllegalStateException.class)
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void flagForRollbackWithNonExistentTransactionContext() {
		TestTransaction.flagForRollback();
	}

	@Test(expectedExceptions = IllegalStateException.class)
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void flagForCommitWithNonExistentTransactionContext() {
		TestTransaction.flagForCommit();
	}

	@Test(expectedExceptions = IllegalStateException.class)
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void isFlaggedForRollbackWithNonExistentTransactionContext() {
		TestTransaction.isFlaggedForRollback();
	}

	@Test(expectedExceptions = IllegalStateException.class)
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void startTxWithNonExistentTransactionContext() {
		TestTransaction.start();
	}

	@Test(expectedExceptions = IllegalStateException.class)
	void startTxWithExistingTransaction() {
		TestTransaction.start();
	}

	@Test(expectedExceptions = IllegalStateException.class)
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void endTxWithNonExistentTransactionContext() {
		TestTransaction.end();
	}

	@Test
	void commitTxAndStartNewTx() {
		assertThatTransaction().isActive();
		assertThat(TestTransaction.isActive()).isTrue();
		assertUsers("Dilbert");
		deleteFromTables("user");
		assertUsers();

		// Commit
		TestTransaction.flagForCommit();
		assertThat(TestTransaction.isFlaggedForRollback()).isFalse();
		TestTransaction.end();
		assertThatTransaction().isNotActive();
		assertThat(TestTransaction.isActive()).isFalse();
		assertUsers();

		executeSqlScript("classpath:/org/springframework/test/context/jdbc/data-add-dogbert.sql", false);
		assertUsers("Dogbert");

		TestTransaction.start();
		assertThatTransaction().isActive();
		assertThat(TestTransaction.isActive()).isTrue();
	}

	@Test
	void commitTxButDoNotStartNewTx() {
		assertThatTransaction().isActive();
		assertThat(TestTransaction.isActive()).isTrue();
		assertUsers("Dilbert");
		deleteFromTables("user");
		assertUsers();

		// Commit
		TestTransaction.flagForCommit();
		assertThat(TestTransaction.isFlaggedForRollback()).isFalse();
		TestTransaction.end();
		assertThat(TestTransaction.isActive()).isFalse();
		assertThatTransaction().isNotActive();
		assertUsers();

		executeSqlScript("classpath:/org/springframework/test/context/jdbc/data-add-dogbert.sql", false);
		assertUsers("Dogbert");
	}

	@Test
	void rollbackTxAndStartNewTx() {
		assertThatTransaction().isActive();
		assertThat(TestTransaction.isActive()).isTrue();
		assertUsers("Dilbert");
		deleteFromTables("user");
		assertUsers();

		// Rollback (automatically)
		assertThat(TestTransaction.isFlaggedForRollback()).isTrue();
		TestTransaction.end();
		assertThat(TestTransaction.isActive()).isFalse();
		assertThatTransaction().isNotActive();
		assertUsers("Dilbert");

		// Start new transaction with default rollback semantics
		TestTransaction.start();
		assertThatTransaction().isActive();
		assertThat(TestTransaction.isFlaggedForRollback()).isTrue();
		assertThat(TestTransaction.isActive()).isTrue();

		executeSqlScript("classpath:/org/springframework/test/context/jdbc/data-add-dogbert.sql", false);
		assertUsers("Dilbert", "Dogbert");
	}

	@Test
	void rollbackTxButDoNotStartNewTx() {
		assertThatTransaction().isActive();
		assertThat(TestTransaction.isActive()).isTrue();
		assertUsers("Dilbert");
		deleteFromTables("user");
		assertUsers();

		// Rollback (automatically)
		assertThat(TestTransaction.isFlaggedForRollback()).isTrue();
		TestTransaction.end();
		assertThat(TestTransaction.isActive()).isFalse();
		assertThatTransaction().isNotActive();
		assertUsers("Dilbert");
	}

	@Test
	@Commit
	void rollbackTxAndStartNewTxWithDefaultCommitSemantics() {
		assertThatTransaction().isActive();
		assertThat(TestTransaction.isActive()).isTrue();
		assertUsers("Dilbert");
		deleteFromTables("user");
		assertUsers();

		// Rollback
		TestTransaction.flagForRollback();
		assertThat(TestTransaction.isFlaggedForRollback()).isTrue();
		TestTransaction.end();
		assertThat(TestTransaction.isActive()).isFalse();
		assertThatTransaction().isNotActive();
		assertUsers("Dilbert");

		// Start new transaction with default commit semantics
		TestTransaction.start();
		assertThatTransaction().isActive();
		assertThat(TestTransaction.isFlaggedForRollback()).isFalse();
		assertThat(TestTransaction.isActive()).isTrue();

		executeSqlScript("classpath:/org/springframework/test/context/jdbc/data-add-dogbert.sql", false);
		assertUsers("Dilbert", "Dogbert");
	}

	// -------------------------------------------------------------------------

	private void assertUsers(String... users) {
		List<String> expected = Arrays.asList(users);
		Collections.sort(expected);
		List<String> actual = jdbcTemplate.queryForList("select name from user", String.class);
		Collections.sort(actual);
		assertThat(actual).as("Users in database;").isEqualTo(expected);
	}


	// -------------------------------------------------------------------------

	@Configuration
	static class Config {

		@Bean
		PlatformTransactionManager transactionManager() {
			return new DataSourceTransactionManager(dataSource());
		}

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()
					.generateUniqueName(true)
					.addScript("classpath:/org/springframework/test/context/jdbc/schema.sql")
					.build();
		}
	}

}
