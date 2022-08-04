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

package org.springframework.orm.jpa;

import java.lang.reflect.Proxy;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TransactionRequiredException;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.orm.jpa.domain.Person;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Integration tests using in-memory database for container-managed JPA
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 */
public class ContainerManagedEntityManagerIntegrationTests extends AbstractEntityManagerFactoryIntegrationTests {

	@Autowired
	private AbstractEntityManagerFactoryBean entityManagerFactoryBean;


	@Test
	public void testExceptionTranslationWithDialectFoundOnIntroducedEntityManagerInfo() throws Exception {
		doTestExceptionTranslationWithDialectFound(((EntityManagerFactoryInfo) entityManagerFactory).getJpaDialect());
	}

	@Test
	public void testExceptionTranslationWithDialectFoundOnEntityManagerFactoryBean() throws Exception {
		assertThat(entityManagerFactoryBean.getJpaDialect()).as("Dialect must have been set").isNotNull();
		doTestExceptionTranslationWithDialectFound(entityManagerFactoryBean);
	}

	protected void doTestExceptionTranslationWithDialectFound(PersistenceExceptionTranslator pet) throws Exception {
		RuntimeException in1 = new RuntimeException("in1");
		PersistenceException in2 = new PersistenceException();
		assertThat(pet.translateExceptionIfPossible(in1)).as("No translation here").isNull();
		DataAccessException dex = pet.translateExceptionIfPossible(in2);
		assertThat(dex).isNotNull();
		assertThat(dex.getCause()).isSameAs(in2);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testEntityManagerProxyIsProxy() {
		EntityManager em = createContainerManagedEntityManager();
		assertThat(Proxy.isProxyClass(em.getClass())).isTrue();
		Query q = em.createQuery("select p from Person as p");
		List<Person> people = q.getResultList();
		assertThat(people.isEmpty()).isTrue();

		assertThat(em.isOpen()).as("Should be open to start with").isTrue();
		assertThatIllegalStateException().as("Close should not work on container managed EM").isThrownBy(
				em::close);
		assertThat(em.isOpen()).isTrue();
	}

	// This would be legal, at least if not actually _starting_ a tx
	@Test
	public void testEntityManagerProxyRejectsProgrammaticTxManagement() {
		assertThatIllegalStateException().isThrownBy(
				createContainerManagedEntityManager()::getTransaction);
	}

	/*
	 * See comments in spec on EntityManager.joinTransaction().
	 * We take the view that this is a valid no op.
	 */
	@Test
	public void testContainerEntityManagerProxyAllowsJoinTransactionInTransaction() {
		createContainerManagedEntityManager().joinTransaction();
	}

	@Test
	public void testContainerEntityManagerProxyRejectsJoinTransactionWithoutTransaction() {
		endTransaction();
		assertThatExceptionOfType(TransactionRequiredException.class).isThrownBy(
				createContainerManagedEntityManager()::joinTransaction);
	}

	@Test
	public void testInstantiateAndSave() {
		EntityManager em = createContainerManagedEntityManager();
		doInstantiateAndSave(em);
	}

	protected void doInstantiateAndSave(EntityManager em) {
		assertThat(countRowsInTable(em, "person")).as("Should be no people from previous transactions").isEqualTo(0);
		Person p = new Person();

		p.setFirstName("Tony");
		p.setLastName("Blair");
		em.persist(p);

		em.flush();
		assertThat(countRowsInTable(em, "person")).as("1 row must have been inserted").isEqualTo(1);
	}

	@Test
	public void testReuseInNewTransaction() {
		EntityManager em = createContainerManagedEntityManager();
		doInstantiateAndSave(em);
		endTransaction();

		//assertFalse(em.getTransaction().isActive());

		startNewTransaction();
		// Call any method: should cause automatic tx invocation
		assertThat(em.contains(new Person())).isFalse();
		//assertTrue(em.getTransaction().isActive());

		doInstantiateAndSave(em);
		setComplete();
		endTransaction();	// Should roll back
		assertThat(countRowsInTable(em, "person")).as("Tx must have committed back").isEqualTo(1);

		// Now clean up the database
		deleteFromTables("person");
	}

	@Test
	public void testRollbackOccurs() {
		EntityManager em = createContainerManagedEntityManager();
		doInstantiateAndSave(em);
		endTransaction();	// Should roll back
		assertThat(countRowsInTable(em, "person")).as("Tx must have been rolled back").isEqualTo(0);
	}

	@Test
	public void testCommitOccurs() {
		EntityManager em = createContainerManagedEntityManager();
		doInstantiateAndSave(em);
		setComplete();
		endTransaction();	// Should roll back
		assertThat(countRowsInTable(em, "person")).as("Tx must have committed back").isEqualTo(1);

		// Now clean up the database
		deleteFromTables("person");
	}

}
