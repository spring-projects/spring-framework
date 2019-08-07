/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.orm.jpa.support;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.SynchronizationType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Juergen Hoeller
 * @since 4.1.2
 */
public class PersistenceContextTransactionTests {

	private EntityManagerFactory factory;

	private EntityManager manager;

	private EntityTransaction tx;

	private TransactionTemplate tt;

	private EntityManagerHoldingBean bean;


	@Before
	public void setUp() throws Exception {
		factory = mock(EntityManagerFactory.class);
		manager = mock(EntityManager.class);
		tx = mock(EntityTransaction.class);

		JpaTransactionManager tm = new JpaTransactionManager(factory);
		tt = new TransactionTemplate(tm);

		given(factory.createEntityManager()).willReturn(manager);
		given(manager.getTransaction()).willReturn(tx);
		given(manager.isOpen()).willReturn(true);

		bean = new EntityManagerHoldingBean();
		@SuppressWarnings("serial")
		PersistenceAnnotationBeanPostProcessor pabpp = new PersistenceAnnotationBeanPostProcessor() {
			@Override
			protected EntityManagerFactory findEntityManagerFactory(@Nullable String unitName, String requestingBeanName) {
				return factory;
			}
		};
		pabpp.postProcessPropertyValues(null, null, bean, "bean");

		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
	}

	@After
	public void tearDown() throws Exception {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}


	@Test
	public void testTransactionCommitWithSharedEntityManager() {
		given(manager.getTransaction()).willReturn(tx);

		tt.execute(status -> {
			bean.sharedEntityManager.flush();
			return null;
		});

		verify(tx).commit();
		verify(manager).flush();
		verify(manager).close();
	}

	@Test
	public void testTransactionCommitWithSharedEntityManagerAndPropagationSupports() {
		given(manager.isOpen()).willReturn(true);

		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		tt.execute(status -> {
			bean.sharedEntityManager.clear();
			return null;
		});

		verify(manager).clear();
		verify(manager).close();
	}

	@Test
	public void testTransactionCommitWithExtendedEntityManager() {
		given(manager.getTransaction()).willReturn(tx);

		tt.execute(status -> {
			bean.extendedEntityManager.flush();
			return null;
		});

		verify(tx, times(2)).commit();
		verify(manager).flush();
		verify(manager).close();
	}

	@Test
	public void testTransactionCommitWithExtendedEntityManagerAndPropagationSupports() {
		given(manager.isOpen()).willReturn(true);

		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		tt.execute(status -> {
			bean.extendedEntityManager.flush();
			return null;
		});

		verify(manager).flush();
	}

	@Test
	public void testTransactionCommitWithSharedEntityManagerUnsynchronized() {
		given(manager.getTransaction()).willReturn(tx);

		tt.execute(status -> {
			bean.sharedEntityManagerUnsynchronized.flush();
			return null;
		});

		verify(tx).commit();
		verify(manager).flush();
		verify(manager, times(2)).close();
	}

	@Test
	public void testTransactionCommitWithSharedEntityManagerUnsynchronizedAndPropagationSupports() {
		given(manager.isOpen()).willReturn(true);

		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		tt.execute(status -> {
			bean.sharedEntityManagerUnsynchronized.clear();
			return null;
		});

		verify(manager).clear();
		verify(manager).close();
	}

	@Test
	public void testTransactionCommitWithExtendedEntityManagerUnsynchronized() {
		given(manager.getTransaction()).willReturn(tx);

		tt.execute(status -> {
			bean.extendedEntityManagerUnsynchronized.flush();
			return null;
		});

		verify(tx).commit();
		verify(manager).flush();
		verify(manager).close();
	}

	@Test
	public void testTransactionCommitWithExtendedEntityManagerUnsynchronizedAndPropagationSupports() {
		given(manager.isOpen()).willReturn(true);

		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		tt.execute(status -> {
			bean.extendedEntityManagerUnsynchronized.flush();
			return null;
		});

		verify(manager).flush();
	}

	@Test
	public void testTransactionCommitWithSharedEntityManagerUnsynchronizedJoined() {
		given(manager.getTransaction()).willReturn(tx);

		tt.execute(status -> {
			bean.sharedEntityManagerUnsynchronized.joinTransaction();
			bean.sharedEntityManagerUnsynchronized.flush();
			return null;
		});

		verify(tx).commit();
		verify(manager).flush();
		verify(manager, times(2)).close();
	}

	@Test
	public void testTransactionCommitWithExtendedEntityManagerUnsynchronizedJoined() {
		given(manager.getTransaction()).willReturn(tx);

		tt.execute(status -> {
			bean.extendedEntityManagerUnsynchronized.joinTransaction();
			bean.extendedEntityManagerUnsynchronized.flush();
			return null;
		});

		verify(tx, times(2)).commit();
		verify(manager).flush();
		verify(manager).close();
	}

	@Test
	public void testTransactionCommitWithExtendedEntityManagerUnsynchronizedJoinedAndPropagationSupports() {
		given(manager.isOpen()).willReturn(true);

		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		tt.execute(status -> {
			bean.extendedEntityManagerUnsynchronized.joinTransaction();
			bean.extendedEntityManagerUnsynchronized.flush();
			return null;
		});

		verify(manager).flush();
	}


	public static class EntityManagerHoldingBean {

		@PersistenceContext
		public EntityManager sharedEntityManager;

		@PersistenceContext(type = PersistenceContextType.EXTENDED)
		public EntityManager extendedEntityManager;

		@PersistenceContext(synchronization = SynchronizationType.UNSYNCHRONIZED)
		public EntityManager sharedEntityManagerUnsynchronized;

		@PersistenceContext(type = PersistenceContextType.EXTENDED, synchronization = SynchronizationType.UNSYNCHRONIZED)
		public EntityManager extendedEntityManagerUnsynchronized;
	}

}
