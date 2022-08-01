/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.context.transaction.ejb;

import jakarta.ejb.EJB;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.transaction.ejb.dao.TestEntityDao;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for all tests involving EJB transaction support in the
 * TestContext framework.
 *
 * @author Sam Brannen
 * @author Xavier Detant
 * @since 4.0.1
 */
@SpringJUnitConfig
@Transactional
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.MethodName.class)
abstract class AbstractEjbTxDaoTests {

	protected static final String TEST_NAME = "test-name";

	@EJB
	protected TestEntityDao dao;

	@PersistenceContext
	protected EntityManager em;


	@Test
	void test1InitialState() {
		int count = dao.getCount(TEST_NAME);
		assertThat(count).as("New TestEntity should have count=0.").isEqualTo(0);
	}

	@Test
	void test2IncrementCount1() {
		int count = dao.incrementCount(TEST_NAME);
		assertThat(count).as("Expected count=1 after first increment.").isEqualTo(1);
	}

	/**
	 * The default implementation of this method assumes that the transaction
	 * for {@link #test2IncrementCount1()} was committed. Therefore, it is
	 * expected that the previous increment has been persisted in the database.
	 */
	@Test
	void test3IncrementCount2() {
		int count = dao.getCount(TEST_NAME);
		assertThat(count).as("Expected count=1 after test2IncrementCount1().").isEqualTo(1);

		count = dao.incrementCount(TEST_NAME);
		assertThat(count).as("Expected count=2 now.").isEqualTo(2);
	}

	@AfterEach
	void synchronizePersistenceContext() {
		em.flush();
	}

}
