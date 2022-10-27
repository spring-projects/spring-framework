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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.lang.Nullable;

/**
 * A collection of {@link ResourcePatternHint} describing whether resources should
 * be made available at runtime using a matching algorithm based on include/exclude
 * patterns.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 6.0
 */
public final class ResourcePatternHints {

	private final List<ResourcePatternHint> includes;

	private final List<ResourcePatternHint> excludes;


	private ResourcePatternHints(Builder builder) {
		this.includes = new ArrayList<>(builder.includes);
		this.excludes = new ArrayList<>(builder.excludes);
	}

	/**
	 * Return the include patterns to use to identify the resources to match.
	 * @return the include patterns
	 */
	public List<ResourcePatternHint> getIncludes() {
		return this.includes;
	}

	/**
	 * Return the exclude patterns to use to identify the resources to match.
	 * @return the exclude patterns
	 */
	public List<ResourcePatternHint> getExcludes() {
		return this.excludes;
	}


	/**
	 * Builder for {@link ResourcePatternHints}.
	 */
	public static class Builder {

		private final Set<ResourcePatternHint> includes = new LinkedHashSet<>();

		private final Set<ResourcePatternHint> excludes = new LinkedHashSet<>();

		Builder() {
		}

		/**
		 * Include resources matching the specified patterns.
		 * @param reachableType the type that should be reachable for this hint to apply
		 * @param includes the include patterns (see {@link ResourcePatternHint} documentation)
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder includes(@Nullable TypeReference reachableType, String... includes) {
			List<ResourcePatternHint> newIncludes = Arrays.stream(includes)
					.map(include -> new ResourcePatternHint(include, reachableType)).toList();
			this.includes.addAll(newIncludes);
			return this;
		}

		/**
		 * Include resources matching the specified patterns.
		 * @param includes the include patterns (see {@link ResourcePatternHint} documentation)
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder includes(String... includes) {
			return includes(null, includes);
		}

		/**
		 * Exclude resources matching the specified patterns.
		 * @param reachableType the type that should be reachable for this hint to apply
		 * @param excludes the exclude patterns (see {@link ResourcePatternHint} documentation)
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder excludes(TypeReference reachableType, String... excludes) {
			List<ResourcePatternHint> newExcludes = Arrays.stream(excludes)
					.map(include -> new ResourcePatternHint(include, reachableType)).toList();
			this.excludes.addAll(newExcludes);
			return this;
		}

		/**
		 * Exclude resources matching the specified patterns.
		 * @param excludes the exclude patterns (see {@link ResourcePatternHint} documentation)
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder excludes(String... excludes) {
			return excludes(null, excludes);
		}

		/**
		 * Create {@link ResourcePatternHints} based on the state of this
		 * builder.
		 * @return resource pattern hints
		 */
		ResourcePatternHints build() {
			return new ResourcePatternHints(this);
		}

	}

}
