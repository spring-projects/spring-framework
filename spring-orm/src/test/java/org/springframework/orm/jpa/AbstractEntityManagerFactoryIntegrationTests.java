/*
 * Copyright 2002-2016 the original author or authors.
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

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("deprecation")
public abstract class AbstractEntityManagerFactoryIntegrationTests extends org.springframework.test.jpa.AbstractJpaTests {

	protected static final String[] ECLIPSELINK_CONFIG_LOCATIONS = new String[] {
			"/org/springframework/orm/jpa/eclipselink/eclipselink-manager.xml", "/org/springframework/orm/jpa/memdb.xml",
			"/org/springframework/orm/jpa/inject.xml"};

	protected static final String[] HIBERNATE_CONFIG_LOCATIONS = new String[] {
			"/org/springframework/orm/jpa/hibernate/hibernate-manager.xml", "/org/springframework/orm/jpa/memdb.xml",
			"/org/springframework/orm/jpa/inject.xml"};

	protected static final String[] OPENJPA_CONFIG_LOCATIONS = new String[] {
			"/org/springframework/orm/jpa/openjpa/openjpa-manager.xml", "/org/springframework/orm/jpa/memdb.xml",
			"/org/springframework/orm/jpa/inject.xml"};


	private static Provider getProvider() {
		String provider = System.getProperty("org.springframework.orm.jpa.provider");
		if (provider != null) {
			if (provider.toLowerCase().contains("hibernate")) {
				return Provider.HIBERNATE;
			}
			if (provider.toLowerCase().contains("openjpa")) {
				return Provider.OPENJPA;
			}
		}
		return Provider.ECLIPSELINK;
	}


	@Override
	protected String getActualOrmXmlLocation() {
		// Specify that we do NOT want to find such a file.
		return null;
	}

	@Override
	protected String[] getConfigPaths() {
		Provider provider = getProvider();
		switch (provider) {
			case ECLIPSELINK:
				return ECLIPSELINK_CONFIG_LOCATIONS;
			case HIBERNATE:
				return HIBERNATE_CONFIG_LOCATIONS;
			case OPENJPA:
				return OPENJPA_CONFIG_LOCATIONS;
			default:
				throw new IllegalStateException("Unknown provider: " + provider);
		}
	}

	@Override
	protected void onTearDownAfterTransaction() throws Exception {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}

	protected int countRowsInTable(EntityManager em, String tableName) {
		Query query = em.createNativeQuery("SELECT COUNT(0) FROM " + tableName);
		return ((Number) query.getSingleResult()).intValue();
	}


	enum Provider {

		ECLIPSELINK, HIBERNATE, OPENJPA
	}

}
