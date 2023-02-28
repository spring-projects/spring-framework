/*
 * Copyright 2002-2023 the original author or authors.
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import jakarta.persistence.SynchronizationType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @since 4.1.2
 */
class PersistenceContextTransactionTests {

	private EntityManagerFactory factory = mock();

	private EntityManager manager = mock();

	private EntityTransaction tx = mock();

	private TransactionTemplate tt = new TransactionTemplate(new JpaTransactionManager(factory));

	private EntityManagerHoldingBean bean = new EntityManagerHoldingBean();


	@BeforeEach
	void setup() {
		given(factory.createEntityManager()).willReturn(manager);
		given(manager.getTransaction()).willReturn(tx);
		given(manager.isOpen()).willReturn(true);

		@SuppressWarnings("serial")
		PersistenceAnnotationBeanPostProcessor pabpp = new PersistenceAnnotationBeanPostProcessor() {
			@Override
			protected EntityManagerFactory findEntityManagerFactory(@Nullable String unitName, String requestingBeanName) {
				return factory;
			}
		};
		pabpp.postProcessProperties(null, bean, "bean");

		assertThat(TransactionSynchronizationManager.getResourceMap().isEmpty()).isTrue();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
	}

	@AfterEach
	void clear() {
		assertThat(TransactionSynchronizationManager.getResourceMap().isEmpty()).isTrue();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
	}


	@Test
	void testTransactionCommitWithSharedEntityManager() {
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
	void testTransactionCommitWithSharedEntityManagerAndPropagationSupports() {
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
	void testTransactionCommitWithExtendedEntityManager() {
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
	void testTransactionCommitWithExtendedEntityManagerAndPropagationSupports() {
		given(manager.isOpen()).willReturn(true);

		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		tt.execute(status -> {
			bean.extendedEntityManager.flush();
			return null;
		});

		verify(manager).flush();
	}

	@Test
	void testTransactionCommitWithSharedEntityManagerUnsynchronized() {
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
	void testTransactionCommitWithSharedEntityManagerUnsynchronizedAndPropagationSupports() {
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
	void testTransactionCommitWithExtendedEntityManagerUnsynchronized() {
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
	void testTransactionCommitWithExtendedEntityManagerUnsynchronizedAndPropagationSupports() {
		given(manager.isOpen()).willReturn(true);

		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		tt.execute(status -> {
			bean.extendedEntityManagerUnsynchronized.flush();
			return null;
		});

		verify(manager).flush();
	}

	@Test
	void testTransactionCommitWithSharedEntityManagerUnsynchronizedJoined() {
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
	void testTransactionCommitWithExtendedEntityManagerUnsynchronizedJoined() {
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
	void testTransactionCommitWithExtendedEntityManagerUnsynchronizedJoinedAndPropagationSupports() {
		given(manager.isOpen()).willReturn(true);

		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		tt.execute(status -> {
			bean.extendedEntityManagerUnsynchronized.joinTransaction();
			bean.extendedEntityManagerUnsynchronized.flush();
			return null;
		});

		verify(manager).flush();
	}


	static class EntityManagerHoldingBean {

		@PersistenceContext
		EntityManager sharedEntityManager;

		@PersistenceContext(type = PersistenceContextType.EXTENDED)
		EntityManager extendedEntityManager;

		@PersistenceContext(synchronization = SynchronizationType.UNSYNCHRONIZED)
		EntityManager sharedEntityManagerUnsynchronized;

		@PersistenceContext(type = PersistenceContextType.EXTENDED, synchronization = SynchronizationType.UNSYNCHRONIZED)
		EntityManager extendedEntityManagerUnsynchronized;
	}

}
