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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.AssertProvider;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.test.http.HttpMessageContentConverter;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link JsonPathValueAssert}.
 *
 * @author Stephane Nicoll
 */
class JsonPathValueAssertTests {

	@Nested
	class AsStringTests {

		@Test
		void asStringWithStringValue() {
			assertThat(forValue("test")).asString().isEqualTo("test");
		}

		@Test
		void asStringWithEmptyValue() {
			assertThat(forValue("")).asString().isEmpty();
		}

		@Test
		void asStringWithNonStringFails() {
			int value = 123;
			AssertProvider<JsonPathValueAssert> actual = forValue(value);
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(actual).asString().isEqualTo("123"))
					.satisfies(hasFailedToBeOfType(value, "a string"));
		}

		@Test
		void asStringWithNullFails() {
			AssertProvider<JsonPathValueAssert> actual = forValue(null);
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(actual).asString().isEqualTo("null"))
					.satisfies(hasFailedToBeOfTypeWhenNull("a string"));
		}
	}

	@Nested
	class AsNumberTests {

		@Test
		void asNumberWithIntegerValue() {
			assertThat(forValue(123)).asNumber().isEqualTo(123);
		}

		@Test
		void asNumberWithDoubleValue() {
			assertThat(forValue(3.1415926)).asNumber()
					.asInstanceOf(InstanceOfAssertFactories.DOUBLE)
					.isEqualTo(3.14, Offset.offset(0.01));
		}

		@Test
		void asNumberWithNonNumberFails() {
			String value = "123";
			AssertProvider<JsonPathValueAssert> actual = forValue(value);
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(actual).asNumber().isEqualTo(123))
					.satisfies(hasFailedToBeOfType(value, "a number"));
		}

		@Test
		void asNumberWithNullFails() {
			AssertProvider<JsonPathValueAssert> actual = forValue(null);
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(actual).asNumber().isEqualTo(0))
					.satisfies(hasFailedToBeOfTypeWhenNull("a number"));
		}
	}

	@Nested
	class AsBooleanTests {

		@Test
		void asBooleanWithBooleanPrimitiveValue() {
			assertThat(forValue(true)).asBoolean().isEqualTo(true);
		}

		@Test
		void asBooleanWithBooleanWrapperValue() {
			assertThat(forValue(Boolean.FALSE)).asBoolean().isEqualTo(false);
		}

		@Test
		void asBooleanWithNonBooleanFails() {
			String value = "false";
			AssertProvider<JsonPathValueAssert> actual = forValue(value);
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(actual).asBoolean().isEqualTo(false))
					.satisfies(hasFailedToBeOfType(value, "a boolean"));
		}

		@Test
		void asBooleanWithNullFails() {
			AssertProvider<JsonPathValueAssert> actual = forValue(null);
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(actual).asBoolean().isEqualTo(false))
					.satisfies(hasFailedToBeOfTypeWhenNull("a boolean"));
		}
	}

	@Nested
	class AsArrayTests { // json path uses List for arrays

		@Test
		void asArrayWithStringValues() {
			assertThat(forValue(List.of("a", "b", "c"))).asArray().contains("a", "c");
		}

		@Test
		void asArrayWithEmptyArray() {
			assertThat(forValue(Collections.emptyList())).asArray().isEmpty();
		}

		@Test
		void asArrayWithNonArrayFails() {
			String value = "test";
			AssertProvider<JsonPathValueAssert> actual = forValue(value);
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(actual).asArray().contains("t"))
					.satisfies(hasFailedToBeOfType(value, "an array"));
		}

		@Test
		void asArrayWithNullFails() {
			AssertProvider<JsonPathValueAssert> actual = forValue(null);
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(actual).asArray().isEqualTo(false))
					.satisfies(hasFailedToBeOfTypeWhenNull("an array"));
		}
	}

	@Nested
	class AsMapTests {

		@Test
		void asMapWithMapValue() {
			assertThat(forValue(Map.of("zero", 0, "one", 1))).asMap().containsKeys("zero", "one")
					.containsValues(0, 1);
		}

		@Test
		void asArrayWithEmptyMap() {
			assertThat(forValue(Collections.emptyMap())).asMap().isEmpty();
		}

		@Test
		void asMapWithNonMapFails() {
			List<String> value = List.of("a", "b");
			AssertProvider<JsonPathValueAssert> actual = forValue(value);
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(actual).asMap().containsKey("a"))
					.satisfies(hasFailedToBeOfType(value, "a map"));
		}

		@Test
		void asMapWithNullFails() {
			AssertProvider<JsonPathValueAssert> actual = forValue(null);
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(actual).asMap().isEmpty())
					.satisfies(hasFailedToBeOfTypeWhenNull("a map"));
		}
	}

	@Nested
	class ConvertToTests {

		private static final HttpMessageContentConverter jsonContentConverter = HttpMessageContentConverter.of(
				new MappingJackson2HttpMessageConverter(new ObjectMapper()));

		@Test
		void convertToWithoutHttpMessageConverter() {
			AssertProvider<JsonPathValueAssert> actual = () -> new JsonPathValueAssert("123", "$.test", null);
			assertThatIllegalStateException().isThrownBy(() -> assertThat(actual).convertTo(Integer.class))
					.withMessage("No JSON message converter available to convert '123'");
		}

		@Test
		void convertObjectToPojo() {
			assertThat(forValue(Map.of("id", 1234, "name", "John", "active", true))).convertTo(User.class)
					.satisfies(user -> {
						assertThat(user.id).isEqualTo(1234);
						assertThat(user.name).isEqualTo("John");
						assertThat(user.active).isTrue();
					});
		}

		@Test
		void convertArrayToListOfPojo() {
			Map<?, ?> user1 = Map.of("id", 1234, "name", "John", "active", true);
			Map<?, ?> user2 = Map.of("id", 5678, "name", "Sarah", "active", false);
			Map<?, ?> user3 = Map.of("id", 9012, "name", "Sophia", "active", true);
			assertThat(forValue(List.of(user1, user2, user3)))
					.convertTo(InstanceOfAssertFactories.list(User.class))
					.hasSize(3).extracting("name").containsExactly("John", "Sarah", "Sophia");
		}

		@Test
		void convertObjectToPojoWithMissingMandatoryField() {
			Map<?, ?> value = Map.of("firstName", "John");
			AssertProvider<JsonPathValueAssert> actual = forValue(value);
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(actual).convertTo(User.class))
					.satisfies(hasFailedToConvertToType(value, User.class))
					.withMessageContaining("firstName");
		}


		private AssertProvider<JsonPathValueAssert> forValue(@Nullable Object actual) {
			return () -> new JsonPathValueAssert(actual, "$.test", jsonContentConverter);
		}


		private record User(long id, String name, boolean active) {}

	}

	@Nested
	class EmptyNotEmptyTests {

		@Test
		void isEmptyWithEmptyString() {
			assertThat(forValue("")).isEmpty();
		}

		@Test
		void isEmptyWithNull() {
			assertThat(forValue(null)).isEmpty();
		}

		@Test
		void isEmptyWithEmptyArray() {
			assertThat(forValue(Collections.emptyList())).isEmpty();
		}

		@Test
		void isEmptyWithEmptyObject() {
			assertThat(forValue(Collections.emptyMap())).isEmpty();
		}

		@Test
		void isEmptyWithWhitespace() {
			AssertProvider<JsonPathValueAssert> actual = forValue("    ");
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(actual).isEmpty())
					.satisfies(hasFailedEmptyCheck("    "));
		}

		@Test
		void isNotEmptyWithString() {
			assertThat(forValue("test")).isNotEmpty();
		}

		@Test
		void isNotEmptyWithArray() {
			assertThat(forValue(List.of("test"))).isNotEmpty();
		}

		@Test
		void isNotEmptyWithObject() {
			assertThat(forValue(Map.of("test", "value"))).isNotEmpty();
		}

		private Consumer<AssertionError> hasFailedEmptyCheck(Object actual) {
			return error -> assertThat(error.getMessage()).containsSubsequence("Expected value at JSON path \"$.test\":",
					"" + StringUtils.quoteIfString(actual), "To be empty");
		}
	}


	private Consumer<AssertionError> hasFailedToBeOfType(Object actual, String expectedDescription) {
		return error -> assertThat(error.getMessage()).containsSubsequence("Expected value at JSON path \"$.test\":",
				"" + StringUtils.quoteIfString(actual), "To be " + expectedDescription, "But was:", actual.getClass().getName());
	}

	private Consumer<AssertionError> hasFailedToBeOfTypeWhenNull(String expectedDescription) {
		return error -> assertThat(error.getMessage()).containsSubsequence("Expected value at JSON path \"$.test\":", "null",
				"To be " + expectedDescription);
	}

	private Consumer<AssertionError> hasFailedToConvertToType(Object actual, Class<?> targetType) {
		return error -> assertThat(error.getMessage()).containsSubsequence("Expected value at JSON path \"$.test\":",
				"" + StringUtils.quoteIfString(actual), "To convert successfully to:", targetType.getTypeName(), "But it failed:");
	}



	private AssertProvider<JsonPathValueAssert> forValue(@Nullable Object actual) {
		return () -> new JsonPathValueAssert(actual, "$.test", null);
	}

}
