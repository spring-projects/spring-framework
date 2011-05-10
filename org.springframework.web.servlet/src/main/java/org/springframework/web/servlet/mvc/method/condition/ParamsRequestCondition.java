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

import org.springframework.web.util.WebUtils;

/**
 * Represents a collection of parameter request conditions, typically obtained from {@link
 * org.springframework.web.bind.annotation.RequestMapping#params() @RequestMapping.params()}.
 *
 * @author Arjen Poutsma
 * @see RequestConditionFactory#parseParams(String...)
 * @since 3.1
 */
public class ParamsRequestCondition
		extends LogicalConjunctionRequestCondition<ParamsRequestCondition.ParamRequestCondition>
		implements Comparable<ParamsRequestCondition> {

	private ParamsRequestCondition(Collection<ParamRequestCondition> conditions) {
		super(conditions);
	}

	ParamsRequestCondition(String... params) {
		this(parseConditions(Arrays.asList(params)));
	}

	private static Set<ParamRequestCondition> parseConditions(List<String> params) {
		Set<ParamRequestCondition> conditions = new LinkedHashSet<ParamRequestCondition>(params.size());
		for (String param : params) {
			conditions.add(new ParamRequestCondition(param));
		}
		return conditions;
	}

	/**
	 * Creates an empty set of parameter request conditions.
	 */
	public ParamsRequestCondition() {
		this(Collections.<ParamRequestCondition>emptySet());
	}

	/**
	 * Returns a new {@code RequestCondition} that contains all conditions of this key that match the request.
	 *
	 * @param request the request
	 * @return a new request condition that contains all matching attributes, or {@code null} if not all conditions match
	 */
	public ParamsRequestCondition getMatchingCondition(HttpServletRequest request) {
		return match(request) ? this : null;
	}

	/**
	 * Combines this collection of request condition with another by combining all parameter request conditions into a
	 * logical AND.
	 *
	 * @param other the condition to combine with
	 */
	public ParamsRequestCondition combine(ParamsRequestCondition other) {
		Set<ParamRequestCondition> conditions = new LinkedHashSet<ParamRequestCondition>(getConditions());
		conditions.addAll(other.getConditions());
		return new ParamsRequestCondition(conditions);
	}

	public int compareTo(ParamsRequestCondition other) {
		return other.getConditions().size() - this.getConditions().size();
	}

	static class ParamRequestCondition extends AbstractNameValueCondition<String> {

		ParamRequestCondition(String expression) {
			super(expression);
		}

		@Override
		protected String parseValue(String valueExpression) {
			return valueExpression;
		}

		@Override
		protected boolean matchName(HttpServletRequest request) {
			return WebUtils.hasSubmitParameter(request, name);
		}

		@Override
		protected boolean matchValue(HttpServletRequest request) {
			return value.equals(request.getParameter(name));
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj != null && obj instanceof ParamRequestCondition) {
				ParamRequestCondition other = (ParamRequestCondition) obj;
				return ((this.name.equals(other.name)) &&
						(this.value != null ? this.value.equals(other.value) : other.value == null) &&
						this.isNegated == other.isNegated);
			}
			return false;
		}

	}
}
