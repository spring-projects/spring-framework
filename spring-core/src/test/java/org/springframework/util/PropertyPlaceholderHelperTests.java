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

package org.springframework.util;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rob Harrop
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

		PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper("${", "}", null, false);
		assertThatIllegalArgumentException().isThrownBy(() ->
				helper.replacePlaceholders(text, props));
	}

}
