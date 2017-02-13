/*
 * Copyright 2011-2013 the original author or authors.
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
package org.springframework.transaction.chained;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.transaction.chained.ChainedTransactionManagerTests.TestPlatformTransactionManager.*;
import static org.springframework.transaction.chained.ChainedTransactionManagerTests.TransactionManagerMatcher.*;
import static org.springframework.transaction.HeuristicCompletionException.*;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.junit.Test;
import org.springframework.transaction.HeuristicCompletionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Integration tests for {@link ChainedTransactionManager}.
 *
 * @author Michael Hunger
 * @author Oliver Gierke
 * @since 1.6
 */
public class ChainedTransactionManagerTests {

	ChainedTransactionManager tm;

	@Test
	public void shouldCompleteSuccessfully() throws Exception {

		PlatformTransactionManager transactionManager = createNonFailingTransactionManager("single");
		setupTransactionManagers(transactionManager);

		createAndCommitTransaction();

		assertThat(transactionManager, isCommitted());
	}

	@Test
	public void shouldThrowRolledBackExceptionForSingleTMFailure() throws Exception {

		setupTransactionManagers(createFailingTransactionManager("single"));

		try {
			createAndCommitTransaction();
			fail("Didn't throw the expected exception");
		} catch (HeuristicCompletionException e) {
			assertEquals(HeuristicCompletionException.STATE_ROLLED_BACK, e.getOutcomeState());
		}
	}

	@Test
	public void shouldCommitAllRegisteredTransactionManagers() {

		PlatformTransactionManager first = createNonFailingTransactionManager("first");
		PlatformTransactionManager second = createNonFailingTransactionManager("second");

		setupTransactionManagers(first, second);
		createAndCommitTransaction();

		assertThat(first, isCommitted());
		assertThat(second, isCommitted());
	}

	@Test
	public void shouldCommitInReverseOrder() {

		PlatformTransactionManager first = createNonFailingTransactionManager("first");
		PlatformTransactionManager second = createNonFailingTransactionManager("second");

		setupTransactionManagers(first, second);
		createAndCommitTransaction();

		assertThat("second tm commited before first ", commitTime(first) >= commitTime(second), is(true));
	}

	@Test
	public void shouldThrowMixedRolledBackExceptionForNonFirstTMFailure() throws Exception {

		setupTransactionManagers(TestPlatformTransactionManager.createFailingTransactionManager("first"),
				createNonFailingTransactionManager("second"));

		try {
			createAndCommitTransaction();
			fail("Didn't throw the expected exception");
		} catch (HeuristicCompletionException e) {
			assertHeuristicException(HeuristicCompletionException.STATE_MIXED, e.getOutcomeState());
		}
	}

	@Test
	public void shouldRollbackAllTransactionManagers() throws Exception {

		PlatformTransactionManager first = createNonFailingTransactionManager("first");
		PlatformTransactionManager second = createNonFailingTransactionManager("second");

		setupTransactionManagers(first, second);
		createAndRollbackTransaction();

		assertThat(first, wasRolledback());
		assertThat(second, wasRolledback());

	}

	@Test(expected = UnexpectedRollbackException.class)
	public void shouldThrowExceptionOnFailingRollback() throws Exception {
		PlatformTransactionManager first = createFailingTransactionManager("first");
		setupTransactionManagers(first);
		createAndRollbackTransaction();
	}

	private void setupTransactionManagers(PlatformTransactionManager... transactionManagers) {
		tm = new ChainedTransactionManager(new TestSynchronizationManager(), transactionManagers);
	}

	private void createAndRollbackTransaction() {
		MultiTransactionStatus transaction = tm.getTransaction(new DefaultTransactionDefinition());
		tm.rollback(transaction);
	}

	private void createAndCommitTransaction() {
		MultiTransactionStatus transaction = tm.getTransaction(new DefaultTransactionDefinition());
		tm.commit(transaction);
	}

	private static void assertHeuristicException(int expected, int actual) {
		assertThat(getStateString(actual), is(getStateString(expected)));
	}

	private static Long commitTime(PlatformTransactionManager transactionManager) {
		return ((TestPlatformTransactionManager) transactionManager).getCommitTime();
	}

	static class TestSynchronizationManager implements SynchronizationManager {

		private boolean synchronizationActive;

		public void initSynchronization() {
			synchronizationActive = true;
		}

		public boolean isSynchronizationActive() {
			return synchronizationActive;
		}

		public void clearSynchronization() {
			synchronizationActive = false;
		}
	}

	static class TestPlatformTransactionManager implements org.springframework.transaction.PlatformTransactionManager {

		private final String name;
		private Long commitTime;
		private Long rollbackTime;

		public TestPlatformTransactionManager(String name) {
			this.name = name;
		}

		@Factory
		static PlatformTransactionManager createFailingTransactionManager(String name) {
			return new TestPlatformTransactionManager(name + "-failing") {
				@Override
				public void commit(TransactionStatus status) throws TransactionException {
					throw new RuntimeException();
				}

				@Override
				public void rollback(TransactionStatus status) throws TransactionException {
					throw new RuntimeException();
				}
			};
		}

		@Factory
		static PlatformTransactionManager createNonFailingTransactionManager(String name) {
			return new TestPlatformTransactionManager(name + "-non-failing");
		}

		@Override
		public String toString() {
			return name + (isCommitted() ? " (committed) " : " (not committed)");
		}

		public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
			return new TestTransactionStatus(definition);
		}

		public void commit(TransactionStatus status) throws TransactionException {
			commitTime = System.currentTimeMillis();
		}

		public void rollback(TransactionStatus status) throws TransactionException {
			rollbackTime = System.currentTimeMillis();
		}

		public boolean isCommitted() {
			return commitTime != null;
		}

		public boolean wasRolledBack() {
			return rollbackTime != null;
		}

		public Long getCommitTime() {
			return commitTime;
		}

		static class TestTransactionStatus implements org.springframework.transaction.TransactionStatus {

			public TestTransactionStatus(TransactionDefinition definition) {
			}

			public boolean isNewTransaction() {
				return false;
			}

			public boolean hasSavepoint() {
				return false;
			}

			public void setRollbackOnly() {

			}

			public boolean isRollbackOnly() {
				return false;
			}

			public void flush() {

			}

			public boolean isCompleted() {
				return false;
			}

			public Object createSavepoint() throws TransactionException {
				return null;
			}

			public void rollbackToSavepoint(Object savepoint) throws TransactionException {

			}

			public void releaseSavepoint(Object savepoint) throws TransactionException {

			}
		}
	}

	static class TransactionManagerMatcher extends
			org.hamcrest.TypeSafeMatcher<org.springframework.transaction.PlatformTransactionManager> {

		private final boolean commitCheck;

		public TransactionManagerMatcher(boolean commitCheck) {
			this.commitCheck = commitCheck;
		}

		@Override
		public boolean matchesSafely(PlatformTransactionManager platformTransactionManager) {
			TestPlatformTransactionManager ptm = (TestPlatformTransactionManager) platformTransactionManager;
			if (commitCheck) {
				return ptm.isCommitted();
			} else {
				return ptm.wasRolledBack();
			}

		}

		public void describeTo(Description description) {
			description.appendText("that a " + (commitCheck ? "committed" : "rolled-back") + " TransactionManager");
		}

		@Factory
		public static TransactionManagerMatcher isCommitted() {
			return new TransactionManagerMatcher(true);
		}

		@Factory
		public static TransactionManagerMatcher wasRolledback() {
			return new TransactionManagerMatcher(false);
		}
	}
}
