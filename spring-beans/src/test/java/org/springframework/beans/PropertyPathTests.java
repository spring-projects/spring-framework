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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.PropertyPath.Segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link PropertyPath}.
 */
class PropertyPathTests {

	@Test
	void canonicalNameOfWellFormedPath() {
		assertThat(canonicalName("name")).isEqualTo("name");
		assertThat(canonicalName("person.name")).isEqualTo("person.name");
		assertThat(canonicalName("person.addresses[1].city")).isEqualTo("person.addresses[1].city");
		assertThat(canonicalName("map[key1]")).isEqualTo("map[key1]");
		assertThat(canonicalName("map['key1']")).isEqualTo("map[key1]");
		assertThat(canonicalName("map[\"key1\"]")).isEqualTo("map[key1]");
		assertThat(canonicalName("map['key1'].name")).isEqualTo("map[key1].name");
		assertThat(canonicalName("map[key1][key2]")).isEqualTo("map[key1][key2]");
		assertThat(canonicalName("map['key1'][\"key2\"]")).isEqualTo("map[key1][key2]");
		assertThat(canonicalName("map[key[0]]")).isEqualTo("map[key[0]]");
		assertThat(canonicalName("map['key[0]']")).isEqualTo("map[key[0]]");
		assertThat(canonicalName("map['key[0]'].name")).isEqualTo("map[key[0]].name");
		assertThat(canonicalName("users['admin[0]']")).isEqualTo("users[admin[0]]");
		assertThat(canonicalName("map[]")).isEqualTo("map[]");
		assertThat(canonicalName("map['']")).isEqualTo("map[]");
		assertThat(canonicalName("map[\"\"]")).isEqualTo("map[]");
		assertThat(canonicalName("map[my.key]")).isEqualTo("map[my.key]");
		assertThat(canonicalName("map[[a]]")).isEqualTo("map[[a]]");
		assertThat(canonicalName("[user]")).isEqualTo("[user]");
		assertThat(canonicalName("[user].name")).isEqualTo("[user].name");
	}

	@Test
	void canonicalNameKeepsQuotesWhenRequired() {
		assertThat(canonicalName("map['a]b']")).isEqualTo("map['a]b']");
		assertThat(canonicalName("map['a[b']")).isEqualTo("map['a[b']");
		assertThat(canonicalName("map[\"a'b\"]")).isEqualTo("map[\"a'b\"]");
		assertThat(canonicalName("map['a\"b']")).isEqualTo("map['a\"b']");
	}

	@Test
	void parseEmptyPath() {
		PropertyPath path = PropertyPath.parse("");

		assertThat(path.canonicalName()).isEmpty();
		assertThat(path.segments()).isEmpty();
	}

	@Test
	void parseSimplePath() {
		assertThat(PropertyPath.parse("name").segments())
				.containsExactly(new Segment("name", List.of()));
	}

	@Test
	void parseNestedPath() {
		assertThat(PropertyPath.parse("person.addresses[1].city").segments())
				.containsExactly(
						new Segment("person", List.of()),
						new Segment("addresses", List.of("1")),
						new Segment("city", List.of()));
	}

	@Test
	void parseMultipleIndexesInOneSegment() {
		assertThat(PropertyPath.parse("map[key1][key2]").segments())
				.containsExactly(new Segment("map", List.of("key1", "key2")));
	}

	@Test
	void parseRootLevelIndexedAccess() {
		assertThat(PropertyPath.parse("[user]").segments())
				.containsExactly(new Segment("", List.of("user")));
	}

	@Test
	void parseStripsQuotesFromKeys() {
		assertThat(PropertyPath.parse("map['key1'][\"key2\"]").segments())
				.containsExactly(new Segment("map", List.of("key1", "key2")));
	}

	@Test
	void parseKeepsDotsInsideKeysOpaque() {
		assertThat(PropertyPath.parse("map[my.key].name").segments())
				.containsExactly(
						new Segment("map", List.of("my.key")),
						new Segment("name", List.of()));
	}

	@Test
	void parseKeepsNestedBracketsInRawKey() {
		assertThat(PropertyPath.parse("map[key[0]]").segments())
				.containsExactly(new Segment("map", List.of("key[0]")));
	}

	@Test
	void parseKeepsUnbalancedBracketInQuotedKey() {
		assertThat(PropertyPath.parse("map['a]b']").segments())
				.containsExactly(new Segment("map", List.of("a]b")));
	}

	@Test
	void parseEmptyKey() {
		assertThat(PropertyPath.parse("map[]").segments())
				.containsExactly(new Segment("map", List.of("")));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "name", "person.name", "person.addresses[1].city", "map[key1]",
			"map['key1']", "map[\"key1\"]", "map[key1][key2]", "map[key[0]]", "map['key[0]']",
			"map[]", "map['']", "map[my.key]", "map[[a]]", "[user]", "[user].name",
			"map['a]b']", "map['a[b']", "map[\"a'b\"]", "map['a\"b']", "map['']['a]b']"})
	void canonicalNameIdempotent(String path) {
		PropertyPath parsed = PropertyPath.parse(path);

		PropertyPath reparsed = PropertyPath.parse(parsed.canonicalName());

		assertThat(reparsed.segments()).isEqualTo(parsed.segments());
		assertThat(reparsed.canonicalName()).isEqualTo(parsed.canonicalName());
		assertThat(reparsed).isEqualTo(parsed);
	}

	@ParameterizedTest
	@ValueSource(strings = {"name", "person.name", "person.addresses[1].city", "person.map[key1]",
			"person.map['key1']", "person.map[\"key1\"]", "map[key1].map[key2]", "map['key[0]']",
			"person.map['a]b']", "person.map['a[b']", "person.map[\"a'b\"]", "person.map['a\"b']", "person.map['']['a]b']"})
	void canonicalPathAndSegmentsAreEquivalent(String path) {
		PropertyPath parsed = PropertyPath.parse(path);
		assertThat(parsed.segments().stream()
				.map(Segment::toCanonicalName)
				.reduce((a, b) -> a + "." + b))
				.hasValue(parsed.canonicalName());
	}

	@Test
	void segmentsCannotBeModified() {
		PropertyPath path = PropertyPath.parse("map[key1]");

		assertThat(path.segments()).hasSize(1);
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> path.segments().clear());
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> path.segments().get(0).keys().clear());
	}

	@Test
	void equalsAndHashCodeUseCanonicalName() {
		assertThat(PropertyPath.parse("map['key1'].name"))
				.isEqualTo(PropertyPath.parse("map[key1].name"))
				.hasSameHashCodeAs(PropertyPath.parse("map[key1].name"));
		assertThat(PropertyPath.parse("map[key1]")).isNotEqualTo(PropertyPath.parse("map[key2]"));
	}

	@Test
	void toStringReturnsCanonicalName() {
		assertThat(PropertyPath.parse("map['key1'].name")).hasToString("map[key1].name");
	}

	@Test
	void parseRejectsNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> PropertyPath.parse(null));
	}

	@ParameterizedTest
	@ValueSource(strings = {"map[key1]other", "map[key1]other.name", "map[key1]IGNORED[key2]",
			"options[priority]X", "options[security]IGNORED[role]", "map[key1]]", "[user]x"})
	void parseRejectsTextAfterIndex(String path) {
		assertThatExceptionOfType(InvalidPropertyPathException.class)
				.isThrownBy(() -> PropertyPath.parse(path))
				.withMessageContaining("after an index")
				.satisfies(ex -> assertThat(ex.getPropertyPath()).isEqualTo(path));
	}

	@ParameterizedTest
	@ValueSource(strings = {"map[key1", "map[", "map[[a]", "map[a][", "map['a'"})
	void parseRejectsUnclosedIndex(String path) {
		assertThatExceptionOfType(InvalidPropertyPathException.class)
				.isThrownBy(() -> PropertyPath.parse(path))
				.withMessageContaining("unclosed '['");
	}

	@ParameterizedTest
	@ValueSource(strings = {"map]", "map]name", "publication.].published", "address.].city", "]"})
	void parseRejectsUnmatchedIndexSuffix(String path) {
		assertThatExceptionOfType(InvalidPropertyPathException.class)
				.isThrownBy(() -> PropertyPath.parse(path))
				.withMessageContaining("without a matching '['");
	}

	@ParameterizedTest
	@ValueSource(strings = {".name", "person.", "person..name", ".", "..", "map[key1]."})
	void parseRejectsEmptySegment(String path) {
		assertThatExceptionOfType(InvalidPropertyPathException.class)
				.isThrownBy(() -> PropertyPath.parse(path))
				.withMessageContaining("empty path segment");
	}

	@ParameterizedTest
	@ValueSource(strings = {"map[']", "map[\"]", "map['key1]", "map[\"key1]", "map['a"})
	void parseRejectsUnterminatedQuote(String path) {
		assertThatExceptionOfType(InvalidPropertyPathException.class)
				.isThrownBy(() -> PropertyPath.parse(path))
				.withMessageContaining("unterminated quote");
	}

	@ParameterizedTest
	@ValueSource(strings = {"map[don't]", "map[a'b]", "map[a\"b]"})
	void parseRejectsQuoteInRawKey(String path) {
		assertThatExceptionOfType(InvalidPropertyPathException.class)
				.isThrownBy(() -> PropertyPath.parse(path))
				.withMessageContaining("in an unquoted key");
	}

	@ParameterizedTest
	@ValueSource(strings = {"map['a'b']", "map['a'b]", "map[\"a\"b\"]"})
	void parseRejectsTextAfterClosingQuote(String path) {
		assertThatExceptionOfType(InvalidPropertyPathException.class)
				.isThrownBy(() -> PropertyPath.parse(path))
				.withMessageContaining("after a closing quote");
	}

	@Test
	void invalidPropertyPathExceptionIsAPropertyAccessException() {
		assertThatExceptionOfType(InvalidPropertyPathException.class)
				.isThrownBy(() -> PropertyPath.parse("map[key1]other"))
				.isInstanceOf(PropertyAccessException.class)
				.withMessageContaining("Invalid property path 'map[key1]other'")
				.satisfies(ex -> {
					assertThat(ex.getErrorCode()).isEqualTo(InvalidPropertyPathException.ERROR_CODE);
					assertThat(ex.getPropertyPath()).isEqualTo("map[key1]other");
				});
	}

	@Test
	void failsWhenExceedsNestingDepth() {
		assertThatExceptionOfType(InvalidPropertyPathException.class)
				.isThrownBy(() ->
						PropertyPath.parse("one.two.three[1].four", PropertyPath.Options.withMaxNestedPathDepth(2)))
				.withMessageContaining("nesting depth exceeds the maximum of 2");
	}

	@Test
	void subPathFromZeroReturnsSamePath() {
		PropertyPath path = PropertyPath.parse("address.country.name");
		assertThat(path.subPath(0)).isSameAs(path);
	}

	@Test
	void subPathDropsLeadingSegments() {
		PropertyPath subPath = PropertyPath.parse("address.country[0].name").subPath(1);
		assertThat(subPath.canonicalName()).isEqualTo("country[0].name");
		assertThat(subPath.segments()).containsExactly(
				new Segment("country", List.of("0")),
				new Segment("name", List.of()));
	}

	@Test
	void subPathAtLastIndexIsEmpty() {
		PropertyPath subPath = PropertyPath.parse("address.name").subPath(2);
		assertThat(subPath.canonicalName()).isEmpty();
		assertThat(subPath.segments()).isEmpty();
	}

	@Test
	void subPathRejectsOutOfBoundsIndex() {
		PropertyPath path = PropertyPath.parse("address.name");
		assertThatIllegalArgumentException().isThrownBy(() -> path.subPath(3));
	}


	private static String canonicalName(String path) {
		return PropertyPath.parse(path).canonicalName();
	}

}
