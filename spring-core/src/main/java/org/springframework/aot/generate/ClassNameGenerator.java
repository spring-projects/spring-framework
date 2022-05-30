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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.javapoet.ClassName;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Generates unique class names that can be used in ahead-of-time generated
 * source code. This class is stateful so the same instance should be used for
 * all name generation. Most commonly the class name generator is obtained via a
 * {@link GenerationContext}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public final class ClassNameGenerator {

	private static final String SEPARATOR = "__";

	private static final String AOT_PACKAGE = "__";


	private final Map<String, AtomicInteger> sequenceGenerator = new ConcurrentHashMap<>();


	/**
	 * Generate a new class name for the given {@code target} /
	 * {@code featureName} combination.
	 * @param target the target of the newly generated class
	 * @param featureName the name of the feature that the generated class
	 * supports
	 * @return a unique generated class name
	 */
	public ClassName generateClassName(Class<?> target, String featureName) {
		Assert.notNull(target, "'target' must not be null");
		String rootName = target.getName().replace("$", "_");
		return generateSequencedClassName(rootName, featureName);
	}

	/**
	 * Generate a new class name for the given {@code name} /
	 * {@code featureName} combination.
	 * @param target the target of the newly generated class. When possible,
	 * this should be a class name
	 * @param featureName the name of the feature that the generated class
	 * supports
	 * @return a unique generated class name
	 */
	public ClassName generateClassName(String target, String featureName) {
		Assert.hasLength(target, "'target' must not be empty");
		target = clean(target);
		String rootName = AOT_PACKAGE + "." + ((!target.isEmpty()) ? target : "Aot");
		return generateSequencedClassName(rootName, featureName);
	}

	private String clean(String name) {
		StringBuilder rootName = new StringBuilder();
		boolean lastNotLetter = true;
		for (char ch : name.toCharArray()) {
			if (!Character.isLetter(ch)) {
				lastNotLetter = true;
				continue;
			}
			rootName.append(lastNotLetter ? Character.toUpperCase(ch) : ch);
			lastNotLetter = false;
		}
		return rootName.toString();
	}

	private ClassName generateSequencedClassName(String rootName, String featureName) {
		Assert.hasLength(featureName, "'featureName' must not be empty");
		Assert.isTrue(featureName.chars().allMatch(Character::isLetter),
				"'featureName' must contain only letters");
		String name = addSequence(
				rootName + SEPARATOR + StringUtils.capitalize(featureName));
		return ClassName.get(ClassUtils.getPackageName(name),
				ClassUtils.getShortName(name));
	}

	private String addSequence(String name) {
		int sequence = this.sequenceGenerator
				.computeIfAbsent(name, key -> new AtomicInteger()).getAndIncrement();
		return (sequence > 0) ? name + sequence : name;
	}

}
