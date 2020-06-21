/*
 * Copyright 2002-2020 the original author or authors.
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

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Additional interface that a {@link HandlerMapping} can implement to expose
 * a request matching API aligned with its internal request matching
 * configuration and implementation.
 *
 * @author Rossen Stoyanchev
 * @since 4.3.1
 * @see HandlerMappingIntrospector
 */
public interface MatchableHandlerMapping extends HandlerMapping {

	/**
	 * Return the parser of this {@code HandlerMapping}, if configured in which
	 * case pre-parsed patterns are used.
	 * @since 5.3
	 */
	@Nullable
	default PathPatternParser getPatternParser() {
		return null;
	}

	/**
	 * Determine whether the request matches the given pattern. Use this method
	 * when {@link #getPatternParser()} is not {@code null} which means that the
	 * {@code HandlerMapping} has pre-parsed patterns enabled.
	 * @param request the current request
	 * @param pattern the pattern to match
	 * @return the result from request matching, or {@code null} if none
	 * @since 5.3
	 */
	@Nullable
	default RequestMatchResult match(HttpServletRequest request, PathPattern pattern) {
		PathContainer path = ServletRequestPathUtils.getParsedRequestPath(request).pathWithinApplication();
		return (pattern.matches(path) ? new RequestMatchResult(pattern, path) : null);
	}

	/**
	 * Determine whether the request matches the given pattern. Use this method
	 * when {@link #getPatternParser()} returns {@code null} which means that the
	 * {@code HandlerMapping} is uses String pattern matching.
	 * @param request the current request
	 * @param pattern the pattern to match
	 * @return the result from request matching, or {@code null} if none
	 */
	@Nullable
	RequestMatchResult match(HttpServletRequest request, String pattern);

}
