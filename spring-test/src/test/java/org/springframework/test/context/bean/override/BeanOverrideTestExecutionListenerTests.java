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

package org.springframework.test.context.bean.override;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

/**
 * Integration tests for {@link BeanOverrideTestExecutionListener}.
 *
 * @author Sam Brannen
 * @since 6.2.6
 */
class BeanOverrideTestExecutionListenerTests {

	@Test
	void beanOverrideWithNoMatchingContextName() {
		executeTests(BeanOverrideWithNoMatchingContextNameTestCase.class)
			.assertThatEvents().haveExactly(1, event(test("test"),
				finishedWithFailure(
					instanceOf(IllegalStateException.class),
					message("""
						Test class BeanOverrideWithNoMatchingContextNameTestCase declares @BeanOverride \
						fields [message, number], but no BeanOverrideHandler has been registered. \
						If you are using @ContextHierarchy, ensure that context names for bean overrides match \
						configured @ContextConfiguration names."""))));
	}

	@Test
	void beanOverrideWithInvalidContextName() {
		executeTests(BeanOverrideWithInvalidContextNameTestCase.class)
			.assertThatEvents().haveExactly(1, event(test("test"),
				finishedWithFailure(
					instanceOf(IllegalStateException.class),
					message(msg ->
						msg.startsWith("No bean override instance found for BeanOverrideHandler") &&
						msg.contains("DummyBeanOverrideHandler") &&
						msg.contains("BeanOverrideWithInvalidContextNameTestCase.message2") &&
						msg.contains("contextName = 'BOGUS'") &&
						msg.endsWith("""
							If you are using @ContextHierarchy, ensure that context names for bean overrides match \
							configured @ContextConfiguration names.""")))));
	}


	private static Events executeTests(Class<?> testClass) {
		return EngineTestKit.engine("junit-jupiter")
				.selectors(selectClass(testClass))
				.execute()
				.testEvents()
				.assertStatistics(stats -> stats.started(1).failed(1));
	}


	@ExtendWith(SpringExtension.class)
	@ContextHierarchy({
		@ContextConfiguration(classes = Config1.class),
		@ContextConfiguration(classes = Config2.class, name = "child")
	})
	@DisabledInAotMode("@ContextHierarchy is not supported in AOT")
	static class BeanOverrideWithNoMatchingContextNameTestCase {

		@DummyBean(contextName = "BOGUS")
		String message;

		@DummyBean(contextName = "BOGUS")
		Integer number;

		@Test
		void test() {
			// no-op
		}
	}

	@ExtendWith(SpringExtension.class)
	@ContextHierarchy({
		@ContextConfiguration(classes = Config1.class),
		@ContextConfiguration(classes = Config2.class, name = "child")
	})
	@DisabledInAotMode("@ContextHierarchy is not supported in AOT")
	static class BeanOverrideWithInvalidContextNameTestCase {

		@DummyBean(contextName = "child")
		String message1;

		@DummyBean(contextName = "BOGUS")
		String message2;

		@Test
		void test() {
			// no-op
		}
	}

	@Configuration
	static class Config1 {

		@Bean
		String message() {
			return "Message 1";
		}
	}

	@Configuration
	static class Config2 {

		@Bean
		String message() {
			return "Message 2";
		}
	}

}
