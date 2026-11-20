/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.spi.PersistenceUnitInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Superclass for unit tests for EntityManagerFactory-creating beans.
 * Note: Subclasses must set expectations on the mock EntityManagerFactory.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
public abstract class AbstractEntityManagerFactoryBeanTests {

	protected static EntityManagerFactory mockEmf;


	@BeforeEach
	void setup() {
		mockEmf = mock();
	}

	@AfterEach
	void cleanup() {
		assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
	}

	protected void checkInvariants(AbstractEntityManagerFactoryBean emfb) {
		assertThat(EntityManagerFactory.class.isAssignableFrom(emfb.getObjectType())).isTrue();
		EntityManagerFactory emf = emfb.getObject();
		assertThat(emf).as("Object created by factory implements EntityManagerFactoryInfo").isInstanceOf(EntityManagerFactoryInfo.class);
		EntityManagerFactoryInfo emfi = (EntityManagerFactoryInfo) emf;
		assertThat(emfb.getObject()).as("Successive invocations of getObject() return same object").isSameAs(emfi);
		assertThat(emfb.getObject()).isSameAs(emfi);
		assertThat(mockEmf).isSameAs(emfi.getNativeEntityManagerFactory());
	}


	protected static class DummyEntityManagerFactoryBean extends AbstractEntityManagerFactoryBean {

		private static final long serialVersionUID = 1L;

		private final transient EntityManagerFactory emf;

		public DummyEntityManagerFactoryBean(EntityManagerFactory emf) {
			this.emf = emf;
		}

		@Override
		protected EntityManagerFactory createNativeEntityManagerFactory() throws PersistenceException {
			return emf;
		}

		@Override
		public PersistenceUnitInfo getPersistenceUnitInfo() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getPersistenceUnitName() {
			return "test";
		}
	}

}
