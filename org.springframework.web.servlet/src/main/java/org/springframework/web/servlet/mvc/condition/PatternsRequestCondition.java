/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.mvc.condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * A logical disjunction (' || ') request condition that matches a request against a set of URL path patterns.  
 * 
 * <p>See Javadoc on individual methods for details on how URL patterns are matched, combined, and compared. 
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class PatternsRequestCondition extends AbstractRequestCondition<PatternsRequestCondition> {
	
	private final Set<String> patterns; 

	private final UrlPathHelper urlPathHelper;
	
	private final PathMatcher pathMatcher;

	/**
	 * Creates a new {@link PatternsRequestCondition} with the given URL patterns.
	 * Each pattern that is not empty and does not start with "/" is prepended with "/".
	 * @param patterns 0 or more URL patterns; if 0 the condition will match to every request. 
	 */
	public PatternsRequestCondition(String... patterns) {
		this(patterns, new UrlPathHelper(), new AntPathMatcher());
	}

	/**
	 * Creates a new {@link PatternsRequestCondition} with the given URL patterns. 
	 * Each pattern that is not empty and does not start with "/" is prepended with "/". 
	 * 
	 * @param patterns the URL patterns to use; if 0, the condition will match to every request. 
	 * @param urlPathHelper a {@link UrlPathHelper} for determining the lookup path for a request
	 * @param pathMatcher a {@link PathMatcher} for pattern path matching
	 */
	public PatternsRequestCondition(String[] patterns, UrlPathHelper urlPathHelper, PathMatcher pathMatcher) {
		this(asList(patterns), urlPathHelper, pathMatcher);
	}

	private static List<String> asList(String... patterns) {
		return patterns != null ? Arrays.asList(patterns) : Collections.<String>emptyList();
	}

	/**
	 * Private constructor.
	 */
	private PatternsRequestCondition(Collection<String> patterns, UrlPathHelper urlPathHelper, PathMatcher pathMatcher) {
		this.patterns = Collections.unmodifiableSet(prependLeadingSlash(patterns));
		this.urlPathHelper = urlPathHelper;
		this.pathMatcher = pathMatcher;
	}

	private static Set<String> prependLeadingSlash(Collection<String> patterns) {
		if (patterns == null) {
			return Collections.emptySet();
		}
		Set<String> result = new LinkedHashSet<String>(patterns.size());
		for (String pattern : patterns) {
			if (StringUtils.hasLength(pattern) && !pattern.startsWith("/")) {
				pattern = "/" + pattern;
			}
			result.add(pattern);
		}
		return result;
	}

	public Set<String> getPatterns() {
		return patterns;
	}

	@Override
	protected Collection<String> getContent() {
		return patterns;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * Returns a new instance with URL patterns from the current instance ("this") and 
	 * the "other" instance as follows: 
	 * <ul>
	 * 	<li>If there are patterns in both instances, combine the patterns in "this" with 
	 * 		the patterns in "other" using {@link PathMatcher#combine(String, String)}.
	 * 	<li>If only one instance has patterns, use them.
	 *  <li>If neither instance has patterns, use "".
	 * </ul>
	 */	
	public PatternsRequestCondition combine(PatternsRequestCondition other) {
		Set<String> result = new LinkedHashSet<String>();
		if (!this.patterns.isEmpty() && !other.patterns.isEmpty()) {
			for (String pattern1 : this.patterns) {
				for (String pattern2 : other.patterns) {
					result.add(pathMatcher.combine(pattern1, pattern2));
				}
			}
		}
		else if (!this.patterns.isEmpty()) {
			result.addAll(this.patterns);
		}
		else if (!other.patterns.isEmpty()) {
			result.addAll(other.patterns);
		}
		else {
			result.add("");
		}
		return new PatternsRequestCondition(result, urlPathHelper, pathMatcher);
	}

	/**
	 * Checks if any of the patterns match the given request and returns an instance that is guaranteed 
	 * to contain matching patterns, sorted via {@link PathMatcher#getPatternComparator(String)}. 
	 * 
	 * <p>A matching pattern is obtained by making checks in the following order:
	 * <ul>
	 * 	<li>Direct match
	 * 	<li>A pattern match with ".*" appended assuming the pattern already doesn't contain "."
	 * 	<li>A pattern match
	 * 	<li>A pattern match with "/" appended assuming the patterns already end with "/"
	 * </ul>
	 * 
	 * @param request the current request
	 * 
	 * @return the same instance if the condition contains no patterns; 
	 * 		or a new condition with sorted matching patterns; or {@code null} if no patterns match.
	 */
	public PatternsRequestCondition getMatchingCondition(HttpServletRequest request) {
		if (patterns.isEmpty()) {
			return this;
		}
		String lookupPath = urlPathHelper.getLookupPathForRequest(request);
		List<String> matches = new ArrayList<String>();
		for (String pattern : patterns) {
			String match = getMatchingPattern(pattern, lookupPath);
			if (match != null) {
				matches.add(match);
			}
		}
		Collections.sort(matches, pathMatcher.getPatternComparator(lookupPath));
		return matches.isEmpty() ? null : new PatternsRequestCondition(matches, urlPathHelper, pathMatcher);
	}

	private String getMatchingPattern(String pattern, String lookupPath) {
		if (pattern.equals(lookupPath)) {
			return pattern;
		}
		boolean hasSuffix = pattern.indexOf('.') != -1;
		if (!hasSuffix && pathMatcher.match(pattern + ".*", lookupPath)) {
			return pattern + ".*";
		}
		if (pathMatcher.match(pattern, lookupPath)) {
			return pattern;
		}
		boolean endsWithSlash = pattern.endsWith("/");
		if (!endsWithSlash && pathMatcher.match(pattern + "/", lookupPath)) {
			return pattern +"/";
		}
		return null;
	}

	/**
	 * Compare the two conditions and return 0 if they match equally to the request, less than one if "this" 
	 * matches the request more, and greater than one if "other" matches the request more. Patterns are 
	 * compared one at a time, from top to bottom via {@link PathMatcher#getPatternComparator(String)}. 
	 * If all compared patterns match equally, but one instance has more patterns, it is a closer match.
	 * 
	 * <p>It is assumed that both instances have been obtained via {@link #getMatchingCondition(HttpServletRequest)}
	 * to ensure they contain only patterns that match the request and are sorted with the best matches on top.
	 */
	public int compareTo(PatternsRequestCondition other, HttpServletRequest request) {
		String lookupPath = urlPathHelper.getLookupPathForRequest(request);
		Comparator<String> patternComparator = pathMatcher.getPatternComparator(lookupPath);

		Iterator<String> iterator = patterns.iterator();
		Iterator<String> iteratorOther = other.patterns.iterator();
		while (iterator.hasNext() && iteratorOther.hasNext()) {
			int result = patternComparator.compare(iterator.next(), iteratorOther.next());
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
