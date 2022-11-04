/*
 * Copyright 2022-2022 the original author or authors.
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Arseniy Skvortsov
 */
@ExtendWith(MockitoExtension.class)
public class SharedEntityManagerCreatorTransactionSyncTests {

	@Test
	public void noSharingInNeverPropagation() {
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = mock(EntityManager.class);
		var transactionManager = new JpaTransactionManager(emf);
		//Could be also remedied by switching this to 'SYNCHRONIZATION_ON_ACTUAL_TRANSACTION'.
		transactionManager.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_ALWAYS);
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		Query query = mock(Query.class);
		given(emf.createEntityManager()).willReturn(em);
		given(em.createQuery("x")).willReturn(query);
		given(em.isOpen()).willReturn(true);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NEVER);
		tt.executeWithoutResult(status -> {
			SharedEntityManagerCreator
					.createSharedEntityManager(emf)
					.createQuery("x")
					.executeUpdate();
			verify(query).executeUpdate();
			verify(em).close();
		});
	}
}
