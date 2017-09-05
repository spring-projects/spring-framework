/*
 * Copyright 2002-2016 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

/**
 * @author Sam Brannen
 * @since 3.2.2
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
	@ContextConfiguration(classes = SingleTestClassWithTwoLevelContextHierarchyTests.ParentConfig.class),
	@ContextConfiguration(classes = SingleTestClassWithTwoLevelContextHierarchyTests.ChildConfig.class) })
public class SingleTestClassWithTwoLevelContextHierarchyTests {

	@Configuration
	public static class ParentConfig {

		@Bean
		public String foo() {
			return "foo";
		}

		@Bean
		public String baz() {
			return "baz-parent";
		}
	}

	@Configuration
	public static class ChildConfig {

		@Bean
		public String bar() {
			return "bar";
		}

		@Bean
		public String baz() {
			return "baz-child";
		}
	}


	@Autowired
	private String foo;

	@Autowired
	private String bar;

	@Autowired
	private String baz;

	@Autowired
	private ApplicationContext context;


	@Test
	public void loadContextHierarchy() {
		assertNotNull("child ApplicationContext", context);
		assertNotNull("parent ApplicationContext", context.getParent());
		assertNull("grandparent ApplicationContext", context.getParent().getParent());
		assertEquals("foo", foo);
		assertEquals("bar", bar);
		assertEquals("baz-child", baz);
	}

}
