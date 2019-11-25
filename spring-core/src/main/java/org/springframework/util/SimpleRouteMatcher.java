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

package org.springframework.util;

import java.util.Comparator;
import java.util.Map;

import org.springframework.lang.Nullable;

/**
 * {@code RouteMatcher} that delegates to a {@link PathMatcher}.
 *
 * <p><strong>Note:</strong> This implementation is not efficient since
 * {@code PathMatcher} treats paths and patterns as Strings. For more optimized
 * performance use the {@code PathPatternRouteMatcher} from {@code spring-web}
 * which enables use of parsed routes and patterns.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class SimpleRouteMatcher implements RouteMatcher {

	private final PathMatcher pathMatcher;


	/**
	 * Create a new {@code SimpleRouteMatcher} for the given
	 * {@link PathMatcher} delegate.
	 */
	public SimpleRouteMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher is required");
		this.pathMatcher = pathMatcher;
	}

	/**
	 * Return the underlying {@link PathMatcher} delegate.
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}


	@Override
	public Route parseRoute(String route) {
		return new DefaultRoute(route);
	}

	@Override
	public boolean isPattern(String route) {
		return this.pathMatcher.isPattern(route);
	}

	@Override
	public String combine(String pattern1, String pattern2) {
		return this.pathMatcher.combine(pattern1, pattern2);
	}

	@Override
	public boolean match(String pattern, Route route) {
		return this.pathMatcher.match(pattern, route.value());
	}

	@Override
	@Nullable
	public Map<String, String> matchAndExtract(String pattern, Route route) {
		if (!match(pattern, route)) {
			return null;
		}
		return this.pathMatcher.extractUriTemplateVariables(pattern, route.value());
	}

	@Override
	public Comparator<String> getPatternComparator(Route route) {
		return this.pathMatcher.getPatternComparator(route.value());
	}


	private static class DefaultRoute implements Route {

		private final String path;

		DefaultRoute(String path) {
			this.path = path;
		}

		@Override
		public String value() {
			return this.path;
		}

		@Override
		public String toString() {
			return value();
		}
	}

}
