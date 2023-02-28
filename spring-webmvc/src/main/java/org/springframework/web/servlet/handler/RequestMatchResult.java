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

package org.springframework.web.servlet.handler;

import java.util.Map;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.web.util.pattern.PathPattern;

/**
 * Container for the result from request pattern matching via
 * {@link MatchableHandlerMapping} with a method to further extract
 * URI template variables from the pattern.
 *
 * @author Rossen Stoyanchev
 * @since 4.3.1
 */
public class RequestMatchResult {

	@Nullable
	private final PathPattern pathPattern;

	@Nullable
	private final PathContainer lookupPathContainer;


	@Nullable
	private final String pattern;

	@Nullable
	private final String lookupPath;

	@Nullable
	private final PathMatcher pathMatcher;


	/**
	 * Create an instance with the matched {@code PathPattern}.
	 * @param pathPattern the pattern that was matched
	 * @param lookupPath the mapping path
	 * @since 5.3
	 */
	public RequestMatchResult(PathPattern pathPattern, PathContainer lookupPath) {
		Assert.notNull(pathPattern, "PathPattern is required");
		Assert.notNull(lookupPath, "PathContainer is required");

		this.pattern = null;
		this.lookupPath = null;
		this.pathMatcher = null;

		this.pathPattern = pathPattern;
		this.lookupPathContainer = lookupPath;

	}

	/**
	 * Create an instance with the matched String pattern.
	 * @param pattern the pattern that was matched, possibly with a '/' appended
	 * @param lookupPath the mapping path
	 * @param pathMatcher the PathMatcher instance used for the match
	 */
	public RequestMatchResult(String pattern, String lookupPath, PathMatcher pathMatcher) {
		Assert.hasText(pattern, "'matchingPattern' is required");
		Assert.hasText(lookupPath, "'lookupPath' is required");
		Assert.notNull(pathMatcher, "PathMatcher is required");

		this.pattern = pattern;
		this.lookupPath = lookupPath;
		this.pathMatcher = pathMatcher;

		this.pathPattern = null;
		this.lookupPathContainer = null;
	}

	/**
	 * Extract URI template variables from the matching pattern as defined in
	 * {@link PathMatcher#extractUriTemplateVariables}.
	 * @return a map with URI template variables
	 */
	@SuppressWarnings("ConstantConditions")
	public Map<String, String> extractUriTemplateVariables() {
		return (this.pathPattern != null ?
				this.pathPattern.matchAndExtract(this.lookupPathContainer).getUriVariables() :
				this.pathMatcher.extractUriTemplateVariables(this.pattern, this.lookupPath));
	}
}
