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

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
class SystemPropertyUtilsTests {

	@Test
	void replaceFromSystemProperty() {
		System.setProperty("test.prop", "bar");
		try {
			String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop}");
			assertThat(resolved).isEqualTo("bar");
		}
		finally {
			System.getProperties().remove("test.prop");
		}
	}

	@Test
	void replaceFromSystemPropertyWithDefault() {
		System.setProperty("test.prop", "bar");
		try {
			String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop:foo}");
			assertThat(resolved).isEqualTo("bar");
		}
		finally {
			System.getProperties().remove("test.prop");
		}
	}

	@Test
	void replaceFromSystemPropertyWithExpressionDefault() {
		System.setProperty("test.prop", "bar");
		try {
			String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop:#{foo.bar}}");
			assertThat(resolved).isEqualTo("bar");
		}
		finally {
			System.getProperties().remove("test.prop");
		}
	}

	@Test
	void replaceFromSystemPropertyWithExpressionContainingDefault() {
		System.setProperty("test.prop", "bar");
		try {
			String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop:Y#{foo.bar}X}");
			assertThat(resolved).isEqualTo("bar");
		}
		finally {
			System.getProperties().remove("test.prop");
		}
	}

	@Test
	void replaceWithDefault() {
		String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop:foo}");
		assertThat(resolved).isEqualTo("foo");
	}

	@Test
	void replaceWithExpressionDefault() {
		String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop:#{foo.bar}}");
		assertThat(resolved).isEqualTo("#{foo.bar}");
	}

	@Test
	void replaceWithExpressionContainingDefault() {
		String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop:Y#{foo.bar}X}");
		assertThat(resolved).isEqualTo("Y#{foo.bar}X");
	}

	@Test
	void replaceWithNoDefault() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				SystemPropertyUtils.resolvePlaceholders("${test.prop}"));
	}

	@Test
	void replaceWithNoDefaultIgnored() {
		String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop}", true);
		assertThat(resolved).isEqualTo("${test.prop}");
	}

	@Test
	void replaceWithEmptyDefault() {
		String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop:}");
		assertThat(resolved).isEqualTo("");
	}

	@Test
	void recursiveFromSystemProperty() {
		System.setProperty("test.prop", "foo=${bar}");
		System.setProperty("bar", "baz");
		try {
			String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop}");
			assertThat(resolved).isEqualTo("foo=baz");
		}
		finally {
			System.getProperties().remove("test.prop");
			System.getProperties().remove("bar");
		}
	}

	@Test
	void replaceFromEnv() {
		Map<String,String> env = System.getenv();
		if (env.containsKey("PATH")) {
			String text = "${PATH}";
			assertThat(SystemPropertyUtils.resolvePlaceholders(text)).isEqualTo(env.get("PATH"));
		}
	}

}
