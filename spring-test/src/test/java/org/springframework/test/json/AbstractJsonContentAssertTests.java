/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.json;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.AssertProvider;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.JSONComparator;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.test.http.HttpMessageContentConverter;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link AbstractJsonContentAssert}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class AbstractJsonContentAssertTests {

	private static final String TYPES = loadJson("types.json");

	private static final String SIMPSONS = loadJson("simpsons.json");

	private static final String NULLS = loadJson("nulls.json");

	private static final String SOURCE = loadJson("source.json");

	private static final String LENIENT_SAME = loadJson("lenient-same.json");

	private static final String DIFFERENT = loadJson("different.json");

	private static final HttpMessageContentConverter jsonContentConverter = HttpMessageContentConverter.of(
			new MappingJackson2HttpMessageConverter(new ObjectMapper()));

	private static final JsonComparator comparator = JsonAssert.comparator(JsonCompareMode.LENIENT);

	@Test
	void isNullWhenActualIsNullShouldPass() {
		assertThat(forJson(null)).isNull();
	}

	@Test
	void satisfiesAllowFurtherAssertions() {
		assertThat(forJson(SIMPSONS)).satisfies(content -> {
			assertThat(content).extractingPath("$.familyMembers[0].name").isEqualTo("Homer");
			assertThat(content).extractingPath("$.familyMembers[1].name").isEqualTo("Marge");
		});
	}

	@Nested
	class ConversionTests {

		@Test
		void convertToTargetType() {
			assertThat(forJson(SIMPSONS, jsonContentConverter))
					.convertTo(Family.class)
					.satisfies(family -> assertThat(family.familyMembers()).hasSize(5));
		}

		@Test
		void convertToIncompatibleTargetTypeShouldFail() {
			AbstractJsonContentAssert<?> jsonAssert = assertThat(forJson(SIMPSONS, jsonContentConverter));
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> jsonAssert.convertTo(Member.class))
					.withMessageContainingAll("To convert successfully to:",
							Member.class.getName(), "But it failed:");
		}

		@Test
		void convertUsingAssertFactory() {
			assertThat(forJson(SIMPSONS, jsonContentConverter))
					.convertTo(new FamilyAssertFactory())
					.hasFamilyMember("Homer");
		}

		private AssertProvider<AbstractJsonContentAssert<?>> forJson(@Nullable String json,
				@Nullable HttpMessageContentConverter jsonContentConverter) {

			return () -> new TestJsonContentAssert(json, jsonContentConverter);
		}

		private static class FamilyAssertFactory extends InstanceOfAssertFactory<Family, FamilyAssert> {
			public FamilyAssertFactory() {
				super(Family.class, FamilyAssert::new);
			}
		}

		private static class FamilyAssert extends AbstractObjectAssert<FamilyAssert, Family> {
			public FamilyAssert(Family family) {
				super(family, FamilyAssert.class);
			}

			public FamilyAssert hasFamilyMember(String name) {
				assertThat(this.actual.familyMembers).anySatisfy(m -> assertThat(m.name()).isEqualTo(name));
				return this.myself;
			}
		}
	}

	@Nested
	class HasPathTests {

		@Test
		void hasPathForNullJson() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(null)).hasPath("no"))
					.withMessageContaining("Expecting actual not to be null");
		}

		@Test
		void hasPathForPresentAndNotNull() {
			assertThat(forJson(NULLS)).hasPath("$.valuename");
		}

		@Test
		void hasPathForPresentAndNull() {
			assertThat(forJson(NULLS)).hasPath("$.nullname");
		}

		@Test
		void hasPathForOperatorMatching() {
			assertThat(forJson(SIMPSONS)).
					hasPath("$.familyMembers[?(@.name == 'Homer')]");
		}

		@Test
		void hasPathForOperatorNotMatching() {
			assertThat(forJson(SIMPSONS)).
					hasPath("$.familyMembers[?(@.name == 'Dilbert')]");
		}

		@Test
		void hasPathForNotPresent() {
			String expression = "$.missing";
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(NULLS)).hasPath(expression))
					.satisfies(hasFailedToMatchPath("$.missing"));
		}

		@Test
		void hasPathSatisfying() {
			assertThat(forJson(TYPES)).hasPathSatisfying("$.str", value -> assertThat(value).isEqualTo("foo"))
					.hasPathSatisfying("$.num", value -> assertThat(value).isEqualTo(5));
		}

		@Test
		void hasPathSatisfyingForPathNotPresent() {
			String expression = "missing";
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(NULLS)).hasPathSatisfying(expression, value -> {}))
					.satisfies(hasFailedToMatchPath(expression));
		}

		@Test
		void doesNotHavePathForMissing() {
			assertThat(forJson(NULLS)).doesNotHavePath("$.missing");
		}

		@Test
		void doesNotHavePathForPresent() {
			String expression = "$.valuename";
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(NULLS)).doesNotHavePath(expression))
					.satisfies(hasFailedToNotMatchPath(expression));
		}
	}

	@Nested
	class ExtractingPathTests {

		@Test
		void extractingPathForNullJson() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(null)).extractingPath("$"))
					.withMessageContaining("Expecting actual not to be null");
		}

		@Test
		void isNullWithNullPathValue() {
			assertThat(forJson(NULLS)).extractingPath("$.nullname").isNull();
		}

		@ParameterizedTest
		@ValueSource(strings = { "$.str", "$.emptyString", "$.num", "$.bool", "$.arr",
				"$.emptyArray", "$.colorMap", "$.emptyMap" })
		void isNotNullWithValue(String path) {
			assertThat(forJson(TYPES)).extractingPath(path).isNotNull();
		}

		@ParameterizedTest
		@MethodSource
		void isEqualToOnRawValue(String path, Object expected) {
			assertThat(forJson(TYPES)).extractingPath(path).isEqualTo(expected);
		}

		static Stream<Arguments> isEqualToOnRawValue() {
			return Stream.of(
					Arguments.of("$.str", "foo"),
					Arguments.of("$.num", 5),
					Arguments.of("$.bool", true),
					Arguments.of("$.arr", List.of(42)),
					Arguments.of("$.colorMap", Map.of("red", "rojo")));
		}

		@Test
		void asStringWithActualValue() {
			assertThat(forJson(TYPES)).extractingPath("@.str").asString().startsWith("f").endsWith("o");
		}

		@Test
		void asStringIsEmpty() {
			assertThat(forJson(TYPES)).extractingPath("@.emptyString").asString().isEmpty();
		}

		@Test
		void asNumberWithActualValue() {
			assertThat(forJson(TYPES)).extractingPath("@.num").asNumber().isEqualTo(5);
		}

		@Test
		void asBooleanWithActualValue() {
			assertThat(forJson(TYPES)).extractingPath("@.bool").asBoolean().isTrue();
		}

		@Test
		void asArrayWithActualValue() {
			assertThat(forJson(TYPES)).extractingPath("@.arr").asArray().containsOnly(42);
		}

		@Test
		void asArrayIsEmpty() {
			assertThat(forJson(TYPES)).extractingPath("@.emptyArray").asArray().isEmpty();
		}

		@Test
		void asArrayWithFilterPredicatesMatching() {
			assertThat(forJson(SIMPSONS))
					.extractingPath("$.familyMembers[?(@.name == 'Bart')]").asArray().hasSize(1);
		}

		@Test
		void asArrayWithFilterPredicatesNotMatching() {
			assertThat(forJson(SIMPSONS)).
					extractingPath("$.familyMembers[?(@.name == 'Dilbert')]").asArray().isEmpty();
		}

		@Test
		void asMapWithActualValue() {
			assertThat(forJson(TYPES)).extractingPath("@.colorMap").asMap().containsOnly(entry("red", "rojo"));
		}

		@Test
		void asMapIsEmpty() {
			assertThat(forJson(TYPES)).extractingPath("@.emptyMap").asMap().isEmpty();
		}

		@Test
		void convertToWithoutHttpMessageConverterShouldFail() {
			JsonPathValueAssert path = assertThat(forJson(SIMPSONS)).extractingPath("$.familyMembers[0]");
			assertThatIllegalStateException()
					.isThrownBy(() -> path.convertTo(Member.class))
					.withMessage("No JSON message converter available to convert {name=Homer}");
		}

		@Test
		void convertToTargetType() {
			assertThat(forJson(SIMPSONS, jsonContentConverter))
					.extractingPath("$.familyMembers[0]").convertTo(Member.class)
					.satisfies(member -> assertThat(member.name).isEqualTo("Homer"));
		}

		@Test
		void convertToIncompatibleTargetTypeShouldFail() {
			JsonPathValueAssert path = assertThat(forJson(SIMPSONS, jsonContentConverter))
					.extractingPath("$.familyMembers[0]");
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> path.convertTo(ExtractingPathTests.Customer.class))
					.withMessageContainingAll("Expected value at JSON path \"$.familyMembers[0]\":",
							Customer.class.getName(), "name");
		}

		@Test
		void convertArrayUsingAssertFactory() {
			assertThat(forJson(SIMPSONS, jsonContentConverter))
					.extractingPath("$.familyMembers")
					.convertTo(InstanceOfAssertFactories.list(Member.class))
					.hasSize(5).element(0).isEqualTo(new Member("Homer"));
		}

		@Test
		void isEmptyWithPathHavingNullValue() {
			assertThat(forJson(NULLS)).extractingPath("nullname").isEmpty();
		}

		@ParameterizedTest
		@ValueSource(strings = { "$.emptyString", "$.emptyArray", "$.emptyMap" })
		void isEmptyWithEmptyValue(String path) {
			assertThat(forJson(TYPES)).extractingPath(path).isEmpty();
		}

		@Test
		void isEmptyForPathWithFilterMatching() {
			String expression = "$.familyMembers[?(@.name == 'Bart')]";
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SIMPSONS)).extractingPath(expression).isEmpty())
					.withMessageContainingAll("Expected value at JSON path \"" + expression + "\"",
							"[{\"name\":\"Bart\"}]", "To be empty");
		}

		@Test
		void isEmptyForPathWithFilterNotMatching() {
			assertThat(forJson(SIMPSONS)).extractingPath("$.familyMembers[?(@.name == 'Dilbert')]").isEmpty();
		}

		@ParameterizedTest
		@ValueSource(strings = { "$.str", "$.num", "$.bool", "$.arr", "$.colorMap" })
		void isNotEmptyWithNonNullValue(String path) {
			assertThat(forJson(TYPES)).extractingPath(path).isNotEmpty();
		}

		@Test
		void isNotEmptyForPathWithFilterMatching() {
			assertThat(forJson(SIMPSONS)).extractingPath("$.familyMembers[?(@.name == 'Bart')]").isNotEmpty();
		}

		@Test
		void isNotEmptyForPathWithFilterNotMatching() {
			String expression = "$.familyMembers[?(@.name == 'Dilbert')]";
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SIMPSONS)).extractingPath(expression).isNotEmpty())
					.withMessageContainingAll("Expected value at JSON path \"" + expression + "\"",
							"To not be empty");
		}


		private record Customer(long id, String username) {}

		private AssertProvider<AbstractJsonContentAssert<?>> forJson(@Nullable String json) {
			return () -> new TestJsonContentAssert(json, null);
		}

		private AssertProvider<AbstractJsonContentAssert<?>> forJson(@Nullable String json, HttpMessageContentConverter jsonContentConverter) {
			return () -> new TestJsonContentAssert(json, jsonContentConverter);
		}
	}

	@Nested
	class EqualsNotEqualsTests {

		@Test
		void isEqualToWhenStringIsMatchingShouldPass() {
			assertThat(forJson(SOURCE)).isEqualTo(SOURCE);
		}

		@Test
		void isEqualToWhenNullActualShouldFail() {
			assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
					assertThat(forJson(null)).isEqualTo(SOURCE));
		}

		@Test
		void isEqualToWhenExpectedIsNotAStringShouldFail() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualTo(SOURCE.getBytes()));
		}
	}

	@Nested
	@TestInstance(Lifecycle.PER_CLASS)
	class JsonAssertTests {

		@Test
		void isEqualToWhenExpectedIsNullShouldFail() {
			CharSequence actual = null;
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualTo(actual, JsonCompareMode.LENIENT));
		}

		@Test
		void isEqualToWhenStringIsMatchingAndLenientShouldPass() {
			assertThat(forJson(SOURCE)).isEqualTo(LENIENT_SAME, JsonCompareMode.LENIENT);
		}

		@Test
		void isEqualToWhenStringIsNotMatchingAndLenientShouldFail() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualTo(DIFFERENT, JsonCompareMode.LENIENT));
		}

		@Test
		void isEqualToWhenResourcePathIsMatchingAndLenientShouldPass() {
			assertThat(forJson(SOURCE)).isEqualTo("lenient-same.json", JsonCompareMode.LENIENT);
		}

		@Test
		void isEqualToWhenResourcePathIsNotMatchingAndLenientShouldFail() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualTo("different.json", JsonCompareMode.LENIENT));
		}

		Stream<Arguments> source() {
			return Stream.of(
					Arguments.of(new ClassPathResource("source.json", AbstractJsonContentAssertTests.class)),
					Arguments.of(new ByteArrayResource(SOURCE.getBytes())),
					Arguments.of(new FileSystemResource(createFile(SOURCE))),
					Arguments.of(new InputStreamResource(createInputStream(SOURCE))));
		}

		Stream<Arguments> lenientSame() {
			return Stream.of(
					Arguments.of(new ClassPathResource("lenient-same.json", AbstractJsonContentAssertTests.class)),
					Arguments.of(new ByteArrayResource(LENIENT_SAME.getBytes())),
					Arguments.of(new FileSystemResource(createFile(LENIENT_SAME))),
					Arguments.of(new InputStreamResource(createInputStream(LENIENT_SAME))));
		}

		Stream<Arguments> different() {
			return Stream.of(
					Arguments.of(new ClassPathResource("different.json", AbstractJsonContentAssertTests.class)),
					Arguments.of(new ByteArrayResource(DIFFERENT.getBytes())),
					Arguments.of(new FileSystemResource(createFile(DIFFERENT))),
					Arguments.of(new InputStreamResource(createInputStream(DIFFERENT))));
		}

		@ParameterizedTest
		@MethodSource("lenientSame")
		void isEqualToWhenResourceIsMatchingAndLenientSameShouldPass(Resource expected) {
			assertThat(forJson(SOURCE)).isEqualTo(expected, JsonCompareMode.LENIENT);
		}

		@ParameterizedTest
		@MethodSource("different")
		void isEqualToWhenResourceIsNotMatchingAndLenientShouldFail(Resource expected) {
			assertThatExceptionOfType(AssertionError.class).isThrownBy(
					() -> assertThat(forJson(SOURCE)).isEqualTo(expected, JsonCompareMode.LENIENT));
		}

		@Test
		void isEqualToWhenStringIsMatchingAndComparatorShouldPass() {
			assertThat(forJson(SOURCE)).isEqualTo(LENIENT_SAME, comparator);
		}

		@Test
		void isEqualToWhenStringIsNotMatchingAndComparatorShouldFail() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualTo(DIFFERENT, comparator));
		}

		@Test
		void isEqualToWhenResourcePathIsMatchingAndComparatorShouldPass() {
			assertThat(forJson(SOURCE)).isEqualTo("lenient-same.json", comparator);
		}

		@Test
		void isEqualToWhenResourcePathIsNotMatchingAndComparatorShouldFail() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualTo("different.json", comparator));
		}

		@ParameterizedTest
		@MethodSource("lenientSame")
		void isEqualToWhenResourceIsMatchingAndComparatorShouldPass(Resource expected) {
			assertThat(forJson(SOURCE)).isEqualTo(expected, comparator);
		}

		@ParameterizedTest
		@MethodSource("different")
		void isEqualToWhenResourceIsNotMatchingAndComparatorShouldFail(Resource expected) {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualTo(expected, comparator));
		}

		@Test
		void isLenientlyEqualToWhenStringIsMatchingShouldPass() {
			assertThat(forJson(SOURCE)).isLenientlyEqualTo(LENIENT_SAME);
		}

		@Test
		void isLenientlyEqualToWhenNullActualShouldFail() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(null)).isLenientlyEqualTo(SOURCE));
		}

		@Test
		void isLenientlyEqualToWhenStringIsNotMatchingShouldFail() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SOURCE)).isLenientlyEqualTo(DIFFERENT));
		}

		@Test
		void isLenientlyEqualToWhenExpectedDoesNotExistShouldFail() {
			assertThatIllegalStateException()
					.isThrownBy(() -> assertThat(forJson(SOURCE)).isLenientlyEqualTo("does-not-exist.json"))
					.withMessage("Unable to load JSON from class path resource [org/springframework/test/json/does-not-exist.json]");
		}

		@Test
		void isLenientlyEqualToWhenResourcePathIsMatchingShouldPass() {
			assertThat(forJson(SOURCE)).isLenientlyEqualTo("lenient-same.json");
		}

		@Test
		void isLenientlyEqualToWhenResourcePathIsNotMatchingShouldFail() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SOURCE)).isLenientlyEqualTo("different.json"));
		}

		@ParameterizedTest
		@MethodSource("lenientSame")
		void isLenientlyEqualToWhenResourceIsMatchingShouldPass(Resource expected) {
			assertThat(forJson(SOURCE)).isLenientlyEqualTo(expected);
		}

		@ParameterizedTest
		@MethodSource("different")
		void isLenientlyEqualToWhenResourceIsNotMatchingShouldFail(Resource expected) {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SOURCE)).isLenientlyEqualTo(expected));
		}

		@Test
		void isStrictlyEqualToWhenStringIsMatchingShouldPass() {
			assertThat(forJson(SOURCE)).isStrictlyEqualTo(SOURCE);
		}

		@Test
		void isStrictlyEqualToWhenStringIsNotMatchingShouldFail() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SOURCE)).isStrictlyEqualTo(LENIENT_SAME));
		}

		@Test
		void isStrictlyEqualToWhenResourcePathIsMatchingShouldPass() {
			assertThat(forJson(SOURCE)).isStrictlyEqualTo("source.json");
		}

		@Test
		void isStrictlyEqualToWhenResourcePathIsNotMatchingShouldFail() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SOURCE)).isStrictlyEqualTo("lenient-same.json"));
		}

		@ParameterizedTest
		@MethodSource("source")
		void isStrictlyEqualToWhenResourceIsMatchingShouldPass(Resource expected) {
			assertThat(forJson(SOURCE)).isStrictlyEqualTo(expected);
		}

		@ParameterizedTest
		@MethodSource("lenientSame")
		void isStrictlyEqualToWhenResourceIsNotMatchingShouldFail(Resource expected) {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SOURCE)).isStrictlyEqualTo(expected));
		}

		@Test
		void isNotEqualToWhenStringIsMatchingShouldFail() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualTo(SOURCE));
		}

		@Test
		void isNotEqualToWhenNullActualShouldPass() {
			assertThat(forJson(null)).isNotEqualTo(SOURCE);
		}

		@Test
		void isNotEqualToWhenStringIsNotMatchingShouldPass() {
			assertThat(forJson(SOURCE)).isNotEqualTo(DIFFERENT);
		}

		@Test
		void isNotEqualToAsObjectWhenExpectedIsNotAStringShouldNotFail() {
			assertThat(forJson(SOURCE)).isNotEqualTo(SOURCE.getBytes());
		}

		@Test
		void isNotEqualToWhenStringIsMatchingAndLenientShouldFail() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualTo(LENIENT_SAME, JsonCompareMode.LENIENT));
		}

		@Test
		void isNotEqualToWhenStringIsNotMatchingAndLenientShouldPass() {
			assertThat(forJson(SOURCE)).isNotEqualTo(DIFFERENT, JsonCompareMode.LENIENT);
		}

		@Test
		void isNotEqualToWhenResourcePathIsMatchingAndLenientShouldFail() {
			assertThatExceptionOfType(AssertionError.class).isThrownBy(
					() -> assertThat(forJson(SOURCE)).isNotEqualTo("lenient-same.json", JsonCompareMode.LENIENT));
		}

		@Test
		void isNotEqualToWhenResourcePathIsNotMatchingAndLenientShouldPass() {
			assertThat(forJson(SOURCE)).isNotEqualTo("different.json", JsonCompareMode.LENIENT);
		}

		@ParameterizedTest
		@MethodSource("lenientSame")
		void isNotEqualToWhenResourceIsMatchingAndLenientShouldFail(Resource expected) {
			assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertThat(forJson(SOURCE))
					.isNotEqualTo(expected, JsonCompareMode.LENIENT));
		}

		@ParameterizedTest
		@MethodSource("different")
		void isNotEqualToWhenResourceIsNotMatchingAndLenientShouldPass(Resource expected) {
			assertThat(forJson(SOURCE)).isNotEqualTo(expected, JsonCompareMode.LENIENT);
		}

		@Test
		void isNotEqualToWhenStringIsMatchingAndComparatorShouldFail() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualTo(LENIENT_SAME, comparator));
		}

		@Test
		void isNotEqualToWhenStringIsNotMatchingAndComparatorShouldPass() {
			assertThat(forJson(SOURCE)).isNotEqualTo(DIFFERENT, comparator);
		}

		@Test
		void isNotEqualToWhenResourcePathIsMatchingAndComparatorShouldFail() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualTo("lenient-same.json", comparator));
		}

		@Test
		void isNotEqualToWhenResourcePathIsNotMatchingAndComparatorShouldPass() {
			assertThat(forJson(SOURCE)).isNotEqualTo("different.json", comparator);
		}

		@ParameterizedTest
		@MethodSource("lenientSame")
		void isNotEqualToWhenResourceIsMatchingAndComparatorShouldFail(Resource expected) {
			assertThatExceptionOfType(AssertionError.class).isThrownBy(
					() -> assertThat(forJson(SOURCE)).isNotEqualTo(expected, comparator));
		}

		@ParameterizedTest
		@MethodSource("different")
		void isNotEqualToWhenResourceIsNotMatchingAndComparatorShouldPass(Resource expected) {
			assertThat(forJson(SOURCE)).isNotEqualTo(expected, comparator);
		}

		@Test
		void isNotEqualToWhenResourceIsMatchingAndComparatorShouldFail() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualTo(createResource(LENIENT_SAME), comparator));
		}

		@Test
		void isNotEqualToWhenResourceIsNotMatchingAndComparatorShouldPass() {
			assertThat(forJson(SOURCE)).isNotEqualTo(createResource(DIFFERENT), comparator);
		}

		@Test
		void isNotLenientlyEqualToWhenNullActualShouldPass() {
			assertThat(forJson(null)).isNotLenientlyEqualTo(SOURCE);
		}

		@Test
		void isNotLenientlyEqualToWhenStringIsNotMatchingShouldPass() {
			assertThat(forJson(SOURCE)).isNotLenientlyEqualTo(DIFFERENT);
		}

		@Test
		void isNotLenientlyEqualToWhenResourcePathIsMatchingShouldFail() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotLenientlyEqualTo("lenient-same.json"));
		}

		@Test
		void isNotLenientlyEqualToWhenResourcePathIsNotMatchingShouldPass() {
			assertThat(forJson(SOURCE)).isNotLenientlyEqualTo("different.json");
		}

		@ParameterizedTest
		@MethodSource("lenientSame")
		void isNotLenientlyEqualToWhenResourceIsMatchingShouldFail(Resource expected) {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotLenientlyEqualTo(expected));
		}

		@ParameterizedTest
		@MethodSource("different")
		void isNotLenientlyEqualToWhenResourceIsNotMatchingShouldPass(Resource expected) {
			assertThat(forJson(SOURCE)).isNotLenientlyEqualTo(expected);
		}

		@Test
		void isNotStrictlyEqualToWhenStringIsMatchingShouldFail() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotStrictlyEqualTo(SOURCE));
		}

		@Test
		void isNotStrictlyEqualToWhenStringIsNotMatchingShouldPass() {
			assertThat(forJson(SOURCE)).isNotStrictlyEqualTo(LENIENT_SAME);
		}

		@Test
		void isNotStrictlyEqualToWhenResourcePathIsMatchingShouldFail() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotStrictlyEqualTo("source.json"));
		}

		@Test
		void isNotStrictlyEqualToWhenResourcePathIsNotMatchingShouldPass() {
			assertThat(forJson(SOURCE)).isNotStrictlyEqualTo("lenient-same.json");
		}

		@ParameterizedTest
		@MethodSource("source")
		void isNotStrictlyEqualToWhenResourceIsMatchingShouldFail(Resource expected) {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotStrictlyEqualTo(expected));
		}

		@ParameterizedTest
		@MethodSource("lenientSame")
		void isNotStrictlyEqualToWhenResourceIsNotMatchingShouldPass(Resource expected) {
			assertThat(forJson(SOURCE)).isNotStrictlyEqualTo(expected);
		}

		@Test
		void isEqualToWithCustomCompareMode() {
			String differentOrder = """
				{
					"spring": [
						"framework",
						"boot"
					]
				}
				""";
			assertThat(forJson(SOURCE)).isEqualTo(differentOrder, JsonAssert.comparator(JSONCompareMode.NON_EXTENSIBLE));
		}

		@Test
		void isEqualToWithCustomJsonComparator() throws JSONException {
			String empty = "{}";
			JSONComparator comparator = mock(JSONComparator.class);
			given(comparator.compareJSON(any(JSONObject.class), any(JSONObject.class))).willReturn(new JSONCompareResult());
			assertThat(forJson(SOURCE)).isEqualTo(empty, JsonAssert.comparator(comparator));
			verify(comparator).compareJSON(any(JSONObject.class), any(JSONObject.class));
		}

		@Test
		void withResourceLoadClassShouldAllowToLoadRelativeContent() {
			AbstractJsonContentAssert<?> jsonAssert = assertThat(forJson(NULLS)).withResourceLoadClass(String.class);
			assertThatIllegalStateException()
					.isThrownBy(() -> jsonAssert.isLenientlyEqualTo("nulls.json"))
					.withMessage("Unable to load JSON from class path resource [java/lang/nulls.json]");

			assertThat(forJson(NULLS)).withResourceLoadClass(JsonContent.class).isLenientlyEqualTo("nulls.json");
		}

		private AssertProvider<AbstractJsonContentAssert<?>> forJson(@Nullable String json) {
			return () -> new TestJsonContentAssert(json, null).withResourceLoadClass(getClass());
		}
	}

	@Nested
	class JsonComparatorTests {

		private final JsonComparator comparator = mock(JsonComparator.class);

		@Test
		void isEqualToInvokesComparator() {
			given(comparator.compare("{  }", "{}")).willReturn(JsonComparison.match());
			assertThat(forJson("{}")).isEqualTo("{  }", this.comparator);
			verify(comparator).compare("{  }", "{}");
		}

		@Test
		void isEqualToWithNoMatchProvidesErrorMessage() {
			given(comparator.compare("{  }", "{}")).willReturn(JsonComparison.mismatch("No additional whitespace expected"));
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(forJson("{}")).isEqualTo("{  }", this.comparator))
					.withMessageContaining("No additional whitespace expected");
			verify(comparator).compare("{  }", "{}");
		}

		private AssertProvider<AbstractJsonContentAssert<?>> forJson(@Nullable String json) {
			return () -> new TestJsonContentAssert(json, null).withResourceLoadClass(getClass());
		}
	}

	private Consumer<AssertionError> hasFailedToMatchPath(String expression) {
		return error -> assertThat(error.getMessage()).containsSubsequence("Expecting:",
				"To match JSON path:", "\"" + expression + "\"");
	}

	private Consumer<AssertionError> hasFailedToNotMatchPath(String expression) {
		return error -> assertThat(error.getMessage()).containsSubsequence("Expecting:",
				"Not to match JSON path:", "\"" + expression + "\"");
	}

	private Path createFile(String content) {
		try {
			Path temp = Files.createTempFile("file", ".json");
			Files.writeString(temp, content);
			return temp;
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private InputStream createInputStream(String content) {
		return new ByteArrayInputStream(content.getBytes());
	}

	private Resource createResource(String content) {
		return new ByteArrayResource(content.getBytes());
	}

	private static String loadJson(String path) {
		try {
			ClassPathResource resource = new ClassPathResource(path, AbstractJsonContentAssertTests.class);
			return new String(FileCopyUtils.copyToByteArray(resource.getInputStream()));
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}

	}

	private AssertProvider<AbstractJsonContentAssert<?>> forJson(@Nullable String json) {
		return () -> new TestJsonContentAssert(json, null);
	}


	record Member(String name) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Family(List<Member> familyMembers) {}

	private static class TestJsonContentAssert extends AbstractJsonContentAssert<TestJsonContentAssert> {

		public TestJsonContentAssert(@Nullable String json, @Nullable HttpMessageContentConverter jsonContentConverter) {
			super((json != null ? new JsonContent(json, jsonContentConverter) : null), TestJsonContentAssert.class);
		}
	}

}
