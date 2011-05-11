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
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/**
 * Represents a collection of header request conditions, typically obtained from {@link
 * org.springframework.web.bind.annotation.RequestMapping#headers()  @RequestMapping.headers()}.
 *
 * @author Arjen Poutsma
 * @see RequestConditionFactory#parseHeaders(String...)
 * @since 3.1
 */
public class HeadersRequestCondition
		extends LogicalConjunctionRequestCondition<HeadersRequestCondition.HeaderRequestCondition>
		implements Comparable<HeadersRequestCondition> {

	HeadersRequestCondition(Collection<HeaderRequestCondition> conditions) {
		super(conditions);
	}

	HeadersRequestCondition(String... headers) {
		this(parseConditions(Arrays.asList(headers)));
	}

	private static Set<HeaderRequestCondition> parseConditions(Collection<String> params) {
		Set<HeaderRequestCondition> conditions = new LinkedHashSet<HeaderRequestCondition>(params.size());
		for (String param : params) {
			conditions.add(new HeaderRequestCondition(param));
		}
		return conditions;
	}

	/**
	 * Creates an empty set of header request conditions.
	 */
	public HeadersRequestCondition() {
		this(Collections.<HeaderRequestCondition>emptySet());
	}

	/**
	 * Returns a new {@code RequestCondition} that contains all conditions that match the request.
	 *
	 * @param request the request
	 * @return a new request condition that contains all matching attributes, or {@code null} if not all conditions match
	 */
	public HeadersRequestCondition getMatchingCondition(HttpServletRequest request) {
		return match(request) ? this : null;
	}


	/**
	 * Combines this collection of request condition with another by combining all header request conditions into a
	 * logical AND.
	 *
	 * @param other the condition to combine with
	 */
	public HeadersRequestCondition combine(HeadersRequestCondition other) {
		Set<HeaderRequestCondition> conditions = new LinkedHashSet<HeaderRequestCondition>(getConditions());
		conditions.addAll(other.getConditions());
		return new HeadersRequestCondition(conditions);
	}


	public int compareTo(HeadersRequestCondition other) {
		return other.getConditions().size() - this.getConditions().size();
	}

	static class HeaderRequestCondition extends AbstractNameValueCondition<String> {

		public HeaderRequestCondition(String expression) {
			super(expression);
		}

		@Override
		protected String parseValue(String valueExpression) {
			return valueExpression;
		}

		@Override
		protected boolean matchName(HttpServletRequest request) {
			return request.getHeader(name) != null;
		}

		@Override
		protected boolean matchValue(HttpServletRequest request) {
			return value.equals(request.getHeader(name));
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj != null && obj instanceof HeaderRequestCondition) {
				HeaderRequestCondition other = (HeaderRequestCondition) obj;
				return ((this.name.equalsIgnoreCase(other.name)) &&
						(this.value != null ? this.value.equals(other.value) : other.value == null) &&
						this.isNegated == other.isNegated);
			}
			return false;
		}

		@Override
		public int hashCode() {
			int result = name.toLowerCase().hashCode();
			result = 31 * result + (value != null ? value.hashCode() : 0);
			result = 31 * result + (isNegated ? 1 : 0);
			return result;
		}


	}
}
