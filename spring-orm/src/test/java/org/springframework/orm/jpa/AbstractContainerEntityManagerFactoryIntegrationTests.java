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

import java.lang.reflect.Proxy;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.FlushModeType;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.orm.jpa.domain.DriversLicense;
import org.springframework.orm.jpa.domain.Person;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.annotation.Timed;
import org.springframework.util.SerializationTestUtils;

/**
 * Integration tests for LocalContainerEntityManagerFactoryBean.
 * Uses an in-memory database.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("deprecation")
public abstract class AbstractContainerEntityManagerFactoryIntegrationTests
		extends AbstractEntityManagerFactoryIntegrationTests {

	@NotTransactional
	public void testEntityManagerFactoryImplementsEntityManagerFactoryInfo() {
		assertTrue(Proxy.isProxyClass(entityManagerFactory.getClass()));
		assertTrue("Must have introduced config interface",
				entityManagerFactory instanceof EntityManagerFactoryInfo);
		EntityManagerFactoryInfo emfi = (EntityManagerFactoryInfo) entityManagerFactory;
		//assertEquals("Person", emfi.getPersistenceUnitName());
		assertNotNull("PersistenceUnitInfo must be available", emfi.getPersistenceUnitInfo());
		assertNotNull("Raw EntityManagerFactory must be available", emfi.getNativeEntityManagerFactory());
	}

	public void testStateClean() {
		assertEquals("Should be no people from previous transactions",
				0, countRowsInTable("person"));
	}

	@Repeat(5)
	public void testJdbcTx1() throws Exception {
		testJdbcTx2();
	}

	@Timed(millis=273)
	public void testJdbcTx2() throws InterruptedException {
		//Thread.sleep(2000);
		assertEquals("Any previous tx must have been rolled back", 0, countRowsInTable("person"));
		//insertPerson("foo");
		executeSqlScript("/org/springframework/orm/jpa/insertPerson.sql", false);
	}

	//@NotTransactional
	@SuppressWarnings({ "unused", "unchecked" })
	public void testEntityManagerProxyIsProxy() {
		assertTrue(Proxy.isProxyClass(sharedEntityManager.getClass()));
		Query q = sharedEntityManager.createQuery("select p from Person as p");
		List<Person> people = q.getResultList();

		assertTrue("Should be open to start with", sharedEntityManager.isOpen());
		sharedEntityManager.close();
		assertTrue("Close should have been silently ignored", sharedEntityManager.isOpen());
	}

	@ExpectedException(RuntimeException.class)
	public void testBogusQuery() {
		Query query = sharedEntityManager.createQuery("It's raining toads");
		// required in OpenJPA case
		query.executeUpdate();
	}

	@ExpectedException(EntityNotFoundException.class)
	public void testGetReferenceWhenNoRow() {
		Person notThere = sharedEntityManager.getReference(Person.class, 666);

		// We may get here (as with Hibernate).
		// Either behaviour is valid: throw exception on first access
		// or on getReference itself.
		notThere.getFirstName();
	}

	public void testLazyLoading() {
		try {
			Person tony = new Person();
			tony.setFirstName("Tony");
			tony.setLastName("Blair");
			tony.setDriversLicense(new DriversLicense("8439DK"));
			sharedEntityManager.persist(tony);
			setComplete();
			endTransaction();

			startNewTransaction();
			sharedEntityManager.clear();
			Person newTony = entityManagerFactory.createEntityManager().getReference(Person.class, tony.getId());
			assertNotSame(newTony, tony);
			endTransaction();

			assertNotNull(newTony.getDriversLicense());

			newTony.getDriversLicense().getSerialNumber();
		}
		finally {
			deleteFromTables(new String[] { "person", "drivers_license" });
			//setComplete();
		}
	}

	@SuppressWarnings("unchecked")
	public void testMultipleResults() {
		// Add with JDBC
		String firstName = "Tony";
		insertPerson(firstName);

		assertTrue(Proxy.isProxyClass(sharedEntityManager.getClass()));
		Query q = sharedEntityManager.createQuery("select p from Person as p");
		List<Person> people = q.getResultList();

		assertEquals(1, people.size());
		assertEquals(firstName, people.get(0).getFirstName());
	}

	protected final void insertPerson(String firstName) {
		String INSERT_PERSON = "INSERT INTO PERSON (ID, FIRST_NAME, LAST_NAME) VALUES (?, ?, ?)";
		simpleJdbcTemplate.update(INSERT_PERSON, 1, firstName, "Blair");
	}

	public void testEntityManagerProxyRejectsProgrammaticTxManagement() {
		try {
			sharedEntityManager.getTransaction();
			fail("Should not be able to create transactions on container managed EntityManager");
		}
		catch (IllegalStateException ex) {
		}
	}

//	public void testAspectJInjectionOfConfigurableEntity() {
//		Person p = new Person();
//		System.err.println(p);
//		assertNotNull("Was injected", p.getTestBean());
//		assertEquals("Ramnivas", p.getTestBean().getName());
//	}

	public void testInstantiateAndSaveWithSharedEmProxy() {
		testInstantiateAndSave(sharedEntityManager);
	}

	protected void testInstantiateAndSave(EntityManager em) {
		assertEquals("Should be no people from previous transactions", 0, countRowsInTable("person"));
		Person p = new Person();
		p.setFirstName("Tony");
		p.setLastName("Blair");
		em.persist(p);

		em.flush();
		assertEquals("1 row must have been inserted", 1, countRowsInTable("person"));
	}

	@SuppressWarnings("unchecked")
	public void testQueryNoPersons() {
		EntityManager em = entityManagerFactory.createEntityManager();
		Query q = em.createQuery("select p from Person as p");
		List<Person> people = q.getResultList();
		assertEquals(0, people.size());
		try {
			assertNull(q.getSingleResult());
			fail("Should have thrown NoResultException");
		}
		catch (NoResultException ex) {
			// expected
		}
	}

	@NotTransactional
	@SuppressWarnings("unchecked")
	public void testQueryNoPersonsNotTransactional() {
		EntityManager em = entityManagerFactory.createEntityManager();
		Query q = em.createQuery("select p from Person as p");
		List<Person> people = q.getResultList();
		assertEquals(0, people.size());
		try {
			assertNull(q.getSingleResult());
			fail("Should have thrown NoResultException");
		}
		catch (NoResultException ex) {
			// expected
		}
	}

	@SuppressWarnings({ "unused", "unchecked" })
	public void testQueryNoPersonsShared() {
		EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);
		Query q = em.createQuery("select p from Person as p");
		q.setFlushMode(FlushModeType.AUTO);
		List<Person> people = q.getResultList();
		try {
			assertNull(q.getSingleResult());
			fail("Should have thrown NoResultException");
		}
		catch (NoResultException ex) {
			// expected
		}
	}

	@NotTransactional
	@SuppressWarnings("unchecked")
	public void testQueryNoPersonsSharedNotTransactional() {
		EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);
		Query q = em.createQuery("select p from Person as p");
		q.setFlushMode(FlushModeType.AUTO);
		List<Person> people = q.getResultList();
		assertEquals(0, people.size());
		try {
			assertNull(q.getSingleResult());
			fail("Should have thrown IllegalStateException");
		}
		catch (Exception ex) {
			// IllegalStateException expected, but PersistenceException thrown by Hibernate
			assertTrue(ex.getMessage().indexOf("closed") != -1);
		}
		q = em.createQuery("select p from Person as p");
		q.setFlushMode(FlushModeType.AUTO);
		try {
			assertNull(q.getSingleResult());
			fail("Should have thrown NoResultException");
		}
		catch (NoResultException ex) {
			// expected
		}
	}

	public void testCanSerializeProxies() throws Exception {
		// just necessary because of AbstractJpaTests magically cloning the BeanFactory
		((DefaultListableBeanFactory) getApplicationContext().getBeanFactory()).setSerializationId("emf-it");

		assertNotNull(SerializationTestUtils.serializeAndDeserialize(entityManagerFactory));
		assertNotNull(SerializationTestUtils.serializeAndDeserialize(sharedEntityManager));
	}

}
