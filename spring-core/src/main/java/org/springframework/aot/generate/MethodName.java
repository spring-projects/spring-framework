/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.aot.generate;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A camel-case method name that can be built from distinct parts.
 *
 * @author Phillip Webb
 * @since 6.0
 */
final class MethodName {

	private static final String[] PREFIXES = { "get", "set", "is" };

	/**
	 * An empty method name.
	 */
	public static final MethodName NONE = of();

	private final String value;


	private MethodName(String value) {
		this.value = value;
	}


	/**
	 * Create a new method name from the specific parts. The returned name will
	 * be in camel-case and will only contain valid characters from the parts.
	 * @param parts the parts the form the name
	 * @return a method name instance
	 */
	static MethodName of(String... parts) {
		Assert.notNull(parts, "'parts' must not be null");
		return new MethodName(join(parts));
	}

	/**
	 * Create a new method name by concatenating the specified name to this name.
	 * @param name the name to concatenate
	 * @return a new method name instance
	 */
	MethodName and(MethodName name) {
		Assert.notNull(name, "'name' must not be null");
		return and(name.value);
	}

	/**
	 * Create a new method name by concatenating the specified parts to this name.
	 * @param parts the parts to concatenate
	 * @return a new method name instance
	 */
	MethodName and(String... parts) {
		Assert.notNull(parts, "'parts' must not be null");
		String joined = join(parts);
		String prefix = getPrefix(joined);
		String suffix = joined.substring(prefix.length());
		return of(prefix, this.value, suffix);
	}

	private String getPrefix(String name) {
		for (String candidate : PREFIXES) {
			if (name.startsWith(candidate)) {
				return candidate;
			}
		}
		return "";
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof MethodName that && this.value.equals(that.value)));
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public String toString() {
		return (!StringUtils.hasLength(this.value)) ? "$$aot" : this.value ;
	}


	private static String join(String[] parts) {
		return StringUtils.uncapitalize(Arrays.stream(parts).map(MethodName::clean)
				.map(StringUtils::capitalize).collect(Collectors.joining()));
	}

	private static String clean(@Nullable String part) {
		char[] chars = (part != null) ? part.toCharArray() : new char[0];
		StringBuilder name = new StringBuilder(chars.length);
		boolean uppercase = false;
		for (char ch : chars) {
			char outputChar = (!uppercase) ? ch : Character.toUpperCase(ch);
			name.append((!Character.isLetter(ch)) ? "" : outputChar);
			uppercase = (ch == '.');
		}
		return name.toString();
	}

}
