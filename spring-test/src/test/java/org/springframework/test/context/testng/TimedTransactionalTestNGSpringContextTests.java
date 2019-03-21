/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.context.testng;

import org.testng.annotations.Test;

import org.springframework.test.context.ContextConfiguration;

import static org.springframework.test.transaction.TransactionTestUtils.*;

/**
 * Timed integration tests for
 * {@link AbstractTransactionalTestNGSpringContextTests}; used to verify claim
 * raised in <a href="https://jira.springframework.org/browse/SPR-6124"
 * target="_blank">SPR-6124</a>.
 *
 * @author Sam Brannen
 * @since 3.0
 */
@ContextConfiguration
public class TimedTransactionalTestNGSpringContextTests extends AbstractTransactionalTestNGSpringContextTests {

	@Test
	public void testWithoutTimeout() {
		assertInTransaction(true);
	}

	// TODO Enable TestNG test with timeout once we have a solution.
	@Test(timeOut = 10000, enabled = false)
	public void testWithTimeout() {
		assertInTransaction(true);
	}

}
