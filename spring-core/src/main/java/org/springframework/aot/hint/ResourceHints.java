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

package org.springframework.aot.hint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * Gather the need for resources available at runtime.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.0
 */
public class ResourceHints {

	private final Set<TypeReference> types;

	private final List<ResourcePatternHints> resourcePatternHints;

	private final Set<ResourceBundleHint> resourceBundleHints;


	public ResourceHints() {
		this.types = new HashSet<>();
		this.resourcePatternHints = new ArrayList<>();
		this.resourceBundleHints = new LinkedHashSet<>();
	}

	/**
	 * Return the resources that should be made available at runtime.
	 * @return a stream of {@link ResourcePatternHints}
	 */
	public Stream<ResourcePatternHints> resourcePatternHints() {
		Stream<ResourcePatternHints> patterns = this.resourcePatternHints.stream();
		return (this.types.isEmpty() ? patterns
				: Stream.concat(Stream.of(typesPatternResourceHint()), patterns));
	}

	/**
	 * Return the resource bundles that should be made available at runtime.
	 * @return a stream of {@link ResourceBundleHint}
	 */
	public Stream<ResourceBundleHint> resourceBundleHints() {
		return this.resourceBundleHints.stream();
	}

	/**
	 * Register a pattern if the given {@code location} is available on the
	 * classpath. This delegates to {@link ClassLoader#getResource(String)}
	 * which validates directories as well. The location is not included in
	 * the hint.
	 * @param classLoader the classloader to use
	 * @param location a '/'-separated path name that should exist
	 * @param resourceHint a builder to customize the resource pattern
	 * @return {@code this}, to facilitate method chaining
	 */
	public ResourceHints registerPatternIfPresent(@Nullable ClassLoader classLoader, String location,
			Consumer<ResourcePatternHints.Builder> resourceHint) {
		ClassLoader classLoaderToUse = (classLoader != null ? classLoader : getClass().getClassLoader());
		if (classLoaderToUse.getResource(location) != null) {
			registerPattern(resourceHint);
		}
		return this;
	}

	/**
	 * Register that the resources matching the specified pattern should be
	 * made available at runtime.
	 * @param resourceHint a builder to further customize the resource pattern
	 * @return {@code this}, to facilitate method chaining
	 */
	public ResourceHints registerPattern(@Nullable Consumer<ResourcePatternHints.Builder> resourceHint) {
		ResourcePatternHints.Builder builder = new ResourcePatternHints.Builder();
		if (resourceHint != null) {
			resourceHint.accept(builder);
		}
		this.resourcePatternHints.add(builder.build());
		return this;
	}

	/**
	 * Register that the resources matching the specified pattern should be
	 * made available at runtime.
	 * @param include a pattern of the resources to include (see {@link ResourcePatternHint} documentation)
	 * @return {@code this}, to facilitate method chaining
	 */
	public ResourceHints registerPattern(String include) {
		return registerPattern(builder -> builder.includes(include));
	}

	/**
	 * Register that the supplied resource should be made available at runtime.
	 * @param resource the resource to register
	 * @throws IllegalArgumentException if the supplied resource is not a
	 * {@link ClassPathResource} or does not {@linkplain Resource#exists() exist}
	 * @see #registerPattern(String)
	 * @see ClassPathResource#getPath()
	 */
	public void registerResource(Resource resource) {
		if (resource instanceof ClassPathResource classPathResource && classPathResource.exists()) {
			registerPattern(classPathResource.getPath());
		}
		else {
			throw new IllegalArgumentException("Resource must be a ClassPathResource that exists: " + resource);
		}
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
	 * @param resourceHint a builder to further customize the resource bundle
	 * @return {@code this}, to facilitate method chaining
	 */
	public ResourceHints registerResourceBundle(String baseName, @Nullable Consumer<ResourceBundleHint.Builder> resourceHint) {
		ResourceBundleHint.Builder builder = new ResourceBundleHint.Builder(baseName);
		if (resourceHint != null) {
			resourceHint.accept(builder);
		}
		this.resourceBundleHints.add(builder.build());
		return this;
	}

	/**
	 * Register that the resource bundle with the specified base name should
	 * be made available at runtime.
	 * @param baseName the base name of the resource bundle
	 * @return {@code this}, to facilitate method chaining
	 */
	public ResourceHints registerResourceBundle(String baseName) {
		return registerResourceBundle(baseName, null);
	}

	private ResourcePatternHints typesPatternResourceHint() {
		ResourcePatternHints.Builder builder = new ResourcePatternHints.Builder();
		this.types.forEach(type -> builder.includes(toIncludePattern(type)));
		return builder.build();
	}

	private String toIncludePattern(TypeReference type) {
		return type.getName().replace(".", "/") + ".class";
	}

}
