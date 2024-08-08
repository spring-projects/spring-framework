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

package org.springframework.util;

import java.util.Properties;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.util.PlaceholderParser.ParsedValue;
import org.springframework.util.PlaceholderParser.TextPart;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link PlaceholderParser}.
 *
 * @author Stephane Nicoll
 */
class PlaceholderParserTests {

	@Nested // Tests with only the basic placeholder feature enabled
	class OnlyPlaceholderTests {

		private final PlaceholderParser parser = new PlaceholderParser("${", "}", null, null, true);

		@ParameterizedTest(name = "{0} -> {1}")
		@MethodSource("placeholders")
		void placeholderIsReplaced(String text, String expected) {
			Properties properties = new Properties();
			properties.setProperty("firstName", "John");
			properties.setProperty("nested0", "first");
			properties.setProperty("nested1", "Name");
			assertThat(this.parser.replacePlaceholders(text, properties::getProperty)).isEqualTo(expected);
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
			Properties properties = new Properties();
			properties.setProperty("p1", "v1");
			properties.setProperty("p2", "v2");
			properties.setProperty("p3", "${p1}:${p2}");              // nested placeholders
			properties.setProperty("p4", "${p3}");                    // deeply nested placeholders
			properties.setProperty("p5", "${p1}:${p2}:${bogus}");     // unresolvable placeholder
			assertThat(this.parser.replacePlaceholders(text, properties::getProperty)).isEqualTo(expected);
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
			assertThat(this.parser.replacePlaceholders("${firstName}", resolver))
					.isEqualTo("John");
			verify(resolver).resolvePlaceholder("firstName");
			verifyNoMoreInteractions(resolver);
		}

		@Test
		void parseWithPlaceholderAndPrefixText() {
			PlaceholderResolver resolver = mockPlaceholderResolver("firstName", "John");
			assertThat(this.parser.replacePlaceholders("This is ${firstName}", resolver))
					.isEqualTo("This is John");
			verify(resolver).resolvePlaceholder("firstName");
			verifyNoMoreInteractions(resolver);
		}

		@Test
		void parseWithMultiplePlaceholdersAndText() {
			PlaceholderResolver resolver = mockPlaceholderResolver("firstName", "John", "lastName", "Smith");
			assertThat(this.parser.replacePlaceholders("User: ${firstName} - ${lastName}.", resolver))
					.isEqualTo("User: John - Smith.");
			verify(resolver).resolvePlaceholder("firstName");
			verify(resolver).resolvePlaceholder("lastName");
			verifyNoMoreInteractions(resolver);
		}

		@Test
		void parseWithNestedPlaceholderInKey() {
			PlaceholderResolver resolver = mockPlaceholderResolver(
					"nested", "Name", "firstName", "John");
			assertThat(this.parser.replacePlaceholders("${first${nested}}", resolver))
					.isEqualTo("John");
			verifyPlaceholderResolutions(resolver, "nested", "firstName");
		}

		@Test
		void parseWithMultipleNestedPlaceholdersInKey() {
			PlaceholderResolver resolver = mockPlaceholderResolver(
					"nested0", "first", "nested1", "Name", "firstName", "John");
			assertThat(this.parser.replacePlaceholders("${${nested0}${nested1}}", resolver))
					.isEqualTo("John");
			verifyPlaceholderResolutions(resolver, "nested0", "nested1", "firstName");
		}

		@Test
		void placeholdersWithSeparatorAreHandledAsIs() {
			PlaceholderResolver resolver = mockPlaceholderResolver("my:test", "value");
			assertThat(this.parser.replacePlaceholders("${my:test}", resolver)).isEqualTo("value");
			verifyPlaceholderResolutions(resolver, "my:test");
		}

		@Test
		void placeholdersWithoutEscapeCharAreNotEscaped() {
			PlaceholderResolver resolver = mockPlaceholderResolver("test", "value");
			assertThat(this.parser.replacePlaceholders("\\${test}", resolver)).isEqualTo("\\value");
			verifyPlaceholderResolutions(resolver, "test");
		}

		@Test
		void textWithInvalidPlaceholderIsMerged() {
			String text = "test${of${with${and${";
			ParsedValue parsedValue = this.parser.parse(text);
			assertThat(parsedValue.parts()).singleElement().isInstanceOfSatisfying(
					TextPart.class, textPart -> assertThat(textPart.text()).isEqualTo(text));
		}

	}

	@Nested // Tests with the use of a separator
	class DefaultValueTests {

		private final PlaceholderParser parser = new PlaceholderParser("${", "}", ":", null, true);

		@ParameterizedTest(name = "{0} -> {1}")
		@MethodSource("placeholders")
		void placeholderIsReplaced(String text, String expected) {
			Properties properties = new Properties();
			properties.setProperty("firstName", "John");
			properties.setProperty("nested0", "first");
			properties.setProperty("nested1", "Name");
			assertThat(this.parser.replacePlaceholders(text, properties::getProperty)).isEqualTo(expected);
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
			Properties properties = new Properties();
			properties.setProperty("p1", "v1");
			properties.setProperty("p2", "v2");
			properties.setProperty("p3", "${p1}:${p2}");              // nested placeholders
			properties.setProperty("p4", "${p3}");                    // deeply nested placeholders
			properties.setProperty("p5", "${p1}:${p2}:${bogus}");     // unresolvable placeholder
			properties.setProperty("p6", "${p1}:${p2}:${bogus:def}"); // unresolvable w/ default
			assertThat(this.parser.replacePlaceholders(text, properties::getProperty)).isEqualTo(expected);
		}

		static Stream<Arguments> nestedPlaceholders() {
			return Stream.of(
					Arguments.of("${p6}", "v1:v2:def"),
					Arguments.of("${invalid:${p1}:${p2}}", "v1:v2"),
					Arguments.of("${invalid:${p3}}", "v1:v2"),
					Arguments.of("${invalid:${p4}}", "v1:v2"),
					Arguments.of("${invalid:${p5}}", "v1:v2:${bogus}"),
					Arguments.of("${invalid:${p6}}", "v1:v2:def")
			);
		}

		@Test
		void parseWithHardcodedFallback() {
			PlaceholderResolver resolver = mockPlaceholderResolver();
			assertThat(this.parser.replacePlaceholders("${firstName:Steve}", resolver))
					.isEqualTo("Steve");
			verifyPlaceholderResolutions(resolver, "firstName");
		}

		@Test
		void parseWithNestedPlaceholderInKeyUsingFallback() {
			PlaceholderResolver resolver = mockPlaceholderResolver("firstName", "John");
			assertThat(this.parser.replacePlaceholders("${first${invalid:Name}}", resolver))
					.isEqualTo("John");
			verifyPlaceholderResolutions(resolver, "invalid", "firstName");
		}

		@Test
		void parseWithFallbackUsingPlaceholder() {
			PlaceholderResolver resolver = mockPlaceholderResolver("firstName", "John");
			assertThat(this.parser.replacePlaceholders("${invalid:${firstName}}", resolver))
					.isEqualTo("John");
			verifyPlaceholderResolutions(resolver, "invalid", "firstName");
		}

	}

	@Nested // Tests with the use of the escape character
	class EscapedTests {

		private final PlaceholderParser parser = new PlaceholderParser("${", "}", ":", '\\', true);

		@ParameterizedTest(name = "{0} -> {1}")
		@MethodSource("escapedPlaceholders")
		void escapedPlaceholderIsNotReplaced(String text, String expected) {
			PlaceholderResolver resolver = mockPlaceholderResolver(
					"firstName", "John", "nested0", "first", "nested1", "Name",
					"${test}", "John",
					"p1", "v1", "p2", "\\${p1:default}", "p3", "${p2}",
					"p4", "adc${p0:\\${p1}}",
					"p5", "adc${\\${p0}:${p1}}",
					"p6", "adc${p0:def\\${p1}}",
					"p7", "adc\\${");
			assertThat(this.parser.replacePlaceholders(text, resolver)).isEqualTo(expected);
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
					Arguments.of("${p7}", "adc\\${"));

		}

		@ParameterizedTest(name = "{0} -> {1}")
		@MethodSource("escapedSeparators")
		void escapedSeparatorIsNotReplaced(String text, String expected) {
			Properties properties = new Properties();
			properties.setProperty("first:Name", "John");
			properties.setProperty("nested0", "first");
			properties.setProperty("nested1", "Name");
			assertThat(this.parser.replacePlaceholders(text, properties::getProperty)).isEqualTo(expected);
		}

		static Stream<Arguments> escapedSeparators() {
			return Stream.of(
					Arguments.of("${first\\:Name}", "John"),
					Arguments.of("${last\\:Name}", "${last:Name}")
			);
		}

	}

	@Nested
	class ExceptionTests {

		private final PlaceholderParser parser = new PlaceholderParser("${", "}", ":", null, false);

		@Test
		void textWithCircularReference() {
			PlaceholderResolver resolver = mockPlaceholderResolver("pL", "${pR}", "pR", "${pL}");
			assertThatThrownBy(() -> this.parser.replacePlaceholders("${pL}", resolver))
					.isInstanceOf(PlaceholderResolutionException.class)
					.hasMessage("Circular placeholder reference 'pL' in value \"${pL}\" <-- \"${pR}\" <-- \"${pL}\"");
		}

		@Test
		void unresolvablePlaceholderIsReported() {
			PlaceholderResolver resolver = mockPlaceholderResolver();
			assertThatExceptionOfType(PlaceholderResolutionException.class)
					.isThrownBy(() -> this.parser.replacePlaceholders("${bogus}", resolver))
					.withMessage("Could not resolve placeholder 'bogus' in value \"${bogus}\"")
					.withNoCause();
		}

		@Test
		void unresolvablePlaceholderInNestedPlaceholderIsReportedWithChain() {
			PlaceholderResolver resolver = mockPlaceholderResolver("p1", "v1", "p2", "v2",
					"p3", "${p1}:${p2}:${bogus}");
			assertThatExceptionOfType(PlaceholderResolutionException.class)
					.isThrownBy(() -> this.parser.replacePlaceholders("${p3}", resolver))
					.withMessage("Could not resolve placeholder 'bogus' in value \"${p1}:${p2}:${bogus}\" <-- \"${p3}\"")
					.withNoCause();
		}

	}

	PlaceholderResolver mockPlaceholderResolver(String... pairs) {
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

	void verifyPlaceholderResolutions(PlaceholderResolver mock, String... placeholders) {
		for (String placeholder : placeholders) {
			verify(mock).resolvePlaceholder(placeholder);
		}
		verifyNoMoreInteractions(mock);
	}

}
