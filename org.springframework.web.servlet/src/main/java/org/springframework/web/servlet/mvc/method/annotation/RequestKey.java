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
import org.springframework.web.servlet.mvc.method.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.condition.RequestConditionFactory;
import org.springframework.web.util.UrlPathHelper;

/**
 * Contains a set of conditions to match to a given request such as URL patterns, HTTP methods, request 
 * parameters and headers. 
 * 
 * <p>A {@link RequestKey} can be combined with another {@link RequestKey} resulting in a new {@link RequestKey}
 * with conditions from both (see {@link #combine(RequestKey, PathMatcher)}).
 * 
 * <p>A {@link RequestKey} can be matched to a request resulting in a new {@link RequestKey} with the subset of
 * conditions relevant to the request (see {@link #getMatchingKey(HttpServletRequest, PathMatcher, UrlPathHelper)}).
 * 
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class RequestKey {

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
	RequestKey(Collection<String> patterns, Collection<RequestMethod> methods) {
		this(patterns, methods, null, null, null);
	}

	/**
	 * Creates a new {@code RequestKey} instance with a full set of conditions.
	 * 
	 * <p>Package protected for testing purposes.
	 */
	RequestKey(Collection<String> patterns,
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
						RequestConditionFactory.parseHeaders(annotation.headers()),
						RequestConditionFactory.parseConsumes(annotation.consumes())
				);
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
		return new RequestKey(Collections.singleton(lookupPath), Collections.singleton(method));
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
	 * 	  <li>If both keys have path patterns combine them according to the rules of the given {@link PathMatcher}.
	 * 	  <li>If either key contains path patterns but not both use only what is available.
	 * 	  <li>If neither key contains path patterns use "/".
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
	public RequestKey combine(RequestKey methodKey, PathMatcher pathMatcher) {
		Set<String> patterns = combinePatterns(this.patterns, methodKey.patterns, pathMatcher);
		Set<RequestMethod> methods = union(this.methods, methodKey.methods);
		RequestCondition params = RequestConditionFactory.and(this.paramsCondition, methodKey.paramsCondition);
		RequestCondition headers = RequestConditionFactory.and(this.headersCondition, methodKey.headersCondition);
		RequestCondition consumes = RequestConditionFactory.mostSpecific(methodKey.consumesCondition, this.consumesCondition);

		return new RequestKey(patterns, methods, params, headers, consumes);
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
	 * Returns a new {@code RequestKey} that contains all conditions of this key that are relevant to the request.
	 * <ul>
	 * <li>The list of URL path patterns is trimmed to contain the patterns that match the URL with matching patterns 
	 * sorted via {@link PathMatcher#getPatternComparator(String)}. 
	 * <li>The list of HTTP methods is trimmed to contain only the method of the request. 
	 * <li>Request parameter and request header conditions are included in full. 
	 * <li>The list of consumes conditions is trimmed and sorted to match the request "Content-Type" header.
	 * </ul>   
	 * @param request the current request
	 * @param pathMatcher to check for matching patterns
	 * @param urlPathHelper to derive the lookup path for the request
	 * @return a new request key that contains all matching attributes, or {@code null} if not all conditions match
	 */
	public RequestKey getMatchingKey(HttpServletRequest request, PathMatcher pathMatcher, UrlPathHelper urlPathHelper) {
		if (!checkMethod(request) || !paramsCondition.match(request) || !headersCondition.match(request) ||
				!consumesCondition.match(request)) {
			return null;
		}
		else {
			List<String> matchingPatterns = getMatchingPatterns(request, pathMatcher, urlPathHelper);
			if (!matchingPatterns.isEmpty()) {
				Set<RequestMethod> matchingMethods = getMatchingMethod(request);
				return new RequestKey(matchingPatterns, matchingMethods, this.paramsCondition, this.headersCondition,
						this.consumesCondition);
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
		if (obj != null && obj instanceof RequestKey) {
			RequestKey other = (RequestKey) obj;
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
