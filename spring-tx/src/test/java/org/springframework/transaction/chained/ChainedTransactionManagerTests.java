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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.springframework.transaction.HeuristicCompletionException.STATE_MIXED;
import static org.springframework.transaction.HeuristicCompletionException.STATE_ROLLED_BACK;
import static org.springframework.transaction.HeuristicCompletionException.getStateString;
import static org.springframework.transaction.chained.TestPlatformTransactionManager.createFailingTransactionManager;
import static org.springframework.transaction.chained.TestPlatformTransactionManager.createNonFailingTransactionManager;
import static org.springframework.transaction.chained.TransactionManagerMatcher.isCommitted;
import static org.springframework.transaction.chained.TransactionManagerMatcher.wasRolledback;

import org.junit.Test;
import org.springframework.transaction.HeuristicCompletionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Integration tests for {@link ChainedTransactionManager}.
 *
 * @author Michael Hunger
 * @author Oliver Gierke
 */
public class ChainedTransactionManagerTests {

	private ChainedTransactionManager tm;

	@Test
	public void shouldCompleteSuccessfully() {

		PlatformTransactionManager transactionManager = createNonFailingTransactionManager("single");
		setupTransactionManagers(transactionManager);

		createAndCommitTransaction();

		assertThat(transactionManager, isCommitted());
	}

	@Test
	public void shouldThrowRolledBackExceptionForSingleTMFailure() {

		setupTransactionManagers(createFailingTransactionManager("single"));

		try {
			createAndCommitTransaction();
			fail("Didn't throw the expected exception");
		} catch (HeuristicCompletionException e) {
			assertEquals(STATE_ROLLED_BACK, e.getOutcomeState());
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

		assertThat("second tm committed before first ", commitTime(first) >= commitTime(second), is(true));
	}

	@Test
	public void shouldThrowMixedRolledBackExceptionForNonFirstTMFailure() {

		setupTransactionManagers(createFailingTransactionManager("first"),
				createNonFailingTransactionManager("second"));

		try {
			createAndCommitTransaction();
			fail("Didn't throw the expected exception");
		} catch (HeuristicCompletionException e) {
			assertHeuristicException(STATE_MIXED, e.getOutcomeState());
		}
	}

	@Test
	public void shouldRollbackAllTransactionManagers() {

		PlatformTransactionManager first = createNonFailingTransactionManager("first");
		PlatformTransactionManager second = createNonFailingTransactionManager("second");

		setupTransactionManagers(first, second);
		createAndRollbackTransaction();

		assertThat(first, wasRolledback());
		assertThat(second, wasRolledback());
	}

	@Test(expected = UnexpectedRollbackException.class)
	public void shouldThrowExceptionOnFailingRollback() {
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
}

final class TestSynchronizationManager implements SynchronizationManager {

	private boolean synchronizationActive;

	@Override
	public void initSynchronization() {
		synchronizationActive = true;
	}

	@Override
	public boolean isSynchronizationActive() {
		return synchronizationActive;
	}

	@Override
	public void clearSynchronization() {
		synchronizationActive = false;
	}
}

