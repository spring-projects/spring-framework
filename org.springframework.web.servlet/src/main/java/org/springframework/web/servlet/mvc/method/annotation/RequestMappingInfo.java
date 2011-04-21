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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.condition.RequestConditionFactory;

/**
 * Contains a set of conditions to match to a given request such as URL patterns, HTTP methods, request parameters 
 * and headers. 
 * 
 * <p>Two {@link RequestMappingInfo}s can be combined resulting in a new {@link RequestMappingInfo} with conditions
 * from both. A {@link RequestMappingInfo} can also match itself to an HTTP request resulting in a new 
 * {@link RequestMappingInfo} with the subset of conditions relevant to the request.
 * 
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class RequestMappingInfo {

	private final Set<String> patterns;

	private final Set<RequestMethod> methods;

	private final RequestCondition paramsCondition;

	private final RequestCondition headersCondition;

	private final RequestCondition consumesCondition;

	private int hash;

	/**
	 * Creates a new {@code RequestKey} instance with the given URL patterns and HTTP methods.
	 * 
	 * <p>Package protected for testing purposes.
	 */
	RequestMappingInfo(Collection<String> patterns, Collection<RequestMethod> methods) {
		this(patterns, methods, null, null, null);
	}

	/**
	 * Creates a new {@code RequestKey} instance with a full set of conditions.
	 */
	public RequestMappingInfo(Collection<String> patterns,
							 Collection<RequestMethod> methods,
							 RequestCondition paramsCondition,
							 RequestCondition headersCondition,
							 RequestCondition consumesCondition) {
		this.patterns = asUnmodifiableSet(prependLeadingSlash(patterns));
		this.methods = asUnmodifiableSet(methods);
		this.paramsCondition = paramsCondition != null ? paramsCondition : RequestConditionFactory.trueCondition();
		this.headersCondition = headersCondition != null ? headersCondition : RequestConditionFactory.trueCondition();
		this.consumesCondition = consumesCondition != null ? consumesCondition : RequestConditionFactory.trueCondition();
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

	private static <T> Set<T> asUnmodifiableSet(Collection<T> collection) {
		if (collection == null) {
			return Collections.emptySet();
		}
		Set<T> result = new LinkedHashSet<T>(collection);
		return Collections.unmodifiableSet(result);
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
	public RequestCondition getParams() {
		return paramsCondition;
	}

	/**
	 * Returns the request headers of this request key.
	 */
	public RequestCondition getHeaders() {
		return headersCondition;
	}

	/**
	 * Combines this {@code RequestKey} with another as follows: 
	 * <ul>
	 * <li>URL patterns:
	 * 	<ul>
	 * 	  <li>If both have patterns combine them according to the rules of the given {@link PathMatcher}
	 * 	  <li>If either contains patterns, but not both, use the available pattern
	 * 	  <li>If neither contains patterns use ""
	 * 	</ul>
	 * <li>HTTP methods are combined as union of all HTTP methods listed in both keys.
	 * <li>Request parameter are combined into a logical AND.
	 * <li>Request header are combined into a logical AND.
	 * <li>Consumes .. TODO
	 * </ul>
	 * @param methodKey the key to combine with
	 * @param pathMatcher to {@linkplain PathMatcher#combine(String, String) combine} the patterns
	 * @return a new request key containing conditions from both keys
	 */
	public RequestMappingInfo combine(RequestMappingInfo methodKey, PathMatcher pathMatcher) {
		Set<String> patterns = combinePatterns(this.patterns, methodKey.patterns, pathMatcher);
		Set<RequestMethod> methods = union(this.methods, methodKey.methods);
		RequestCondition params = RequestConditionFactory.and(this.paramsCondition, methodKey.paramsCondition);
		RequestCondition headers = RequestConditionFactory.and(this.headersCondition, methodKey.headersCondition);
		RequestCondition consumes = RequestConditionFactory.mostSpecific(methodKey.consumesCondition, this.consumesCondition);

		return new RequestMappingInfo(patterns, methods, params, headers, consumes);
	}

	private static Set<String> combinePatterns(Collection<String> typePatterns,
											   Collection<String> methodPatterns,
											   PathMatcher pathMatcher) {
		Set<String> result = new LinkedHashSet<String>();
		if (!typePatterns.isEmpty() && !methodPatterns.isEmpty()) {
			for (String pattern1 : typePatterns) {
				for (String pattern2 : methodPatterns) {
					result.add(pathMatcher.combine(pattern1, pattern2));
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
	 * Returns a new {@code RequestKey} that contains all conditions of this key that are relevant to the request.
	 * <ul>
	 * <li>The list of URL path patterns is trimmed to contain the patterns that match the URL with matching patterns 
	 * sorted via {@link PathMatcher#getPatternComparator(String)}. 
	 * <li>The list of HTTP methods is trimmed to contain only the method of the request. 
	 * <li>Request parameter and request header conditions are included in full. 
	 * <li>The list of consumes conditions is trimmed and sorted to match the request "Content-Type" header.
	 * </ul>   
	 * @param lookupPath mapping lookup path within the current servlet mapping if applicable
	 * @param request the current request
	 * @param pathMatcher to check for matching patterns
	 * @return a new request key that contains all matching attributes, or {@code null} if not all conditions match
	 */
	public RequestMappingInfo getMatchingRequestMapping(String lookupPath, HttpServletRequest request, PathMatcher pathMatcher) {
		if (!checkMethod(request) || !paramsCondition.match(request) || !headersCondition.match(request) ||
				!consumesCondition.match(request)) {
			return null;
		}
		else {
			List<String> matchingPatterns = getMatchingPatterns(lookupPath, request, pathMatcher);
			if (!matchingPatterns.isEmpty()) {
				Set<RequestMethod> matchingMethods = getMatchingMethod(request);
				return new RequestMappingInfo(matchingPatterns, matchingMethods, this.paramsCondition, this.headersCondition,
						this.consumesCondition);
			}
			else {
				return null;
			}
		}
	}

	private List<String> getMatchingPatterns(String lookupPath, 
											 HttpServletRequest request,
											 PathMatcher pathMatcher) {

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

	private Set<RequestMethod> getMatchingMethod(HttpServletRequest request) {
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

	private String getMatchingPattern(String pattern, String lookupPath, PathMatcher pathMatcher) {
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

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj != null && obj instanceof RequestMappingInfo) {
			RequestMappingInfo other = (RequestMappingInfo) obj;
			return (this.patterns.equals(other.patterns) && this.methods.equals(other.methods) &&
					this.paramsCondition.equals(other.paramsCondition) &&
					this.headersCondition.equals(other.headersCondition));
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = hash;
		if (result == 0) {
			result = patterns.hashCode();
			result = 31 * result + methods.hashCode();
			result = 31 * result + paramsCondition.hashCode();
			result = 31 * result + headersCondition.hashCode();
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
		builder.append(",params=").append(paramsCondition.toString());
		builder.append(",headers=").append(headersCondition.toString());
		builder.append(",consumes=").append(consumesCondition.toString());
		builder.append('}');
		return builder.toString();
	}

}
