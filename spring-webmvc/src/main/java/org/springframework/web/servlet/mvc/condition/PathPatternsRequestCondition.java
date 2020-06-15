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

package org.springframework.web.servlet.mvc.condition;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * A logical disjunction (' || ') request condition that matches a request
 * against a set of URL path patterns.
 *
 * <p>In contrast to {@link PatternsRequestCondition}, this condition uses
 * parsed {@link PathPattern}s instead of String pattern matching with
 * {@link org.springframework.util.AntPathMatcher AntPathMatcher}.
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public final class PathPatternsRequestCondition extends AbstractRequestCondition<PathPatternsRequestCondition> {

	private static final SortedSet<PathPattern> EMPTY_PATH_PATTERN =
			new TreeSet<>(Collections.singleton(new PathPatternParser().parse("")));

	private static final Set<String> EMPTY_PATH = Collections.singleton("");


	private final SortedSet<PathPattern> patterns;


	/**
	 * Default constructor resulting in an {@code ""} (empty path) mapping.
	 */
	public PathPatternsRequestCondition() {
		this(EMPTY_PATH_PATTERN);
	}

	/**
	 * Constructor with patterns to use.
	 */
	public PathPatternsRequestCondition(PathPatternParser parser, String... patterns) {
		this(parse(parser, patterns));
	}

	private static SortedSet<PathPattern> parse(PathPatternParser parser, String... patterns) {
		if (patterns.length == 0 || (patterns.length == 1 && !StringUtils.hasText(patterns[0]))) {
			return EMPTY_PATH_PATTERN;
		}
		SortedSet<PathPattern> result = new TreeSet<>();
		for (String path : patterns) {
			if (StringUtils.hasText(path) && !path.startsWith("/")) {
				path = "/" + path;
			}
			result.add(parser.parse(path));
		}
		return result;
	}

	private PathPatternsRequestCondition(SortedSet<PathPattern> patterns) {
		this.patterns = patterns;
	}

	/**
	 * Return the patterns in this condition. If only the first (top) pattern
	 * is needed use {@link #getFirstPattern()}.
	 */
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

	/**
	 * Return the first pattern.
	 */
	public PathPattern getFirstPattern() {
		return this.patterns.first();
	}

	/**
	 * Whether the condition is the "" (empty path) mapping.
	 */
	public boolean isEmptyPathMapping() {
		return this.patterns == EMPTY_PATH_PATTERN;
	}

	/**
	 * Return the mapping paths that are not patterns.
	 */
	public Set<String> getDirectPaths() {
		if (isEmptyPathMapping()) {
			return EMPTY_PATH;
		}
		Set<String> result = Collections.emptySet();
		for (PathPattern pattern : this.patterns) {
			if (pattern.hasPatternSyntax()) {
				result = (result.isEmpty() ? new HashSet<>(1) : result);
				result.add(pattern.getPatternString());
			}
		}
		return result;
	}

	/**
	 * Return the {@link #getPatterns()} mapped to Strings.
	 */
	public Set<String> getPatternValues() {
		return (isEmptyPathMapping() ? EMPTY_PATH :
				getPatterns().stream().map(PathPattern::getPatternString).collect(Collectors.toSet()));
	}

	/**
	 * Returns a new instance with URL patterns from the current instance
	 * ("this") and the "other" instance as follows:
	 * <ul>
	 * <li>If there are patterns in both instances, combine the patterns in
	 * "this" with the patterns in "other" using
	 * {@link PathPattern#combine(PathPattern)}.
	 * <li>If only one instance has patterns, use them.
	 * <li>If neither instance has patterns, use an empty String (i.e. "").
	 * </ul>
	 */
	@Override
	public PathPatternsRequestCondition combine(PathPatternsRequestCondition other) {
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
			return new PathPatternsRequestCondition(combined);
		}
	}

	/**
	 * Checks if any of the patterns match the given request and returns an
	 * instance that is guaranteed to contain matching patterns, sorted.
	 * @param request the current request
	 * @return the same instance if the condition contains no patterns;
	 * or a new condition with sorted matching patterns;
	 * or {@code null} if no patterns match.
	 */
	@Override
	@Nullable
	public PathPatternsRequestCondition getMatchingCondition(HttpServletRequest request) {
		PathContainer path = ServletRequestPathUtils.getParsedRequestPath(request).pathWithinApplication();
		SortedSet<PathPattern> matches = getMatchingPatterns(path);
		return (matches != null ? new PathPatternsRequestCondition(matches) : null);
	}

	@Nullable
	private SortedSet<PathPattern> getMatchingPatterns(PathContainer path) {
		TreeSet<PathPattern> result = null;
		for (PathPattern pattern : this.patterns) {
			if (pattern.matches(path)) {
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
	 * {@link #getMatchingCondition(HttpServletRequest)} to ensure they
	 * contain only patterns that match the request and are sorted with
	 * the best matches on top.
	 */
	@Override
	public int compareTo(PathPatternsRequestCondition other, HttpServletRequest request) {
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
