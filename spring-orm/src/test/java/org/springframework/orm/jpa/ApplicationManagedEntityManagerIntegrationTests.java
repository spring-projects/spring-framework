/*
 * Copyright 2002-2009 the original author or authors.
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

import java.util.List;
import java.lang.reflect.Proxy;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TransactionRequiredException;

import org.springframework.aop.support.AopUtils;
import org.springframework.orm.jpa.domain.Person;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryIntegrationTests;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.annotation.Transactional;

/**
 * An application-managed entity manager can join an existing transaction,
 * but such joining must be made programmatically, not transactionally.
 *
 * @author Rod Johnson
 * @since 2.0
 */
public class ApplicationManagedEntityManagerIntegrationTests extends AbstractEntityManagerFactoryIntegrationTests {
	
	@NotTransactional
	public void testEntityManagerIsProxy() {
		assertTrue("EntityManagerFactory is proxied", Proxy.isProxyClass(entityManagerFactory.getClass()));
	}

	@Transactional(readOnly=true)
	public void testEntityManagerProxyIsProxy() {
		EntityManager em = entityManagerFactory.createEntityManager();
		assertTrue(Proxy.isProxyClass(em.getClass()));
		Query q = em.createQuery("select p from Person as p");
		List<Person> people = q.getResultList();
		
		assertTrue("Should be open to start with", em.isOpen());
		em.close();
		assertFalse("Close should work on application managed EM", em.isOpen());
	}
	
	public void testEntityManagerProxyAcceptsProgrammaticTxJoining() {
		EntityManager em = entityManagerFactory.createEntityManager();
		em.joinTransaction();
	}
	
	public void testInstantiateAndSave() {
		EntityManager em = entityManagerFactory.createEntityManager();
		em.joinTransaction();
		doInstantiateAndSave(em);
	}
	
	public void testCannotFlushWithoutGettingTransaction() {
		EntityManager em = entityManagerFactory.createEntityManager();		
		try {
			doInstantiateAndSave(em);
			fail("Should have thrown TransactionRequiredException");
		}
		catch (TransactionRequiredException ex) {
			// expected
		}
		
		// TODO following lines are a workaround for Hibernate bug
		// If Hibernate throws an exception due to flush(),
		// it actually HAS flushed, meaning that the database
		// was updated outside the transaction
		deleteAllPeopleUsingEntityManager(sharedEntityManager);
		setComplete();
	}
	
	public void doInstantiateAndSave(EntityManager em) {
		testStateClean();
		Person p = new Person();
		
		p.setFirstName("Tony");
		p.setLastName("Blair");
		em.persist(p);
		
		em.flush();
		assertEquals("1 row must have been inserted", 1, countRowsInTable("person"));
	}

	public void testStateClean() {
		assertEquals("Should be no people from previous transactions", 0, countRowsInTable("person"));
	}
	
	public void testReuseInNewTransaction() {
		EntityManager em = entityManagerFactory.createEntityManager();
		em.joinTransaction();
		
		doInstantiateAndSave(em);
		endTransaction();
		
		assertFalse(em.getTransaction().isActive());
		
		startNewTransaction();
		// Call any method: should cause automatic tx invocation
		assertFalse(em.contains(new Person()));
		
		assertFalse(em.getTransaction().isActive());
		em.joinTransaction();
		
		assertTrue(em.getTransaction().isActive());
		
		doInstantiateAndSave(em);
		setComplete();
		endTransaction();	// Should rollback
		assertEquals("Tx must have committed back",
				1, countRowsInTable("person"));
		
		// Now clean up the database
		startNewTransaction();
		em.joinTransaction();
		deleteAllPeopleUsingEntityManager(em);
		assertEquals("People have been killed",
				0, countRowsInTable("person"));
		setComplete();
	}
	
	public static void deleteAllPeopleUsingEntityManager(EntityManager em) {
		em.createQuery("delete from Person p").executeUpdate();
	}

	public void testRollbackOccurs() {
		EntityManager em = entityManagerFactory.createEntityManager();
		em.joinTransaction();
		doInstantiateAndSave(em);
		endTransaction();	// Should rollback
		assertEquals("Tx must have been rolled back",
				0, countRowsInTable("person"));
	}
	
	public void testCommitOccurs() {
		EntityManager em = entityManagerFactory.createEntityManager();
		em.joinTransaction();
		doInstantiateAndSave(em);
		
		setComplete();
		endTransaction();	// Should rollback
		assertEquals("Tx must have committed back",
				1, countRowsInTable("person"));
		
		// Now clean up the database
		deleteFromTables(new String[] { "person" });
	}
	
}
