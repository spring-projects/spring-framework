/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.beans;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PropertyAccessorUtils}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class PropertyAccessorUtilsTests {

	@Test
	public void testGetPropertyName() {
		assertThat(PropertyAccessorUtils.getPropertyName("foo"))
				.isEqualTo("foo");
		assertThat(PropertyAccessorUtils.getPropertyName("[foo]"))
				.isEqualTo("");
	}

	@Test
	public void testIsNestedOrIndexedProperty() {
		assertThat(PropertyAccessorUtils.isNestedOrIndexedProperty(null))
				.isFalse();
		assertThat(PropertyAccessorUtils.isNestedOrIndexedProperty("foo"))
				.isFalse();
		assertThat(PropertyAccessorUtils.isNestedOrIndexedProperty("[foo]"))
				.isTrue();
		assertThat(PropertyAccessorUtils.isNestedOrIndexedProperty("foo.txt"))
				.isTrue();
	}

	@Test
	public void testGetFirstNestedPropertySeparatorIndex() {
		assertThat(PropertyAccessorUtils
				.getFirstNestedPropertySeparatorIndex("[foo]")).isEqualTo(-1);
		assertThat(PropertyAccessorUtils
				.getFirstNestedPropertySeparatorIndex("foo.txt")).isEqualTo(3);
	}

	@Test
	public void testGetLastNestedPropertySeparatorIndex() {
		assertThat(PropertyAccessorUtils
				.getLastNestedPropertySeparatorIndex("[foo]")).isEqualTo(-1);
		assertThat(PropertyAccessorUtils
				.getLastNestedPropertySeparatorIndex("foo.txt")).isEqualTo(3);
	}

	@Test
	public void testMatchesProperty() {
		assertThat(PropertyAccessorUtils
				.matchesProperty("foo", "bar")).isFalse();
		assertThat(PropertyAccessorUtils
				.matchesProperty("foobar", "foo")).isFalse();
		assertThat(PropertyAccessorUtils
				.matchesProperty("bar[foo]", "foo")).isFalse();

		assertThat(PropertyAccessorUtils
				.matchesProperty("foo", "foo")).isTrue();
		assertThat(PropertyAccessorUtils
				.matchesProperty("foo[bar]", "foo")).isTrue();
	}

	@Test
	public void canonicalPropertyName() {
		assertThat(PropertyAccessorUtils.canonicalPropertyName(null)).isEqualTo("");
		assertThat(PropertyAccessorUtils.canonicalPropertyName("map")).isEqualTo("map");
		assertThat(PropertyAccessorUtils.canonicalPropertyName("map[key1]")).isEqualTo("map[key1]");
		assertThat(PropertyAccessorUtils.canonicalPropertyName("map['key1']")).isEqualTo("map[key1]");
		assertThat(PropertyAccessorUtils.canonicalPropertyName("map[\"key1\"]")).isEqualTo("map[key1]");
		assertThat(PropertyAccessorUtils.canonicalPropertyName("map[key1][key2]")).isEqualTo("map[key1][key2]");
		assertThat(PropertyAccessorUtils.canonicalPropertyName("map['key1'][\"key2\"]")).isEqualTo("map[key1][key2]");
		assertThat(PropertyAccessorUtils.canonicalPropertyName("map[key1].name")).isEqualTo("map[key1].name");
		assertThat(PropertyAccessorUtils.canonicalPropertyName("map['key1'].name")).isEqualTo("map[key1].name");
		assertThat(PropertyAccessorUtils.canonicalPropertyName("map[\"key1\"].name")).isEqualTo("map[key1].name");
	}

	@Test
	public void canonicalPropertyNames() {
		String[] original =
				new String[] {"map", "map[key1]", "map['key1']", "map[\"key1\"]", "map[key1][key2]",
											"map['key1'][\"key2\"]", "map[key1].name", "map['key1'].name", "map[\"key1\"].name"};
		String[] canonical =
				new String[] {"map", "map[key1]", "map[key1]", "map[key1]", "map[key1][key2]",
											"map[key1][key2]", "map[key1].name", "map[key1].name", "map[key1].name"};

		assertThat(PropertyAccessorUtils.canonicalPropertyNames(original)).isEqualTo(canonical);
		assertThat(PropertyAccessorUtils.canonicalPropertyNames(null)).isNull();
	}

}
