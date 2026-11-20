/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import jakarta.persistence.SynchronizationType;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

		@SuppressWarnings("serial")
		PersistenceAnnotationBeanPostProcessor pabpp = new PersistenceAnnotationBeanPostProcessor() {
			@Override
			protected EntityManagerFactory findEntityManagerFactory(@Nullable String unitName, String requestingBeanName) {
				return factory;
			}
		};
		pabpp.postProcessProperties(null, bean, "bean");

		assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
	}

	@AfterEach
	void cleanup() {
		assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
	}


	@Test
	void transactionCommitWithSharedEntityManager() {
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
	void transactionCommitWithSharedEntityManagerAndPropagationSupports() {
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		tt.execute(status -> {
			bean.sharedEntityManager.clear();
			return null;
		});

		verify(manager).clear();
		verify(manager).close();
	}

	@Test
	void transactionCommitWithExtendedEntityManager() {
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
	void transactionCommitWithExtendedEntityManagerAndPropagationSupports() {
		given(manager.isOpen()).willReturn(true);

		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		tt.execute(status -> {
			bean.extendedEntityManager.flush();
			return null;
		});

		verify(manager).flush();
	}

	@Test
	void transactionCommitWithSharedEntityManagerUnsynchronized() {
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
	void transactionCommitWithSharedEntityManagerUnsynchronizedAndPropagationSupports() {
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
	void transactionCommitWithExtendedEntityManagerUnsynchronized() {
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
	void transactionCommitWithExtendedEntityManagerUnsynchronizedAndPropagationSupports() {
		given(manager.isOpen()).willReturn(true);

		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		tt.execute(status -> {
			bean.extendedEntityManagerUnsynchronized.flush();
			return null;
		});

		verify(manager).flush();
	}

	@Test
	void transactionCommitWithSharedEntityManagerUnsynchronizedJoined() {
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
	void transactionCommitWithExtendedEntityManagerUnsynchronizedJoined() {
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
	void transactionCommitWithExtendedEntityManagerUnsynchronizedJoinedAndPropagationSupports() {
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
