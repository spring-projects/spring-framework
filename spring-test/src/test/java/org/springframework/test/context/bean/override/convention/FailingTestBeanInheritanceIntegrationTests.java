/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.context.bean.override.convention;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.bean.override.convention.AbstractTestBeanIntegrationTestCase.Pojo;
import org.springframework.test.context.junit.EngineTestKitUtils;

import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

/**
 * {@link TestBean @TestBean} inheritance integration tests for failure scenarios.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 * @see TestBeanInheritanceIntegrationTests
 */
class FailingTestBeanInheritanceIntegrationTests {

	@Test
	void failsIfFieldInSupertypeButNoMethod() {
		Class<?> testClass = FieldInSupertypeButNoMethodTestCase.class;
		EngineTestKitUtils.executeTestsForClass(testClass).assertThatEvents().haveExactly(1,
			finishedWithFailure(
					instanceOf(IllegalStateException.class),
					message("No static method found named someBean() in %s with return type %s"
							.formatted(FieldInSupertypeButNoMethodTestCase.class.getName(), Pojo.class.getName()))));
	}

	@Test
	void failsIfMethod1InSupertypeAndMethod2InType() {
		Class<?> testClass = Method1InSupertypeAndMethod2InTypeTestCase.class;
		EngineTestKitUtils.executeTestsForClass(testClass).assertThatEvents().haveExactly(1,
			finishedWithFailure(
					instanceOf(IllegalStateException.class),
					message("Found 2 competing static methods named anotherBean() or thirdBean() in %s with return type %s"
							.formatted(Method1InSupertypeAndMethod2InTypeTestCase.class.getName(), Pojo.class.getName()))));
	}


	static class FieldInSupertypeButNoMethodTestCase extends AbstractTestBeanIntegrationTestCase {

		@Test
		void test() {
		}
	}

	static class Method1InSupertypeAndMethod2InTypeTestCase extends AbstractTestBeanIntegrationTestCase {

		static Pojo someBean() {
			return new FakePojo("ignored");
		}

		static Pojo anotherBean() {
			return new FakePojo("sub2");
		}

		@Test
		void test() {
		}
	}

}
