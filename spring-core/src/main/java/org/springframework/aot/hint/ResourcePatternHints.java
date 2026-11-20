/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * A collection of {@link ResourcePatternHint} describing whether resources should
 * be made available at runtime using a matching algorithm based on include/exclude
 * patterns.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 6.0
 */
public final class ResourcePatternHints {

	private final List<ResourcePatternHint> includes;


	private ResourcePatternHints(Builder builder) {
		this.includes = new ArrayList<>(builder.includes);
	}

	/**
	 * Return the include patterns to use to identify the resources to match.
	 * @return the include patterns
	 */
	public List<ResourcePatternHint> getIncludes() {
		return this.includes;
	}


	/**
	 * Builder for {@link ResourcePatternHints}.
	 */
	public static class Builder {

		private final Set<ResourcePatternHint> includes = new LinkedHashSet<>();

		Builder() {
		}

		/**
		 * Include resources matching the specified glob patterns.
		 * @param reachableType the type that should be reachable for this hint to apply
		 * @param includes the include patterns (see {@link ResourcePatternHint} documentation)
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder includes(@Nullable TypeReference reachableType, String... includes) {
			Arrays.stream(includes)
					.map(this::expandToIncludeDirectories)
					.flatMap(List::stream)
					.map(include -> new ResourcePatternHint(include, reachableType))
					.forEach(this.includes::add);
			return this;
		}

		/**
		 * Expand the supplied include pattern into multiple patterns that include
		 * all parent directories for the ultimate resource or resources.
		 * <p>This is necessary to support classpath scanning within a GraalVM
		 * native image.
		 * @see <a href="https://github.com/spring-projects/spring-framework/issues/29403">gh-29403</a>
		 */
		private List<String> expandToIncludeDirectories(String includePattern) {
			// Resource in root or no explicit subdirectories?
			if (!includePattern.contains("/")) {
				// Include the root directory as well as the pattern
				return List.of("/", includePattern);
			}

			List<String> includePatterns = new ArrayList<>();
			// Ensure the root directory and original pattern are always included
			includePatterns.add("/");
			includePatterns.add(includePattern);
			StringBuilder path = new StringBuilder();
			for (String pathElement : includePattern.split("/")) {
				if (pathElement.isEmpty()) {
					// Skip empty path elements
					continue;
				}
				if (pathElement.contains("*")) {
					// Stop at the first encountered wildcard, since we cannot reliably reason
					// any further about the directory structure below this path element.
					break;
				}
				if (!path.isEmpty()) {
					path.append("/");
				}
				path.append(pathElement);
				includePatterns.add(path.toString());
			}
			return includePatterns;
		}

		/**
		 * Include resources matching the specified glob patterns.
		 * @param includes the include patterns (see {@link ResourcePatternHint} documentation)
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder includes(String... includes) {
			return includes(null, includes);
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
