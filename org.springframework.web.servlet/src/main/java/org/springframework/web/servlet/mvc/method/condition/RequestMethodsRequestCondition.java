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

package org.springframework.web.servlet.mvc.method.condition;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Represents a collection of {@link RequestMethod} conditions, typically obtained from {@link
 * org.springframework.web.bind.annotation.RequestMapping#method() @RequestMapping.methods()}.
 *
 * @author Arjen Poutsma
 * @see RequestConditionFactory#parseMethods(RequestMethod...)
 * @since 3.1
 */
public class RequestMethodsRequestCondition
		extends LogicalDisjunctionRequestCondition<RequestMethodsRequestCondition.RequestMethodRequestCondition>
		implements Comparable<RequestMethodsRequestCondition> {

	private RequestMethodsRequestCondition(Collection<RequestMethodRequestCondition> conditions) {
		super(conditions);
	}

	RequestMethodsRequestCondition(RequestMethod... methods) {
		this(parseConditions(Arrays.asList(methods)));
	}

	private static Set<RequestMethodRequestCondition> parseConditions(List<RequestMethod> methods) {
		Set<RequestMethodRequestCondition> conditions =
				new LinkedHashSet<RequestMethodRequestCondition>(methods.size());
		for (RequestMethod method : methods) {
			conditions.add(new RequestMethodRequestCondition(method));
		}
		return conditions;
	}


	/**
	 * Creates an empty set of method request conditions.
	 */
	public RequestMethodsRequestCondition() {
		this(Collections.<RequestMethodRequestCondition>emptySet());
	}

	/**
	 * Returns all {@link RequestMethod}s contained in this condition.
	 */
	public Set<RequestMethod> getMethods() {
		Set<RequestMethod> result = new LinkedHashSet<RequestMethod>();
		for (RequestMethodRequestCondition condition : getConditions()) {
			result.add(condition.getMethod());
		}
		return result;
	}

	public int compareTo(RequestMethodsRequestCondition other) {
		return other.getConditions().size() - this.getConditions().size();
	}

	/**
	 * Returns a new {@code RequestMethodsRequestCondition} that contains all conditions that match the request.
	 *
	 * @param request the request
	 * @return a new request condition that contains all matching attributes, or {@code null} if not all conditions match
	 */
	public RequestMethodsRequestCondition getMatchingCondition(HttpServletRequest request) {
		if (isEmpty()) {
			return this;
		}
		else {
			if (match(request)) {
				return new RequestMethodsRequestCondition(RequestMethod.valueOf(request.getMethod()));
			}
			else {
				return null;
			}
		}

	}

	/**
	 * Combines this collection of request method conditions with another by combining all methods into a logical OR.
	 *
	 * @param other the condition to combine with
	 */
	public RequestMethodsRequestCondition combine(RequestMethodsRequestCondition other) {
		Set<RequestMethodRequestCondition> conditions =
				new LinkedHashSet<RequestMethodRequestCondition>(getConditions());
		conditions.addAll(other.getConditions());
		return new RequestMethodsRequestCondition(conditions);
	}

	static class RequestMethodRequestCondition implements RequestCondition {

		private final RequestMethod method;

		RequestMethodRequestCondition(RequestMethod method) {
			this.method = method;
		}

		RequestMethod getMethod() {
			return method;
		}

		public boolean match(HttpServletRequest request) {
			RequestMethod method = RequestMethod.valueOf(request.getMethod());
			return this.method.equals(method);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj != null && obj instanceof RequestMethodRequestCondition) {
				RequestMethodRequestCondition other = (RequestMethodRequestCondition) obj;
				return this.method.equals(other.method);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return method.hashCode();
		}

		@Override
		public String toString() {
			return method.toString();
		}
	}
}
