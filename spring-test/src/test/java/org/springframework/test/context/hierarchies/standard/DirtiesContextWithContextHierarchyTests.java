/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.context.hierarchies.standard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for {@link DirtiesContext.HierarchyMode}
 * in conjunction with context hierarchies configured via {@link ContextHierarchy}.
 *
 * <p>Note that correct method execution order is essential, thus the use of
 * {@link TestMethodOrder @TestMethodOrder}.
 *
 * @author Sam Brannen
 * @since 3.2.2
 */
@ExtendWith(SpringExtension.class)
@ContextHierarchy({
	@ContextConfiguration(classes = DirtiesContextWithContextHierarchyTests.ParentConfig.class),
	@ContextConfiguration(classes = DirtiesContextWithContextHierarchyTests.ChildConfig.class)
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisabledInAotMode // @ContextHierarchy is not supported in AOT.
class DirtiesContextWithContextHierarchyTests {

	@Autowired
	private StringBuilder foo;

	@Autowired
	private StringBuilder baz;

	@Autowired
	private ApplicationContext context;


	@BeforeEach
	void verifyContextHierarchy() {
		assertThat(context).as("child ApplicationContext").isNotNull();
		assertThat(context.getParent()).as("parent ApplicationContext").isNotNull();
		assertThat(context.getParent().getParent()).as("grandparent ApplicationContext").isNull();
	}

	@Test
	@Order(1)
	void verifyOriginalStateAndDirtyContexts() {
		assertOriginalState();
		reverseStringBuilders();
	}

	@Test
	@Order(2)
	@DirtiesContext
	void verifyContextsWereDirtiedAndTriggerExhaustiveCacheClearing() {
		assertDirtyParentContext();
		assertDirtyChildContext();
	}

	@Test
	@Order(3)
	@DirtiesContext(hierarchyMode = HierarchyMode.CURRENT_LEVEL)
	void verifyOriginalStateWasReinstatedAndDirtyContextsAndTriggerCurrentLevelCacheClearing() {
		assertOriginalState();
		reverseStringBuilders();
	}

	@Test
	@Order(4)
	void verifyParentContextIsStillDirtyButChildContextHasBeenReinstated() {
		assertDirtyParentContext();
		assertCleanChildContext();
	}

	private void reverseStringBuilders() {
		foo.reverse();
		baz.reverse();
	}

	private void assertOriginalState() {
		assertCleanParentContext();
		assertCleanChildContext();
	}

	private void assertCleanParentContext() {
		assertThat(foo.toString()).isEqualTo("foo");
	}

	private void assertCleanChildContext() {
		assertThat(baz.toString()).isEqualTo("baz-child");
	}

	private void assertDirtyParentContext() {
		assertThat(foo.toString()).isEqualTo("oof");
	}

	private void assertDirtyChildContext() {
		assertThat(baz.toString()).isEqualTo("dlihc-zab");
	}


	@Configuration
	static class ParentConfig {

		@Bean
		StringBuilder foo() {
			return new StringBuilder("foo");
		}

		@Bean
		StringBuilder baz() {
			return new StringBuilder("baz-parent");
		}
	}

	@Configuration
	static class ChildConfig {

		@Bean
		StringBuilder baz() {
			return new StringBuilder("baz-child");
		}
	}

}
