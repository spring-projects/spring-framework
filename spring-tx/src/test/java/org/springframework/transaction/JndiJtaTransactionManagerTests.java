/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.transaction;

import javax.transaction.Status;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.mock.jndi.ExpectedLookupTemplate;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.jta.UserTransactionAdapter;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Juergen Hoeller
 * @since 05.08.2005
 */
public class JndiJtaTransactionManagerTests extends TestCase {

	public void testJtaTransactionManagerWithDefaultJndiLookups1() throws Exception {
		doTestJtaTransactionManagerWithDefaultJndiLookups("java:comp/TransactionManager", true, true);
	}

	public void testJtaTransactionManagerWithDefaultJndiLookups2() throws Exception {
		doTestJtaTransactionManagerWithDefaultJndiLookups("java:/TransactionManager", true, true);
	}

	public void testJtaTransactionManagerWithDefaultJndiLookupsAndNoTmFound() throws Exception {
		doTestJtaTransactionManagerWithDefaultJndiLookups("java:/tm", false, true);
	}

	public void testJtaTransactionManagerWithDefaultJndiLookupsAndNoUtFound() throws Exception {
		doTestJtaTransactionManagerWithDefaultJndiLookups("java:/TransactionManager", true, false);
	}

	private void doTestJtaTransactionManagerWithDefaultJndiLookups(String tmName, boolean tmFound, boolean defaultUt)
			throws Exception {

		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
		if (defaultUt) {
			ut.getStatus();
			utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
			ut.getStatus();
			utControl.setReturnValue(Status.STATUS_ACTIVE, 2);
			ut.begin();
			utControl.setVoidCallable(1);
			ut.commit();
			utControl.setVoidCallable(1);
		}
		utControl.replay();

		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		if (!defaultUt) {
			tm.getStatus();
			tmControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
			tm.getStatus();
			tmControl.setReturnValue(Status.STATUS_ACTIVE, 2);
			tm.begin();
			tmControl.setVoidCallable(1);
			tm.commit();
			tmControl.setVoidCallable(1);
		}
		tmControl.replay();

		JtaTransactionManager ptm = new JtaTransactionManager();
		ExpectedLookupTemplate jndiTemplate = new ExpectedLookupTemplate();
		if (defaultUt) {
			jndiTemplate.addObject("java:comp/UserTransaction", ut);
		}
		jndiTemplate.addObject(tmName, tm);
		ptm.setJndiTemplate(jndiTemplate);
		ptm.afterPropertiesSet();

		if (tmFound) {
			assertEquals(tm, ptm.getTransactionManager());
		}
		else {
			assertNull(ptm.getTransactionManager());
		}

		if (defaultUt) {
			assertEquals(ut, ptm.getUserTransaction());
		}
		else {
			assertTrue(ptm.getUserTransaction() instanceof UserTransactionAdapter);
			UserTransactionAdapter uta = (UserTransactionAdapter) ptm.getUserTransaction();
			assertEquals(tm, uta.getTransactionManager());
		}

		TransactionTemplate tt = new TransactionTemplate(ptm);
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// something transactional
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
			}
		});
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		utControl.verify();
		tmControl.verify();
	}

	public void testJtaTransactionManagerWithCustomJndiLookups() throws Exception {
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

		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();

		JtaTransactionManager ptm = new JtaTransactionManager();
		ptm.setUserTransactionName("jndi-ut");
		ptm.setTransactionManagerName("jndi-tm");
		ExpectedLookupTemplate jndiTemplate = new ExpectedLookupTemplate();
		jndiTemplate.addObject("jndi-ut", ut);
		jndiTemplate.addObject("jndi-tm", tm);
		ptm.setJndiTemplate(jndiTemplate);
		ptm.afterPropertiesSet();

		assertEquals(ut, ptm.getUserTransaction());
		assertEquals(tm, ptm.getTransactionManager());

		TransactionTemplate tt = new TransactionTemplate(ptm);
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// something transactional
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
			}
		});
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		utControl.verify();
	}

	public void testJtaTransactionManagerWithNotCacheUserTransaction() throws Exception {
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

		MockControl ut2Control = MockControl.createControl(UserTransaction.class);
		UserTransaction ut2 = (UserTransaction) ut2Control.getMock();
		ut2.getStatus();
		ut2Control.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		ut2.getStatus();
		ut2Control.setReturnValue(Status.STATUS_ACTIVE, 2);
		ut2.begin();
		ut2Control.setVoidCallable(1);
		ut2.commit();
		ut2Control.setVoidCallable(1);
		ut2Control.replay();

		JtaTransactionManager ptm = new JtaTransactionManager();
		ptm.setJndiTemplate(new ExpectedLookupTemplate("java:comp/UserTransaction", ut));
		ptm.setCacheUserTransaction(false);
		ptm.afterPropertiesSet();

		assertEquals(ut, ptm.getUserTransaction());

		TransactionTemplate tt = new TransactionTemplate(ptm);
		assertEquals(JtaTransactionManager.SYNCHRONIZATION_ALWAYS, ptm.getTransactionSynchronization());
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// something transactional
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
			}
		});

		ptm.setJndiTemplate(new ExpectedLookupTemplate("java:comp/UserTransaction", ut2));
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// something transactional
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
			}
		});
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		utControl.verify();
		ut2Control.verify();
	}

	/**
	 * Prevent any side-effects due to this test modifying ThreadLocals that might
	 * affect subsequent tests when all tests are run in the same JVM, as with Eclipse.
	 */
	@Override
	protected void tearDown() {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertNull(TransactionSynchronizationManager.getCurrentTransactionName());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}

}
