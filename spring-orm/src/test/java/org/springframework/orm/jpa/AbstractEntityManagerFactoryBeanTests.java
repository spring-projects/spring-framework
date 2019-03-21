/*
 * Copyright 2002-2013 the original author or authors.
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

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceUnitInfo;

import org.junit.After;
import org.junit.Before;

import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

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

	@Before
	public void setUp() throws Exception {
		mockEmf = mock(EntityManagerFactory.class);
	}

	@After
	public void tearDown() throws Exception {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}

	protected void checkInvariants(AbstractEntityManagerFactoryBean demf) {
		assertTrue(EntityManagerFactory.class.isAssignableFrom(demf.getObjectType()));
		Object gotObject = demf.getObject();
		assertTrue("Object created by factory implements EntityManagerFactoryInfo",
				gotObject instanceof EntityManagerFactoryInfo);
		EntityManagerFactoryInfo emfi = (EntityManagerFactoryInfo) demf.getObject();
		assertSame("Successive invocations of getObject() return same object", emfi, demf.getObject());
		assertSame(emfi, demf.getObject());
		assertSame(emfi.getNativeEntityManagerFactory(), mockEmf);
	}


	protected static class DummyEntityManagerFactoryBean extends AbstractEntityManagerFactoryBean {

		private static final long serialVersionUID = 1L;

		private final EntityManagerFactory emf;

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
