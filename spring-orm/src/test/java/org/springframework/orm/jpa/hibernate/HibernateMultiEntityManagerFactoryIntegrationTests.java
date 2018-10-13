/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.orm.jpa.hibernate;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.AbstractContainerEntityManagerFactoryIntegrationTests;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;

import static org.junit.Assert.*;

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


	@Test
	public void testEntityManagerFactoryImplementsEntityManagerFactoryInfo() {
		assertTrue("Must have introduced config interface", this.entityManagerFactory instanceof EntityManagerFactoryInfo);
		EntityManagerFactoryInfo emfi = (EntityManagerFactoryInfo) this.entityManagerFactory;
		assertEquals("Drivers", emfi.getPersistenceUnitName());
		assertNotNull("PersistenceUnitInfo must be available", emfi.getPersistenceUnitInfo());
		assertNotNull("Raw EntityManagerFactory must be available", emfi.getNativeEntityManagerFactory());
	}

	@Test
	public void testEntityManagerFactory2() {
		EntityManager em = this.entityManagerFactory2.createEntityManager();
		try {
			em.createQuery("select tb from TestBean");
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
		finally {
			em.close();
		}
	}

}
