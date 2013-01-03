/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.transaction.jta;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import com.ibm.wsspi.uow.UOWAction;
import com.ibm.wsspi.uow.UOWException;
import com.ibm.wsspi.uow.UOWManager;
import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.tests.mock.jndi.ExpectedLookupTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Juergen Hoeller
 */
public class WebSphereUowTransactionManagerTests extends TestCase {

	public void testUowManagerFoundInJndi() {
		MockUOWManager manager = new MockUOWManager();
		ExpectedLookupTemplate jndiTemplate =
				new ExpectedLookupTemplate(WebSphereUowTransactionManager.DEFAULT_UOW_MANAGER_NAME, manager);
		WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager();
		ptm.setJndiTemplate(jndiTemplate);
		ptm.afterPropertiesSet();

		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		assertEquals("result", ptm.execute(definition, new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				return "result";
			}
		}));

		assertEquals(UOWManager.UOW_TYPE_GLOBAL_TRANSACTION, manager.getUOWType());
		assertFalse(manager.getJoined());
		assertFalse(manager.getRollbackOnly());
	}

	public void testUowManagerAndUserTransactionFoundInJndi() throws Exception {
		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_ACTIVE, 2);
		ut.begin();
		utControl.setVoidCallable(1);
		ut.commit();
		utControl.setVoidCallable(1);
		utControl.replay();

		MockUOWManager manager = new MockUOWManager();
		ExpectedLookupTemplate jndiTemplate = new ExpectedLookupTemplate();
		jndiTemplate.addObject(WebSphereUowTransactionManager.DEFAULT_USER_TRANSACTION_NAME, ut);
		jndiTemplate.addObject(WebSphereUowTransactionManager.DEFAULT_UOW_MANAGER_NAME, manager);
		WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager();
		ptm.setJndiTemplate(jndiTemplate);
		ptm.afterPropertiesSet();

		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		TransactionStatus ts = ptm.getTransaction(definition);
		ptm.commit(ts);
		assertEquals("result", ptm.execute(definition, new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				return "result";
			}
		}));

		assertEquals(UOWManager.UOW_TYPE_GLOBAL_TRANSACTION, manager.getUOWType());
		assertFalse(manager.getJoined());
		assertFalse(manager.getRollbackOnly());
	}

	public void testPropagationMandatoryFailsInCaseOfNoExistingTransaction() {
		MockUOWManager manager = new MockUOWManager();
		WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_MANDATORY);

		try {
			ptm.execute(definition, new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					return "result";
				}
			});
			fail("Should have thrown IllegalTransactionStateException");
		}
		catch (IllegalTransactionStateException ex) {
			// expected
		}
	}

	public void testNewTransactionSynchronizationUsingPropagationSupports() {
		doTestNewTransactionSynchronization(
				TransactionDefinition.PROPAGATION_SUPPORTS, WebSphereUowTransactionManager.SYNCHRONIZATION_ALWAYS);
	}

	public void testNewTransactionSynchronizationUsingPropagationNotSupported() {
		doTestNewTransactionSynchronization(
				TransactionDefinition.PROPAGATION_NOT_SUPPORTED, WebSphereUowTransactionManager.SYNCHRONIZATION_ALWAYS);
	}

	public void testNewTransactionSynchronizationUsingPropagationNever() {
		doTestNewTransactionSynchronization(
				TransactionDefinition.PROPAGATION_NEVER, WebSphereUowTransactionManager.SYNCHRONIZATION_ALWAYS);
	}

	public void testNewTransactionSynchronizationUsingPropagationSupportsAndSynchOnActual() {
		doTestNewTransactionSynchronization(
				TransactionDefinition.PROPAGATION_SUPPORTS, WebSphereUowTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
	}

	public void testNewTransactionSynchronizationUsingPropagationNotSupportedAndSynchOnActual() {
		doTestNewTransactionSynchronization(
				TransactionDefinition.PROPAGATION_NOT_SUPPORTED, WebSphereUowTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
	}

	public void testNewTransactionSynchronizationUsingPropagationNeverAndSynchOnActual() {
		doTestNewTransactionSynchronization(
				TransactionDefinition.PROPAGATION_NEVER, WebSphereUowTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
	}

	public void testNewTransactionSynchronizationUsingPropagationSupportsAndSynchNever() {
		doTestNewTransactionSynchronization(
				TransactionDefinition.PROPAGATION_SUPPORTS, WebSphereUowTransactionManager.SYNCHRONIZATION_NEVER);
	}

	public void testNewTransactionSynchronizationUsingPropagationNotSupportedAndSynchNever() {
		doTestNewTransactionSynchronization(
				TransactionDefinition.PROPAGATION_NOT_SUPPORTED, WebSphereUowTransactionManager.SYNCHRONIZATION_NEVER);
	}

	public void testNewTransactionSynchronizationUsingPropagationNeverAndSynchNever() {
		doTestNewTransactionSynchronization(
				TransactionDefinition.PROPAGATION_NEVER, WebSphereUowTransactionManager.SYNCHRONIZATION_NEVER);
	}

	private void doTestNewTransactionSynchronization(int propagationBehavior, final int synchMode) {
		MockUOWManager manager = new MockUOWManager();
		WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		ptm.setTransactionSynchronization(synchMode);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setPropagationBehavior(propagationBehavior);
		definition.setReadOnly(true);

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		assertEquals("result", ptm.execute(definition, new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				if (synchMode == WebSphereUowTransactionManager.SYNCHRONIZATION_ALWAYS) {
					assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
					assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
					assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				}
				else {
					assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
					assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
					assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				}
				return "result";
			}
		}));

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		assertEquals(0, manager.getUOWTimeout());
		assertEquals(UOWManager.UOW_TYPE_LOCAL_TRANSACTION, manager.getUOWType());
		assertFalse(manager.getJoined());
		assertFalse(manager.getRollbackOnly());
	}

	public void testNewTransactionWithCommitUsingPropagationRequired() {
		doTestNewTransactionWithCommit(
				TransactionDefinition.PROPAGATION_REQUIRED, WebSphereUowTransactionManager.SYNCHRONIZATION_ALWAYS);
	}

	public void testNewTransactionWithCommitUsingPropagationRequiresNew() {
		doTestNewTransactionWithCommit(
				TransactionDefinition.PROPAGATION_REQUIRES_NEW, WebSphereUowTransactionManager.SYNCHRONIZATION_ALWAYS);
	}

	public void testNewTransactionWithCommitUsingPropagationNested() {
		doTestNewTransactionWithCommit(
				TransactionDefinition.PROPAGATION_NESTED, WebSphereUowTransactionManager.SYNCHRONIZATION_ALWAYS);
	}

	public void testNewTransactionWithCommitUsingPropagationRequiredAndSynchOnActual() {
		doTestNewTransactionWithCommit(
				TransactionDefinition.PROPAGATION_REQUIRED, WebSphereUowTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
	}

	public void testNewTransactionWithCommitUsingPropagationRequiresNewAndSynchOnActual() {
		doTestNewTransactionWithCommit(
				TransactionDefinition.PROPAGATION_REQUIRES_NEW, WebSphereUowTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
	}

	public void testNewTransactionWithCommitUsingPropagationNestedAndSynchOnActual() {
		doTestNewTransactionWithCommit(
				TransactionDefinition.PROPAGATION_NESTED, WebSphereUowTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
	}

	public void testNewTransactionWithCommitUsingPropagationRequiredAndSynchNever() {
		doTestNewTransactionWithCommit(
				TransactionDefinition.PROPAGATION_REQUIRED, WebSphereUowTransactionManager.SYNCHRONIZATION_NEVER);
	}

	public void testNewTransactionWithCommitUsingPropagationRequiresNewAndSynchNever() {
		doTestNewTransactionWithCommit(
				TransactionDefinition.PROPAGATION_REQUIRES_NEW, WebSphereUowTransactionManager.SYNCHRONIZATION_NEVER);
	}

	public void testNewTransactionWithCommitUsingPropagationNestedAndSynchNever() {
		doTestNewTransactionWithCommit(
				TransactionDefinition.PROPAGATION_NESTED, WebSphereUowTransactionManager.SYNCHRONIZATION_NEVER);
	}

	private void doTestNewTransactionWithCommit(int propagationBehavior, final int synchMode) {
		MockUOWManager manager = new MockUOWManager();
		WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		ptm.setTransactionSynchronization(synchMode);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setPropagationBehavior(propagationBehavior);
		definition.setReadOnly(true);

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		assertEquals("result", ptm.execute(definition, new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				if (synchMode != WebSphereUowTransactionManager.SYNCHRONIZATION_NEVER) {
					assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
					assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
					assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				}
				else {
					assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
					assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
					assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				}
				return "result";
			}
		}));

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		assertEquals(0, manager.getUOWTimeout());
		assertEquals(UOWManager.UOW_TYPE_GLOBAL_TRANSACTION, manager.getUOWType());
		assertFalse(manager.getJoined());
		assertFalse(manager.getRollbackOnly());
	}

	public void testNewTransactionWithCommitAndTimeout() {
		MockUOWManager manager = new MockUOWManager();
		WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setTimeout(10);
		definition.setReadOnly(true);

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		assertEquals("result", ptm.execute(definition, new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				return "result";
			}
		}));

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		assertEquals(10, manager.getUOWTimeout());
		assertEquals(UOWManager.UOW_TYPE_GLOBAL_TRANSACTION, manager.getUOWType());
		assertFalse(manager.getJoined());
		assertFalse(manager.getRollbackOnly());
	}

	public void testNewTransactionWithCommitException() {
		final RollbackException rex = new RollbackException();
		MockUOWManager manager = new MockUOWManager() {
			@Override
			public void runUnderUOW(int type, boolean join, UOWAction action) throws UOWException {
				throw new UOWException(rex);
			}
		};
		WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		try {
			ptm.execute(definition, new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
					assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
					assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
					return "result";
				}
			});
			fail("Should have thrown TransactionSystemException");
		}
		catch (TransactionSystemException ex) {
			// expected
			assertTrue(ex.getCause() instanceof UOWException);
			assertSame(rex, ex.getRootCause());
			assertSame(rex, ex.getMostSpecificCause());
		}

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		assertEquals(0, manager.getUOWTimeout());
	}

	public void testNewTransactionWithRollback() {
		MockUOWManager manager = new MockUOWManager();
		WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		try {
			ptm.execute(definition, new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
					assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
					assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
					throw new OptimisticLockingFailureException("");
				}
			});
			fail("Should have thrown OptimisticLockingFailureException");
		}
		catch (OptimisticLockingFailureException ex) {
			// expected
		}

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		assertEquals(0, manager.getUOWTimeout());
		assertEquals(UOWManager.UOW_TYPE_GLOBAL_TRANSACTION, manager.getUOWType());
		assertFalse(manager.getJoined());
		assertTrue(manager.getRollbackOnly());
	}

	public void testNewTransactionWithRollbackOnly() {
		MockUOWManager manager = new MockUOWManager();
		WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		assertEquals("result", ptm.execute(definition, new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				status.setRollbackOnly();
				return "result";
			}
		}));

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		assertEquals(0, manager.getUOWTimeout());
		assertEquals(UOWManager.UOW_TYPE_GLOBAL_TRANSACTION, manager.getUOWType());
		assertFalse(manager.getJoined());
		assertTrue(manager.getRollbackOnly());
	}

	public void testExistingNonSpringTransaction() {
		MockUOWManager manager = new MockUOWManager();
		manager.setUOWStatus(UOWManager.UOW_STATUS_ACTIVE);
		WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		assertEquals("result", ptm.execute(definition, new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				return "result";
			}
		}));

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		assertEquals(0, manager.getUOWTimeout());
		assertEquals(UOWManager.UOW_TYPE_GLOBAL_TRANSACTION, manager.getUOWType());
		assertTrue(manager.getJoined());
		assertFalse(manager.getRollbackOnly());
	}

	public void testPropagationNeverFailsInCaseOfExistingTransaction() {
		MockUOWManager manager = new MockUOWManager();
		manager.setUOWStatus(UOWManager.UOW_STATUS_ACTIVE);
		WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_NEVER);

		try {
			ptm.execute(definition, new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					return "result";
				}
			});
			fail("Should have thrown IllegalTransactionStateException");
		}
		catch (IllegalTransactionStateException ex) {
			// expected
		}
	}

	public void testPropagationNestedFailsInCaseOfExistingTransaction() {
		MockUOWManager manager = new MockUOWManager();
		manager.setUOWStatus(UOWManager.UOW_STATUS_ACTIVE);
		WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);

		try {
			ptm.execute(definition, new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					return "result";
				}
			});
			fail("Should have thrown NestedTransactionNotSupportedException");
		}
		catch (NestedTransactionNotSupportedException ex) {
			// expected
		}
	}

	public void testExistingTransactionWithParticipationUsingPropagationRequired() {
		doTestExistingTransactionWithParticipation(TransactionDefinition.PROPAGATION_REQUIRED);
	}

	public void testExistingTransactionWithParticipationUsingPropagationSupports() {
		doTestExistingTransactionWithParticipation(TransactionDefinition.PROPAGATION_SUPPORTS);
	}

	public void testExistingTransactionWithParticipationUsingPropagationMandatory() {
		doTestExistingTransactionWithParticipation(TransactionDefinition.PROPAGATION_MANDATORY);
	}

	private void doTestExistingTransactionWithParticipation(int propagationBehavior) {
		MockUOWManager manager = new MockUOWManager();
		final WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		final DefaultTransactionDefinition definition2 = new DefaultTransactionDefinition();
		definition2.setPropagationBehavior(propagationBehavior);
		definition2.setReadOnly(true);

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		assertEquals("result", ptm.execute(definition, new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertEquals("result2", ptm.execute(definition2, new TransactionCallback() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
						assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
						assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
						return "result2";
					}
				}));
				return "result";
			}
		}));

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		assertEquals(0, manager.getUOWTimeout());
		assertEquals(UOWManager.UOW_TYPE_GLOBAL_TRANSACTION, manager.getUOWType());
		assertTrue(manager.getJoined());
		assertFalse(manager.getRollbackOnly());
	}

	public void testExistingTransactionWithSuspensionUsingPropagationRequiresNew() {
		doTestExistingTransactionWithSuspension(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	public void testExistingTransactionWithSuspensionUsingPropagationNotSupported() {
		doTestExistingTransactionWithSuspension(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
	}

	private void doTestExistingTransactionWithSuspension(final int propagationBehavior) {
		MockUOWManager manager = new MockUOWManager();
		final WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		final DefaultTransactionDefinition definition2 = new DefaultTransactionDefinition();
		definition2.setPropagationBehavior(propagationBehavior);
		definition2.setReadOnly(true);

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		assertEquals("result", ptm.execute(definition, new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertEquals("result2", ptm.execute(definition2, new TransactionCallback() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
						assertEquals(propagationBehavior == TransactionDefinition.PROPAGATION_REQUIRES_NEW,
								TransactionSynchronizationManager.isActualTransactionActive());
						assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
						return "result2";
					}
				}));
				return "result";
			}
		}));

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		assertEquals(0, manager.getUOWTimeout());
		if (propagationBehavior == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
			assertEquals(UOWManager.UOW_TYPE_GLOBAL_TRANSACTION, manager.getUOWType());
		}
		else {
			assertEquals(UOWManager.UOW_TYPE_LOCAL_TRANSACTION, manager.getUOWType());
		}
		assertFalse(manager.getJoined());
		assertFalse(manager.getRollbackOnly());
	}

	public void testExistingTransactionUsingPropagationNotSupported() {
		MockUOWManager manager = new MockUOWManager();
		final WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		final DefaultTransactionDefinition definition2 = new DefaultTransactionDefinition();
		definition2.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
		definition2.setReadOnly(true);

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		assertEquals("result", ptm.execute(definition, new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertEquals("result2", ptm.execute(definition2, new TransactionCallback() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
						assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
						assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
						return "result2";
					}
				}));
				return "result";
			}
		}));

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		assertEquals(0, manager.getUOWTimeout());
		assertEquals(UOWManager.UOW_TYPE_LOCAL_TRANSACTION, manager.getUOWType());
		assertFalse(manager.getJoined());
		assertFalse(manager.getRollbackOnly());
	}

}
