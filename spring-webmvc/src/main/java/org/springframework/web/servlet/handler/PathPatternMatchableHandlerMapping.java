/*
 * Copyright 2002-2025 the original author or authors.
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
import org.jspecify.annotations.Nullable;

import org.springframework.http.server.PathContainer;
import org.springframework.util.Assert;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Decorate another {@link MatchableHandlerMapping} that's configured with a
 * {@link PathPatternParser} in order to parse and cache String patterns passed
 * into the {@code match} method.
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 * @deprecated together with {@link HandlerMappingIntrospector} without a replacement.
 */
@SuppressWarnings("removal")
@Deprecated(since = "7.0", forRemoval = true)
class PathPatternMatchableHandlerMapping implements MatchableHandlerMapping {

	private final MatchableHandlerMapping delegate;

	private final PathPatternParser parser;

	private final Map<String, PathPattern> pathPatternCache = new ConcurrentHashMap<>();

	private final int cacheLimit;


	public PathPatternMatchableHandlerMapping(MatchableHandlerMapping delegate, int cacheLimit) {
		Assert.notNull(delegate, "HandlerMapping to delegate to is required.");
		Assert.notNull(delegate.getPatternParser(), "Expected HandlerMapping configured to use PatternParser.");
		this.delegate = delegate;
		this.parser = delegate.getPatternParser();
		this.cacheLimit = cacheLimit;
	}

	@SuppressWarnings("removal")
	@Deprecated(since = "7.0", forRemoval = true)
	@Override
	public @Nullable RequestMatchResult match(HttpServletRequest request, String pattern) {
		PathPattern pathPattern = this.pathPatternCache.computeIfAbsent(pattern, value -> {
			Assert.state(this.pathPatternCache.size() < this.cacheLimit, "Max size for pattern cache exceeded.");
			return this.parser.parse(pattern);
		});
		PathContainer path = ServletRequestPathUtils.getParsedRequestPath(request).pathWithinApplication();
		return (pathPattern.matches(path) ? new RequestMatchResult(pathPattern, path) : null);
	}

	@Override
	public @Nullable HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		return this.delegate.getHandler(request);
	}

}
