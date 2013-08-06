/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.test.jpa.AbstractJpaTests;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public abstract class AbstractEntityManagerFactoryIntegrationTests extends AbstractJpaTests {

	public static final String[] ECLIPSELINK_CONFIG_LOCATIONS = new String[] {
			"/org/springframework/orm/jpa/eclipselink/eclipselink-manager.xml", "/org/springframework/orm/jpa/memdb.xml",
			"/org/springframework/orm/jpa/inject.xml"};

	public static final String[] HIBERNATE_CONFIG_LOCATIONS = new String[] {
			"/org/springframework/orm/jpa/hibernate/hibernate-manager.xml", "/org/springframework/orm/jpa/memdb.xml",
			"/org/springframework/orm/jpa/inject.xml"};

	public static final String[] OPENJPA_CONFIG_LOCATIONS = new String[] {
			"/org/springframework/orm/jpa/openjpa/openjpa-manager.xml", "/org/springframework/orm/jpa/memdb.xml",
			"/org/springframework/orm/jpa/inject.xml"};


	public static Provider getProvider() {
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
	protected String[] getConfigLocations() {
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


	public enum Provider {
		ECLIPSELINK, HIBERNATE, OPENJPA
	}

}
