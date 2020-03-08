/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.RouteMatcher;

/**
 * {@code RouteMatcher} built on {@link PathPatternParser} that uses
 * {@link PathContainer} and {@link PathPattern} as parsed representations of
 * routes and patterns.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class PathPatternRouteMatcher implements RouteMatcher {

	private final PathPatternParser parser;

	private final Map<String, PathPattern> pathPatternCache = new ConcurrentHashMap<>();


	/**
	 * Default constructor with {@link PathPatternParser} customized for
	 * {@link org.springframework.http.server.PathContainer.Options#MESSAGE_ROUTE MESSAGE_ROUTE}
	 * and without matching of trailing separator.
	 */
	public PathPatternRouteMatcher() {
		this.parser = new PathPatternParser();
		this.parser.setPathOptions(PathContainer.Options.MESSAGE_ROUTE);
		this.parser.setMatchOptionalTrailingSeparator(false);
	}

	/**
	 * Constructor with given {@link PathPatternParser}.
	 */
	public PathPatternRouteMatcher(PathPatternParser parser) {
		Assert.notNull(parser, "PathPatternParser must not be null");
		this.parser = parser;
	}


	@Override
	public Route parseRoute(String routeValue) {
		return new PathContainerRoute(PathContainer.parsePath(routeValue, this.parser.getPathOptions()));
	}

	@Override
	public boolean isPattern(String route) {
		return getPathPattern(route).hasPatternSyntax();
	}

	@Override
	public String combine(String pattern1, String pattern2) {
		return getPathPattern(pattern1).combine(getPathPattern(pattern2)).getPatternString();
	}

	@Override
	public boolean match(String pattern, Route route) {
		return getPathPattern(pattern).matches(getPathContainer(route));
	}

	@Override
	@Nullable
	public Map<String, String> matchAndExtract(String pattern, Route route) {
		PathPattern.PathMatchInfo info = getPathPattern(pattern).matchAndExtract(getPathContainer(route));
		return info != null ? info.getUriVariables() : null;
	}

	@Override
	public Comparator<String> getPatternComparator(Route route) {
		return Comparator.comparing(this::getPathPattern);
	}

	private PathPattern getPathPattern(String pattern) {
		return this.pathPatternCache.computeIfAbsent(pattern, this.parser::parse);
	}

	private PathContainer getPathContainer(Route route) {
		Assert.isInstanceOf(PathContainerRoute.class, route);
		return ((PathContainerRoute) route).pathContainer;
	}


	private static class PathContainerRoute implements Route {

		private final PathContainer pathContainer;


		PathContainerRoute(PathContainer pathContainer) {
			this.pathContainer = pathContainer;
		}


		@Override
		public String value() {
			return this.pathContainer.value();
		}


		@Override
		public String toString() {
			return value();
		}
	}

}
