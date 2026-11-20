/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit4;

import org.junit.runners.model.InitializationError;

/**
 * {@code SpringRunner} is an <em>alias</em> for the {@link SpringJUnit4ClassRunner}.
 *
 * <p>To use this class, annotate a JUnit 4 based test class with
 * {@code @RunWith(SpringRunner.class)}.
 *
 * <p>If you would like to use the Spring TestContext Framework with a runner other than
 * this one, use {@link org.springframework.test.context.junit4.rules.SpringClassRule}
 * and {@link org.springframework.test.context.junit4.rules.SpringMethodRule}.
 *
 * <p><strong>NOTE:</strong> This class requires JUnit 4.12 or higher.
 *
 * @author Sam Brannen
 * @since 4.3
 * @see SpringJUnit4ClassRunner
 * @see org.springframework.test.context.junit4.rules.SpringClassRule
 * @see org.springframework.test.context.junit4.rules.SpringMethodRule
 * @deprecated since Spring Framework 7.0 in favor of the
 * {@link org.springframework.test.context.junit.jupiter.SpringExtension SpringExtension}
 * and JUnit Jupiter
 */
@Deprecated(since = "7.0")
public final class SpringRunner extends SpringJUnit4ClassRunner {

	/**
	 * Construct a new {@code SpringRunner} and initialize a
	 * {@link org.springframework.test.context.TestContextManager TestContextManager}
	 * to provide Spring testing functionality to standard JUnit 4 tests.
	 * @param clazz the test class to be run
	 * @see #createTestContextManager(Class)
	 */
	public SpringRunner(Class<?> clazz) throws InitializationError {
		super(clazz);
	}

}
