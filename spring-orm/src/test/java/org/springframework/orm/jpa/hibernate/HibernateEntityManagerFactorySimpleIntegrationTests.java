/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.hibernate;

import org.junit.jupiter.api.Test;

import org.springframework.orm.jpa.EntityManagerFactoryInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
class HibernateEntityManagerFactorySimpleIntegrationTests extends HibernateEntityManagerFactoryIntegrationTests {

	@Override
	protected String[] getConfigLocations() {
		return new String[] {"/org/springframework/orm/jpa/hibernate/hibernate-manager-simple.xml",
				"/org/springframework/orm/jpa/memdb.xml", "/org/springframework/orm/jpa/inject.xml"};
	}

	@Override
	@Test
	protected void entityManagerFactoryImplementsEntityManagerFactoryInfo() {
		assertThat(entityManagerFactory).isInstanceOf(EntityManagerFactoryInfo.class);
		EntityManagerFactoryInfo emfi = (EntityManagerFactoryInfo) entityManagerFactory;
		assertThat(emfi.getPersistenceUnitName()).isEqualTo("Person");
		assertThat(emfi.getPersistenceUnitInfo()).as("PersistenceUnitInfo must not be used").isNull();
		assertThat(emfi.getNativeEntityManagerFactory()).as("Raw EntityManagerFactory must be available").isNotNull();
	}

}
