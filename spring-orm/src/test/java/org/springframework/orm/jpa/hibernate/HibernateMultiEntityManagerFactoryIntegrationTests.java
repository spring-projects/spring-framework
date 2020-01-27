/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.orm.jpa.hibernate;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

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
public class HibernateMultiEntityManagerFactoryIntegrationTests extends AbstractContainerEntityManagerFactoryIntegrationTests {

	@Autowired
	private EntityManagerFactory entityManagerFactory2;


	@Override
	protected String[] getConfigLocations() {
		return new String[] {"/org/springframework/orm/jpa/hibernate/hibernate-manager-multi.xml",
				"/org/springframework/orm/jpa/memdb.xml"};
	}


	@Override
	@Test
	public void testEntityManagerFactoryImplementsEntityManagerFactoryInfo() {
		boolean condition = this.entityManagerFactory instanceof EntityManagerFactoryInfo;
		assertThat(condition).as("Must have introduced config interface").isTrue();
		EntityManagerFactoryInfo emfi = (EntityManagerFactoryInfo) this.entityManagerFactory;
		assertThat(emfi.getPersistenceUnitName()).isEqualTo("Drivers");
		assertThat(emfi.getPersistenceUnitInfo()).as("PersistenceUnitInfo must be available").isNotNull();
		assertThat(emfi.getNativeEntityManagerFactory()).as("Raw EntityManagerFactory must be available").isNotNull();
	}

	@Test
	public void testEntityManagerFactory2() {
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
