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

import java.util.List;

import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.AbstractContainerEntityManagerFactoryIntegrationTests;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.orm.jpa.domain.Person;

import static org.junit.Assert.*;

/**
 * Hibernate-specific JPA tests with native SessionFactory setup and getCurrentSession interaction.
 *
 * @author Juergen Hoeller
 * @since 5.1
 */
public class HibernateNativeEntityManagerFactoryIntegrationTests extends AbstractContainerEntityManagerFactoryIntegrationTests {

	@Autowired
	private SessionFactory sessionFactory;

	@Autowired
	private ApplicationContext applicationContext;


	@Override
	protected String[] getConfigLocations() {
		return new String[] {"/org/springframework/orm/jpa/hibernate/hibernate-manager-native.xml",
				"/org/springframework/orm/jpa/memdb.xml", "/org/springframework/orm/jpa/inject.xml"};
	}


	@Test
	public void testEntityManagerFactoryImplementsEntityManagerFactoryInfo() {
		assertFalse("Must not have introduced config interface", entityManagerFactory instanceof EntityManagerFactoryInfo);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testEntityListener() {
		String firstName = "Tony";
		insertPerson(firstName);

		List<Person> people = sharedEntityManager.createQuery("select p from Person as p").getResultList();
		assertEquals(1, people.size());
		assertEquals(firstName, people.get(0).getFirstName());
		assertSame(applicationContext, people.get(0).postLoaded);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testCurrentSession() {
		String firstName = "Tony";
		insertPerson(firstName);

		Query q = sessionFactory.getCurrentSession().createQuery("select p from Person as p");
		List<Person> people = q.getResultList();
		assertEquals(1, people.size());
		assertEquals(firstName, people.get(0).getFirstName());
		assertSame(applicationContext, people.get(0).postLoaded);
	}

	@Test  // SPR-16956
	public void testReadOnly() {
		assertSame(FlushMode.AUTO, sessionFactory.getCurrentSession().getHibernateFlushMode());
		assertFalse(sessionFactory.getCurrentSession().isDefaultReadOnly());
		endTransaction();

		this.transactionDefinition.setReadOnly(true);
		startNewTransaction();
		assertSame(FlushMode.MANUAL, sessionFactory.getCurrentSession().getHibernateFlushMode());
		assertTrue(sessionFactory.getCurrentSession().isDefaultReadOnly());
	}

}
