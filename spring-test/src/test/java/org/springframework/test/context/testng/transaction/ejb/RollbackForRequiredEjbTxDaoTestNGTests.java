/*
 * Copyright 2002-2015 the original author or authors.
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

import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.*;

/**
 * Extension of {@link CommitForRequiredEjbTxDaoTestNGTests} which sets the default
 * rollback semantics for the {@link TransactionalTestExecutionListener} to
 * {@code true}. The transaction managed by the TestContext framework will be
 * rolled back after each test method. Consequently, any work performed in
 * transactional methods that participate in the test-managed transaction will
 * be rolled back automatically.
 *
 * @author Sam Brannen
 * @since 4.0.1
 */
@Test(suiteName = "Rollback for REQUIRED")
@Rollback
public class RollbackForRequiredEjbTxDaoTestNGTests extends CommitForRequiredEjbTxDaoTestNGTests {

	/**
	 * Overrides parent implementation in order to change expectations to align with
	 * behavior associated with "required" transactions on repositories/DAOs and
	 * default rollback semantics for transactions managed by the TestContext
	 * framework.
	 */
	@Test(dependsOnMethods = "test2IncrementCount1")
	@Override
	public void test3IncrementCount2() {
		int count = dao.getCount(TEST_NAME);
		// Expecting count=0 after test2IncrementCount1() since REQUIRED transactions
		// participate in the existing transaction (if present), which in this case is the
		// transaction managed by the TestContext framework which will be rolled back
		// after each test method.
		assertEquals("Expected count=0 after test2IncrementCount1().", 0, count);

		count = dao.incrementCount(TEST_NAME);
		assertEquals("Expected count=1 now.", 1, count);
	}

}