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

package org.springframework.util;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;

import org.springframework.util.PlaceholderParser.ParsedValue;
import org.springframework.util.PlaceholderParser.TextPart;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link PlaceholderParser}.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
class PlaceholderParserTests {

	@Nested // Tests with only the basic placeholder feature enabled
	class OnlyPlaceholderTests {

		private final PlaceholderParser parser = new PlaceholderParser("${", "}", null, null, true);

		@ParameterizedTest(name = "{0} -> {1}")
		@MethodSource("placeholders")
		void placeholderIsReplaced(String text, String expected) {
			Map<String, String> properties = Map.of(
					"firstName", "John",
					"nested0", "first",
					"nested1", "Name");
			assertThat(this.parser.replacePlaceholders(text, properties::get)).isEqualTo(expected);
		}

		static Stream<Arguments> placeholders() {
			return Stream.of(
					Arguments.of("${firstName}", "John"),
					Arguments.of("$${firstName}", "$John"),
					Arguments.of("}${firstName}", "}John"),
					Arguments.of("${firstName}$", "John$"),
					Arguments.of("${firstName}}", "John}"),
					Arguments.of("${firstName} ${firstName}", "John John"),
					Arguments.of("First name: ${firstName}", "First name: John"),
					Arguments.of("${firstName} is the first name", "John is the first name"),
					Arguments.of("${first${nested1}}", "John"),
					Arguments.of("${${nested0}${nested1}}", "John")
			);
		}

		@ParameterizedTest(name = "{0} -> {1}")
		@MethodSource("nestedPlaceholders")
		void nestedPlaceholdersAreReplaced(String text, String expected) {
			Map<String, String> properties = Map.of(
					"p1", "v1",
					"p2", "v2",
					"p3", "${p1}:${p2}",              // nested placeholders
					"p4", "${p3}",                    // deeply nested placeholders
					"p5", "${p1}:${p2}:${bogus}");    // unresolvable placeholder
			assertThat(this.parser.replacePlaceholders(text, properties::get)).isEqualTo(expected);
		}

		static Stream<Arguments> nestedPlaceholders() {
			return Stream.of(
					Arguments.of("${p1}:${p2}", "v1:v2"),
					Arguments.of("${p3}", "v1:v2"),
					Arguments.of("${p4}", "v1:v2"),
					Arguments.of("${p5}", "v1:v2:${bogus}"),
					Arguments.of("${p0${p0}}", "${p0${p0}}")
			);
		}

		@Test
		void parseWithSinglePlaceholder() {
			PlaceholderResolver resolver = mockPlaceholderResolver("firstName", "John");
			assertThat(this.parser.replacePlaceholders("${firstName}", resolver)).isEqualTo("John");
			verifyPlaceholderResolutions(resolver, "firstName");
		}

		@Test
		void parseWithPlaceholderAndPrefixText() {
			PlaceholderResolver resolver = mockPlaceholderResolver("firstName", "John");
			assertThat(this.parser.replacePlaceholders("This is ${firstName}", resolver)).isEqualTo("This is John");
			verifyPlaceholderResolutions(resolver, "firstName");
		}

		@Test
		void parseWithMultiplePlaceholdersAndText() {
			PlaceholderResolver resolver = mockPlaceholderResolver("firstName", "John", "lastName", "Smith");
			assertThat(this.parser.replacePlaceholders("User: ${firstName} - ${lastName}.", resolver))
					.isEqualTo("User: John - Smith.");
			verifyPlaceholderResolutions(resolver, "firstName", "lastName");
		}

		@Test
		void parseWithNestedPlaceholderInKey() {
			PlaceholderResolver resolver = mockPlaceholderResolver("nested", "Name", "firstName", "John");
			assertThat(this.parser.replacePlaceholders("${first${nested}}", resolver)).isEqualTo("John");
			verifyPlaceholderResolutions(resolver, "nested", "firstName");
		}

		@Test
		void parseWithMultipleNestedPlaceholdersInKey() {
			PlaceholderResolver resolver = mockPlaceholderResolver("nested0", "first", "nested1", "Name", "firstName", "John");
			assertThat(this.parser.replacePlaceholders("${${nested0}${nested1}}", resolver)).isEqualTo("John");
			verifyPlaceholderResolutions(resolver, "nested0", "nested1", "firstName");
		}

		@Test
		void placeholderValueContainingSeparatorIsHandledAsIs() {
			PlaceholderResolver resolver = mockPlaceholderResolver("my:test", "value");
			assertThat(this.parser.replacePlaceholders("${my:test}", resolver)).isEqualTo("value");
			verifyPlaceholderResolutions(resolver, "my:test");
		}

		@Test
		void placeholdersWithoutEscapeCharAreNotEscaped() {
			PlaceholderResolver resolver = mockPlaceholderResolver("p1", "v1", "p2", "v2", "p3", "v3", "p4", "v4");
			assertThat(this.parser.replacePlaceholders("\\${p1}", resolver)).isEqualTo("\\v1");
			assertThat(this.parser.replacePlaceholders("\\\\${p2}", resolver)).isEqualTo("\\\\v2");
			assertThat(this.parser.replacePlaceholders("\\${p3}\\", resolver)).isEqualTo("\\v3\\");
			assertThat(this.parser.replacePlaceholders("a\\${p4}\\z", resolver)).isEqualTo("a\\v4\\z");
			verifyPlaceholderResolutions(resolver, "p1", "p2", "p3", "p4");
		}

		@Test
		void textWithInvalidPlaceholderSyntaxIsMerged() {
			String text = "test${of${with${and${";
			ParsedValue parsedValue = this.parser.parse(text);
			assertThat(parsedValue.parts()).singleElement().isInstanceOfSatisfying(TextPart.class,
					textPart -> assertThat(textPart.text()).isEqualTo(text));
		}

	}

	@Nested // Tests with the use of a separator
	class DefaultValueTests {

		private final PlaceholderParser parser = new PlaceholderParser("${", "}", ":", null, true);

		@ParameterizedTest(name = "{0} -> {1}")
		@MethodSource("placeholders")
		void placeholderIsReplaced(String text, String expected) {
			Map<String, String> properties = Map.of(
					"firstName", "John",
					"nested0", "first",
					"nested1", "Name");
			assertThat(this.parser.replacePlaceholders(text, properties::get)).isEqualTo(expected);
		}

		static Stream<Arguments> placeholders() {
			return Stream.of(
					Arguments.of("${invalid:John}", "John"),
					Arguments.of("${first${invalid:Name}}", "John"),
					Arguments.of("${invalid:${firstName}}", "John"),
					Arguments.of("${invalid:${${nested0}${nested1}}}", "John"),
					Arguments.of("${invalid:$${firstName}}", "$John"),
					Arguments.of("${invalid: }${firstName}", " John"),
					Arguments.of("${invalid:}", ""),
					Arguments.of("${:}", "")
			);
		}

		@ParameterizedTest(name = "{0} -> {1}")
		@MethodSource("nestedPlaceholders")
		void nestedPlaceholdersAreReplaced(String text, String expected) {
			Map<String, String> properties = Map.of(
					"p1", "v1",
					"p2", "v2",
					"p3", "${p1}:${p2}",               // nested placeholders
					"p4", "${p3}",                     // deeply nested placeholders
					"p5", "${p1}:${p2}:${bogus}",      // unresolvable placeholder
					"p6", "${p1}:${p2}:${bogus:def}"); // unresolvable w/ default
			assertThat(this.parser.replacePlaceholders(text, properties::get)).isEqualTo(expected);
		}

		static Stream<Arguments> nestedPlaceholders() {
			return Stream.of(
					Arguments.of("${p6}", "v1:v2:def"),
					Arguments.of("${p6:not-used}", "v1:v2:def"),
					Arguments.of("${p6:${invalid}}", "v1:v2:def"),
					Arguments.of("${invalid:${p1}:${p2}}", "v1:v2"),
					Arguments.of("${invalid:${p3}}", "v1:v2"),
					Arguments.of("${invalid:${p4}}", "v1:v2"),
					Arguments.of("${invalid:${p5}}", "v1:v2:${bogus}"),
					Arguments.of("${invalid:${p6}}", "v1:v2:def")
			);
		}

		@ParameterizedTest(name = "{0} -> {1}")
		@MethodSource("exactMatchPlaceholders")
		void placeholdersWithExactMatchAreConsidered(String text, String expected) {
			Map<String, String> properties = Map.of(
					"prefix://my-service", "example-service",
					"px", "prefix",
					"p1", "${prefix://my-service}");
			assertThat(this.parser.replacePlaceholders(text, properties::get)).isEqualTo(expected);
		}

		static Stream<Arguments> exactMatchPlaceholders() {
			return Stream.of(
					Arguments.of("${prefix://my-service}", "example-service"),
					Arguments.of("${p1}", "example-service")
			);
		}

		@Test
		void parseWithKeyEqualsToText() {
			PlaceholderResolver resolver = mockPlaceholderResolver("firstName", "Steve");
			assertThat(this.parser.replacePlaceholders("${firstName}", resolver)).isEqualTo("Steve");
			verifyPlaceholderResolutions(resolver, "firstName");
		}

		@Test
		void parseWithHardcodedFallback() {
			PlaceholderResolver resolver = mockPlaceholderResolver();
			assertThat(this.parser.replacePlaceholders("${firstName:Steve}", resolver)).isEqualTo("Steve");
			verifyPlaceholderResolutions(resolver, "firstName:Steve", "firstName");
		}

		@Test
		void parseWithNestedPlaceholderInKeyUsingFallback() {
			PlaceholderResolver resolver = mockPlaceholderResolver("firstName", "John");
			assertThat(this.parser.replacePlaceholders("${first${invalid:Name}}", resolver)).isEqualTo("John");
			verifyPlaceholderResolutions(resolver, "invalid:Name", "invalid", "firstName");
		}

		@Test
		void parseWithFallbackUsingPlaceholder() {
			PlaceholderResolver resolver = mockPlaceholderResolver("firstName", "John");
			assertThat(this.parser.replacePlaceholders("${invalid:${firstName}}", resolver)).isEqualTo("John");
			verifyPlaceholderResolutions(resolver, "invalid", "firstName");
		}

	}

	/**
	 * Tests that use the escape character.
	 */
	@Nested
	class EscapedTests {

		private final PlaceholderParser parser = new PlaceholderParser("${", "}", ":", '\\', true);

		@ParameterizedTest(name = "{0} -> {1}")
		@MethodSource("escapedPlaceholders")
		void escapedPlaceholderIsNotReplaced(String text, String expected) {
			Map<String, String> properties = Map.of(
					"firstName", "John",
					"${test}", "John",
					"p1", "v1",
					"p2", "\\${p1:default}",
					"p3", "${p2}",
					"p4", "adc${p0:\\${p1}}",
					"p5", "adc${\\${p0}:${p1}}",
					"p6", "adc${p0:def\\${p1}}",
					"p7", "adc\\${");
			assertThat(this.parser.replacePlaceholders(text, properties::get)).isEqualTo(expected);
		}

		static Stream<Arguments> escapedPlaceholders() {
			return Stream.of(
					Arguments.of("\\${firstName}", "${firstName}"),
					Arguments.of("First name: \\${firstName}", "First name: ${firstName}"),
					Arguments.of("$\\${firstName}", "$${firstName}"),
					Arguments.of("\\}${firstName}", "\\}John"),
					Arguments.of("${\\${test}}", "John"),
					Arguments.of("${p2}", "${p1:default}"),
					Arguments.of("${p3}", "${p1:default}"),
					Arguments.of("${p4}", "adc${p1}"),
					Arguments.of("${p5}", "adcv1"),
					Arguments.of("${p6}", "adcdef${p1}"),
					Arguments.of("${p7}", "adc\\${"),
					// Double backslash
					Arguments.of("DOMAIN\\\\${user.name}", "DOMAIN\\${user.name}"),
					// Triple backslash
					Arguments.of("triple\\\\\\${backslash}", "triple\\\\${backslash}"),
					// Multiple escaped placeholders
					Arguments.of("start\\${prop1}middle\\${prop2}end", "start${prop1}middle${prop2}end")
				);
		}

		@ParameterizedTest(name = "{0} -> {1}")
		@MethodSource("escapedSeparators")
		void escapedSeparatorIsNotReplaced(String text, String expected) {
			Map<String, String> properties = Map.of("first:Name", "John");
			assertThat(this.parser.replacePlaceholders(text, properties::get)).isEqualTo(expected);
		}

		static Stream<Arguments> escapedSeparators() {
			return Stream.of(
					Arguments.of("${first\\:Name}", "John"),
					Arguments.of("${last\\:Name}", "${last:Name}")
			);
		}

		@ParameterizedTest(name = "{0} -> {1}")
		@MethodSource("escapedSeparatorsInNestedPlaceholders")
		void escapedSeparatorInNestedPlaceholderIsNotReplaced(String text, String expected) {
			Map<String, String> properties = Map.of(
					"app.environment", "qa",
					"app.service", "protocol",
					"protocol://host/qa/name", "protocol://example.com/qa/name",
					"service/host/qa/name", "https://example.com/qa/name",
					"service/host/qa/name:value", "https://example.com/qa/name-value");
			assertThat(this.parser.replacePlaceholders(text, properties::get)).isEqualTo(expected);
		}

		static Stream<Arguments> escapedSeparatorsInNestedPlaceholders() {
			return Stream.of(
					Arguments.of("${protocol\\://host/${app.environment}/name}", "protocol://example.com/qa/name"),
					Arguments.of("${${app.service}\\://host/${app.environment}/name}", "protocol://example.com/qa/name"),
					Arguments.of("${service/host/${app.environment}/name:\\value}", "https://example.com/qa/name"),
					Arguments.of("${service/host/${name\\:value}/}", "${service/host/${name:value}/}"));
		}

	}

	@Nested
	class ExceptionTests {

		private final PlaceholderParser parser = new PlaceholderParser("${", "}", ":", null, false);

		@Test
		void textWithCircularReference() {
			Map<String, String> properties = Map.of(
					"pL", "${pR}",
					"pR", "${pL}");
			assertThatThrownBy(() -> this.parser.replacePlaceholders("${pL}", properties::get))
					.isInstanceOf(PlaceholderResolutionException.class)
					.hasMessage("Circular placeholder reference 'pL' in value \"${pL}\" <-- \"${pR}\" <-- \"${pL}\"");
		}

		@Test
		void unresolvablePlaceholderIsReported() {
			assertThatExceptionOfType(PlaceholderResolutionException.class)
					.isThrownBy(() -> this.parser.replacePlaceholders("X${bogus}Z", placeholderName -> null))
					.withMessage("Could not resolve placeholder 'bogus' in value \"X${bogus}Z\"")
					.withNoCause();
		}

		@Test
		void unresolvablePlaceholderInNestedPlaceholderIsReportedWithChain() {
			Map<String, String> properties = Map.of(
					"p1", "v1",
					"p2", "v2",
					"p3", "${p1}:${p2}:${bogus}");
			assertThatExceptionOfType(PlaceholderResolutionException.class)
					.isThrownBy(() -> this.parser.replacePlaceholders("${p3}", properties::get))
					.withMessage("Could not resolve placeholder 'bogus' in value \"${p1}:${p2}:${bogus}\" <-- \"${p3}\"")
					.withNoCause();
		}

	}


	private static PlaceholderResolver mockPlaceholderResolver(String... pairs) {
		if (pairs.length % 2 == 1) {
			throw new IllegalArgumentException("size must be even, it is a set of key=value pairs");
		}
		PlaceholderResolver resolver = mock();
		for (int i = 0; i < pairs.length; i += 2) {
			String key = pairs[i];
			String value = pairs[i + 1];
			given(resolver.resolvePlaceholder(key)).willReturn(value);
		}
		return resolver;
	}

	private static void verifyPlaceholderResolutions(PlaceholderResolver mock, String... placeholders) {
		InOrder ordered = inOrder(mock);
		for (String placeholder : placeholders) {
			ordered.verify(mock).resolvePlaceholder(placeholder);
		}
		verifyNoMoreInteractions(mock);
	}

}
