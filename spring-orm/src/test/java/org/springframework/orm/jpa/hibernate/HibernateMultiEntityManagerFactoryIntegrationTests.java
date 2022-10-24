/*
 * Copyright 2002-2022 the original author or authors.
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.AbstractContainerEntityManagerFactoryIntegrationTests;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.orm.jpa.domain.Person;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Hibernate-specific JPA tests with multiple EntityManagerFactory instances.
 *
 * @author Juergen Hoeller
 * @author Réda Housni Alaoui
 */
public class HibernateMultiEntityManagerFactoryIntegrationTests extends AbstractContainerEntityManagerFactoryIntegrationTests {

	@Autowired
	private EntityManagerFactory entityManagerFactory2;

	@Autowired
	private PlatformTransactionManager transactionManager2;


	@Override
	protected String[] getConfigLocations() {
		return new String[]{"/org/springframework/orm/jpa/hibernate/hibernate-manager-multi.xml",
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

	@Test
	public void testInstantiateAndSaveWithSharedEmProxyUnderTheWrongTransaction() {
		endTransaction();
		TransactionStatus transaction = this.transactionManager2.getTransaction(this.transactionDefinition);

		assertThat(countRowsInTable("person")).as("Should be no people from previous transactions").isEqualTo(0);
		Person person = new Person();
		person.setFirstName("Tony");
		person.setLastName("Blair");
		assertThatThrownBy(() -> sharedEntityManager.persist(person))
				.hasMessageContaining("No EntityManager with actual transaction available for current thread");

		transactionManager2.rollback(transaction);
	}

}
