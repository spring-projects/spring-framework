/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit4;

import org.junit.runner.RunWith;

import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.aot.DisabledInAotMode;

/**
 * Simple unit test to verify that the {@link SpringRunner} does not
 * hinder correct functionality of standard JUnit 4 testing features.
 *
 * <p>Note that {@link TestExecutionListeners @TestExecutionListeners} is
 * explicitly configured with an empty list, thus disabling all default
 * listeners.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see StandardJUnit4FeaturesTests
 */
@RunWith(SpringRunner.class)
@TestExecutionListeners({})
@DisabledInAotMode("Does not load an ApplicationContext and thus not supported for AOT processing")
@SuppressWarnings("deprecation")
public class StandardJUnit4FeaturesSpringRunnerTests extends StandardJUnit4FeaturesTests {

	/* All tests are in the parent class... */

}
