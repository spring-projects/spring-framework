/*
 * Copyright 2002-2024 the original author or authors.
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
	void setUp() {
		mockEmf = mock();
	}

	@AfterEach
	void tearDown() {
		assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
	}

	protected void checkInvariants(AbstractEntityManagerFactoryBean demf) {
		assertThat(EntityManagerFactory.class.isAssignableFrom(demf.getObjectType())).isTrue();
		Object gotObject = demf.getObject();
		boolean condition = gotObject instanceof EntityManagerFactoryInfo;
		assertThat(condition).as("Object created by factory implements EntityManagerFactoryInfo").isTrue();
		EntityManagerFactoryInfo emfi = (EntityManagerFactoryInfo) demf.getObject();
		assertThat(demf.getObject()).as("Successive invocations of getObject() return same object").isSameAs(emfi);
		assertThat(demf.getObject()).isSameAs(emfi);
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
