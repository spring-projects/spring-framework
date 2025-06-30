/*
 * Copyright 2002-present the original author or authors.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsUtils;

/**
 * A logical disjunction (' || ') request condition that matches a request
 * against a set of {@link RequestMethod RequestMethods}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class RequestMethodsRequestCondition extends AbstractRequestCondition<RequestMethodsRequestCondition> {

	/** Per HTTP method cache to return ready instances from getMatchingCondition. */
	private static final Map<String, RequestMethodsRequestCondition> requestMethodConditionCache;

	static {
		requestMethodConditionCache = CollectionUtils.newHashMap(RequestMethod.values().length);
		for (RequestMethod method : RequestMethod.values()) {
			requestMethodConditionCache.put(method.name(), new RequestMethodsRequestCondition(method));
		}
	}


	private final Set<RequestMethod> methods;


	/**
	 * Create a new instance with the given request methods.
	 * @param requestMethods 0 or more HTTP request methods;
	 * if, 0 the condition will match to every request
	 */
	public RequestMethodsRequestCondition(RequestMethod... requestMethods) {
		this.methods = (ObjectUtils.isEmpty(requestMethods) ?
				Collections.emptySet() : new LinkedHashSet<>(Arrays.asList(requestMethods)));
	}

	/**
	 * Private constructor for internal use when combining conditions.
	 */
	private RequestMethodsRequestCondition(Set<RequestMethod> methods) {
		this.methods = methods;
	}


	/**
	 * Returns all {@link RequestMethod RequestMethods} contained in this condition.
	 */
	public Set<RequestMethod> getMethods() {
		return this.methods;
	}

	@Override
	protected Collection<RequestMethod> getContent() {
		return this.methods;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * Returns a new instance with a union of the HTTP request methods
	 * from "this" and the "other" instance.
	 */
	@Override
	public RequestMethodsRequestCondition combine(RequestMethodsRequestCondition other) {
		if (isEmpty() && other.isEmpty()) {
			return this;
		}
		else if (other.isEmpty()) {
			return this;
		}
		else if (isEmpty()) {
			return other;
		}
		Set<RequestMethod> set = new LinkedHashSet<>(this.methods);
		set.addAll(other.methods);
		return new RequestMethodsRequestCondition(set);
	}

	/**
	 * Check if any of the HTTP request methods match the given request and
	 * return an instance that contains the matching HTTP request method only.
	 * @param request the current request
	 * @return the same instance if the condition is empty (unless the request
	 * method is HTTP OPTIONS), a new condition with the matched request method,
	 * or {@code null} if there is no match or the condition is empty and the
	 * request method is OPTIONS.
	 */
	@Override
	public @Nullable RequestMethodsRequestCondition getMatchingCondition(HttpServletRequest request) {
		if (CorsUtils.isPreFlightRequest(request)) {
			return matchPreFlight(request);
		}

		if (getMethods().isEmpty()) {
			if (RequestMethod.OPTIONS.name().equals(request.getMethod()) &&
					!DispatcherType.ERROR.equals(request.getDispatcherType())) {

				return null; // We handle OPTIONS transparently, so don't match if no explicit declarations
			}
			return this;
		}

		return matchRequestMethod(request.getMethod());
	}

	/**
	 * On a pre-flight request match to the would-be, actual request.
	 * Hence empty conditions is a match, otherwise try to match to the HTTP
	 * method in the "Access-Control-Request-Method" header.
	 */
	private @Nullable RequestMethodsRequestCondition matchPreFlight(HttpServletRequest request) {
		if (getMethods().isEmpty()) {
			return this;
		}
		String expectedMethod = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
		return matchRequestMethod(expectedMethod);
	}

	private @Nullable RequestMethodsRequestCondition matchRequestMethod(String httpMethodValue) {
		RequestMethod requestMethod = RequestMethod.resolve(httpMethodValue);
		if (requestMethod != null) {
			if (getMethods().contains(requestMethod)) {
				return requestMethodConditionCache.get(httpMethodValue);
			}
			if (requestMethod.equals(RequestMethod.HEAD) && getMethods().contains(RequestMethod.GET)) {
				return requestMethodConditionCache.get(HttpMethod.GET.name());
			}
		}
		return null;
	}

	/**
	 * Returns:
	 * <ul>
	 * <li>0 if the two conditions contain the same number of HTTP request methods
	 * <li>Less than 0 if "this" instance has an HTTP request method but "other" doesn't
	 * <li>Greater than 0 "other" has an HTTP request method but "this" doesn't
	 * </ul>
	 * <p>It is assumed that both instances have been obtained via
	 * {@link #getMatchingCondition(HttpServletRequest)} and therefore each instance
	 * contains the matching HTTP request method only or is otherwise empty.
	 */
	@Override
	public int compareTo(RequestMethodsRequestCondition other, HttpServletRequest request) {
		if (other.methods.size() != this.methods.size()) {
			return other.methods.size() - this.methods.size();
		}
		else if (this.methods.size() == 1) {
			if (this.methods.contains(RequestMethod.HEAD) && other.methods.contains(RequestMethod.GET)) {
				return -1;
			}
			else if (this.methods.contains(RequestMethod.GET) && other.methods.contains(RequestMethod.HEAD)) {
				return 1;
			}
		}
		return 0;
	}

}
