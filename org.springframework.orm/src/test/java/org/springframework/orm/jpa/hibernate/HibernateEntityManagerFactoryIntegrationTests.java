/*
 * Copyright 2002-2008 the original author or authors.
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

import java.util.List;

import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.ejb.HibernateEntityManager;
import org.hibernate.ejb.HibernateEntityManagerFactory;
import org.junit.Ignore;

import org.springframework.orm.jpa.AbstractContainerEntityManagerFactoryIntegrationTests;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.orm.jpa.domain.Person;

/**
 * Hibernate-specific JPA tests.
 * 
 * @author Juergen Hoeller
 * @author Rod Johnson
 */
@Ignore // cannot find AnnotationBeanConfigurerAspect
public class HibernateEntityManagerFactoryIntegrationTests extends
		AbstractContainerEntityManagerFactoryIntegrationTests {

	private SessionFactory sessionFactory;


	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	protected String[] getConfigLocations() {
		return HIBERNATE_CONFIG_LOCATIONS;
	}


	public void testCanCastNativeEntityManagerFactoryToHibernateEntityManagerFactoryImpl() {
		EntityManagerFactoryInfo emfi = (EntityManagerFactoryInfo) entityManagerFactory;
		assertTrue(emfi.getNativeEntityManagerFactory() instanceof HibernateEntityManagerFactory);
	}

	public void testCanCastSharedEntityManagerProxyToHibernateEntityManager() {
		assertTrue(sharedEntityManager instanceof HibernateEntityManager);
		HibernateEntityManager hibernateEntityManager = (HibernateEntityManager) sharedEntityManager;
		assertNotNull(hibernateEntityManager.getSession());
	}

	public void testWithHibernateSessionFactory() {
		// Add with JDBC
		String firstName = "Tony";
		insertPerson(firstName);

		Query q = this.sessionFactory.getCurrentSession().createQuery("select p from Person as p");
		List<Person> people = q.list();

		assertEquals(1, people.size());
		assertEquals(firstName, people.get(0).getFirstName());
	}

	public void testConfigurablePerson() {
		Query q = this.sessionFactory.getCurrentSession().createQuery("select p from ContextualPerson as p");
		assertEquals(0, q.list().size());
		//assertNotNull(new ContextualPerson().entityManager);  TODO
	}

}
