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

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.ProviderUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * @author Rod Johnson
 * @author Phillip Webb
 */
@SuppressWarnings("rawtypes")
class LocalEntityManagerFactoryBeanTests extends AbstractEntityManagerFactoryBeanTests {

	// Static fields set by inner class DummyPersistenceProvider

	private static String actualName;

	private static Map actualProps;

	@AfterEach
	void verifyClosed() {
		verify(mockEmf).close();
	}

	@Test
	void testValidUsageWithDefaultProperties() throws Exception {
		testValidUsage(null);
	}

	@Test
	void testValidUsageWithExplicitProperties() throws Exception {
		testValidUsage(new Properties());
	}

	@SuppressWarnings("unchecked")
	protected void testValidUsage(Properties props) {
		// This will be set by DummyPersistenceProvider
		actualName = null;
		actualProps = null;

		LocalEntityManagerFactoryBean lemfb = new LocalEntityManagerFactoryBean();
		String entityManagerName = "call me Bob";

		lemfb.setPersistenceUnitName(entityManagerName);
		lemfb.setPersistenceProviderClass(DummyPersistenceProvider.class);
		if (props != null) {
			lemfb.setJpaProperties(props);
		}
		lemfb.afterPropertiesSet();

		assertThat(actualName).isSameAs(entityManagerName);
		if (props != null) {
			assertThat(actualProps).isEqualTo(props);
		}
		checkInvariants(lemfb);

		lemfb.destroy();
	}


	protected static class DummyPersistenceProvider implements PersistenceProvider {

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

}
