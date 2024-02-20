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

import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link PropertyPlaceholderHelper}.
 *
 * @author Rob Harrop
 * @author Stephane Nicoll
 */
class PropertyPlaceholderHelperTests {

	private final PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper("${", "}");

	@Test
	void withProperties() {
		String text = "foo=${foo}";
		Properties props = new Properties();
		props.setProperty("foo", "bar");

		assertThat(this.helper.replacePlaceholders(text, props)).isEqualTo("foo=bar");
	}

	@Test
	void withMultipleProperties() {
		String text = "foo=${foo},bar=${bar}";
		Properties props = new Properties();
		props.setProperty("foo", "bar");
		props.setProperty("bar", "baz");

		assertThat(this.helper.replacePlaceholders(text, props)).isEqualTo("foo=bar,bar=baz");
	}

	@Test
	void recurseInProperty() {
		String text = "foo=${bar}";
		Properties props = new Properties();
		props.setProperty("bar", "${baz}");
		props.setProperty("baz", "bar");

		assertThat(this.helper.replacePlaceholders(text, props)).isEqualTo("foo=bar");
	}

	@Test
	void recurseInPlaceholder() {
		String text = "foo=${b${inner}}";
		Properties props = new Properties();
		props.setProperty("bar", "bar");
		props.setProperty("inner", "ar");

		assertThat(this.helper.replacePlaceholders(text, props)).isEqualTo("foo=bar");

		text = "${top}";
		props = new Properties();
		props.setProperty("top", "${child}+${child}");
		props.setProperty("child", "${${differentiator}.grandchild}");
		props.setProperty("differentiator", "first");
		props.setProperty("first.grandchild", "actualValue");

		assertThat(this.helper.replacePlaceholders(text, props)).isEqualTo("actualValue+actualValue");
	}

	@Test
	void withResolver() {
		String text = "foo=${foo}";
		PlaceholderResolver resolver = placeholderName -> "foo".equals(placeholderName) ? "bar" : null;

		assertThat(this.helper.replacePlaceholders(text, resolver)).isEqualTo("foo=bar");
	}

	@Test
	void unresolvedPlaceholderIsIgnored() {
		String text = "foo=${foo},bar=${bar}";
		Properties props = new Properties();
		props.setProperty("foo", "bar");

		assertThat(this.helper.replacePlaceholders(text, props)).isEqualTo("foo=bar,bar=${bar}");
	}

	@Test
	void unresolvedPlaceholderAsError() {
		String text = "foo=${foo},bar=${bar}";
		Properties props = new Properties();
		props.setProperty("foo", "bar");

		PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper("${", "}", null, null, false);
		assertThatExceptionOfType(PlaceholderResolutionException.class).isThrownBy(() ->
				helper.replacePlaceholders(text, props));
	}

	@Nested
	class DefaultValueTests {

		private final PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper("${", "}", ":", null, true);

		@ParameterizedTest(name = "{0} -> {1}")
		@MethodSource("defaultValues")
		void defaultValueIsApplied(String text, String value) {
			Properties properties = new Properties();
			properties.setProperty("one", "1");
			properties.setProperty("two", "2");
			assertThat(this.helper.replacePlaceholders(text, properties)).isEqualTo(value);
		}

		@Test
		void defaultValueIsNotEvaluatedEarly() {
			PlaceholderResolver resolver = mockPlaceholderResolver("one", "1");
			assertThat(this.helper.replacePlaceholders("This is ${one:or${two}}", resolver)).isEqualTo("This is 1");
			verify(resolver).resolvePlaceholder("one");
			verify(resolver, never()).resolvePlaceholder("two");
		}

		static Stream<Arguments> defaultValues() {
			return Stream.of(
					Arguments.of("${invalid:test}", "test"),
					Arguments.of("${invalid:${one}}", "1"),
					Arguments.of("${invalid:${one}${two}}", "12"),
					Arguments.of("${invalid:${one}:${two}}", "1:2"),
					Arguments.of("${invalid:${also_invalid:test}}", "test"),
					Arguments.of("${invalid:${also_invalid:${one}}}", "1")
			);
		}

	}

	PlaceholderResolver mockPlaceholderResolver(String... pairs) {
		if (pairs.length % 2 == 1) {
			throw new IllegalArgumentException("size must be even, it is a set of key=value pairs");
		}
		PlaceholderResolver resolver = mock(PlaceholderResolver.class);
		for (int i = 0; i < pairs.length; i += 2) {
			String key = pairs[i];
			String value = pairs[i + 1];
			given(resolver.resolvePlaceholder(key)).willReturn(value);
		}
		return resolver;
	}


}
