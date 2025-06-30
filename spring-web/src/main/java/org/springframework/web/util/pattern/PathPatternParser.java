/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.util.pattern;

import org.springframework.http.server.PathContainer;
import org.springframework.util.StringUtils;

/**
 * Parser for URI path patterns producing {@link PathPattern} instances that can
 * then be matched to requests.
 *
 * <p>The {@link PathPatternParser} and {@link PathPattern} are specifically
 * designed for use with HTTP URL paths in web applications where a large number
 * of URI path patterns, continuously matched against incoming requests,
 * motivates the need for efficient matching.
 *
 * <p>For details of the path pattern syntax see {@link PathPattern}.
 *
 * @author Andy Clement
 * @since 5.0
 */
public class PathPatternParser {

	private boolean caseSensitive = true;

	private PathContainer.Options pathOptions = PathContainer.Options.HTTP_PATH;


	/**
	 * Configure whether path pattern matching should be case-sensitive.
	 * <p>The default is {@code true}.
	 */
	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	/**
	 * Whether case-sensitive pattern matching is enabled.
	 */
	public boolean isCaseSensitive() {
		return this.caseSensitive;
	}

	/**
	 * Set options for parsing patterns. These should be the same as the
	 * options used to parse input paths.
	 * <p>{@link org.springframework.http.server.PathContainer.Options#HTTP_PATH}
	 * is used by default.
	 * @since 5.2
	 */
	public void setPathOptions(PathContainer.Options pathOptions) {
		this.pathOptions = pathOptions;
	}

	/**
	 * Get the {@link #setPathOptions configured} pattern parsing options.
	 * @since 5.2
	 */
	public PathContainer.Options getPathOptions() {
		return this.pathOptions;
	}


	/**
	 * Prepare the given pattern for use in matching to full URL paths.
	 * <p>By default, prepend a leading slash if needed for non-empty patterns.
	 * @param pattern the pattern to initialize
	 * @return the updated pattern
	 * @since 5.2.25
	 */
	public String initFullPathPattern(String pattern) {
		return (StringUtils.hasLength(pattern) && !pattern.startsWith("/") ? "/" + pattern : pattern);
	}

	/**
	 * Process the path pattern content, a character at a time, breaking it into
	 * path elements around separator boundaries and verifying the structure at each
	 * stage. Produces a PathPattern object that can be used for fast matching
	 * against paths. Each invocation of this method delegates to a new instance of
	 * the {@link InternalPathPatternParser} because that class is not thread-safe.
	 * @param pathPattern the input path pattern, for example, /project/{name}
	 * @return a PathPattern for quickly matching paths against request paths
	 * @throws PatternParseException in case of parse errors
	 */
	public PathPattern parse(String pathPattern) throws PatternParseException {
		return new InternalPathPatternParser(this).parse(pathPattern);
	}


	/**
	 * Shared, read-only instance of {@code PathPatternParser}.
	 * <p>Uses default settings:
	 * <ul>
	 * <li>{@code matchOptionalTrailingSeparator = false}
	 * <li>{@code caseSensitive = true}
	 * <li>{@code pathOptions = PathContainer.Options.HTTP_PATH}
	 * </ul>
	 */
	public static final PathPatternParser defaultInstance = new PathPatternParser() {

		@Override
		public void setCaseSensitive(boolean caseSensitive) {
			raiseError();
		}

		@Override
		public void setPathOptions(PathContainer.Options pathOptions) {
			raiseError();
		}

		private void raiseError() {
			throw new UnsupportedOperationException(
					"This is a read-only, shared instance that cannot be modified");
		}

	};

}
