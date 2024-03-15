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

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link JsonPathAssert}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class JsonPathAssertTests {

	private static final String TYPES = loadJson("types.json");

	private static final String SIMPSONS = loadJson("simpsons.json");

	private static final String NULLS = loadJson("nulls.json");

	private static final MappingJackson2HttpMessageConverter jsonHttpMessageConverter =
			new MappingJackson2HttpMessageConverter(new ObjectMapper());


	@Nested
	class HasPathTests {

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
			assertThatIllegalStateException().isThrownBy(() -> path.convertTo(Member.class))
					.withMessage("No JSON message converter available to convert {name=Homer}");
		}

		@Test
		void convertToTargetType() {
			assertThat(forJson(SIMPSONS, jsonHttpMessageConverter))
					.extractingPath("$.familyMembers[0]").convertTo(Member.class)
					.satisfies(member -> assertThat(member.name).isEqualTo("Homer"));
		}

		@Test
		void convertToIncompatibleTargetTypeShouldFail() {
			JsonPathValueAssert path = assertThat(forJson(SIMPSONS, jsonHttpMessageConverter))
					.extractingPath("$.familyMembers[0]");
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> path.convertTo(Customer.class))
					.withMessageContainingAll("Expected value at JSON path \"$.familyMembers[0]\":",
							Customer.class.getName(), "name");
		}

		@Test
		void convertArrayToParameterizedType() {
			assertThat(forJson(SIMPSONS, jsonHttpMessageConverter))
					.extractingPath("$.familyMembers")
					.convertTo(new ParameterizedTypeReference<List<Member>>() {})
					.satisfies(family -> assertThat(family).hasSize(5).element(0).isEqualTo(new Member("Homer")));
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


		private record Member(String name) {}

		private record Customer(long id, String username) {}

	}

	private Consumer<AssertionError> hasFailedToMatchPath(String expression) {
		return error -> assertThat(error.getMessage()).containsSubsequence("Expecting:",
				"To match JSON path:", "\"" + expression + "\"");
	}

	private Consumer<AssertionError> hasFailedToNotMatchPath(String expression) {
		return error -> assertThat(error.getMessage()).containsSubsequence("Expecting:",
				"To not match JSON path:", "\"" + expression + "\"");
	}


	private static String loadJson(String path) {
		try {
			ClassPathResource resource = new ClassPathResource(path, JsonPathAssertTests.class);
			return new String(FileCopyUtils.copyToByteArray(resource.getInputStream()));
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private AssertProvider<JsonPathAssert> forJson(String json) {
		return forJson(json, null);
	}

	private AssertProvider<JsonPathAssert> forJson(String json,
			@Nullable GenericHttpMessageConverter<Object> jsonHttpMessageConverter) {
		return () -> new JsonPathAssert(json, jsonHttpMessageConverter);
	}

}
