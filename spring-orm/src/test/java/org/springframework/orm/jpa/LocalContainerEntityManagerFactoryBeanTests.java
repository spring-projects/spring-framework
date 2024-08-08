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

import java.util.Map;
import java.util.Properties;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import jakarta.persistence.spi.ProviderUtil;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.testfixture.io.SerializationTestUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
@SuppressWarnings("rawtypes")
class LocalContainerEntityManagerFactoryBeanTests extends AbstractEntityManagerFactoryBeanTests {

	// Static fields set by inner class DummyPersistenceProvider

	private static Map actualProps;

	private static PersistenceUnitInfo actualPui;


	@Test
	void testValidPersistenceUnit() throws Exception {
		parseValidPersistenceUnit();
	}

	@Test
	void testExceptionTranslationWithNoDialect() throws Exception {
		LocalContainerEntityManagerFactoryBean cefb = parseValidPersistenceUnit();
		cefb.getObject();
		assertThat(cefb.getJpaDialect()).as("No dialect set").isNull();

		RuntimeException in1 = new RuntimeException("in1");
		PersistenceException in2 = new PersistenceException();
		assertThat(cefb.translateExceptionIfPossible(in1)).as("No translation here").isNull();
		DataAccessException dex = cefb.translateExceptionIfPossible(in2);
		assertThat(dex).isNotNull();
		assertThat(dex.getCause()).isSameAs(in2);
	}

	@Test
	void testEntityManagerFactoryIsProxied() throws Exception {
		LocalContainerEntityManagerFactoryBean cefb = parseValidPersistenceUnit();
		EntityManagerFactory emf = cefb.getObject();
		assertThat(cefb.getObject()).as("EntityManagerFactory reference must be cached after init").isSameAs(emf);

		assertThat(emf).as("EMF must be proxied").isNotSameAs(mockEmf);
		assertThat(emf).isEqualTo(emf);

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.setSerializationId("emf-bf");
		bf.registerSingleton("emf", cefb);
		cefb.setBeanFactory(bf);
		cefb.setBeanName("emf");
		assertThat(SerializationTestUtils.serializeAndDeserialize(emf)).isNotNull();
	}

	@Test
	void testApplicationManagedEntityManagerWithoutTransaction() throws Exception {
		Object testEntity = new Object();
		EntityManager mockEm = mock();

		given(mockEmf.createEntityManager()).willReturn(mockEm);

		LocalContainerEntityManagerFactoryBean cefb = parseValidPersistenceUnit();
		EntityManagerFactory emf = cefb.getObject();
		assertThat(cefb.getObject()).as("EntityManagerFactory reference must be cached after init").isSameAs(emf);

		assertThat(emf).as("EMF must be proxied").isNotSameAs(mockEmf);
		EntityManager em = emf.createEntityManager();
		assertThat(em.contains(testEntity)).isFalse();

		cefb.destroy();

		verify(mockEmf).close();
	}

	@Test
	void testApplicationManagedEntityManagerWithTransaction() throws Exception {
		Object testEntity = new Object();

		EntityTransaction mockTx = mock();

		// This one's for the tx (shared)
		EntityManager sharedEm = mock();
		given(sharedEm.getTransaction()).willReturn(new NoOpEntityTransaction());

		// This is the application-specific one
		EntityManager mockEm = mock();
		given(mockEm.getTransaction()).willReturn(mockTx);

		given(mockEmf.createEntityManager()).willReturn(sharedEm, mockEm);

		LocalContainerEntityManagerFactoryBean cefb = parseValidPersistenceUnit();

		JpaTransactionManager jpatm = new JpaTransactionManager();
		jpatm.setEntityManagerFactory(cefb.getObject());

		TransactionStatus txStatus = jpatm.getTransaction(new DefaultTransactionAttribute());

		EntityManagerFactory emf = cefb.getObject();
		assertThat(cefb.getObject()).as("EntityManagerFactory reference must be cached after init").isSameAs(emf);

		assertThat(emf).as("EMF must be proxied").isNotSameAs(mockEmf);
		EntityManager em = emf.createEntityManager();
		em.joinTransaction();
		assertThat(em.contains(testEntity)).isFalse();

		jpatm.commit(txStatus);

		cefb.destroy();

		verify(mockTx).begin();
		verify(mockTx).commit();
		verify(mockEm).contains(testEntity);
		verify(mockEmf).close();
	}

	@Test
	void testApplicationManagedEntityManagerWithTransactionAndCommitException() throws Exception {
		Object testEntity = new Object();

		EntityTransaction mockTx = mock();
		willThrow(new OptimisticLockException()).given(mockTx).commit();

		// This one's for the tx (shared)
		EntityManager sharedEm = mock();
		given(sharedEm.getTransaction()).willReturn(new NoOpEntityTransaction());

		// This is the application-specific one
		EntityManager mockEm = mock();
		given(mockEm.getTransaction()).willReturn(mockTx);

		given(mockEmf.createEntityManager()).willReturn(sharedEm, mockEm);

		LocalContainerEntityManagerFactoryBean cefb = parseValidPersistenceUnit();

		JpaTransactionManager jpatm = new JpaTransactionManager();
		jpatm.setEntityManagerFactory(cefb.getObject());

		TransactionStatus txStatus = jpatm.getTransaction(new DefaultTransactionAttribute());

		EntityManagerFactory emf = cefb.getObject();
		assertThat(cefb.getObject()).as("EntityManagerFactory reference must be cached after init").isSameAs(emf);

		assertThat(emf).as("EMF must be proxied").isNotSameAs(mockEmf);
		EntityManager em = emf.createEntityManager();
		em.joinTransaction();
		assertThat(em.contains(testEntity)).isFalse();

		assertThatExceptionOfType(OptimisticLockingFailureException.class).isThrownBy(() ->
				jpatm.commit(txStatus));

		cefb.destroy();

		verify(mockTx).begin();
		verify(mockEm).contains(testEntity);
		verify(mockEmf).close();
	}

	@Test
	void testApplicationManagedEntityManagerWithJtaTransaction() throws Exception {
		Object testEntity = new Object();

		// This one's for the tx (shared)
		EntityManager sharedEm = mock();
		given(sharedEm.getTransaction()).willReturn(new NoOpEntityTransaction());

		// This is the application-specific one
		EntityManager mockEm = mock();

		given(mockEmf.createEntityManager()).willReturn(sharedEm, mockEm);

		LocalContainerEntityManagerFactoryBean cefb = parseValidPersistenceUnit();
		MutablePersistenceUnitInfo pui = ((MutablePersistenceUnitInfo) cefb.getPersistenceUnitInfo());
		pui.setTransactionType(PersistenceUnitTransactionType.JTA);

		JpaTransactionManager jpatm = new JpaTransactionManager();
		jpatm.setEntityManagerFactory(cefb.getObject());

		TransactionStatus txStatus = jpatm.getTransaction(new DefaultTransactionAttribute());

		EntityManagerFactory emf = cefb.getObject();
		assertThat(cefb.getObject()).as("EntityManagerFactory reference must be cached after init").isSameAs(emf);

		assertThat(emf).as("EMF must be proxied").isNotSameAs(mockEmf);
		EntityManager em = emf.createEntityManager();
		em.joinTransaction();
		assertThat(em.contains(testEntity)).isFalse();

		jpatm.commit(txStatus);

		cefb.destroy();

		verify(mockEm).joinTransaction();
		verify(mockEm).contains(testEntity);
		verify(mockEmf).close();
	}

	public LocalContainerEntityManagerFactoryBean parseValidPersistenceUnit() throws Exception {
		return createEntityManagerFactoryBean(
				"org/springframework/orm/jpa/domain/persistence.xml", null,
				"Person");
	}

	@Test
	void testInvalidPersistenceUnitName() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				createEntityManagerFactoryBean("org/springframework/orm/jpa/domain/persistence.xml", null, "call me Bob"));
	}

	@SuppressWarnings("unchecked")
	protected LocalContainerEntityManagerFactoryBean createEntityManagerFactoryBean(
			String persistenceXml, Properties props, String entityManagerName) {

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

		assertThat(actualPui.getPersistenceUnitName()).isEqualTo(entityManagerName);
		if (props != null) {
			assertThat(actualProps).isEqualTo(props);
		}
		//checkInvariants(containerEmfb);

		return containerEmfb;

		//containerEmfb.destroy();
		//emfMc.verify();
	}

	@Test
	void testRejectsMissingPersistenceUnitInfo() {
		LocalContainerEntityManagerFactoryBean containerEmfb = new LocalContainerEntityManagerFactoryBean();
		String entityManagerName = "call me Bob";

		containerEmfb.setPersistenceUnitName(entityManagerName);
		containerEmfb.setPersistenceProviderClass(DummyContainerPersistenceProvider.class);

		assertThatIllegalArgumentException().isThrownBy(
				containerEmfb::afterPropertiesSet);
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
		@Override
		public void generateSchema(PersistenceUnitInfo persistenceUnitInfo, Map map) {
			throw new UnsupportedOperationException();
		}

		// JPA 2.1 method
		@Override
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
