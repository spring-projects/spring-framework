/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

import static org.springframework.test.transaction.TransactionAssert.assertThatTransaction;

/**
 * This class is an extension of {@link TimedTransactionalSpringRunnerTests}
 * that has been modified to use {@link SpringClassRule} and
 * {@link SpringMethodRule}.
 *
 * @author Sam Brannen
 * @since 4.2
 */
@RunWith(JUnit4.class)
@SuppressWarnings("deprecation")
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
		assertThatTransaction().isNotActive();
	}

	/**
	 * {@code timeout} explicitly not declared due to presence of Timeout rule.
	 */
	@Test
	public void transactionalWithJUnitRuleBasedTimeout() {
		assertThatTransaction().isActive();
	}

	// All other tests are in superclass.

}
