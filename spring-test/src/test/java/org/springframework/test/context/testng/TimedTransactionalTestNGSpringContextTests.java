/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.testng;

import org.testng.annotations.Test;

import org.springframework.test.context.ContextConfiguration;

import static org.springframework.test.transaction.TransactionAssert.assertThatTransaction;

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
class TimedTransactionalTestNGSpringContextTests extends AbstractTransactionalTestNGSpringContextTests {

	@Test
	void testWithoutTimeout() {
		assertThatTransaction().isActive();
	}

	// TODO Enable TestNG test with timeout once we have a solution.
	@Test(timeOut = 10000, enabled = false)
	void testWithTimeout() {
		assertThatTransaction().isActive();
	}

}
