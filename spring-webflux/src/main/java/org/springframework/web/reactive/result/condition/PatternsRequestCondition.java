/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.result.condition;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;

import org.springframework.util.PathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.support.HttpRequestPathHelper;
import org.springframework.web.util.patterns.PathPattern;
import org.springframework.web.util.patterns.PathPatternRegistry;

/**
 * A logical disjunction (' || ') request condition that matches a request
 * against a set of URL path patterns.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public final class PatternsRequestCondition extends AbstractRequestCondition<PatternsRequestCondition> {

	private final PathPatternRegistry patternRegistry;

	private final HttpRequestPathHelper pathHelper;

	/**
	 * Creates a new instance with the given URL patterns.
	 * Each pattern that is not empty and does not start with "/" is prepended with "/".
	 * @param patterns 0 or more URL patterns; if 0 the condition will match to every request.
	 */
	public PatternsRequestCondition(String... patterns) {
		this(patterns, null, null);
	}

	/**
	 * Creates a new instance with the given URL patterns.
	 * Each pattern that is not empty and does not start with "/" is pre-pended with "/".
	 * @param patterns the URL patterns to use; if 0, the condition will match to every request.
	 * @param pathHelper to determine the lookup path for a request
	 * @param pathPatternRegistry the pattern registry in which we'll register the given paths
	 */
	public PatternsRequestCondition(String[] patterns, HttpRequestPathHelper pathHelper,
			PathPatternRegistry pathPatternRegistry) {
		this(createPatternSet(patterns, pathPatternRegistry),
				(pathHelper != null ? pathHelper : new HttpRequestPathHelper()));
	}

	private static PathPatternRegistry createPatternSet(String[] patterns, PathPatternRegistry pathPatternRegistry) {
		PathPatternRegistry patternSet = pathPatternRegistry != null ? pathPatternRegistry : new PathPatternRegistry();
		if(patterns != null) {
			Arrays.asList(patterns).stream().forEach(p -> patternSet.register(p));
		}
		return patternSet;
	}

	private PatternsRequestCondition(PathPatternRegistry patternRegistry, HttpRequestPathHelper pathHelper) {
		this.patternRegistry = patternRegistry;
		this.pathHelper = pathHelper;
	}


	public Set<PathPattern> getPatterns() {
		return this.patternRegistry.getPatterns();
	}

	@Override
	protected Collection<PathPattern> getContent() {
		return this.patternRegistry.getPatterns();
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * Returns a new instance with URL patterns from the current instance ("this") and
	 * the "other" instance as follows:
	 * <ul>
	 * <li>If there are patterns in both instances, combine the patterns in "this" with
	 * the patterns in "other" using {@link PathMatcher#combine(String, String)}.
	 * <li>If only one instance has patterns, use them.
	 * <li>If neither instance has patterns, use an empty String (i.e. "").
	 * </ul>
	 */
	@Override
	public PatternsRequestCondition combine(PatternsRequestCondition other) {
		return new PatternsRequestCondition(this.patternRegistry.combine(other.patternRegistry), this.pathHelper);
	}

	/**
	 * Checks if any of the patterns match the given request and returns an instance
	 * that is guaranteed to contain matching patterns, sorted via
	 * {@link PathMatcher#getPatternComparator(String)}.
	 * <p>A matching pattern is obtained by making checks in the following order:
	 * <ul>
	 * <li>Direct match
	 * <li>Pattern match with ".*" appended if the pattern doesn't already contain a "."
	 * <li>Pattern match
	 * <li>Pattern match with "/" appended if the pattern doesn't already end in "/"
	 * </ul>
	 * @param exchange the current exchange
	 * @return the same instance if the condition contains no patterns;
	 * or a new condition with sorted matching patterns;
	 * or {@code null} if no patterns match.
	 */
	@Override
	public PatternsRequestCondition getMatchingCondition(ServerWebExchange exchange) {
		if (this.patternRegistry.getPatterns().isEmpty()) {
			return this;
		}

		String lookupPath = this.pathHelper.getLookupPathForRequest(exchange);
		SortedSet<PathPattern> matches = getMatchingPatterns(lookupPath);

		if(!matches.isEmpty()) {
			PathPatternRegistry registry = new PathPatternRegistry(matches);
			return new PatternsRequestCondition(registry, this.pathHelper);
		}
		return null;
	}

	/**
	 * Find the patterns matching the given lookup path. Invoking this method should
	 * yield results equivalent to those of calling
	 * {@link #getMatchingCondition(ServerWebExchange)}.
	 * This method is provided as an alternative to be used if no request is available
	 * (e.g. introspection, tooling, etc).
	 * @param lookupPath the lookup path to match to existing patterns
	 * @return a sorted set of matching patterns sorted with the closest match first
	 */
	public SortedSet<PathPattern> getMatchingPatterns(String lookupPath) {
		return this.patternRegistry.findMatches(lookupPath);
	}

	/**
	 * Compare the two conditions based on the URL patterns they contain.
	 * Patterns are compared one at a time, from top to bottom via
	 * {@link PathPatternRegistry#getComparator(String)}. If all compared
	 * patterns match equally, but one instance has more patterns, it is
	 * considered a closer match.
	 * <p>It is assumed that both instances have been obtained via
	 * {@link #getMatchingCondition(ServerWebExchange)} to ensure they
	 * contain only patterns that match the request and are sorted with
	 * the best matches on top.
	 */
	@Override
	public int compareTo(PatternsRequestCondition other, ServerWebExchange exchange) {
		String lookupPath = this.pathHelper.getLookupPathForRequest(exchange);
		Comparator<PathPatternRegistry> comparator = this.patternRegistry.getComparator(lookupPath);
		return comparator.compare(this.patternRegistry, other.patternRegistry);
	}

}
