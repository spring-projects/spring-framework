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

package org.springframework.test.context.transaction.ejb;

import java.util.concurrent.atomic.AtomicReference;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.transaction.ejb.dao.TestEntityService;
import org.springframework.test.context.transaction.ejb.model.TestEntity;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author Vlad MIhalcea
 */
@ContextConfiguration("required-tx-config.xml")
public class ReadOnlyJpaTxDaoTests extends AbstractJUnit4SpringContextTests {

	protected static final String TEST_NAME = "test-name";

	@PersistenceContext
	private EntityManager em;

	@Autowired
	private TestEntityService testEntityService;

	@Autowired
	protected JpaTransactionManager transactionManager;

	@Before
	public void init() {
		new TransactionTemplate(transactionManager).execute(
		(TransactionCallback<Void>) status -> {
			TestEntity testEntity = new TestEntity();
			testEntity.setName(TEST_NAME);

			em.persist(testEntity);
			return null;
		} );
	}

	@Test
	public void testReadOnlyTx() {
		AtomicReference<TestEntity> entityHolder = new AtomicReference<>();
		assertEquals(0, em.find(TestEntity.class, TEST_NAME).getCount());

		testEntityService.onReadOnly( em -> {
			TestEntity entity = em.find(TestEntity.class, TEST_NAME);
			entity.setCount(100);
			//By setting setDefaultReadOnly(true), the user can no longer modify any entity.
			em.flush();
			em.refresh(entity);
			entityHolder.set(entity);
		});

		assertEquals(0, entityHolder.get().getCount());
	}

}