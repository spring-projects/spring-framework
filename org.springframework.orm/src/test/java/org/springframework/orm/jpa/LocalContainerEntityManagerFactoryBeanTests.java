/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.orm.jpa;

import java.util.Map;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.easymock.MockControl;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.util.SerializationTestUtils;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class LocalContainerEntityManagerFactoryBeanTests extends AbstractEntityManagerFactoryBeanTests {

	// Static fields set by inner class DummyPersistenceProvider

	private static Map actualProps;

	private static PersistenceUnitInfo actualPui;


	public void testValidPersistenceUnit() throws Exception {
		parseValidPersistenceUnit();
	}

	public void testExceptionTranslationWithNoDialect() throws Exception {
		LocalContainerEntityManagerFactoryBean cefb = parseValidPersistenceUnit();
		EntityManagerFactory emf = cefb.getObject();
		assertNull("No dialect set", cefb.getJpaDialect());

		RuntimeException in1 = new RuntimeException("in1");
		PersistenceException in2 = new PersistenceException();
		assertNull("No translation here", cefb.translateExceptionIfPossible(in1));
		DataAccessException dex = cefb.translateExceptionIfPossible(in2);
		assertNotNull(dex);
		assertSame(in2, dex.getCause());
	}

	public void testEntityManagerFactoryIsProxied() throws Exception {
		LocalContainerEntityManagerFactoryBean cefb = parseValidPersistenceUnit();
		EntityManagerFactory emf = cefb.getObject();
		assertSame("EntityManagerFactory reference must be cached after init", emf, cefb.getObject());

		assertNotSame("EMF must be proxied", mockEmf, emf);
		assertTrue(emf.equals(emf));

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.setSerializationId("emf-bf");
		bf.registerSingleton("emf", cefb);
		cefb.setBeanFactory(bf);
		cefb.setBeanName("emf");
		assertNotNull(SerializationTestUtils.serializeAndDeserialize(emf));
	}

	public void testApplicationManagedEntityManagerWithoutTransaction() throws Exception {
		Object testEntity = new Object();

		MockControl emMc = MockControl.createControl(EntityManager.class);
		EntityManager mockEm = (EntityManager) emMc.getMock();
		mockEm.contains(testEntity);
		emMc.setReturnValue(false);
		emMc.replay();

		// finish recording mock calls
		mockEmf.createEntityManager();
		emfMc.setReturnValue(mockEm);
		mockEmf.close();
		emfMc.setVoidCallable();
		emfMc.replay();

		LocalContainerEntityManagerFactoryBean cefb = parseValidPersistenceUnit();
		EntityManagerFactory emf = cefb.getObject();
		assertSame("EntityManagerFactory reference must be cached after init", emf, cefb.getObject());

		assertNotSame("EMF must be proxied", mockEmf, emf);
		EntityManager em = emf.createEntityManager();
		assertFalse(em.contains(testEntity));

		cefb.destroy();

		emfMc.verify();
		emMc.verify();
	}

	public void testApplicationManagedEntityManagerWithTransaction() throws Exception {
		Object testEntity = new Object();

		MockControl tmMc = MockControl.createControl(EntityTransaction.class);
		EntityTransaction mockTx = (EntityTransaction) tmMc.getMock();
		mockTx.isActive();
		tmMc.setReturnValue(false);
		mockTx.begin();
		tmMc.setVoidCallable();
		mockTx.commit();
		tmMc.setVoidCallable();
		tmMc.replay();

		// This one's for the tx (shared)
		MockControl sharedEmMc = MockControl.createControl(EntityManager.class);
		EntityManager sharedEm = (EntityManager) sharedEmMc.getMock();
		sharedEm.getTransaction();
		sharedEmMc.setReturnValue(new NoOpEntityTransaction(), 3);
		sharedEm.close();
		sharedEmMc.setVoidCallable();
		sharedEmMc.replay();
		mockEmf.createEntityManager();
		emfMc.setReturnValue(sharedEm);

		// This is the application-specific one
		MockControl emMc = MockControl.createControl(EntityManager.class);
		EntityManager mockEm = (EntityManager) emMc.getMock();
		mockEm.getTransaction();
		emMc.setReturnValue(mockTx, 3);
		mockEm.contains(testEntity);
		emMc.setReturnValue(false);
		emMc.replay();

		mockEmf.createEntityManager();
		emfMc.setReturnValue(mockEm);
		mockEmf.close();
		emfMc.setVoidCallable();
		emfMc.replay();

		LocalContainerEntityManagerFactoryBean cefb = parseValidPersistenceUnit();

		JpaTransactionManager jpatm = new JpaTransactionManager();
		jpatm.setEntityManagerFactory(cefb.getObject());

		TransactionStatus txStatus = jpatm.getTransaction(new DefaultTransactionAttribute());

		EntityManagerFactory emf = cefb.getObject();
		assertSame("EntityManagerFactory reference must be cached after init", emf, cefb.getObject());

		assertNotSame("EMF must be proxied", mockEmf, emf);
		EntityManager em = emf.createEntityManager();
		em.joinTransaction();
		assertFalse(em.contains(testEntity));

		jpatm.commit(txStatus);

		cefb.destroy();

		emfMc.verify();
		emMc.verify();
		tmMc.verify();
	}

	public void testApplicationManagedEntityManagerWithTransactionAndCommitException() throws Exception {
		Object testEntity = new Object();

		MockControl tmMc = MockControl.createControl(EntityTransaction.class);
		EntityTransaction mockTx = (EntityTransaction) tmMc.getMock();
		mockTx.isActive();
		tmMc.setReturnValue(false);
		mockTx.begin();
		tmMc.setVoidCallable();
		mockTx.commit();
		tmMc.setThrowable(new OptimisticLockException());
		tmMc.replay();

		// This one's for the tx (shared)
		MockControl sharedEmMc = MockControl.createControl(EntityManager.class);
		EntityManager sharedEm = (EntityManager) sharedEmMc.getMock();
		sharedEm.getTransaction();
		sharedEmMc.setReturnValue(new NoOpEntityTransaction(), 3);
		sharedEm.close();
		sharedEmMc.setVoidCallable();
		sharedEmMc.replay();
		mockEmf.createEntityManager();
		emfMc.setReturnValue(sharedEm);

		// This is the application-specific one
		MockControl emMc = MockControl.createControl(EntityManager.class);
		EntityManager mockEm = (EntityManager) emMc.getMock();
		mockEm.getTransaction();
		emMc.setReturnValue(mockTx, 3);
		mockEm.contains(testEntity);
		emMc.setReturnValue(false);
		emMc.replay();

		mockEmf.createEntityManager();
		emfMc.setReturnValue(mockEm);
		mockEmf.close();
		emfMc.setVoidCallable();
		emfMc.replay();

		LocalContainerEntityManagerFactoryBean cefb = parseValidPersistenceUnit();

		JpaTransactionManager jpatm = new JpaTransactionManager();
		jpatm.setEntityManagerFactory(cefb.getObject());

		TransactionStatus txStatus = jpatm.getTransaction(new DefaultTransactionAttribute());

		EntityManagerFactory emf = cefb.getObject();
		assertSame("EntityManagerFactory reference must be cached after init", emf, cefb.getObject());

		assertNotSame("EMF must be proxied", mockEmf, emf);
		EntityManager em = emf.createEntityManager();
		em.joinTransaction();
		assertFalse(em.contains(testEntity));

		try {
			jpatm.commit(txStatus);
			fail("Should have thrown OptimisticLockingFailureException");
		}
		catch (OptimisticLockingFailureException ex) {
			// expected
		}

		cefb.destroy();

		emfMc.verify();
		emMc.verify();
		tmMc.verify();
	}

	public void testApplicationManagedEntityManagerWithJtaTransaction() throws Exception {
		Object testEntity = new Object();

		// This one's for the tx (shared)
		MockControl sharedEmMc = MockControl.createControl(EntityManager.class);
		EntityManager sharedEm = (EntityManager) sharedEmMc.getMock();
		sharedEm.getTransaction();
		sharedEmMc.setReturnValue(new NoOpEntityTransaction(), 3);
		sharedEm.close();
		sharedEmMc.setVoidCallable(1);
		sharedEmMc.replay();
		mockEmf.createEntityManager();
		emfMc.setReturnValue(sharedEm);

		// This is the application-specific one
		MockControl emMc = MockControl.createControl(EntityManager.class);
		EntityManager mockEm = (EntityManager) emMc.getMock();
		mockEm.joinTransaction();
		emMc.setVoidCallable(1);
		mockEm.contains(testEntity);
		emMc.setReturnValue(false);
		emMc.replay();

		mockEmf.createEntityManager();
		emfMc.setReturnValue(mockEm);
		mockEmf.close();
		emfMc.setVoidCallable();
		emfMc.replay();

		LocalContainerEntityManagerFactoryBean cefb = parseValidPersistenceUnit();
		MutablePersistenceUnitInfo pui = ((MutablePersistenceUnitInfo) cefb.getPersistenceUnitInfo());
		pui.setTransactionType(PersistenceUnitTransactionType.JTA);

		JpaTransactionManager jpatm = new JpaTransactionManager();
		jpatm.setEntityManagerFactory(cefb.getObject());

		TransactionStatus txStatus = jpatm.getTransaction(new DefaultTransactionAttribute());

		EntityManagerFactory emf = cefb.getObject();
		assertSame("EntityManagerFactory reference must be cached after init", emf, cefb.getObject());

		assertNotSame("EMF must be proxied", mockEmf, emf);
		EntityManager em = emf.createEntityManager();
		em.joinTransaction();
		assertFalse(em.contains(testEntity));

		jpatm.commit(txStatus);

		cefb.destroy();

		emfMc.verify();
		emMc.verify();
	}

	public LocalContainerEntityManagerFactoryBean parseValidPersistenceUnit() throws Exception {
		LocalContainerEntityManagerFactoryBean emfb = createEntityManagerFactoryBean(
				"org/springframework/orm/jpa/domain/persistence.xml", null,
				"Person");
		return emfb;
	}

	public void testInvalidPersistenceUnitName() throws Exception {
		try {
			createEntityManagerFactoryBean("org/springframework/orm/jpa/domain/persistence.xml", null, "call me Bob");
			fail("Should not create factory with this name");
		}
		catch (IllegalArgumentException ex) {
			// Ok
		}
	}

	protected LocalContainerEntityManagerFactoryBean createEntityManagerFactoryBean(
			String persistenceXml, Properties props, String entityManagerName) throws Exception {

		// This will be set by DummyPersistenceProvider
		actualPui = null;
		actualProps = null;

		LocalContainerEntityManagerFactoryBean containerEmfb = new LocalContainerEntityManagerFactoryBean();

		containerEmfb.setPersistenceUnitName(entityManagerName);
		containerEmfb.setPersistenceProviderClass(DummyContainerPersistenceProvider.class);
		if (props != null) {
			containerEmfb.setJpaProperties(props);
		}
		containerEmfb.setLoadTimeWeaver(new InstrumentationLoadTimeWeaver());
		containerEmfb.setPersistenceXmlLocation(persistenceXml);
		containerEmfb.afterPropertiesSet();

		assertEquals(entityManagerName, actualPui.getPersistenceUnitName());
		if (props != null) {
			assertEquals(props, actualProps);
		}
		//checkInvariants(containerEmfb);

		return containerEmfb;

		//containerEmfb.destroy();
		//emfMc.verify();
	}

	public void testRejectsMissingPersistenceUnitInfo() throws Exception {
		LocalContainerEntityManagerFactoryBean containerEmfb = new LocalContainerEntityManagerFactoryBean();
		String entityManagerName = "call me Bob";

		containerEmfb.setPersistenceUnitName(entityManagerName);
		containerEmfb.setPersistenceProviderClass(DummyContainerPersistenceProvider.class);

		try {
			containerEmfb.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException ex) {
			// Ok
		}
	}


	private static class DummyContainerPersistenceProvider implements PersistenceProvider {

		public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo pui, Map map) {
			actualPui = pui;
			actualProps = map;
			return mockEmf;
		}

		public EntityManagerFactory createEntityManagerFactory(String emfName, Map properties) {
			throw new UnsupportedOperationException();
		}
	}


	private static class NoOpEntityTransaction implements EntityTransaction {

		public void begin() {
		}

		public void commit() {
		}

		public void rollback() {
		}

		public void setRollbackOnly() {
			throw new UnsupportedOperationException();
		}

		public boolean getRollbackOnly() {
			return false;
		}

		public boolean isActive() {
			return false;
		}
	}

}
