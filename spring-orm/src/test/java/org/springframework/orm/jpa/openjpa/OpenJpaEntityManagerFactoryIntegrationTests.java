/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.orm.jpa.openjpa;

import java.util.List;

import javax.persistence.FlushModeType;
import javax.persistence.Query;

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;

import org.springframework.orm.jpa.AbstractContainerEntityManagerFactoryIntegrationTests;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.domain.Person;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * OpenJPA-specific JPA tests.
 *
 * @author Costin Leau
 */
public class OpenJpaEntityManagerFactoryIntegrationTests extends AbstractContainerEntityManagerFactoryIntegrationTests {

	protected String[] getConfigLocations() {
		return OPENJPA_CONFIG_LOCATIONS;
	}

	public void testCanCastNativeEntityManagerFactoryToOpenJpaEntityManagerFactoryImpl() {
		EntityManagerFactoryInfo emfi = (EntityManagerFactoryInfo) entityManagerFactory;
		assertTrue("native EMF expected", emfi.getNativeEntityManagerFactory() instanceof OpenJPAEntityManagerFactory);
	}

	public void testCanCastSharedEntityManagerProxyToOpenJpaEntityManager() {
		assertTrue("native EM expected", sharedEntityManager instanceof OpenJPAEntityManager);
	}

	public void testCanGetSharedOpenJpaEntityManagerProxy() {
		OpenJPAEntityManager openJPAEntityManager = (OpenJPAEntityManager) SharedEntityManagerCreator.createSharedEntityManager(
				entityManagerFactory, null, OpenJPAEntityManager.class);
		assertNotNull(openJPAEntityManager.getDelegate());
	}

	public void testSavepoint() {
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		tt.setPropagationBehavior(TransactionTemplate.PROPAGATION_NESTED);
		tt.execute(new TransactionCallbackWithoutResult() {
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				Person tony = new Person();
				tony.setFirstName("Tony");
				sharedEntityManager.merge(tony);
				Query q = sharedEntityManager.createQuery("select p from Person as p");
				q.setFlushMode(FlushModeType.COMMIT);
				List<Person> people = q.getResultList();
				assertEquals(1, people.size());
				assertEquals("Tony", people.get(0).getFirstName());
				status.setRollbackOnly();
			}
		});
		Query q = sharedEntityManager.createQuery("select p from Person as p");
		List<Person> people = q.getResultList();
		assertEquals(0, people.size());
	}

}
