/*
 * Copyright 2002-2022 the original author or authors.
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Generates unique method names that can be used in ahead-of-time generated
 * source code. This class is stateful so one instance should be used per
 * generated type.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public class MethodNameGenerator {

	private final Map<String, AtomicInteger> sequenceGenerator = new ConcurrentHashMap<>();


	/**
	 * Create a new {@link MethodNameGenerator} instance without any reserved
	 * names.
	 */
	public MethodNameGenerator() {
	}

	/**
	 * Create a new {@link MethodNameGenerator} instance with the specified
	 * reserved names.
	 * @param reservedNames the method names to reserve
	 */
	public MethodNameGenerator(String... reservedNames) {
		this(List.of(reservedNames));
	}

	/**
	 * Create a new {@link MethodNameGenerator} instance with the specified
	 * reserved names.
	 * @param reservedNames the method names to reserve
	 */
	public MethodNameGenerator(Iterable<String> reservedNames) {
		Assert.notNull(reservedNames, "'reservedNames' must not be null");
		for (String reservedName : reservedNames) {
			addSequence(StringUtils.uncapitalize(reservedName));
		}
	}


	/**
	 * Generate a new method name from the given parts.
	 * @param parts the parts used to build the name.
	 * @return the generated method name
	 */
	public String generateMethodName(Object... parts) {
		String generatedName = join(parts);
		return addSequence(generatedName.isEmpty() ? "$$aot" : generatedName);
	}

	private String addSequence(String name) {
		int sequence = this.sequenceGenerator
				.computeIfAbsent(name, key -> new AtomicInteger()).getAndIncrement();
		return (sequence > 0) ? name + sequence : name;
	}

	/**
	 * Join the specified parts to create a valid camel case method name.
	 * @param parts the parts to join
	 * @return a method name from the joined parts.
	 */
	public static String join(Object... parts) {
		Stream<String> capitalizedPartNames = Arrays.stream(parts)
				.map(MethodNameGenerator::getPartName).map(StringUtils::capitalize);
		return StringUtils
				.uncapitalize(capitalizedPartNames.collect(Collectors.joining()));
	}

	private static String getPartName(@Nullable Object part) {
		if (part == null) {
			return "";
		}
		if (part instanceof Class<?> clazz) {
			return clean(ClassUtils.getShortName(clazz));
		}
		return clean(part.toString());
	}

	private static String clean(String string) {
		char[] chars = string.toCharArray();
		StringBuilder name = new StringBuilder(chars.length);
		boolean uppercase = false;
		for (char ch : chars) {
			char outputChar = (!uppercase) ? ch : Character.toUpperCase(ch);
			name.append((!Character.isLetter(ch)) ? "" : outputChar);
			uppercase = ch == '.';
		}
		return name.toString();
	}

}
