/*
 * Copyright 2002-2025 the original author or authors.
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.aot.DisabledInAotMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies support for JUnit 4.7 {@link Rule Rules} in conjunction with the
 * {@link SpringRunner}. The body of this test class is taken from the
 * JUnit 4.7 release notes.
 *
 * @author JUnit 4.7 Team
 * @author Sam Brannen
 * @since 3.0
 */
@RunWith(SpringRunner.class)
@TestExecutionListeners({})
@DisabledInAotMode("Does not load an ApplicationContext and thus not supported for AOT processing")
@SuppressWarnings("deprecation")
public class SpringJUnit47ClassRunnerRuleTests {

	@Rule
	public TestName name = new TestName();


	@Test
	public void testA() {
		assertThat(name.getMethodName()).isEqualTo("testA");
	}

	@Test
	public void testB() {
		assertThat(name.getMethodName()).isEqualTo("testB");
	}

}
