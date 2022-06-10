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

package org.springframework.aot.hint;

import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.util.ConcurrentLruCache;

/**
 * Generator of {@link ResourceHints} predicates, testing whether the given hints
 * match the expected behavior for resources.
 * @author Brian Clozel
 * @since 6.0
 */
public class ResourceHintsPredicates {

	private static final ConcurrentLruCache<String, Pattern> CACHED_RESOURCE_PATTERNS = new ConcurrentLruCache<>(32, Pattern::compile);

	ResourceHintsPredicates() {
	}

	/**
	 * Return a predicate that checks whether a resource hint is registered for the given bundle name.
	 * @param bundleName the resource bundle name
	 * @return the {@link RuntimeHints} predicate
	 */
	public Predicate<RuntimeHints> forBundle(String bundleName) {
		Assert.hasText(bundleName, "resource bundle name should not be empty");
		return runtimeHints -> runtimeHints.resources().resourceBundles()
				.anyMatch(bundleHint -> bundleName.equals(bundleHint.getBaseName()));
	}

	/**
	 * Return a predicate that checks whether a resource hint is registered for the given
	 * resource name, located in the given type's package.
	 * <p>For example, {@code forResource(org.example.MyClass, "myResource.txt")}
	 * will match for {@code "/org/example/myResource.txt"}.
	 * @param type the type's package where to look for the resource
	 * @param resourceName the resource name
	 * @return the {@link RuntimeHints} predicate
	 */
	public Predicate<RuntimeHints> forResource(TypeReference type, String resourceName) {
		String absoluteName = resolveAbsoluteResourceName(type, resourceName);
		return forResource(absoluteName);
	}

	private String resolveAbsoluteResourceName(TypeReference type, String resourceName) {
		if (resourceName.startsWith("/")) {
			return resourceName;
		}
		else {
			return "/" + type.getPackageName().replace('.', '/')
					+ "/" + resourceName;
		}
	}

	/**
	 * Return a predicate that checks whether a resource hint is registered for
	 * the given resource name.
	 * @param resourceName the full resource name
	 * @return the {@link RuntimeHints} predicate
	 */
	public Predicate<RuntimeHints> forResource(String resourceName) {
		return hints -> hints.resources().resourcePatterns().reduce(ResourcePatternHints::merge)
				.map(hint -> {
					boolean isExcluded = hint.getExcludes().stream()
							.anyMatch(excluded -> CACHED_RESOURCE_PATTERNS.get(excluded.getPattern()).matcher(resourceName).matches());
					if (isExcluded) {
						return false;
					}
					return hint.getIncludes().stream()
							.anyMatch(included -> CACHED_RESOURCE_PATTERNS.get(included.getPattern()).matcher(resourceName).matches());
				}).orElse(false);
	}

}
