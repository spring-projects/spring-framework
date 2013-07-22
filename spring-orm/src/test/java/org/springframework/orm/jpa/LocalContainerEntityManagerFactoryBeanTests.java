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

package org.springframework.orm.jpa;

import java.util.Map;
import java.util.Properties;
import javax.persistence.*;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.persistence.spi.ProviderUtil;

import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.util.SerializationTestUtils;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.*;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
public class LocalContainerEntityManagerFactoryBeanTests extends AbstractEntityManagerFactoryBeanTests {

	private static final String SCHEMA_GENERATION_ACTION_PROPERTY =
			"javax.persistence.schema-generation.database.action";

	// Static fields set by inner class DummyPersistenceProvider

	private static Map actualProps;

	private static PersistenceUnitInfo actualPui;


	@Test
	public void testValidPersistenceUnit() throws Exception {
		parseValidPersistenceUnit();
	}

	@Test
	public void testExceptionTranslationWithNoDialect() throws Exception {
		LocalContainerEntityManagerFactoryBean cefb = parseValidPersistenceUnit();
		cefb.getObject();
		assertNull("No dialect set", cefb.getJpaDialect());

		RuntimeException in1 = new RuntimeException("in1");
		PersistenceException in2 = new PersistenceException();
		assertNull("No translation here", cefb.translateExceptionIfPossible(in1));
		DataAccessException dex = cefb.translateExceptionIfPossible(in2);
		assertNotNull(dex);
		assertSame(in2, dex.getCause());
	}

	@Test
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

	@Test
	public void testApplicationManagedEntityManagerWithoutTransaction() throws Exception {
		Object testEntity = new Object();
		EntityManager mockEm = mock(EntityManager.class);

		given(mockEmf.createEntityManager()).willReturn(mockEm);

		LocalContainerEntityManagerFactoryBean cefb = parseValidPersistenceUnit();
		EntityManagerFactory emf = cefb.getObject();
		assertSame("EntityManagerFactory reference must be cached after init", emf, cefb.getObject());

		assertNotSame("EMF must be proxied", mockEmf, emf);
		EntityManager em = emf.createEntityManager();
		assertFalse(em.contains(testEntity));

		cefb.destroy();

		verify(mockEmf).close();
	}

	@Test
	public void testApplicationManagedEntityManagerWithTransaction() throws Exception {
		Object testEntity = new Object();

		EntityTransaction mockTx = mock(EntityTransaction.class);

		// This one's for the tx (shared)
		EntityManager sharedEm = mock(EntityManager.class);
		given(sharedEm.getTransaction()).willReturn(new NoOpEntityTransaction());

		// This is the application-specific one
		EntityManager mockEm = mock(EntityManager.class);
		given(mockEm.getTransaction()).willReturn(mockTx);

		given(mockEmf.createEntityManager()).willReturn(sharedEm, mockEm);

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

		verify(mockTx).begin();
		verify(mockTx).commit();
		verify(mockEm).contains(testEntity);
		verify(mockEmf).close();
	}

	@Test
	public void testApplicationManagedEntityManagerWithTransactionAndCommitException() throws Exception {
		Object testEntity = new Object();

		EntityTransaction mockTx = mock(EntityTransaction.class);
		willThrow(new OptimisticLockException()).given(mockTx).commit();

		// This one's for the tx (shared)
		EntityManager sharedEm = mock(EntityManager.class);
		given(sharedEm.getTransaction()).willReturn(new NoOpEntityTransaction());

		// This is the application-specific one
		EntityManager mockEm = mock(EntityManager.class);
		given(mockEm.getTransaction()).willReturn(mockTx);

		given(mockEmf.createEntityManager()).willReturn(sharedEm, mockEm);

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

		verify(mockTx).begin();
		verify(mockEm).contains(testEntity);
		verify(mockEmf).close();
	}

	@Test
	public void testApplicationManagedEntityManagerWithJtaTransaction() throws Exception {
		Object testEntity = new Object();

		// This one's for the tx (shared)
		EntityManager sharedEm = mock(EntityManager.class);
		given(sharedEm.getTransaction()).willReturn(new NoOpEntityTransaction());

		// This is the application-specific one
		EntityManager mockEm = mock(EntityManager.class);

		given(mockEmf.createEntityManager()).willReturn(sharedEm, mockEm);

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

		verify(mockEm).joinTransaction();
		verify(mockEm).contains(testEntity);
		verify(mockEmf).close();
	}

	@Test
	public void testTransactionTypeCacheModeValidationModeAndGenerationModeDefaults() {
		actualPui = null;
		actualProps = null;

		LocalContainerEntityManagerFactoryBean containerEmfb = new LocalContainerEntityManagerFactoryBean();
		containerEmfb.setPackagesToScan("org.example.foo.bar");
		containerEmfb.setPersistenceProviderClass(DummyContainerPersistenceProvider.class);
		containerEmfb.setLoadTimeWeaver(new InstrumentationLoadTimeWeaver());
		containerEmfb.afterPropertiesSet();

		assertEquals("default", actualPui.getPersistenceUnitName());
		assertSame("The transaction type is not correct.", PersistenceUnitTransactionType.RESOURCE_LOCAL,
				actualPui.getTransactionType());
		assertSame("The cache mode is not correct.", SharedCacheMode.UNSPECIFIED, actualPui.getSharedCacheMode());
		assertSame("The validation mode is not correct.", ValidationMode.AUTO, actualPui.getValidationMode());
		assertNull("The generation mode is not correct.", actualProps.get(SCHEMA_GENERATION_ACTION_PROPERTY));
	}

	@Test
	public void testTransactionTypeCacheModeValidationModeAndGenerationModeCustom() {
		actualPui = null;
		actualProps = null;

		LocalContainerEntityManagerFactoryBean containerEmfb = new LocalContainerEntityManagerFactoryBean();
		containerEmfb.setPackagesToScan("org.example.foo.bar");
		containerEmfb.setPersistenceProviderClass(DummyContainerPersistenceProvider.class);
		containerEmfb.setLoadTimeWeaver(new InstrumentationLoadTimeWeaver());
		containerEmfb.setTransactionType(PersistenceUnitTransactionType.JTA);
		containerEmfb.setSharedCacheMode(SharedCacheMode.ENABLE_SELECTIVE);
		containerEmfb.setValidationMode(ValidationMode.CALLBACK);
		containerEmfb.setSchemaGenerationAction("fooBar");
		containerEmfb.afterPropertiesSet();

		assertEquals("default", actualPui.getPersistenceUnitName());
		assertSame("The transaction type is not correct.", PersistenceUnitTransactionType.JTA,
				actualPui.getTransactionType());
		assertSame("The cache mode is not correct.", SharedCacheMode.ENABLE_SELECTIVE, actualPui.getSharedCacheMode());
		assertSame("The validation mode is not correct.", ValidationMode.CALLBACK, actualPui.getValidationMode());
		assertEquals("The generation mode is not correct.", "fooBar",
				actualProps.get(SCHEMA_GENERATION_ACTION_PROPERTY));
	}

	public LocalContainerEntityManagerFactoryBean parseValidPersistenceUnit() throws Exception {
		LocalContainerEntityManagerFactoryBean emfb = createEntityManagerFactoryBean(
				"org/springframework/orm/jpa/domain/persistence.xml", null,
				"Person");
		return emfb;
	}

	@Test
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

	@Test
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

		@Override
		public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo pui, Map map) {
			actualPui = pui;
			actualProps = map;
			return mockEmf;
		}

		@Override
		public EntityManagerFactory createEntityManagerFactory(String emfName, Map properties) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ProviderUtil getProviderUtil() {
			throw new UnsupportedOperationException();
		}

		// JPA 2.1 method
		public void generateSchema(PersistenceUnitInfo persistenceUnitInfo, Map map) {
			throw new UnsupportedOperationException();
		}

		// JPA 2.1 method
		public boolean generateSchema(String persistenceUnitName, Map map) {
			throw new UnsupportedOperationException();
		}
	}


	private static class NoOpEntityTransaction implements EntityTransaction {

		@Override
		public void begin() {
		}

		@Override
		public void commit() {
		}

		@Override
		public void rollback() {
		}

		@Override
		public void setRollbackOnly() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean getRollbackOnly() {
			return false;
		}

		@Override
		public boolean isActive() {
			return false;
		}
	}

}
