/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.aop.aspectj;

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for matching of bean() pointcut designator.
 *
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
class BeanNamePointcutMatchingTests {

	@Test
	void testMatchingPointcuts() {
		assertMatch("someName", "bean(someName)");

		// Spring bean names are less restrictive compared to AspectJ names (methods, types etc.)
		// MVC Controller-kind
		assertMatch("someName/someOtherName", "bean(someName/someOtherName)");
		assertMatch("someName/foo/someOtherName", "bean(someName/*/someOtherName)");
		assertMatch("someName/foo/bar/someOtherName", "bean(someName/*/someOtherName)");
		assertMatch("someName/*/**", "bean(someName/*)");
		// JMX-kind
		assertMatch("service:name=traceService", "bean(service:name=traceService)");
		assertMatch("service:name=traceService", "bean(service:name=*)");
		assertMatch("service:name=traceService", "bean(*:name=traceService)");

		// Wildcards
		assertMatch("someName", "bean(*someName)");
		assertMatch("someName", "bean(*Name)");
		assertMatch("someName", "bean(*)");
		assertMatch("someName", "bean(someName*)");
		assertMatch("someName", "bean(some*)");
		assertMatch("someName", "bean(some*Name)");
		assertMatch("someName", "bean(*some*Name*)");
		assertMatch("someName", "bean(*s*N*)");

		// Or, and, not expressions
		assertMatch("someName", "bean(someName) || bean(someOtherName)");
		assertMatch("someOtherName", "bean(someName) || bean(someOtherName)");

		assertMatch("someName", "!bean(someOtherName)");

		assertMatch("someName", "bean(someName) || !bean(someOtherName)");
		assertMatch("someName", "bean(someName) && !bean(someOtherName)");
	}

	@Test
	void testNonMatchingPointcuts() {
		assertMisMatch("someName", "bean(someNamex)");
		assertMisMatch("someName", "bean(someX*Name)");

		// And, not expressions
		assertMisMatch("someName", "bean(someName) && bean(someOtherName)");
		assertMisMatch("someName", "!bean(someName)");
		assertMisMatch("someName", "!bean(someName) && bean(someOtherName)");
		assertMisMatch("someName", "!bean(someName) || bean(someOtherName)");
	}


	private void assertMatch(String beanName, String pcExpression) {
		assertThat(matches(beanName, pcExpression)).as("Unexpected mismatch for bean \"" + beanName + "\" for pcExpression \"" + pcExpression + "\"").isTrue();
	}

	private void assertMisMatch(String beanName, String pcExpression) {
		assertThat(matches(beanName, pcExpression)).as("Unexpected match for bean \"" + beanName + "\" for pcExpression \"" + pcExpression + "\"").isFalse();
	}

	private static boolean matches(final String beanName, String pcExpression) {
		AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut() {
			@Override
			protected String getCurrentProxiedBeanName() {
				return beanName;
			}
		};
		pointcut.setExpression(pcExpression);
		return pointcut.matches(TestBean.class);
	}

}
