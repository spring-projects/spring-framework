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

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.PathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.util.UrlPathHelper;

/**
 * TODO
 * 
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public final class RequestKey {

	private final Set<String> patterns;

	private final Set<RequestMethod> methods;

	private final Set<RequestCondition> paramConditions;

	private final Set<RequestCondition> headerConditions;

	private int hash;

	/**
	 * Creates a new {@code RequestKey} instance with the given parameters.
	 *
	 * <p/>Package protected for testing purposes.
	 */
	RequestKey(Collection<String> patterns,
			   Collection<RequestMethod> methods,
			   Collection<RequestCondition> paramConditions,
			   Collection<RequestCondition> headerConditions) {
		this.patterns = asUnmodifiableSet(prependLeadingSlash(patterns));
		this.methods = asUnmodifiableSet(methods);
		this.paramConditions = asUnmodifiableSet(paramConditions);
		this.headerConditions = asUnmodifiableSet(headerConditions);
	}

	private static Set<String> prependLeadingSlash(Collection<String> patterns) {
		if (patterns == null) {
			return Collections.emptySet();
		}
		Set<String> result = new LinkedHashSet<String>(patterns.size());
		for (String pattern : patterns) {
			if (!pattern.startsWith("/")) {
				pattern = "/" + pattern;
			}
			result.add(pattern);
		}
		return result;
	}

	private static <T> Set<T> asUnmodifiableSet(Collection<T> collection) {
		if (collection == null) {
			return Collections.emptySet();
		}
		Set<T> result = new LinkedHashSet<T>(collection);
		return Collections.unmodifiableSet(result);
	}

	/**
	 * Creates a new {@code RequestKey} from a {@link RequestMapping @RequestMapping} annotation.
	 *
	 * @param annotation the annotation
	 * @return the request key created from the annotation
	 */
	public static RequestKey createFromRequestMapping(RequestMapping annotation) {
		return new RequestKey(Arrays.asList(annotation.value()), Arrays.asList(annotation.method()),
						RequestConditionFactory.parseParams(annotation.params()),
						RequestConditionFactory.parseHeaders(annotation.headers()));
	}

	/**
	 * Creates a new {@code RequestKey} from a {@link HttpServletRequest}.
	 *
	 * @param request the servlet request
	 * @param urlPathHelper to create the {@linkplain UrlPathHelper#getLookupPathForRequest(HttpServletRequest) lookup
	 * path}
	 * @return the request key created from the servlet request
	 */
	public static RequestKey createFromServletRequest(HttpServletRequest request, UrlPathHelper urlPathHelper) {
		String lookupPath = urlPathHelper.getLookupPathForRequest(request);
		RequestMethod method = RequestMethod.valueOf(request.getMethod());
		return new RequestKey(Arrays.asList(lookupPath), Arrays.asList(method), null, null);
	}

	/**
	 * Returns the patterns of this request key.
	 */
	public Set<String> getPatterns() {
		return patterns;
	}

	/**
	 * Returns the request methods of this request key.
	 */
	public Set<RequestMethod> getMethods() {
		return methods;
	}

	/**
	 * Returns the request parameters of this request key.
	 */
	public Set<RequestCondition> getParams() {
		return paramConditions;
	}

	/**
	 * Returns the request headers of this request key.
	 */
	public Set<RequestCondition> getHeaders() {
		return headerConditions;
	}

	/**
	 * Creates a new {@code RequestKey} by combining it with another. The typical use case for this is combining type
	 * and method-level {@link RequestMapping @RequestMapping} annotations.
	 *
	 * @param methodKey the method-level RequestKey
	 * @param pathMatcher to {@linkplain PathMatcher#combine(String, String) combine} the patterns
	 * @return the combined request key
	 */
	public RequestKey combine(RequestKey methodKey, PathMatcher pathMatcher) {
		Set<String> patterns = combinePatterns(this.patterns, methodKey.patterns, pathMatcher);
		Set<RequestMethod> methods = union(this.methods, methodKey.methods);
		Set<RequestCondition> params = union(this.paramConditions, methodKey.paramConditions);
		Set<RequestCondition> headers = union(this.headerConditions, methodKey.headerConditions);

		return new RequestKey(patterns, methods, params, headers);
	}

	private static Set<String> combinePatterns(Collection<String> typePatterns,
											   Collection<String> methodPatterns,
											   PathMatcher pathMatcher) {
		Set<String> result = new LinkedHashSet<String>();
		if (!typePatterns.isEmpty() && !methodPatterns.isEmpty()) {
			for (String pattern1 : typePatterns) {
				for (String p2 : methodPatterns) {
					result.add(pathMatcher.combine(pattern1, p2));
				}
			}
		}
		else if (!typePatterns.isEmpty()) {
			result.addAll(typePatterns);
		}
		else if (!methodPatterns.isEmpty()) {
			result.addAll(methodPatterns);
		}
		else {
			result.add("");
		}
		return result;
	}

	private static <T> Set<T> union(Collection<T> s1, Collection<T> s2) {
		Set<T> union = new LinkedHashSet<T>(s1);
		union.addAll(s2);
		return union;
	}

	/**
	 * Returns a new {@code RequestKey} that contains all matching attributes of this key, given the {@link
	 * HttpServletRequest}. Matching patterns in the returned RequestKey are sorted according to {@link
	 * PathMatcher#getPatternComparator(String)} with the best matching pattern at the top.
	 *
	 * @param request the servlet request
	 * @param pathMatcher to {@linkplain PathMatcher#match(String, String) match} patterns
	 * @param urlPathHelper to create the {@linkplain UrlPathHelper#getLookupPathForRequest(HttpServletRequest) lookup
	 * path}
	 * @return a new request key that contains all matching attributes
	 */
	public RequestKey getMatchingKey(HttpServletRequest request, PathMatcher pathMatcher, UrlPathHelper urlPathHelper) {
		if (!checkMethod(request) || !checkParams(request) || !checkHeaders(request)) {
			return null;
		}
		else {
			List<String> matchingPatterns = getMatchingPatterns(request, pathMatcher, urlPathHelper);
			if (!matchingPatterns.isEmpty()) {
				Set<RequestMethod> matchingMethods = getMatchingMethods(request);
				return new RequestKey(matchingPatterns, matchingMethods, this.paramConditions, this.headerConditions);
			}
			else {
				return null;
			}
		}
	}

	private List<String> getMatchingPatterns(HttpServletRequest request,
											 PathMatcher pathMatcher,
											 UrlPathHelper urlPathHelper) {
		String lookupPath = urlPathHelper.getLookupPathForRequest(request);

		List<String> matchingPatterns = new ArrayList<String>();
		for (String pattern : this.patterns) {
			String matchingPattern = getMatchingPattern(pattern, lookupPath, pathMatcher);
			if (matchingPattern != null) {
				matchingPatterns.add(matchingPattern);
			}
		}

		Collections.sort(matchingPatterns, pathMatcher.getPatternComparator(lookupPath));

		return matchingPatterns;
	}

	private Set<RequestMethod> getMatchingMethods(HttpServletRequest request) {
		if (this.methods.isEmpty()) {
			return this.methods;
		}
		else {
			return Collections.singleton(RequestMethod.valueOf(request.getMethod()));
		}
	}

	private boolean checkMethod(HttpServletRequest request) {
		return methods.isEmpty() || methods.contains(RequestMethod.valueOf(request.getMethod()));
	}

	private boolean checkParams(HttpServletRequest request) {
		return checkConditions(paramConditions, request);
	}

	private boolean checkHeaders(HttpServletRequest request) {
		return checkConditions(headerConditions, request);
	}

	private String getMatchingPattern(String pattern, String lookupPath, PathMatcher pathMatcher) {
		if (pattern.equals(lookupPath) || pathMatcher.match(pattern, lookupPath)) {
			return pattern;
		}
		boolean hasSuffix = pattern.indexOf('.') != -1;
		if (!hasSuffix && pathMatcher.match(pattern + ".*", lookupPath)) {
			return pattern + ".*";
		}
		boolean endsWithSlash = pattern.endsWith("/");
		if (!endsWithSlash && pathMatcher.match(pattern + "/", lookupPath)) {
			return pattern +"/";
		}
		return null;
	}
	
	private static boolean checkConditions(Set<RequestCondition> conditions, HttpServletRequest request) {
		for (RequestCondition condition : conditions) {
			if (!condition.match(request)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj != null && obj instanceof RequestKey) {
			RequestKey other = (RequestKey) obj;
			return (this.patterns.equals(other.patterns) && this.methods.equals(other.methods) &&
					this.paramConditions.equals(other.paramConditions) &&
					this.headerConditions.equals(other.headerConditions));
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = hash;
		if (result == 0) {
			result = patterns.hashCode();
			result = 31 * result + methods.hashCode();
			result = 31 * result + paramConditions.hashCode();
			result = 31 * result + headerConditions.hashCode();
			hash = result;
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("{");
		builder.append(patterns);
		if (!methods.isEmpty()) {
			builder.append(',');
			builder.append(methods);
		}
		if (!headerConditions.isEmpty()) {
			builder.append(',');
			builder.append(headerConditions);
		}
		if (!paramConditions.isEmpty()) {
			builder.append(',');
			builder.append(paramConditions);
		}
		builder.append('}');
		return builder.toString();
	}

}
