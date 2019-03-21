/*
 * Copyright 2002-2015 the original author or authors.
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

import javax.transaction.Status;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.junit.After;
import org.junit.Test;

import org.springframework.tests.mock.jndi.ExpectedLookupTemplate;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.jta.UserTransactionAdapter;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Juergen Hoeller
 * @since 05.08.2005
 */
public class JndiJtaTransactionManagerTests {

	@Test
	public void jtaTransactionManagerWithDefaultJndiLookups1() throws Exception {
		doTestJtaTransactionManagerWithDefaultJndiLookups("java:comp/TransactionManager", true, true);
	}

	@Test
	public void jtaTransactionManagerWithDefaultJndiLookups2() throws Exception {
		doTestJtaTransactionManagerWithDefaultJndiLookups("java:/TransactionManager", true, true);
	}

	@Test
	public void jtaTransactionManagerWithDefaultJndiLookupsAndNoTmFound() throws Exception {
		doTestJtaTransactionManagerWithDefaultJndiLookups("java:/tm", false, true);
	}

	@Test
	public void jtaTransactionManagerWithDefaultJndiLookupsAndNoUtFound() throws Exception {
		doTestJtaTransactionManagerWithDefaultJndiLookups("java:/TransactionManager", true, false);
	}

	private void doTestJtaTransactionManagerWithDefaultJndiLookups(String tmName, boolean tmFound, boolean defaultUt)
			throws Exception {

		UserTransaction ut = mock(UserTransaction.class);
		TransactionManager tm = mock(TransactionManager.class);
		if (defaultUt) {
			given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
		}
		else {
			given(tm.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
		}

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


		if (defaultUt) {
			verify(ut).begin();
			verify(ut).commit();
		}
		else {
			verify(tm).begin();
			verify(tm).commit();
		}

	}

	@Test
	public void jtaTransactionManagerWithCustomJndiLookups() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

		TransactionManager tm = mock(TransactionManager.class);

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

		verify(ut).begin();
		verify(ut).commit();
	}

	@Test
	public void jtaTransactionManagerWithNotCacheUserTransaction() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

		UserTransaction ut2 = mock(UserTransaction.class);
		given(ut2.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

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

		verify(ut).begin();
		verify(ut).commit();
		verify(ut2).begin();
		verify(ut2).commit();
	}

	/**
	 * Prevent any side-effects due to this test modifying ThreadLocals that might
	 * affect subsequent tests when all tests are run in the same JVM, as with Eclipse.
	 */
	@After
	public void tearDown() {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertNull(TransactionSynchronizationManager.getCurrentTransactionName());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}

}
