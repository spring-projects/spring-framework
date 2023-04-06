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

package org.springframework.aot.hint.predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.ResourcePatternHint;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentLruCache;

/**
 * Generator of {@link ResourceHints} predicates, testing whether the given hints
 * match the expected behavior for resources.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.0
 */
public class ResourceHintsPredicates {

	private static final ConcurrentLruCache<ResourcePatternHint, Pattern> CACHED_RESOURCE_PATTERNS = new ConcurrentLruCache<>(32, ResourcePatternHint::toRegex);

	ResourceHintsPredicates() {
	}

	/**
	 * Return a predicate that checks whether a resource hint is registered for the given bundle name.
	 * @param bundleName the resource bundle name
	 * @return the {@link RuntimeHints} predicate
	 */
	public Predicate<RuntimeHints> forBundle(String bundleName) {
		Assert.hasText(bundleName, "resource bundle name should not be empty");
		return runtimeHints -> runtimeHints.resources().resourceBundleHints()
				.anyMatch(bundleHint -> bundleName.equals(bundleHint.getBaseName()));
	}

	/**
	 * Return a predicate that checks whether a resource hint is registered for the given
	 * resource name, located in the given type's package.
	 * <p>For example, {@code forResource(org.example.MyClass, "myResource.txt")}
	 * will match against {@code "org/example/myResource.txt"}.
	 * <p>If the given resource name is an absolute path (i.e., starts with a
	 * leading slash), the supplied type will be ignored. For example,
	 * {@code forResource(org.example.MyClass, "/myResource.txt")} will match against
	 * {@code "myResource.txt"}.
	 * @param type the type's package where to look for the resource
	 * @param resourceName the resource name
	 * @return the {@link RuntimeHints} predicate
	 */
	public Predicate<RuntimeHints> forResource(TypeReference type, String resourceName) {
		String absoluteName = resolveAbsoluteResourceName(type, resourceName);
		return forResource(absoluteName);
	}

	private String resolveAbsoluteResourceName(TypeReference type, String resourceName) {
		// absolute path
		if (resourceName.startsWith("/")) {
			return resourceName.substring(1);
		}
		// default package
		else if (type.getPackageName().isEmpty()) {
			return resourceName;
		}
		// relative path
		else {
			return type.getPackageName().replace('.', '/') + "/" + resourceName;
		}
	}

	/**
	 * Return a predicate that checks whether a resource hint is registered for
	 * the given resource name.
	 * <p>A leading slash will be removed.
	 * @param resourceName the absolute resource name
	 * @return the {@link RuntimeHints} predicate
	 */
	public Predicate<RuntimeHints> forResource(String resourceName) {
		String resourceNameToUse = (resourceName.startsWith("/") ? resourceName.substring(1) : resourceName);
		return hints -> {
			AggregatedResourcePatternHints aggregatedResourcePatternHints = AggregatedResourcePatternHints.of(
					hints.resources());
			boolean isExcluded = aggregatedResourcePatternHints.excludes().stream().anyMatch(excluded ->
					CACHED_RESOURCE_PATTERNS.get(excluded).matcher(resourceNameToUse).matches());
			if (isExcluded) {
				return false;
			}
			return aggregatedResourcePatternHints.includes().stream().anyMatch(included ->
					CACHED_RESOURCE_PATTERNS.get(included).matcher(resourceNameToUse).matches());
		};
	}

	private record AggregatedResourcePatternHints(List<ResourcePatternHint> includes, List<ResourcePatternHint> excludes) {

		static AggregatedResourcePatternHints of(ResourceHints resourceHints) {
			List<ResourcePatternHint> includes = new ArrayList<>();
			List<ResourcePatternHint> excludes = new ArrayList<>();
			resourceHints.resourcePatternHints().forEach(resourcePatternHint -> {
				includes.addAll(resourcePatternHint.getIncludes());
				excludes.addAll(resourcePatternHint.getExcludes());
			});
			return new AggregatedResourcePatternHints(includes, excludes);
		}

	}

}
