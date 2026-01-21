/*
 * Copyright 2002-present the original author or authors.
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

import java.net.URL;
import java.util.Map;
import java.util.Properties;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.ProviderUtil;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.testfixture.io.SerializationTestUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;
import org.springframework.orm.jpa.domain.DriversLicense;
import org.springframework.orm.jpa.domain.Person;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
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

	@Test
	void validPersistenceUnit() {
		parseValidPersistenceUnit();
	}

	@Test
	void exceptionTranslationWithNoDialect() {
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
	void entityManagerFactoryIsProxied() throws Exception {
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
	void applicationManagedEntityManagerWithoutTransaction() {
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
	void applicationManagedEntityManagerWithTransaction() {
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
	void applicationManagedEntityManagerWithTransactionAndCommitException() {
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
	void applicationManagedEntityManagerWithJtaTransaction() {
		Object testEntity = new Object();

		// This one's for the tx (shared)
		EntityManager sharedEm = mock();
		given(sharedEm.getTransaction()).willReturn(new NoOpEntityTransaction());

		// This is the application-specific one
		EntityManager mockEm = mock();

		given(mockEmf.createEntityManager()).willReturn(sharedEm, mockEm);

		LocalContainerEntityManagerFactoryBean cefb = parseValidPersistenceUnit(
				pui -> pui.setTransactionType(PersistenceUnitTransactionType.JTA));

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

	@Test
	void invalidPersistenceUnitName() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				createEntityManagerFactoryBean("org/springframework/orm/jpa/domain/persistence.xml", null, "call me Bob"));
	}

	@Test
	void rejectsMissingPersistenceUnitInfo() {
		LocalContainerEntityManagerFactoryBean emfb = new LocalContainerEntityManagerFactoryBean();
		emfb.setPersistenceProviderClass(DummyContainerPersistenceProvider.class);

		String entityManagerName = "call me Bob";
		emfb.setPersistenceUnitName(entityManagerName);

		assertThatIllegalArgumentException().isThrownBy(emfb::afterPropertiesSet);
	}

	@Test
	void acceptsStandardPersistenceConfiguration() {
		DummyContainerPersistenceProvider persistenceProvider = new DummyContainerPersistenceProvider();
		LocalContainerEntityManagerFactoryBean emfb = new LocalContainerEntityManagerFactoryBean();
		emfb.setPersistenceProvider(persistenceProvider);

		String entityManagerName = "call me Bob";
		emfb.setPersistenceConfiguration(new PersistenceConfiguration(entityManagerName).
				managedClass(DriversLicense.class).managedClass(Person.class));

		emfb.afterPropertiesSet();
		assertThat(persistenceProvider.actualPui.getPersistenceUnitName()).isEqualTo(entityManagerName);
		assertThat(persistenceProvider.actualPui.getManagedClassNames()).containsExactly(
				DriversLicense.class.getName(), Person.class.getName());
	}

	@Test
	void acceptsHibernatePersistenceConfiguration() throws Exception {
		DummyContainerPersistenceProvider persistenceProvider = new DummyContainerPersistenceProvider();
		LocalContainerEntityManagerFactoryBean emfb = new LocalContainerEntityManagerFactoryBean();
		emfb.setPersistenceProvider(persistenceProvider);

		String entityManagerName = "call me Bob";
		URL rootUrl = new ClassPathResource("", getClass()).getURL();
		URL jarUrl = new ClassPathResource("jpa-archive.jar", getClass()).getURL();
		emfb.setPersistenceConfiguration(new HibernatePersistenceConfiguration(entityManagerName, rootUrl).
				jarFileUrl(jarUrl).managedClass(DriversLicense.class).managedClass(Person.class));

		emfb.afterPropertiesSet();
		assertThat(persistenceProvider.actualPui.getPersistenceUnitName()).isEqualTo(entityManagerName);
		assertThat(persistenceProvider.actualPui.getPersistenceUnitRootUrl()).isEqualTo(rootUrl);
		assertThat(persistenceProvider.actualPui.getJarFileUrls()).containsExactly(jarUrl);
		assertThat(persistenceProvider.actualPui.getManagedClassNames()).containsExactly(
				DriversLicense.class.getName(), Person.class.getName());
	}


	private LocalContainerEntityManagerFactoryBean parseValidPersistenceUnit(PersistenceUnitPostProcessor... postProcessors) {
		return createEntityManagerFactoryBean(
				"org/springframework/orm/jpa/domain/persistence.xml", null,
				"Person", postProcessors);
	}

	@SuppressWarnings("unchecked")
	private LocalContainerEntityManagerFactoryBean createEntityManagerFactoryBean(
			String persistenceXml, Properties props, String entityManagerName,
			PersistenceUnitPostProcessor... postProcessors) {

		DummyContainerPersistenceProvider persistenceProvider = new DummyContainerPersistenceProvider();
		LocalContainerEntityManagerFactoryBean emfb = new LocalContainerEntityManagerFactoryBean();

		emfb.setPersistenceUnitName(entityManagerName);
		emfb.setPersistenceProvider(persistenceProvider);
		if (props != null) {
			emfb.setJpaProperties(props);
		}
		emfb.setLoadTimeWeaver(new InstrumentationLoadTimeWeaver());
		emfb.setPersistenceXmlLocation(persistenceXml);
		emfb.setPersistenceUnitPostProcessors(postProcessors);
		emfb.afterPropertiesSet();

		assertThat(persistenceProvider.actualPui.getPersistenceUnitName()).isEqualTo(entityManagerName);
		if (props != null) {
			assertThat(persistenceProvider.actualProps).isEqualTo(props);
		}
		checkInvariants(emfb);

		return emfb;
	}


	private static class DummyContainerPersistenceProvider implements PersistenceProvider {

		PersistenceUnitInfo actualPui;

		Map actualProps;

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
		public EntityManagerFactory createEntityManagerFactory(PersistenceConfiguration persistenceConfiguration) {
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

		@Override
		public void setTimeout(Integer integer) {
		}

		@Override
		public Integer getTimeout() {
			return null;
		}
	}

}
