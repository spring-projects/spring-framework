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

package org.springframework.transaction;

import org.junit.After;
import org.junit.Test;

import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Juergen Hoeller
 * @since 29.04.2003
 */
public class TransactionSupportTests {

	@Test
	public void noExistingTransaction() {
		PlatformTransactionManager tm = new TestTransactionManager(false, true);
		DefaultTransactionStatus status1 = (DefaultTransactionStatus)
				tm.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS));
		assertFalse("Must not have transaction", status1.hasTransaction());

		DefaultTransactionStatus status2 = (DefaultTransactionStatus)
				tm.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED));
		assertTrue("Must have transaction", status2.hasTransaction());
		assertTrue("Must be new transaction", status2.isNewTransaction());

		assertThatExceptionOfType(IllegalTransactionStateException.class).isThrownBy(() ->
				tm.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_MANDATORY)));
	}

	@Test
	public void existingTransaction() {
		PlatformTransactionManager tm = new TestTransactionManager(true, true);
		DefaultTransactionStatus status1 = (DefaultTransactionStatus)
				tm.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS));
		assertTrue("Must have transaction", status1.getTransaction() != null);
		assertTrue("Must not be new transaction", !status1.isNewTransaction());

		DefaultTransactionStatus status2 = (DefaultTransactionStatus)
				tm.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED));
		assertTrue("Must have transaction", status2.getTransaction() != null);
		assertTrue("Must not be new transaction", !status2.isNewTransaction());

		DefaultTransactionStatus status3 = (DefaultTransactionStatus)
				tm.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_MANDATORY));
		assertTrue("Must have transaction", status3.getTransaction() != null);
		assertTrue("Must not be new transaction", !status3.isNewTransaction());
	}

	@Test
	public void commitWithoutExistingTransaction() {
		TestTransactionManager tm = new TestTransactionManager(false, true);
		TransactionStatus status = tm.getTransaction(null);
		tm.commit(status);

		assertTrue("triggered begin", tm.begin);
		assertTrue("triggered commit", tm.commit);
		assertFalse("no rollback", tm.rollback);
		assertFalse("no rollbackOnly", tm.rollbackOnly);
	}

	@Test
	public void rollbackWithoutExistingTransaction() {
		TestTransactionManager tm = new TestTransactionManager(false, true);
		TransactionStatus status = tm.getTransaction(null);
		tm.rollback(status);

		assertTrue("triggered begin", tm.begin);
		assertFalse("no commit", tm.commit);
		assertTrue("triggered rollback", tm.rollback);
		assertFalse("no rollbackOnly", tm.rollbackOnly);
	}

	@Test
	public void rollbackOnlyWithoutExistingTransaction() {
		TestTransactionManager tm = new TestTransactionManager(false, true);
		TransactionStatus status = tm.getTransaction(null);
		status.setRollbackOnly();
		tm.commit(status);

		assertTrue("triggered begin", tm.begin);
		assertFalse("no commit", tm.commit);
		assertTrue("triggered rollback", tm.rollback);
		assertFalse("no rollbackOnly", tm.rollbackOnly);
	}

	@Test
	public void commitWithExistingTransaction() {
		TestTransactionManager tm = new TestTransactionManager(true, true);
		TransactionStatus status = tm.getTransaction(null);
		tm.commit(status);

		assertFalse("no begin", tm.begin);
		assertFalse("no commit", tm.commit);
		assertFalse("no rollback", tm.rollback);
		assertFalse("no rollbackOnly", tm.rollbackOnly);
	}

	@Test
	public void rollbackWithExistingTransaction() {
		TestTransactionManager tm = new TestTransactionManager(true, true);
		TransactionStatus status = tm.getTransaction(null);
		tm.rollback(status);

		assertFalse("no begin", tm.begin);
		assertFalse("no commit", tm.commit);
		assertFalse("no rollback", tm.rollback);
		assertTrue("triggered rollbackOnly", tm.rollbackOnly);
	}

	@Test
	public void rollbackOnlyWithExistingTransaction() {
		TestTransactionManager tm = new TestTransactionManager(true, true);
		TransactionStatus status = tm.getTransaction(null);
		status.setRollbackOnly();
		tm.commit(status);

		assertFalse("no begin", tm.begin);
		assertFalse("no commit", tm.commit);
		assertFalse("no rollback", tm.rollback);
		assertTrue("triggered rollbackOnly", tm.rollbackOnly);
	}

	@Test
	public void transactionTemplate() {
		TestTransactionManager tm = new TestTransactionManager(false, true);
		TransactionTemplate template = new TransactionTemplate(tm);
		template.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
			}
		});

		assertTrue("triggered begin", tm.begin);
		assertTrue("triggered commit", tm.commit);
		assertFalse("no rollback", tm.rollback);
		assertFalse("no rollbackOnly", tm.rollbackOnly);
	}

	@Test
	public void transactionTemplateWithCallbackPreference() {
		MockCallbackPreferringTransactionManager ptm = new MockCallbackPreferringTransactionManager();
		TransactionTemplate template = new TransactionTemplate(ptm);
		template.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
			}
		});

		assertSame(template, ptm.getDefinition());
		assertFalse(ptm.getStatus().isRollbackOnly());
	}

	@Test
	public void transactionTemplateWithException() {
		TestTransactionManager tm = new TestTransactionManager(false, true);
		TransactionTemplate template = new TransactionTemplate(tm);
		final RuntimeException ex = new RuntimeException("Some application exception");
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
				template.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						throw ex;
					}
				}))
			.isSameAs(ex);
		assertTrue("triggered begin", tm.begin);
		assertFalse("no commit", tm.commit);
		assertTrue("triggered rollback", tm.rollback);
		assertFalse("no rollbackOnly", tm.rollbackOnly);
	}

	@SuppressWarnings("serial")
	@Test
	public void transactionTemplateWithRollbackException() {
		final TransactionSystemException tex = new TransactionSystemException("system exception");
		TestTransactionManager tm = new TestTransactionManager(false, true) {
			@Override
			protected void doRollback(DefaultTransactionStatus status) {
				super.doRollback(status);
				throw tex;
			}
		};
		TransactionTemplate template = new TransactionTemplate(tm);
		final RuntimeException ex = new RuntimeException("Some application exception");
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
				template.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						throw ex;
					}
				}))
			.isSameAs(tex);
		assertTrue("triggered begin", tm.begin);
		assertFalse("no commit", tm.commit);
		assertTrue("triggered rollback", tm.rollback);
		assertFalse("no rollbackOnly", tm.rollbackOnly);
	}

	@Test
	public void transactionTemplateWithError() {
		TestTransactionManager tm = new TestTransactionManager(false, true);
		TransactionTemplate template = new TransactionTemplate(tm);
		assertThatExceptionOfType(Error.class).isThrownBy(() ->
				template.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						throw new Error("Some application error");
					}
				}));
		assertTrue("triggered begin", tm.begin);
		assertFalse("no commit", tm.commit);
		assertTrue("triggered rollback", tm.rollback);
		assertFalse("no rollbackOnly", tm.rollbackOnly);
	}

	@Test
	public void transactionTemplateInitialization() {
		TestTransactionManager tm = new TestTransactionManager(false, true);
		TransactionTemplate template = new TransactionTemplate();
		template.setTransactionManager(tm);
		assertTrue("correct transaction manager set", template.getTransactionManager() == tm);

		assertThatIllegalArgumentException().isThrownBy(() ->
				template.setPropagationBehaviorName("TIMEOUT_DEFAULT"));
		template.setPropagationBehaviorName("PROPAGATION_SUPPORTS");
		assertTrue("Correct propagation behavior set", template.getPropagationBehavior() == TransactionDefinition.PROPAGATION_SUPPORTS);

		assertThatIllegalArgumentException().isThrownBy(() ->
				template.setPropagationBehavior(999));
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_MANDATORY);
		assertTrue("Correct propagation behavior set", template.getPropagationBehavior() == TransactionDefinition.PROPAGATION_MANDATORY);

		assertThatIllegalArgumentException().isThrownBy(() ->
				template.setIsolationLevelName("TIMEOUT_DEFAULT"));
		template.setIsolationLevelName("ISOLATION_SERIALIZABLE");
		assertTrue("Correct isolation level set", template.getIsolationLevel() == TransactionDefinition.ISOLATION_SERIALIZABLE);

		assertThatIllegalArgumentException().isThrownBy(() ->
				template.setIsolationLevel(999));

		template.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		assertTrue("Correct isolation level set", template.getIsolationLevel() == TransactionDefinition.ISOLATION_REPEATABLE_READ);
	}

	@Test
	public void transactionTemplateEquality() {
		TestTransactionManager tm1 = new TestTransactionManager(false, true);
		TestTransactionManager tm2 = new TestTransactionManager(false, true);
		TransactionTemplate template1 = new TransactionTemplate(tm1);
		TransactionTemplate template2 = new TransactionTemplate(tm2);
		TransactionTemplate template3 = new TransactionTemplate(tm2);

		assertNotEquals(template1, template2);
		assertNotEquals(template1, template3);
		assertEquals(template2, template3);
	}


	@After
	public void clear() {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
	}

}
