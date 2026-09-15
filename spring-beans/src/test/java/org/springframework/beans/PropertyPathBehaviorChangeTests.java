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

package org.springframework.beans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests that check the behavior changes for a code migration
 * from {@link PropertyAccessorUtils} and {@link AbstractNestablePropertyAccessor}
 * to {@link PropertyPath}.
 * <p>This test class should be removed after migration.
 *
 * @author Brian Clozel
 */
class PropertyPathBehaviorChangeTests {

	/**
	 * The canonical name is the policy-facing form of a path: it is what
	 * {@code DataBinder.isAllowed} matches against {@code allowedFields} and
	 * {@code disallowedFields}, and what {@code AbstractPropertyBindingResult}
	 * reports as the field name.
	 */
	@Nested
	class CanonicalPropertyName {

		@Test
		void wellFormedPathsAreCanonicalized() {
			assertCanonical("", "");
			assertCanonical("name", "name");
			assertCanonical("person.name", "person.name");
			assertCanonical("map[key1]", "map[key1]");
			assertCanonical("map['key1']", "map[key1]");
			assertCanonical("map[\"key1\"]", "map[key1]");
			assertCanonical("map[key1][key2]", "map[key1][key2]");
			assertCanonical("map['key1'].name", "map[key1].name");
			assertCanonical("map[key[0]]", "map[key[0]]");
			assertCanonical("map['key[0]']", "map[key[0]]");
			assertCanonical("map[]", "map[]");
			assertCanonical("map['']", "map[]");
			assertCanonical("[user]", "[user]");
		}

		@Test  // Malformed paths are passed through unchanged, not rejected.
		void malformedPathsArePassedThrough() {
			assertCanonical("map[key1]other", "map[key1]other");
			assertCanonical("map[key1]other.name", "map[key1]other.name");
			assertCanonical("map[key1]IGNORED[key2]", "map[key1]IGNORED[key2]");
			assertCanonical(".name", ".name");
			assertCanonical("person.", "person.");
			assertCanonical("person..name", "person..name");
			assertCanonical("map[key1", "map[key1");
			assertCanonical("map]", "map]");
			assertCanonical("address.].city", "address.].city");
		}

		@Test  // An unterminated quote falls back to treating the quote as literal content.
		void unterminatedQuotesAreTreatedAsLiteralContent() {
			assertCanonical("map['key1]", "map['key1]");
			assertCanonical("map[\"key1]", "map[\"key1]");
			assertCanonical("map[']", "map[']");
			assertCanonical("map[\"]", "map[\"]");
		}

		@Test  // Quotes are stripped whenever the key merely starts and ends with one.
		void outerQuotesAreStrippedWithoutRegardToNesting() {
			assertCanonical("map['a'b']", "map[a'b]");
			assertCanonical("map[a'b]", "map[a'b]");
			assertCanonical("map['a]b']", "map['a]b']");
		}

		private void assertCanonical(String path, String expected) {
			assertThat(PropertyAccessorUtils.canonicalPropertyName(path))
					.as("canonicalPropertyName(\"%s\")", path)
					.isEqualTo(expected);
		}
	}


	/**
	 * {@code getPropertyName} only strips keys when the path happens to end
	 * with {@code ]}, which makes it inconsistent between otherwise similar
	 * malformed paths.
	 */
	@Nested
	class GetPropertyName {

		@Test
		void keysAreStrippedOnlyWhenThePathEndsWithAKey() {
			assertThat(PropertyAccessorUtils.getPropertyName("map[key1]")).isEqualTo("map");
			assertThat(PropertyAccessorUtils.getPropertyName("map[key1][key2]")).isEqualTo("map");
			assertThat(PropertyAccessorUtils.getPropertyName("[user]")).isEmpty();

			// Not stripped: the path does not end with ']'.
			assertThat(PropertyAccessorUtils.getPropertyName("map[key1].name")).isEqualTo("map[key1].name");
			assertThat(PropertyAccessorUtils.getPropertyName("map[key1]other")).isEqualTo("map[key1]other");

			// Stripped, even though the path is malformed, because it does end with ']'.
			assertThat(PropertyAccessorUtils.getPropertyName("map[key1]IGNORED[key2]")).isEqualTo("map");
		}
	}


	/**
	 * The access-facing resolution: which property the accessor actually reads
	 * or writes for a given path.
	 */
	@Nested
	class AccessorResolution {

		@Test
		void wellFormedPathsResolveAsExpected() {
			assertThat(bind("name")).containsEntry("name", "V");
			assertThat(bind("nested.name")).containsEntry("nested.name", "V");
			assertThat(bind("map[key1]")).containsEntry("map", "{key1=V}");
			assertThat(bind("map['key1']")).containsEntry("map", "{key1=V}");
			assertThat(bind("map[\"key1\"]")).containsEntry("map", "{key1=V}");
			assertThat(bind("map[key[0]]")).containsEntry("map", "{key[0]=V}");
			assertThat(bind("map['key[0]']")).containsEntry("map", "{key[0]=V}");
			assertThat(bind("map[]")).containsEntry("map", "{=V}");
			assertThat(bind("map['']")).containsEntry("map", "{=V}");
			assertThat(bind("list[0]")).containsEntry("list", "[V]");
			assertThat(bind("nestedMap[a][b]")).containsEntry("nestedMap", "{a={b=V}}");
		}

		@ParameterizedTest  // gh-36999
		@ValueSource(strings = {"map[key1", "map]", "nested.].name", "nested.[.name",
				"nested.[[.name", "nested.]].name", "nested.][.name"})
		void unbalancedBracketsAreRejectedOnWrite(String path) {
			assertThatExceptionOfType(NotWritablePropertyException.class)
					.isThrownBy(() -> bind(path))
					.withMessageContaining("Nested property in path '" + path + "' does not exist");
		}

		@ParameterizedTest  // gh-36999
		@ValueSource(strings = {"map[key1", "map]", "nested.].name", "nested.[.name"})
		void unbalancedBracketsAreRejectedOnRead(String path) {
			BeanWrapperImpl accessor = new BeanWrapperImpl(new Target());

			assertThatExceptionOfType(NotReadablePropertyException.class)
					.isThrownBy(() -> accessor.getPropertyValue(path))
					.withMessageEndingWith("contains unbalanced brackets");
		}

		@ParameterizedTest
		@ValueSource(strings = {".name", "person.", "nested..name"})
		void emptySegmentsAreRejectedOnWrite(String path) {
			assertThatExceptionOfType(NotWritablePropertyException.class)
					.isThrownBy(() -> bind(path));
		}

		/**
		 * The behavior that matters most for {@code DataBinder}: because
		 * {@code ignoreUnknownFields} defaults to {@code true} and
		 * {@code AbstractPropertyAccessor.setPropertyValues} swallows
		 * {@code NotWritablePropertyException} in that mode, a malformed path
		 * is currently dropped without any error being recorded.
		 */
		@ParameterizedTest
		@ValueSource(strings = {"map[key1", "map]", "nested.].name", ".name", "person."})
		void malformedPathsAreSilentlyDroppedWhenIgnoringUnknownFields(String path) {
			Target target = new Target();
			BeanWrapperImpl accessor = new BeanWrapperImpl(target);
			MutablePropertyValues pvs = new MutablePropertyValues(Map.of(path, "V"));

			assertThatNoException().isThrownBy(() -> accessor.setPropertyValues(pvs, true, true));

			assertThat(target.getMap()).isEmpty();
			assertThat(target.getName()).isEmpty();
		}
	}


	/**
	 * The rows where {@link PropertyPath} knowingly differs from the current
	 * implementation. Each test asserts the old behavior and the new behavior
	 * together, so that the diff is explicit rather than discovered later.
	 */
	@Nested
	class IntentionalDivergences {

		/**
		 * The primary target of the refactor: trailing text after an index is
		 * dropped by the accessor but kept by {@code canonicalPropertyName}.
		 */
		@Test
		void trailingTextAfterAnIndexIsDroppedByTheAccessorButKeptInTheCanonicalName() {
			assertThat(PropertyAccessorUtils.canonicalPropertyName("map[key1]other"))
					.isEqualTo("map[key1]other");
			assertThat(bind("map[key1]other")).containsEntry("map", "{key1=V}");

			assertThatExceptionOfType(InvalidPropertyPathException.class)
					.isThrownBy(() -> PropertyPath.parse("map[key1]other"));
		}

		@Test
		void interleavedTextBetweenIndexesIsDroppedByTheAccessor() {
			assertThat(PropertyAccessorUtils.canonicalPropertyName("nestedMap[a]X[b]"))
					.isEqualTo("nestedMap[a]X[b]");
			assertThat(bind("nestedMap[a]X[b]")).containsEntry("nestedMap", "{a={b=V}}");

			assertThatExceptionOfType(InvalidPropertyPathException.class)
					.isThrownBy(() -> PropertyPath.parse("nestedMap[a]X[b]"));
		}

		@Test
		void trailingTextBeforeANestedSeparatorIsDroppedByTheAccessor() {
			assertThatExceptionOfType(NotWritablePropertyException.class)
					.isThrownBy(() -> bind("map[key1]other.name"))
					// The accessor resolved 'map[key1].name', dropping 'other'.
					.withMessageContaining("map[key1].name");

			assertThatExceptionOfType(InvalidPropertyPathException.class)
					.isThrownBy(() -> PropertyPath.parse("map[key1]other.name"));
		}

		/**
		 * A key consisting of nothing but a quote character is
		 * <em>deliberately supported</em> today: commit {@code d3152c11c7}
		 * ("Consistently expose map key quotes", gh-36765) added
		 * {@code map.put("'", …)} and {@code map.put("\"", …)} to the shared
		 * {@code IndexedTestBean} fixture and asserted
		 * {@code getPropertyValue("map['].name")} in
		 * {@link AbstractPropertyAccessorTests}. The lenient fallback it
		 * relies on goes back further, to SPR-14293 ({@code cf0a0cd5d8}),
		 * where treating an unterminated quote as literal content was the
		 * chosen remedy for a {@code StringIndexOutOfBoundsException}.
		 * <p>The strict quote grammar knowingly reverts that: an opened quote
		 * must be closed. This is the one intentional divergence that removes
		 * a documented capability rather than an accident, so it needs
		 * explicit sign-off from the Beans/Core owners before the migration
		 * of {@link PropertyAccessorUtils} lands.
		 * <p>Auditing every property path literal in the accessor test suites
		 * found exactly three affected by the strict grammar, all from this
		 * lineage: {@code map['].name}, {@code map["].name} and {@code [']}.
		 */
		@ParameterizedTest  // gh-36765
		@ValueSource(strings = {"map[']", "map[\"]"})
		void deliberatelySupportedQuoteOnlyKeyNoLongerBinds(String path) {
			assertThatNoException().isThrownBy(() -> bind(path));

			assertThatExceptionOfType(InvalidPropertyPathException.class)
					.isThrownBy(() -> PropertyPath.parse(path))
					.withMessageContaining("unterminated quote");
		}

		/**
		 * These shapes also bind today, but unlike
		 * {@link #deliberatelySupportedQuoteOnlyKeyNoLongerBinds} they are
		 * asserted nowhere in the test suite: they are incidental consequences
		 * of the same SPR-14293 leniency rather than intended behavior. Under
		 * the strict grammar a raw key may not contain quote characters at all,
		 * and a closing quote must be followed immediately by {@code ]}.
		 */
		@ParameterizedTest
		@ValueSource(strings = {"map[don't]", "map[a'b]", "map['key1]", "map[\"key1]", "map['a'b']"})
		void incidentallyAcceptedQuoteCharactersInKeysNoLongerBind(String path) {
			assertThatNoException().isThrownBy(() -> bind(path));

			assertThatExceptionOfType(InvalidPropertyPathException.class)
					.isThrownBy(() -> PropertyPath.parse(path));
		}

		/**
		 * Today's canonical name is not a fixed point for nested quotes, so
		 * canonicalizing twice yields a third, different key. Nothing exploits
		 * this because {@code DataBinder} canonicalizes patterns once and
		 * fields once, but it is the fragility that the strict grammar removes:
		 * these inputs no longer parse, and for everything that does parse
		 * {@code PropertyPath.canonicalName()} is idempotent.
		 * @see PropertyPathTests#canonicalNameIdempotent(String) ()
		 */
		@Test
		void canonicalNameIsNotIdempotentForNestedQuotes() {
			assertThat(PropertyAccessorUtils.canonicalPropertyName("map[''a'']")).isEqualTo("map['a']");
			assertThat(PropertyAccessorUtils.canonicalPropertyName("map['a']")).isEqualTo("map[a]");

			assertThatExceptionOfType(InvalidPropertyPathException.class)
					.isThrownBy(() -> PropertyPath.parse("map[''a'']"));
		}

		/**
		 * Conversely, a quoted key is now opaque, so a key containing an
		 * unbalanced {@code ]} becomes legal where it is rejected today.
		 */
		@Test
		void quotedKeyWithUnbalancedBracketBecomesLegal() {
			assertThatExceptionOfType(NotWritablePropertyException.class)
					.isThrownBy(() -> bind("map['a]b']"));

			assertThat(PropertyPath.parse("map['a]b']").segments())
					.containsExactly(new PropertyPath.Segment("map", List.of("a]b")));
		}
	}


	/**
	 * Bind {@code "V"} to the given path and return a description of the
	 * resulting target state, keyed by property name.
	 */
	private static Map<String, String> bind(String path) {
		Target target = new Target();
		BeanWrapperImpl accessor = new BeanWrapperImpl(target);
		accessor.setAutoGrowNestedPaths(true);
		accessor.setPropertyValue(path, "V");
		return target.describe();
	}


	@SuppressWarnings("unused")
	public static class Target {

		private String name = "";

		private Target nested;

		private Map<String, String> map = new LinkedHashMap<>();

		private Map<String, Map<String, String>> nestedMap = new LinkedHashMap<>();

		private List<String> list = new ArrayList<>();

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Target getNested() {
			return this.nested;
		}

		public void setNested(Target nested) {
			this.nested = nested;
		}

		public Map<String, String> getMap() {
			return this.map;
		}

		public void setMap(Map<String, String> map) {
			this.map = map;
		}

		public Map<String, Map<String, String>> getNestedMap() {
			return this.nestedMap;
		}

		public void setNestedMap(Map<String, Map<String, String>> nestedMap) {
			this.nestedMap = nestedMap;
		}

		public List<String> getList() {
			return this.list;
		}

		public void setList(List<String> list) {
			this.list = list;
		}

		Map<String, String> describe() {
			Map<String, String> description = new HashMap<>();
			if (!this.name.isEmpty()) {
				description.put("name", this.name);
			}
			if (!this.map.isEmpty()) {
				description.put("map", this.map.toString());
			}
			if (!this.nestedMap.isEmpty()) {
				description.put("nestedMap", this.nestedMap.toString());
			}
			if (!this.list.isEmpty()) {
				description.put("list", this.list.toString());
			}
			if (this.nested != null) {
				this.nested.describe().forEach((key, value) -> description.put("nested." + key, value));
			}
			return description;
		}
	}

}
