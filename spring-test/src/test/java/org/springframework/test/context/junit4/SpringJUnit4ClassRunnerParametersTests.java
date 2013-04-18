/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.junit4;

import java.util.Arrays;
import java.util.Collection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.Assert.*;

/**
 * Tests the behaviour of {@code Parameters} annotation in a spring test.
 * 
 * @author Gaetan Pitteloud
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="SpringJUnit4ClassRunnerAppCtxTests-context.xml")
public class SpringJUnit4ClassRunnerParametersTests {
	
	private static int beforeClassCount;
	private static int testExecutionCount;
	private static boolean afterClassInvoked;

	private int expected;

	private int actual;

	@Autowired
	ApplicationContext context;

	@Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[] { 1, 1 }, new Object[] { 2, 2 }, new Object[] {3, 3 });
	}

	public SpringJUnit4ClassRunnerParametersTests(int expected, int actual) {
		this.expected = expected;
		this.actual = actual;
	}
	
	@BeforeClass public static void before() {
		beforeClassCount++;
	}

	@Test
	public void basicTest() throws Exception {
		assertEquals(expected, actual);
		assertNotNull(context);
		assertEquals("Foo", context.getBean("foo"));
		testExecutionCount++;
	}
	
	@AfterClass public static void sumUp() {
		assertEquals(3, testExecutionCount);
		// verify that beforeClass/afterClass are invoked only once
		assertEquals(1, beforeClassCount);
		assertFalse(afterClassInvoked);
		afterClassInvoked = true;
	}

}
