/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SimpleAliasRegistry}.
 *
 * @author Juergen Hoeller
 * @author Nha Vuong
 * @author Sam Brannen
 */
class SimpleAliasRegistryTests {

	private final SimpleAliasRegistry registry = new SimpleAliasRegistry();

	@Test
	void aliasChaining() {
		registry.registerAlias("test", "testAlias");
		registry.registerAlias("testAlias", "testAlias2");
		registry.registerAlias("testAlias2", "testAlias3");

		assertThat(registry.hasAlias("test", "testAlias")).isTrue();
		assertThat(registry.hasAlias("test", "testAlias2")).isTrue();
		assertThat(registry.hasAlias("test", "testAlias3")).isTrue();
		assertThat(registry.canonicalName("testAlias")).isEqualTo("test");
		assertThat(registry.canonicalName("testAlias2")).isEqualTo("test");
		assertThat(registry.canonicalName("testAlias3")).isEqualTo("test");
	}

	@Test  // SPR-17191
	void aliasChainingWithMultipleAliases() {
		registry.registerAlias("name", "alias_a");
		registry.registerAlias("name", "alias_b");
		assertThat(registry.hasAlias("name", "alias_a")).isTrue();
		assertThat(registry.hasAlias("name", "alias_b")).isTrue();

		registry.registerAlias("real_name", "name");
		assertThat(registry.hasAlias("real_name", "name")).isTrue();
		assertThat(registry.hasAlias("real_name", "alias_a")).isTrue();
		assertThat(registry.hasAlias("real_name", "alias_b")).isTrue();

		registry.registerAlias("name", "alias_c");
		assertThat(registry.hasAlias("real_name", "name")).isTrue();
		assertThat(registry.hasAlias("real_name", "alias_a")).isTrue();
		assertThat(registry.hasAlias("real_name", "alias_b")).isTrue();
		assertThat(registry.hasAlias("real_name", "alias_c")).isTrue();
	}

	@Test
	void removeAlias() {
		registry.registerAlias("real_name", "nickname");
		assertThat(registry.hasAlias("real_name", "nickname")).isTrue();

		registry.removeAlias("nickname");
		assertThat(registry.hasAlias("real_name", "nickname")).isFalse();
	}

	@Test
	void isAlias() {
		registry.registerAlias("real_name", "nickname");
		assertThat(registry.isAlias("nickname")).isTrue();
		assertThat(registry.isAlias("real_name")).isFalse();
		assertThat(registry.isAlias("fake")).isFalse();
	}

	@Test
	void getAliases() {
		registry.registerAlias("test", "testAlias1");
		assertThat(registry.getAliases("test")).containsExactly("testAlias1");

		registry.registerAlias("testAlias1", "testAlias2");
		registry.registerAlias("testAlias2", "testAlias3");
		assertThat(registry.getAliases("test")).containsExactlyInAnyOrder("testAlias1", "testAlias2", "testAlias3");
		assertThat(registry.getAliases("testAlias1")).containsExactlyInAnyOrder("testAlias2", "testAlias3");
		assertThat(registry.getAliases("testAlias2")).containsExactly("testAlias3");

		assertThat(registry.getAliases("testAlias3")).isEmpty();
	}

}
