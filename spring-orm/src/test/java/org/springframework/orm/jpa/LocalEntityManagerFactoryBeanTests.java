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

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Rod Johnson
 * @author Phillip Webb
 */
public class LocalEntityManagerFactoryBeanTests extends AbstractEntityManagerFactoryBeanTests {

	// Static fields set by inner class DummyPersistenceProvider

	private static String actualName;

	private static Map actualProps;

	@After
	public void verifyClosed() throws Exception {
		verify(mockEmf).close();
	}

	@Test
	public void testValidUsageWithDefaultProperties() throws Exception {
		testValidUsage(null);
	}

	@Test
	public void testValidUsageWithExplicitProperties() throws Exception {
		testValidUsage(new Properties());
	}

	protected void testValidUsage(Properties props) throws Exception {
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

		assertSame(entityManagerName, actualName);
		if (props != null) {
			assertEquals(props, actualProps);
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
	}

}
