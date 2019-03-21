/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.context.junit4;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

/**
 * Simple unit test to verify the expected functionality of standard JUnit 4.4+
 * testing features.
 * <p>
 * Currently testing: {@link Test @Test} (including expected exceptions and
 * timeouts), {@link BeforeClass @BeforeClass}, {@link Before @Before}, and
 * <em>assumptions</em>.
 * </p>
 * <p>
 * Due to the fact that JUnit does not guarantee a particular ordering of test
 * method execution, the following are currently not tested:
 * {@link org.junit.AfterClass @AfterClass} and {@link org.junit.After @After}.
 * </p>
 *
 * @author Sam Brannen
 * @since 2.5
 * @see StandardJUnit4FeaturesSpringRunnerTests
 */
public class StandardJUnit4FeaturesTests {

	private static int staticBeforeCounter = 0;


	@BeforeClass
	public static void incrementStaticBeforeCounter() {
		StandardJUnit4FeaturesTests.staticBeforeCounter++;
	}


	private int beforeCounter = 0;


	@Test
	@Ignore
	public void alwaysFailsButShouldBeIgnored() {
		fail("The body of an ignored test should never be executed!");
	}

	@Test
	public void alwaysSucceeds() {
		assertTrue(true);
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void expectingAnIndexOutOfBoundsException() {
		new ArrayList<>().get(1);
	}

	@Test
	public void failedAssumptionShouldPrecludeImminentFailure() {
		assumeTrue(false);
		fail("A failed assumption should preclude imminent failure!");
	}

	@Before
	public void incrementBeforeCounter() {
		this.beforeCounter++;
	}

	@Test(timeout = 10000)
	public void noOpShouldNotTimeOut() {
		/* no-op */
	}

	@Test
	public void verifyBeforeAnnotation() {
		assertEquals(1, this.beforeCounter);
	}

	@Test
	public void verifyBeforeClassAnnotation() {
		// Instead of testing for equality to 1, we just assert that the value
		// was incremented at least once, since this test class may serve as a
		// parent class to other tests in a suite, etc.
		assertTrue(StandardJUnit4FeaturesTests.staticBeforeCounter > 0);
	}

}
