/*
 * Copyright 2002-2021 the original author or authors.
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
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Wraps {@link MatchableHandlerMapping}s configured with a {@link PathPatternParser}
 * in order to parse patterns lazily and cache them for re-ues.
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
class PathPatternMatchableHandlerMapping implements MatchableHandlerMapping {

	private static final int MAX_PATTERNS = 1024;


	private final MatchableHandlerMapping delegate;

	private final PathPatternParser parser;

	private final Map<String, PathPattern> pathPatternCache = new ConcurrentHashMap<>();


	public PathPatternMatchableHandlerMapping(MatchableHandlerMapping delegate) {
		Assert.notNull(delegate, "Delegate MatchableHandlerMapping is required.");
		Assert.notNull(delegate.getPatternParser(), "PatternParser is required.");
		this.delegate = delegate;
		this.parser = delegate.getPatternParser();
	}

	@Nullable
	@Override
	public RequestMatchResult match(HttpServletRequest request, String pattern) {
		PathPattern pathPattern = this.pathPatternCache.computeIfAbsent(pattern, value -> {
			Assert.isTrue(this.pathPatternCache.size() < MAX_PATTERNS, "Max size for pattern cache exceeded.");
			return this.parser.parse(pattern);
		});
		PathContainer path = ServletRequestPathUtils.getParsedRequestPath(request).pathWithinApplication();
		return (pathPattern.matches(path) ? new RequestMatchResult(pathPattern, path) : null);
	}

	@Nullable
	@Override
	public HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		return this.delegate.getHandler(request);
	}

}
