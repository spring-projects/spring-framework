/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito.integration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.mockito.BDDMockito.then;

/**
 * Tests that {@link MockitoSpyBean @MockitoSpyBean} can be used to replace an
 * existing bean with circular dependencies with {@link Autowired @Autowired}
 * setter methods and a {@link Lazy @Lazy} resolution proxy.
 *
 * <p>In contrast to {@link MockitoSpyBeanAndCircularDependenciesWithAutowiredSettersIntegrationTests},
 * this test class works in AOT mode.
 *
 * @author Sam Brannen
 * @author Andy Wilkinson
 * @since 6.2
 * @see MockitoSpyBeanAndCircularDependenciesWithAutowiredSettersIntegrationTests
 */
@SpringJUnitConfig
class MockitoSpyBeanAndCircularDependenciesWithLazyResolutionProxyIntegrationTests {

	@MockitoSpyBean
	One one;

	@Autowired
	Two two;


	@Test
	void beanWithCircularDependenciesCanBeSpied() {
		two.callOne();
		then(one).should().doSomething();
	}


	@Configuration
	@Import({ One.class, Two.class })
	static class Config {
	}

	static class One {

		@SuppressWarnings("unused")
		private Two two;

		@Autowired
		void setTwo(@Lazy Two two) {
			this.two = two;
		}

		void doSomething() {
		}
	}

	static class Two {

		private One one;

		@Autowired
		void setOne(One one) {
			this.one = one;
		}

		void callOne() {
			this.one.doSomething();
		}
	}

}
