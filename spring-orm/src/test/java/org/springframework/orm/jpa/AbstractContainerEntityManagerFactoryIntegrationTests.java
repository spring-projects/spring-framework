/*
 * Copyright 2002-2014 the original author or authors.
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.SerializationTestUtils;

/**
 * Integration tests for LocalContainerEntityManagerFactoryBean.
 * Uses an in-memory database.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("deprecation")
public abstract class AbstractContainerEntityManagerFactoryIntegrationTests extends
		AbstractEntityManagerFactoryIntegrationTests {

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void testEntityManagerFactoryImplementsEntityManagerFactoryInfo() {
		assertTrue(Proxy.isProxyClass(entityManagerFactory.getClass()));
		assertTrue("Must have introduced config interface", entityManagerFactory instanceof EntityManagerFactoryInfo);
		EntityManagerFactoryInfo emfi = (EntityManagerFactoryInfo) entityManagerFactory;
		// assertEquals("Person", emfi.getPersistenceUnitName());
		assertNotNull("PersistenceUnitInfo must be available", emfi.getPersistenceUnitInfo());
		assertNotNull("Raw EntityManagerFactory must be available", emfi.getNativeEntityManagerFactory());
	}

	public void testStateClean() {
		assertEquals("Should be no people from previous transactions", 0, countRowsInTable("person"));
	}

	public void testJdbcTx1_1() {
		testJdbcTx2();
	}

	public void testJdbcTx1_2() {
		testJdbcTx2();
	}

	public void testJdbcTx1_3() {
		testJdbcTx2();
	}

	public void testJdbcTx2() {
		assertEquals("Any previous tx must have been rolled back", 0, countRowsInTable("person"));
		executeSqlScript("/org/springframework/orm/jpa/insertPerson.sql", false);
	}

	@SuppressWarnings({ "unused", "unchecked" })
	public void testEntityManagerProxyIsProxy() {
		assertTrue(Proxy.isProxyClass(sharedEntityManager.getClass()));
		Query q = sharedEntityManager.createQuery("select p from Person as p");
		List<Person> people = q.getResultList();

		assertTrue("Should be open to start with", sharedEntityManager.isOpen());
		sharedEntityManager.close();
		assertTrue("Close should have been silently ignored", sharedEntityManager.isOpen());
	}

	public void testBogusQuery() {
		try {
			Query query = sharedEntityManager.createQuery("It's raining toads");
			// required in OpenJPA case
			query.executeUpdate();
			fail("Should have thrown a RuntimeException");
		}
		catch (RuntimeException e) {
			/* expected */
		}
	}

	public void testGetReferenceWhenNoRow() {
		try {
			Person notThere = sharedEntityManager.getReference(Person.class, 666);

			// We may get here (as with Hibernate).
			// Either behaviour is valid: throw exception on first access
			// or on getReference itself.
			notThere.getFirstName();
			fail("Should have thrown an EntityNotFoundException");
		}
		catch (EntityNotFoundException e) {
			/* expected */
		}
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
			deleteFromTables("person", "drivers_license");
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
		jdbcTemplate.update(INSERT_PERSON, 1, firstName, "Blair");
	}

	public void testEntityManagerProxyRejectsProgrammaticTxManagement() {
		try {
			sharedEntityManager.getTransaction();
			fail("Should not be able to create transactions on container managed EntityManager");
		}
		catch (IllegalStateException ex) {
		}
	}

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

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
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

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
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
			// We would typically expect an IllegalStateException, but Hibernate throws a
			// PersistenceException. So we assert the contents of the exception message
			// instead.
			assertTrue(ex.getMessage().contains("closed"));
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
