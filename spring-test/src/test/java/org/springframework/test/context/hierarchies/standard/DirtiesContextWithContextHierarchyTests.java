/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.test.context.hierarchies.standard;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

/**
 * Integration tests that verify support for {@link DirtiesContext.HierarchyMode}
 * in conjunction with context hierarchies configured via {@link ContextHierarchy}.
 *
 * <p>Note that correct method execution order is essential, thus the use of
 * {@link FixMethodOrder}.
 *
 * @author Sam Brannen
 * @since 3.2.2
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({ @ContextConfiguration(classes = DirtiesContextWithContextHierarchyTests.ParentConfig.class),
	@ContextConfiguration(classes = DirtiesContextWithContextHierarchyTests.ChildConfig.class) })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DirtiesContextWithContextHierarchyTests {

	@Configuration
	static class ParentConfig {

		@Bean
		public StringBuffer foo() {
			return new StringBuffer("foo");
		}

		@Bean
		public StringBuffer baz() {
			return new StringBuffer("baz-parent");
		}
	}

	@Configuration
	static class ChildConfig {

		@Bean
		public StringBuffer baz() {
			return new StringBuffer("baz-child");
		}
	}


	@Autowired
	private StringBuffer foo;

	@Autowired
	private StringBuffer baz;

	@Autowired
	private ApplicationContext context;


	// -------------------------------------------------------------------------

	private void reverseStringBuffers() {
		foo.reverse();
		baz.reverse();
	}

	private void assertOriginalState() {
		assertCleanParentContext();
		assertCleanChildContext();
	}

	private void assertCleanParentContext() {
		assertEquals("foo", foo.toString());
	}

	private void assertCleanChildContext() {
		assertEquals("baz-child", baz.toString());
	}

	private void assertDirtyParentContext() {
		assertEquals("oof", foo.toString());
	}

	private void assertDirtyChildContext() {
		assertEquals("dlihc-zab", baz.toString());
	}

	// -------------------------------------------------------------------------

	@Before
	public void verifyContextHierarchy() {
		assertNotNull("child ApplicationContext", context);
		assertNotNull("parent ApplicationContext", context.getParent());
		assertNull("grandparent ApplicationContext", context.getParent().getParent());
	}

	@Test
	public void test1_verifyOriginalStateAndDirtyContexts() {
		assertOriginalState();
		reverseStringBuffers();
	}

	@Test
	@DirtiesContext
	public void test2_verifyContextsWereDirtiedAndTriggerExhaustiveCacheClearing() {
		assertDirtyParentContext();
		assertDirtyChildContext();
	}

	@Test
	@DirtiesContext(hierarchyMode = HierarchyMode.CURRENT_LEVEL)
	public void test3_verifyOriginalStateWasReinstatedAndDirtyContextsAndTriggerCurrentLevelCacheClearing() {
		assertOriginalState();
		reverseStringBuffers();
	}

	@Test
	public void test4_verifyParentContextIsStillDirtyButChildContextHasBeenReinstated() {
		assertDirtyParentContext();
		assertCleanChildContext();
	}

}
