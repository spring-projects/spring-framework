/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit4.concurrency;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.ParallelComputer;

import org.springframework.core.testfixture.TestGroup;
import org.springframework.test.context.junit4.SpringJUnit47ClassRunnerRuleTests;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.junit4.TimedTransactionalSpringRunnerTests;
import org.springframework.test.context.junit4.rules.BaseAppCtxRuleTests;
import org.springframework.test.context.junit4.rules.BasicAnnotationConfigWacSpringRuleTests;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assume.assumeTrue;
import static org.springframework.core.annotation.AnnotatedElementUtils.hasAnnotation;
import static org.springframework.test.context.junit4.JUnitTestingUtils.runTestsAndAssertCounters;

/**
 * Concurrency tests for the {@link SpringRunner}, {@link SpringClassRule}, and
 * {@link SpringMethodRule} that use JUnit 4's experimental {@link ParallelComputer}
 * to execute tests in parallel.
 *
 * <p>The tests executed by this test class come from a hand-picked collection of test
 * classes within the test suite that is intended to cover most categories of tests
 * that are currently supported by the TestContext Framework on JUnit 4.
 *
 * <p>The chosen test classes intentionally do <em>not</em> include any classes that
 * fall under the following categories.
 *
 * <ul>
 * <li>tests that make use of Spring's {@code @DirtiesContext} support
 * <li>tests that make use of JUnit 4's {@code @FixMethodOrder} support
 * <li>tests that commit changes to the state of a shared in-memory database
 * </ul>
 *
 * <p><strong>NOTE</strong>: these tests only run if the {@link TestGroup#LONG_RUNNING
 * LONG_RUNNING} test group is enabled.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see org.springframework.test.context.TestContextConcurrencyTests
 */
public class SpringJUnit4ConcurrencyTests {

	private final Class<?>[] testClasses = new Class<?>[] {
		// Basics
			SpringJUnit47ClassRunnerRuleTests.class,
			BaseAppCtxRuleTests.class,
		// Transactional
			TimedTransactionalSpringRunnerTests.class,
		// Web and Scopes
			BasicAnnotationConfigWacSpringRuleTests.class,
	};


	@BeforeClass
	public static void abortIfLongRunningTestGroupIsNotEnabled() {
		assumeTrue("TestGroup " + TestGroup.LONG_RUNNING + " is not active.", TestGroup.LONG_RUNNING.isActive());
	}

	@Test
	public void runAllTestsConcurrently() throws Exception {
		final int FAILED = 0;
		final int ABORTED = 0;
		final int IGNORED = countAnnotatedMethods(Ignore.class);
		final int TESTS = countAnnotatedMethods(Test.class) - IGNORED;

		runTestsAndAssertCounters(new ParallelComputer(true, true), TESTS, FAILED, TESTS, IGNORED, ABORTED,
				this.testClasses);
	}

	private int countAnnotatedMethods(Class<? extends Annotation> annotationType) {
		return (int) Arrays.stream(this.testClasses)
				.map(ReflectionUtils::getUniqueDeclaredMethods)
				.flatMap(Arrays::stream)
				.filter(method -> hasAnnotation(method, annotationType))
				.count();
	}

}
