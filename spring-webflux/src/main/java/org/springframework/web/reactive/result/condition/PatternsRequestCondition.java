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

package org.springframework.web.reactive.result.condition;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
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

	private static final SortedSet<PathPattern> EMPTY_PATH_PATTERN =
			new TreeSet<>(Collections.singleton(PathPatternParser.defaultInstance.parse("")));

	private static final Set<String> EMPTY_PATH = Collections.singleton("");


	private final SortedSet<PathPattern> patterns;


	/**
	 * Creates a new instance with the given URL patterns.
	 * @param patterns 0 or more URL patterns; if 0 the condition will match to every request.
	 */
	public PatternsRequestCondition(PathPattern... patterns) {
		this(ObjectUtils.isEmpty(patterns) ? Collections.emptyList() : Arrays.asList(patterns));
	}

	/**
	 * Creates a new instance with the given URL patterns.
	 */
	public PatternsRequestCondition(List<PathPattern> patterns) {
		this.patterns = (patterns.isEmpty() ? EMPTY_PATH_PATTERN : new TreeSet<>(patterns));
	}

	private PatternsRequestCondition(SortedSet<PathPattern> patterns) {
		this.patterns = patterns;
	}


	public Set<PathPattern> getPatterns() {
		return this.patterns;
	}

	@Override
	protected Collection<PathPattern> getContent() {
		return this.patterns;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	private boolean isEmptyPathMapping() {
		return this.patterns == EMPTY_PATH_PATTERN;
	}

	/**
	 * Return the mapping paths that are not patterns.
	 * @since 5.3
	 */
	public Set<String> getDirectPaths() {
		if (isEmptyPathMapping()) {
			return EMPTY_PATH;
		}
		Set<String> result = Collections.emptySet();
		for (PathPattern pattern : this.patterns) {
			if (!pattern.hasPatternSyntax()) {
				result = (result.isEmpty() ? new HashSet<>(1) : result);
				result.add(pattern.getPatternString());
			}
		}
		return result;
	}

	/**
	 * Returns a new instance with URL patterns from the current instance ("this") and
	 * the "other" instance as follows:
	 * <ul>
	 * <li>If there are patterns in both instances, combine the patterns in "this" with
	 * the patterns in "other" using {@link PathPattern#combine(PathPattern)}.
	 * <li>If only one instance has patterns, use them.
	 * <li>If neither instance has patterns, use an empty String (i.e. "").
	 * </ul>
	 */
	@Override
	public PatternsRequestCondition combine(PatternsRequestCondition other) {
		if (isEmptyPathMapping() && other.isEmptyPathMapping()) {
			return this;
		}
		else if (other.isEmptyPathMapping()) {
			return this;
		}
		else if (isEmptyPathMapping()) {
			return other;
		}
		else {
			SortedSet<PathPattern> combined = new TreeSet<>();
			for (PathPattern pattern1 : this.patterns) {
				for (PathPattern pattern2 : other.patterns) {
					combined.add(pattern1.combine(pattern2));
				}
			}
			return new PatternsRequestCondition(combined);
		}
	}

	/**
	 * Checks if any of the patterns match the given request and returns an instance
	 * that is guaranteed to contain matching patterns, sorted.
	 * @param exchange the current exchange
	 * @return the same instance if the condition contains no patterns;
	 * or a new condition with sorted matching patterns;
	 * or {@code null} if no patterns match.
	 */
	@Override
	@Nullable
	public PatternsRequestCondition getMatchingCondition(ServerWebExchange exchange) {
		SortedSet<PathPattern> matches = getMatchingPatterns(exchange);
		return (matches != null ? new PatternsRequestCondition(matches) : null);
	}

	@Nullable
	private SortedSet<PathPattern> getMatchingPatterns(ServerWebExchange exchange) {
		PathContainer lookupPath = exchange.getRequest().getPath().pathWithinApplication();
		TreeSet<PathPattern> result = null;
		for (PathPattern pattern : this.patterns) {
			if (pattern.matches(lookupPath)) {
				result = (result != null ? result : new TreeSet<>());
				result.add(pattern);
			}
		}
		return result;
	}

	/**
	 * Compare the two conditions based on the URL patterns they contain.
	 * Patterns are compared one at a time, from top to bottom. If all compared
	 * patterns match equally, but one instance has more patterns, it is
	 * considered a closer match.
	 * <p>It is assumed that both instances have been obtained via
	 * {@link #getMatchingCondition(ServerWebExchange)} to ensure they
	 * contain only patterns that match the request and are sorted with
	 * the best matches on top.
	 */
	@Override
	public int compareTo(PatternsRequestCondition other, ServerWebExchange exchange) {
		Iterator<PathPattern> iterator = this.patterns.iterator();
		Iterator<PathPattern> iteratorOther = other.getPatterns().iterator();
		while (iterator.hasNext() && iteratorOther.hasNext()) {
			int result = PathPattern.SPECIFICITY_COMPARATOR.compare(iterator.next(), iteratorOther.next());
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
