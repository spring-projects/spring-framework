/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.support;

import java.util.Collections;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;

/**
 * Container for the result from request pattern matching via
 * {@link MatchableHandlerMapping} with a method to further extract URI template
 * variables from the pattern.
 *
 * @author Rossen Stoyanchev
 * @since 4.3.1
 */
public class RequestMatchResult {

	private final String matchingPattern;

	private final String lookupPath;

	private final PathMatcher pathMatcher;


	/**
	 * Create an instance with a matching pattern.
	 * @param matchingPattern the matching pattern, possibly not the same as the
	 * input pattern, e.g. inputPattern="/foo" and matchingPattern="/foo/".
	 * @param lookupPath the lookup path extracted from the request
	 * @param pathMatcher the PathMatcher used
	 */
	public RequestMatchResult(String matchingPattern, String lookupPath, PathMatcher pathMatcher) {
		Assert.hasText(matchingPattern, "'matchingPattern' is required");
		Assert.hasText(lookupPath, "'lookupPath' is required");
		Assert.notNull(pathMatcher, "'pathMatcher' is required");
		this.matchingPattern = matchingPattern;
		this.lookupPath = lookupPath;
		this.pathMatcher = pathMatcher;
	}


	/**
	 * Whether the pattern was matched to the request.
	 */
	public boolean isMatch() {
		return (this.matchingPattern != null);
	}

	/**
	 * Extract URI template variables from the matching pattern as defined in
	 * {@link PathMatcher#extractUriTemplateVariables}.
	 * @return a map with URI template variables
	 */
	public Map<String, String> extractUriTemplateVariables() {
		if (!isMatch()) {
			return Collections.<String, String>emptyMap();
		}
		return this.pathMatcher.extractUriTemplateVariables(this.matchingPattern, this.lookupPath);
	}

}
