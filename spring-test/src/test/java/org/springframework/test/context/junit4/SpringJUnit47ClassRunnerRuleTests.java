/*
 * Copyright 2009 the original author or authors.
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

package org.springframework.test.context.junit4;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.test.context.TestExecutionListeners;

/**
 * Verifies support for JUnit 4.7 {@link Rule Rules} in conjunction with the
 * {@link SpringJUnit4ClassRunner}. The body of this test class is taken from
 * the JUnit 4.7 release notes.
 *
 * @author JUnit 4.7 Team
 * @author Sam Brannen
 * @since 3.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners( {})
public class SpringJUnit47ClassRunnerRuleTests {

	@Rule
	public TestName name = new TestName();


	@Test
	public void testA() {
		assertEquals("testA", name.getMethodName());
	}

	@Test
	public void testB() {
		assertEquals("testB", name.getMethodName());
	}
}
