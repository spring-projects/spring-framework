/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 */
public class SimpleAliasRegistryTests {

	@Test
	public void testAliasChaining() {
		SimpleAliasRegistry registry = new SimpleAliasRegistry();
		registry.registerAlias("test", "testAlias");
		registry.registerAlias("testAlias", "testAlias2");
		registry.registerAlias("testAlias2", "testAlias3");

		assertTrue(registry.hasAlias("test", "testAlias"));
		assertTrue(registry.hasAlias("test", "testAlias2"));
		assertTrue(registry.hasAlias("test", "testAlias3"));
		assertSame("test", registry.canonicalName("testAlias"));
		assertSame("test", registry.canonicalName("testAlias2"));
		assertSame("test", registry.canonicalName("testAlias3"));
	}

	@Test  // SPR-17191
	public void testAliasChainingWithMultipleAliases() {
		SimpleAliasRegistry registry = new SimpleAliasRegistry();
		registry.registerAlias("name", "alias_a");
		registry.registerAlias("name", "alias_b");
		assertTrue(registry.hasAlias("name", "alias_a"));
		assertTrue(registry.hasAlias("name", "alias_b"));

		registry.registerAlias("real_name", "name");
		assertTrue(registry.hasAlias("real_name", "name"));
		assertTrue(registry.hasAlias("real_name", "alias_a"));
		assertTrue(registry.hasAlias("real_name", "alias_b"));

		registry.registerAlias("name", "alias_c");
		assertTrue(registry.hasAlias("real_name", "name"));
		assertTrue(registry.hasAlias("real_name", "alias_a"));
		assertTrue(registry.hasAlias("real_name", "alias_b"));
		assertTrue(registry.hasAlias("real_name", "alias_c"));
	}

}
