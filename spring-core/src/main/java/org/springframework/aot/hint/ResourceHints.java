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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.springframework.aot.hint.ResourcePatternHint.Builder;

/**
 * Gather the need for resources available at runtime.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public class ResourceHints {

	private final Set<TypeReference> types;

	private final List<Builder> resourcePatternHints;

	private final Set<String> resourceBundleHints;


	public ResourceHints() {
		this.types = new HashSet<>();
		this.resourcePatternHints = new ArrayList<>();
		this.resourceBundleHints = new LinkedHashSet<>();
	}

	/**
	 * Return the resources that should be made available at runtime.
	 * @return a stream of {@link ResourcePatternHint}
	 */
	public Stream<ResourcePatternHint> resourcePatterns() {
		Stream<ResourcePatternHint> patterns = this.resourcePatternHints.stream().map(Builder::build);
		return (this.types.isEmpty() ? patterns
				: Stream.concat(Stream.of(typesPatternResourceHint()), patterns));
	}

	/**
	 * Return the resource bundles that should be made available at runtime.
	 * @return a stream of {@link ResourceBundleHint}
	 */
	public Stream<ResourceBundleHint> resourceBundles() {
		return this.resourceBundleHints.stream().map(ResourceBundleHint::new);
	}

	/**
	 * Register that the resources matching the specified pattern should be
	 * made available at runtime.
	 * @param include a pattern of the resources to include
	 * @param resourceHint a builder to further customize the resource pattern
	 * @return {@code this}, to facilitate method chaining
	 */
	public ResourceHints registerPattern(String include, Consumer<Builder> resourceHint) {
		Builder builder = new Builder().includes(include);
		if (resourceHint != null) {
			resourceHint.accept(builder);
		}
		this.resourcePatternHints.add(builder);
		return this;
	}

	/**
	 * Register that the resources matching the specified pattern should be
	 * made available at runtime.
	 * @param include a pattern of the resources to include
	 * @return {@code this}, to facilitate method chaining
	 */
	public ResourceHints registerPattern(String include) {
		return registerPattern(include, null);
	}

	/**
	 * Register that the bytecode of the type defined by the specified
	 * {@link TypeReference} should be made available at runtime.
	 * @param type the type to include
	 * @return {@code this}, to facilitate method chaining
	 */
	public ResourceHints registerType(TypeReference type) {
		this.types.add(type);
		return this;
	}

	/**
	 * Register that the bytecode of the specified type should be made
	 * available at runtime.
	 * @param type the type to include
	 * @return {@code this}, to facilitate method chaining
	 */
	public ResourceHints registerType(Class<?> type) {
		return registerType(TypeReference.of(type));
	}

	/**
	 * Register that the resource bundle with the specified base name should
	 * be made available at runtime.
	 * @param baseName the base name of the resource bundle
	 * @return {@code this}, to facilitate method chaining
	 */
	public ResourceHints registerResourceBundle(String baseName) {
		this.resourceBundleHints.add(baseName);
		return this;
	}

	private ResourcePatternHint typesPatternResourceHint() {
		Builder builder = new Builder();
		this.types.forEach(type -> builder.includes(toIncludePattern(type)));
		return builder.build();
	}

	private String toIncludePattern(TypeReference type) {
		StringBuilder names = new StringBuilder();
		buildName(type, names);
		String candidate = type.getPackageName() + "." + names;
		return candidate.replace(".", "/") + ".class";
	}

	private void buildName(TypeReference type, StringBuilder sb) {
		if (type == null) {
			return;
		}
		String typeName = (type.getEnclosingType() != null) ? "$" + type.getSimpleName() : type.getSimpleName();
		sb.insert(0, typeName);
		buildName(type.getEnclosingType(), sb);
	}

}
