/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.test.context.testng.transaction.ejb;

import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.test.context.transaction.ejb.dao.TestEntityDao;

import static org.testng.AssertJUnit.*;

/**
 * Abstract base class for all TestNG-based tests involving EJB transaction
 * support in the TestContext framework.
 *
 * @author Sam Brannen
 * @author Xavier Detant
 * @since 4.0.1
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractEjbTxDaoTestNGTests extends AbstractTransactionalTestNGSpringContextTests {

	protected static final String TEST_NAME = "test-name";

	@EJB
	protected TestEntityDao dao;

	@PersistenceContext
	protected EntityManager em;


	@Test
	public void test1InitialState() {
		int count = dao.getCount(TEST_NAME);
		assertEquals("New TestEntity should have count=0.", 0, count);
	}

	@Test(dependsOnMethods = "test1InitialState")
	public void test2IncrementCount1() {
		int count = dao.incrementCount(TEST_NAME);
		assertEquals("Expected count=1 after first increment.", 1, count);
	}

	/**
	 * The default implementation of this method assumes that the transaction
	 * for {@link #test2IncrementCount1()} was committed. Therefore, it is
	 * expected that the previous increment has been persisted in the database.
	 */
	@Test(dependsOnMethods = "test2IncrementCount1")
	public void test3IncrementCount2() {
		int count = dao.getCount(TEST_NAME);
		assertEquals("Expected count=1 after test2IncrementCount1().", 1, count);

		count = dao.incrementCount(TEST_NAME);
		assertEquals("Expected count=2 now.", 2, count);
	}

	@AfterMethod(alwaysRun = true)
	public void synchronizePersistenceContext() {
		em.flush();
	}

}
