/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	void matchingPointcuts() {
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
	void nonMatchingPointcuts() {
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
