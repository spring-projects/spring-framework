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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternComparator;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * A logical disjunction (' || ') request condition that matches a request
 * against a set of URL path patterns.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public final class PatternsRequestCondition extends AbstractRequestCondition<PatternsRequestCondition> {

	private final List<PathPattern> patterns;

	private final PathPatternParser parser;

	/**
	 * Creates a new instance with the given URL patterns.
	 * Each pattern is prepended with "/" if not already.
	 * @param patterns 0 or more URL patterns; if 0 the condition will match to every request.
	 */
	public PatternsRequestCondition(String... patterns) {
		this(asList(patterns), null);
	}

	/**
	 * Creates a new instance with the given URL patterns.
	 * Each pattern that is not empty and does not start with "/" is pre-pended with "/".
	 * @param patterns the URL patterns to use; if 0, the condition will match to every request.
	 * @param patternParser for parsing string patterns
	 */
	public PatternsRequestCondition(String[] patterns, PathPatternParser patternParser) {

		this(asList(patterns), patternParser);
	}

	/**
	 * Private constructor accepting a collection of raw patterns.
	 */
	private PatternsRequestCondition(Collection<String> patterns, PathPatternParser patternParser) {
		this.parser = patternParser != null ? patternParser : new PathPatternParser();
		this.patterns = new ArrayList<>();
		patterns.forEach(pattern -> {
			if (StringUtils.hasText(pattern) && !pattern.startsWith("/")) {
				pattern = "/" + pattern;
			}
			this.patterns.add(this.parser.parse(pattern));
		});
	}

	/**
	 * Private constructor accepting a list of path patterns.
	 */
	private PatternsRequestCondition(List<PathPattern> patterns, PathPatternParser patternParser) {
		this.patterns = patterns;
		this.parser = patternParser;
	}


	private static List<String> asList(String... patterns) {
		return (patterns != null ? Arrays.asList(patterns) : Collections.emptyList());
	}

	public Set<PathPattern> getPatterns() {
		return new TreeSet<>(this.patterns);
	}

	public Set<String> getPatternStrings() {
		return this.patterns.stream()
				.map(PathPattern::toString).collect(Collectors.toSet());
	}

	@Override
	protected Collection<PathPattern> getContent() {
		return this.patterns;
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
	 * the patterns in "other" using {@link PathPattern#combine(String)}.
	 * <li>If only one instance has patterns, use them.
	 * <li>If neither instance has patterns, use an empty String (i.e. "").
	 * </ul>
	 */
	@Override
	public PatternsRequestCondition combine(PatternsRequestCondition other) {
		List<PathPattern> combined = new ArrayList<>();
		if (!this.patterns.isEmpty() && !other.patterns.isEmpty()) {
			for (PathPattern pattern1 : this.patterns) {
				for (PathPattern pattern2 : other.patterns) {
					String combinedPattern = pattern1.combine(pattern2.getPatternString());
					combined.add(this.parser.parse(combinedPattern));
				}
			}
		}
		else if (!this.patterns.isEmpty()) {
			combined.addAll(this.patterns);
		}
		else if (!other.patterns.isEmpty()) {
			combined.addAll(other.patterns);
		}
		else {
			combined.add(this.parser.parse(""));
		}

		return new PatternsRequestCondition(combined, this.parser);
	}

	/**
	 * Checks if any of the patterns match the given request and returns an instance
	 * that is guaranteed to contain matching patterns, sorted with a
	 * {@link PathPatternComparator}.
	 * @param exchange the current exchange
	 * @return the same instance if the condition contains no patterns;
	 * or a new condition with sorted matching patterns;
	 * or {@code null} if no patterns match.
	 */
	@Override
	@Nullable
	public PatternsRequestCondition getMatchingCondition(ServerWebExchange exchange) {
		if (this.patterns.isEmpty()) {
			return this;
		}

		String lookupPath = exchange.getRequest().getPath().pathWithinApplication().value();
		SortedSet<PathPattern> matches = getMatchingPatterns(lookupPath);
		return matches.isEmpty() ? null : new PatternsRequestCondition(new ArrayList<>(matches), this.parser);
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
		return patterns.stream()
				.filter(pattern -> pattern.matches(lookupPath))
				.collect(Collectors.toCollection(() ->
						new TreeSet<>(new PathPatternComparator(lookupPath))));
	}

	/**
	 * Compare the two conditions based on the URL patterns they contain.
	 * Patterns are compared one at a time, from top to bottom via
	 * {@link PathPatternComparator}. If all compared
	 * patterns match equally, but one instance has more patterns, it is
	 * considered a closer match.
	 * <p>It is assumed that both instances have been obtained via
	 * {@link #getMatchingCondition(ServerWebExchange)} to ensure they
	 * contain only patterns that match the request and are sorted with
	 * the best matches on top.
	 */
	@Override
	public int compareTo(PatternsRequestCondition other, ServerWebExchange exchange) {
		String lookupPath = exchange.getRequest().getPath().pathWithinApplication().value();
		PathPatternComparator comparator = new PathPatternComparator(lookupPath);
		Iterator<PathPattern> iterator = this.patterns.stream()
				.sorted(comparator).collect(Collectors.toList()).iterator();
		Iterator<PathPattern> iteratorOther = other.getPatterns().stream()
				.sorted(comparator).collect(Collectors.toList()).iterator();
		while (iterator.hasNext() && iteratorOther.hasNext()) {
			int result = comparator.compare(iterator.next(), iteratorOther.next());
			if (result != 0) {
				return result;
			}
		}
		if (iterator.hasNext()) {
			return -1;
		}
		else if (iteratorOther.hasNext()) {
			return 1;
		}
		else {
			return 0;
		}
	}

}
