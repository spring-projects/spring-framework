/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test.context.junit4.rules;

import java.util.concurrent.TimeUnit;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.junit4.TimedTransactionalSpringRunnerTests;

import static org.springframework.test.transaction.TransactionTestUtils.*;

/**
 * This class is an extension of {@link TimedTransactionalSpringRunnerTests}
 * that has been modified to use {@link SpringClassRule} and
 * {@link SpringMethodRule}.
 *
 * @author Sam Brannen
 * @since 4.2
 */
@RunWith(JUnit4.class)
public class TimedTransactionalSpringRuleTests extends TimedTransactionalSpringRunnerTests {

	@ClassRule
	public static final SpringClassRule springClassRule = new SpringClassRule();

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();

	@Rule
	public Timeout timeout = Timeout.builder().withTimeout(10, TimeUnit.SECONDS).build();


	/**
	 * Overridden since Spring's Rule-based JUnit support cannot properly
	 * integrate with timed execution that is controlled by a third-party runner.
	 */
	@Test(timeout = 10000)
	@Repeat(5)
	@Override
	public void transactionalWithJUnitTimeout() {
		assertInTransaction(false);
	}

	/**
	 * {@code timeout} explicitly not declared due to presence of Timeout rule.
	 */
	@Test
	public void transactionalWithJUnitRuleBasedTimeout() {
		assertInTransaction(true);
	}

	// All other tests are in superclass.

}
