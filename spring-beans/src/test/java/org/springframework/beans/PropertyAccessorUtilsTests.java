/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertyAccessorUtils}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 */
class PropertyAccessorUtilsTests {

	@Test
	void getPropertyName() {
		assertThat(PropertyAccessorUtils.getPropertyName("")).isEmpty();
		assertThat(PropertyAccessorUtils.getPropertyName("[user]")).isEmpty();
		assertThat(PropertyAccessorUtils.getPropertyName("user")).isEqualTo("user");
	}

	@Test
	void isNestedOrIndexedProperty() {
		assertThat(PropertyAccessorUtils.isNestedOrIndexedProperty(null)).isFalse();
		assertThat(PropertyAccessorUtils.isNestedOrIndexedProperty("")).isFalse();
		assertThat(PropertyAccessorUtils.isNestedOrIndexedProperty("user")).isFalse();

		assertThat(PropertyAccessorUtils.isNestedOrIndexedProperty("[user]")).isTrue();
		assertThat(PropertyAccessorUtils.isNestedOrIndexedProperty("user.name")).isTrue();
	}

	@Test
	void getFirstNestedPropertySeparatorIndex() {
		assertThat(PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex("[user]")).isEqualTo(-1);
		assertThat(PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex("user.name")).isEqualTo(4);
	}

	@Test
	void getLastNestedPropertySeparatorIndex() {
		assertThat(PropertyAccessorUtils.getLastNestedPropertySeparatorIndex("[user]")).isEqualTo(-1);
		assertThat(PropertyAccessorUtils.getLastNestedPropertySeparatorIndex("user.address.street")).isEqualTo(12);
	}

	@Test
	void matchesProperty() {
		assertThat(PropertyAccessorUtils.matchesProperty("user", "email")).isFalse();
		assertThat(PropertyAccessorUtils.matchesProperty("username", "user")).isFalse();
		assertThat(PropertyAccessorUtils.matchesProperty("admin[user]", "user")).isFalse();

		assertThat(PropertyAccessorUtils.matchesProperty("user", "user")).isTrue();
		assertThat(PropertyAccessorUtils.matchesProperty("user[name]", "user")).isTrue();
	}

	@Test
	void canonicalPropertyName() {
		assertThat(PropertyAccessorUtils.canonicalPropertyName(null)).isEmpty();
		assertThat(PropertyAccessorUtils.canonicalPropertyName("map")).isEqualTo("map");
		assertThat(PropertyAccessorUtils.canonicalPropertyName("map[key1]")).isEqualTo("map[key1]");
		assertThat(PropertyAccessorUtils.canonicalPropertyName("map['key1']")).isEqualTo("map[key1]");
		assertThat(PropertyAccessorUtils.canonicalPropertyName("map[\"key1\"]")).isEqualTo("map[key1]");
		assertThat(PropertyAccessorUtils.canonicalPropertyName("map[key1][key2]")).isEqualTo("map[key1][key2]");
		assertThat(PropertyAccessorUtils.canonicalPropertyName("map['key1'][\"key2\"]")).isEqualTo("map[key1][key2]");
		assertThat(PropertyAccessorUtils.canonicalPropertyName("map[key1].name")).isEqualTo("map[key1].name");
		assertThat(PropertyAccessorUtils.canonicalPropertyName("map['key1'].name")).isEqualTo("map[key1].name");
		assertThat(PropertyAccessorUtils.canonicalPropertyName("map[\"key1\"].name")).isEqualTo("map[key1].name");
		assertThat(PropertyAccessorUtils.canonicalPropertyName("map['key1]")).isEqualTo("map['key1]");
		assertThat(PropertyAccessorUtils.canonicalPropertyName("map[\"key1]")).isEqualTo("map[\"key1]");
		assertThat(PropertyAccessorUtils.canonicalPropertyName("map[']")).isEqualTo("map[']");
		assertThat(PropertyAccessorUtils.canonicalPropertyName("map[\"]")).isEqualTo("map[\"]");
	}

	@Test  // gh-36999
	void hasUnbalancedBrackets() {
		assertThat(PropertyAccessorUtils.hasUnbalancedBrackets("property")).isFalse();
		assertThat(PropertyAccessorUtils.hasUnbalancedBrackets("array[]")).isFalse();
		assertThat(PropertyAccessorUtils.hasUnbalancedBrackets("array[0]")).isFalse();
		assertThat(PropertyAccessorUtils.hasUnbalancedBrackets("map[key1]")).isFalse();
		assertThat(PropertyAccessorUtils.hasUnbalancedBrackets("map[key1][key2]")).isFalse();
		assertThat(PropertyAccessorUtils.hasUnbalancedBrackets("map[key1].name")).isFalse();
		assertThat(PropertyAccessorUtils.hasUnbalancedBrackets("map['key[0]'].name")).isFalse();

		// A lone '[' or ']' anywhere in the path is unbalanced.
		assertThat(PropertyAccessorUtils.hasUnbalancedBrackets("map.[.name")).isTrue();
		assertThat(PropertyAccessorUtils.hasUnbalancedBrackets("map.].name")).isTrue();
		assertThat(PropertyAccessorUtils.hasUnbalancedBrackets("map.[X.name")).isTrue();
		assertThat(PropertyAccessorUtils.hasUnbalancedBrackets("map.X[.name")).isTrue();
		assertThat(PropertyAccessorUtils.hasUnbalancedBrackets("[map")).isTrue();
		assertThat(PropertyAccessorUtils.hasUnbalancedBrackets("map]")).isTrue();

		// Multiple stray brackets are unbalanced.
		assertThat(PropertyAccessorUtils.hasUnbalancedBrackets("map.[[.name")).isTrue();
		assertThat(PropertyAccessorUtils.hasUnbalancedBrackets("map.]].name")).isTrue();

		// A ']' before any preceding '[' is unbalanced even if a '[' follows later.
		assertThat(PropertyAccessorUtils.hasUnbalancedBrackets("map.][.name")).isTrue();
	}

	@Test
	void canonicalPropertyNames() {
		assertThat(PropertyAccessorUtils.canonicalPropertyNames(null)).isNull();

		String[] original =
				new String[] {"map", "map[key1]", "map['key1']", "map[\"key1\"]", "map[key1][key2]",
											"map['key1'][\"key2\"]", "map[key1].name", "map['key1'].name", "map[\"key1\"].name"};
		String[] canonical =
				new String[] {"map", "map[key1]", "map[key1]", "map[key1]", "map[key1][key2]",
											"map[key1][key2]", "map[key1].name", "map[key1].name", "map[key1].name"};

		assertThat(PropertyAccessorUtils.canonicalPropertyNames(original)).isEqualTo(canonical);
	}

}
