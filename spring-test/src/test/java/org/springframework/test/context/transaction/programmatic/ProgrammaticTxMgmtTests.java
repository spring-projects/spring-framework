/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.transaction.programmatic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;
import static org.springframework.test.transaction.TransactionTestUtils.*;

/**
 * JUnit-based integration tests that verify support for programmatic transaction
 * management within the <em>Spring TestContext Framework</em>.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@ContextConfiguration
public class ProgrammaticTxMgmtTests extends AbstractTransactionalJUnit4SpringContextTests {

	@Rule
	public TestName testName = new TestName();


	@BeforeTransaction
	public void beforeTransaction() {
		deleteFromTables("user");
		executeSqlScript("classpath:/org/springframework/test/context/jdbc/data.sql", false);
	}

	@AfterTransaction
	public void afterTransaction() {
		String method = testName.getMethodName();
		switch (method) {
			case "commitTxAndStartNewTx":
			case "commitTxButDoNotStartNewTx": {
				assertUsers("Dogbert");
				break;
			}
			case "rollbackTxAndStartNewTx":
			case "rollbackTxButDoNotStartNewTx":
			case "startTxWithExistingTransaction": {
				assertUsers("Dilbert");
				break;
			}
			case "rollbackTxAndStartNewTxWithDefaultCommitSemantics": {
				assertUsers("Dilbert", "Dogbert");
				break;
			}
			default: {
				fail("missing 'after transaction' assertion for test method: " + method);
			}
		}
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void isActiveWithNonExistentTransactionContext() {
		assertFalse(TestTransaction.isActive());
	}

	@Test(expected = IllegalStateException.class)
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void flagForRollbackWithNonExistentTransactionContext() {
		TestTransaction.flagForRollback();
	}

	@Test(expected = IllegalStateException.class)
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void flagForCommitWithNonExistentTransactionContext() {
		TestTransaction.flagForCommit();
	}

	@Test(expected = IllegalStateException.class)
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void isFlaggedForRollbackWithNonExistentTransactionContext() {
		TestTransaction.isFlaggedForRollback();
	}

	@Test(expected = IllegalStateException.class)
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void startTxWithNonExistentTransactionContext() {
		TestTransaction.start();
	}

	@Test(expected = IllegalStateException.class)
	public void startTxWithExistingTransaction() {
		TestTransaction.start();
	}

	@Test(expected = IllegalStateException.class)
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void endTxWithNonExistentTransactionContext() {
		TestTransaction.end();
	}

	@Test
	public void commitTxAndStartNewTx() {
		assertInTransaction(true);
		assertTrue(TestTransaction.isActive());
		assertUsers("Dilbert");
		deleteFromTables("user");
		assertUsers();

		// Commit
		TestTransaction.flagForCommit();
		assertFalse(TestTransaction.isFlaggedForRollback());
		TestTransaction.end();
		assertInTransaction(false);
		assertFalse(TestTransaction.isActive());
		assertUsers();

		executeSqlScript("classpath:/org/springframework/test/context/jdbc/data-add-dogbert.sql", false);
		assertUsers("Dogbert");

		TestTransaction.start();
		assertInTransaction(true);
		assertTrue(TestTransaction.isActive());
	}

	@Test
	public void commitTxButDoNotStartNewTx() {
		assertInTransaction(true);
		assertTrue(TestTransaction.isActive());
		assertUsers("Dilbert");
		deleteFromTables("user");
		assertUsers();

		// Commit
		TestTransaction.flagForCommit();
		assertFalse(TestTransaction.isFlaggedForRollback());
		TestTransaction.end();
		assertFalse(TestTransaction.isActive());
		assertInTransaction(false);
		assertUsers();

		executeSqlScript("classpath:/org/springframework/test/context/jdbc/data-add-dogbert.sql", false);
		assertUsers("Dogbert");
	}

	@Test
	public void rollbackTxAndStartNewTx() {
		assertInTransaction(true);
		assertTrue(TestTransaction.isActive());
		assertUsers("Dilbert");
		deleteFromTables("user");
		assertUsers();

		// Rollback (automatically)
		assertTrue(TestTransaction.isFlaggedForRollback());
		TestTransaction.end();
		assertFalse(TestTransaction.isActive());
		assertInTransaction(false);
		assertUsers("Dilbert");

		// Start new transaction with default rollback semantics
		TestTransaction.start();
		assertInTransaction(true);
		assertTrue(TestTransaction.isFlaggedForRollback());
		assertTrue(TestTransaction.isActive());

		executeSqlScript("classpath:/org/springframework/test/context/jdbc/data-add-dogbert.sql", false);
		assertUsers("Dilbert", "Dogbert");
	}

	@Test
	public void rollbackTxButDoNotStartNewTx() {
		assertInTransaction(true);
		assertTrue(TestTransaction.isActive());
		assertUsers("Dilbert");
		deleteFromTables("user");
		assertUsers();

		// Rollback (automatically)
		assertTrue(TestTransaction.isFlaggedForRollback());
		TestTransaction.end();
		assertFalse(TestTransaction.isActive());
		assertInTransaction(false);
		assertUsers("Dilbert");
	}

	@Test
	@Rollback(false)
	public void rollbackTxAndStartNewTxWithDefaultCommitSemantics() {
		assertInTransaction(true);
		assertTrue(TestTransaction.isActive());
		assertUsers("Dilbert");
		deleteFromTables("user");
		assertUsers();

		// Rollback
		TestTransaction.flagForRollback();
		assertTrue(TestTransaction.isFlaggedForRollback());
		TestTransaction.end();
		assertFalse(TestTransaction.isActive());
		assertInTransaction(false);
		assertUsers("Dilbert");

		// Start new transaction with default commit semantics
		TestTransaction.start();
		assertInTransaction(true);
		assertFalse(TestTransaction.isFlaggedForRollback());
		assertTrue(TestTransaction.isActive());

		executeSqlScript("classpath:/org/springframework/test/context/jdbc/data-add-dogbert.sql", false);
		assertUsers("Dilbert", "Dogbert");
	}

	// -------------------------------------------------------------------------

	private void assertUsers(String... users) {
		List<String> expected = Arrays.asList(users);
		Collections.sort(expected);
		List<String> actual = jdbcTemplate.queryForList("select name from user", String.class);
		Collections.sort(actual);
		assertEquals("Users in database;", expected, actual);
	}


	// -------------------------------------------------------------------------

	@Configuration
	static class Config {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new DataSourceTransactionManager(dataSource());
		}

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()//
			.setName("programmatic-tx-mgmt-test-db")//
			.addScript("classpath:/org/springframework/test/context/jdbc/schema.sql") //
			.build();
		}
	}

}
