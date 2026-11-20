/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.hibernate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.AbstractContainerEntityManagerFactoryIntegrationTests;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Hibernate-specific JPA tests with multiple EntityManagerFactory instances.
 *
 * @author Juergen Hoeller
 */
class HibernateMultiEntityManagerFactoryIntegrationTests extends AbstractContainerEntityManagerFactoryIntegrationTests {

	@Autowired
	private EntityManagerFactory entityManagerFactory2;


	@Override
	protected String[] getConfigLocations() {
		return new String[] {"/org/springframework/orm/jpa/hibernate/hibernate-manager-multi.xml",
				"/org/springframework/orm/jpa/memdb.xml"};
	}


	@Override
	@Test
	protected void entityManagerFactoryImplementsEntityManagerFactoryInfo() {
		assertThat(this.entityManagerFactory).isInstanceOf(EntityManagerFactoryInfo.class);
		EntityManagerFactoryInfo emfi = (EntityManagerFactoryInfo) this.entityManagerFactory;
		assertThat(emfi.getPersistenceUnitName()).isEqualTo("Drivers");
		assertThat(emfi.getPersistenceUnitInfo()).as("PersistenceUnitInfo must be available").isNotNull();
		assertThat(emfi.getNativeEntityManagerFactory()).as("Raw EntityManagerFactory must be available").isNotNull();
	}

	@Test
	void entityManagerFactory2() {
		EntityManager em = this.entityManagerFactory2.createEntityManager();
		try {
			assertThatIllegalArgumentException().isThrownBy(() ->
					em.createQuery("select tb from TestBean"));
		}
		finally {
			em.close();
		}
	}

}
