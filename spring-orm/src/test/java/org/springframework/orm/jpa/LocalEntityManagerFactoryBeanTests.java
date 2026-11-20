/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa;

import java.util.Map;
import java.util.Properties;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.ProviderUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
@SuppressWarnings({"rawtypes", "unchecked"})
class LocalEntityManagerFactoryBeanTests extends AbstractEntityManagerFactoryBeanTests {

	@AfterEach
	void verifyClosed() {
		verify(mockEmf).close();
	}


	@Test
	void withDefault() {
		DummyPersistenceProvider persistenceProvider = new DummyPersistenceProvider();
		LocalEntityManagerFactoryBean emfb = new LocalEntityManagerFactoryBean();

		emfb.setPersistenceProvider(persistenceProvider);
		emfb.afterPropertiesSet();

		assertThat(persistenceProvider.actualName).isSameAs(
				DefaultPersistenceUnitManager.ORIGINAL_DEFAULT_PERSISTENCE_UNIT_NAME);
		assertThat(persistenceProvider.actualProps).isEqualTo(emfb.getJpaPropertyMap());
		checkInvariants(emfb);

		emfb.destroy();
	}

	@Test
	void withName() {
		DummyPersistenceProvider persistenceProvider = new DummyPersistenceProvider();
		LocalEntityManagerFactoryBean emfb = new LocalEntityManagerFactoryBean();
		String name = "call me Bob";

		emfb.setPersistenceUnitName(name);
		emfb.setPersistenceProvider(persistenceProvider);
		emfb.afterPropertiesSet();

		assertThat(persistenceProvider.actualName).isSameAs(name);
		assertThat(persistenceProvider.actualProps).isEqualTo(emfb.getJpaPropertyMap());
		checkInvariants(emfb);

		emfb.destroy();
	}

	@Test
	void withNameAndExplicitProperties() {
		DummyPersistenceProvider persistenceProvider = new DummyPersistenceProvider();
		LocalEntityManagerFactoryBean emfb = new LocalEntityManagerFactoryBean();
		String name = "call me Bob";
		Properties props = new Properties();
		props.setProperty("myProp", "myVal");

		emfb.setPersistenceUnitName(name);
		emfb.setPersistenceProvider(persistenceProvider);
		emfb.setJpaProperties(props);
		emfb.afterPropertiesSet();

		assertThat(persistenceProvider.actualName).isSameAs(name);
		assertThat(persistenceProvider.actualProps).isEqualTo(props);
		checkInvariants(emfb);

		emfb.destroy();
	}

	@Test
	void withDefaultPersistenceConfiguration() {
		DummyPersistenceProvider persistenceProvider = new DummyPersistenceProvider();
		LocalEntityManagerFactoryBean emfb = new LocalEntityManagerFactoryBean();

		emfb.getPersistenceConfiguration().property("myProp", "myVal");
		emfb.setPersistenceProvider(persistenceProvider);
		emfb.afterPropertiesSet();

		assertThat(persistenceProvider.actualName).isSameAs(
				DefaultPersistenceUnitManager.ORIGINAL_DEFAULT_PERSISTENCE_UNIT_NAME);
		assertThat(persistenceProvider.actualProps).containsEntry("myProp", "myVal");
		checkInvariants(emfb);

		emfb.destroy();
	}

	@Test
	void withNameAndDefaultPersistenceConfiguration() {
		DummyPersistenceProvider persistenceProvider = new DummyPersistenceProvider();
		LocalEntityManagerFactoryBean emfb = new LocalEntityManagerFactoryBean();
		String name = "call me Bob";

		emfb.setPersistenceUnitName(name);
		emfb.getPersistenceConfiguration().property("myProp", "myVal");
		emfb.setPersistenceProvider(persistenceProvider);
		emfb.afterPropertiesSet();

		assertThat(persistenceProvider.actualName).isSameAs(name);
		assertThat(persistenceProvider.actualProps).containsEntry("myProp", "myVal");
		checkInvariants(emfb);

		emfb.destroy();
	}

	@Test
	void withExplicitPersistenceConfiguration() {
		DummyPersistenceProvider persistenceProvider = new DummyPersistenceProvider();
		LocalEntityManagerFactoryBean emfb = new LocalEntityManagerFactoryBean();
		String name = "call me Bob";
		PersistenceConfiguration config = new PersistenceConfiguration(name);
		config.property("myProp", "myVal");

		emfb.setPersistenceConfiguration(config);
		emfb.setPersistenceProvider(persistenceProvider);
		emfb.afterPropertiesSet();

		assertThat(persistenceProvider.actualName).isSameAs(name);
		assertThat(persistenceProvider.actualProps).containsEntry("myProp", "myVal");
		checkInvariants(emfb);

		emfb.destroy();
	}


	private static class DummyPersistenceProvider implements PersistenceProvider {

		String actualName;

		Map actualProps;

		@Override
		public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo pui, Map map) {
			throw new UnsupportedOperationException();
		}

		@Override
		public EntityManagerFactory createEntityManagerFactory(String emfName, Map properties) {
			actualName = emfName;
			actualProps = properties;
			return mockEmf;
		}

		@Override
		public EntityManagerFactory createEntityManagerFactory(PersistenceConfiguration config) {
			actualName = config.name();
			actualProps = config.properties();
			return mockEmf;
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

		@SuppressWarnings("unused")  // JPA 4.0 method
		public boolean generateSchema(PersistenceConfiguration persistenceConfiguration) {
			throw new UnsupportedOperationException();
		}

		@SuppressWarnings("unused")  // JPA 4.0 method
		public ClassTransformer getClassTransformer(PersistenceUnitInfo persistenceUnitInfo, Map<?,?> map) {
			return null;
		}

		@SuppressWarnings("unused")  // JPA 4.0 method
		public ClassTransformer getClientClassTransformer(PersistenceUnitInfo info, Map<?, ?> properties) {
			return null;
		}
	}

}
