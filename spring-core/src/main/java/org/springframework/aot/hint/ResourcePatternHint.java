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

/**
 * A hint that describes resources that should be made available at runtime.
 *
 * <p>The patterns may be a simple path which has a one-to-one mapping to a
 * resource on the classpath, or alternatively may contain the special
 * {@code *} character to indicate a wildcard search.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public final class ResourcePatternHint {

	private final List<String> includes;

	private final List<String> excludes;


	private ResourcePatternHint(Builder builder) {
		this.includes = new ArrayList<>(builder.includes);
		this.excludes = new ArrayList<>(builder.excludes);
	}

	/**
	 * Return the include patterns to use to identify the resources to match.
	 * @return the include patterns
	 */
	public List<String> getIncludes() {
		return this.includes;
	}

	/**
	 * Return the exclude patterns to use to identify the resources to match.
	 * @return the exclude patterns
	 */
	public List<String> getExcludes() {
		return this.excludes;
	}


	/**
	 * Builder for {@link ResourcePatternHint}.
	 */
	public static class Builder {

		private final Set<String> includes = new LinkedHashSet<>();

		private final Set<String> excludes = new LinkedHashSet<>();


		/**
		 * Includes the resources matching the specified pattern.
		 * @param includes the include patterns
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder includes(String... includes) {
			this.includes.addAll(Arrays.asList(includes));
			return this;
		}

		/**
		 * Exclude resources matching the specified pattern.
		 * @param excludes the excludes pattern
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder excludes(String... excludes) {
			this.excludes.addAll(Arrays.asList(excludes));
			return this;
		}

		/**
		 * Creates a {@link ResourcePatternHint} based on the state of this
		 * builder.
		 * @return a resource pattern hint
		 */
		public ResourcePatternHint build() {
			return new ResourcePatternHint(this);
		}

	}
}
