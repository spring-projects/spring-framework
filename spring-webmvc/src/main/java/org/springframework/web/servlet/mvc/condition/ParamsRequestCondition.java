/*
 * Copyright 2002-2021 the original author or authors.
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
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.WebUtils;

/**
 * A logical conjunction ({@code ' && '}) request condition that matches a request against
 * a set parameter expressions with syntax defined in {@link RequestMapping#params()}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class ParamsRequestCondition extends AbstractRequestCondition<ParamsRequestCondition> {

	private final Set<ParamExpression> expressions;


	/**
	 * Create a new instance from the given param expressions.
	 * @param params expressions with syntax defined in {@link RequestMapping#params()};
	 * 	if 0, the condition will match to every request.
	 */
	public ParamsRequestCondition(String... params) {
		this.expressions = parseExpressions(params);
	}

	private static Set<ParamExpression> parseExpressions(String... params) {
		if (ObjectUtils.isEmpty(params)) {
			return Collections.emptySet();
		}
		Set<ParamExpression> expressions = new LinkedHashSet<>(params.length);
		for (String param : params) {
			expressions.add(new ParamExpression(param));
		}
		return expressions;
	}

	private ParamsRequestCondition(Set<ParamExpression> conditions) {
		this.expressions = conditions;
	}


	/**
	 * Return the contained request parameter expressions.
	 */
	public Set<NameValueExpression<String>> getExpressions() {
		return new LinkedHashSet<>(this.expressions);
	}

	@Override
	protected Collection<ParamExpression> getContent() {
		return this.expressions;
	}

	@Override
	protected String getToStringInfix() {
		return " && ";
	}

	/**
	 * Returns a new instance with the union of the param expressions
	 * from "this" and the "other" instance.
	 */
	@Override
	public ParamsRequestCondition combine(ParamsRequestCondition other) {
		if (isEmpty() && other.isEmpty()) {
			return this;
		}
		else if (other.isEmpty()) {
			return this;
		}
		else if (isEmpty()) {
			return other;
		}
		Set<ParamExpression> set = new LinkedHashSet<>(this.expressions);
		set.addAll(other.expressions);
		return new ParamsRequestCondition(set);
	}

	/**
	 * Returns "this" instance if the request matches all param expressions;
	 * or {@code null} otherwise.
	 */
	@Override
	@Nullable
	public ParamsRequestCondition getMatchingCondition(HttpServletRequest request) {
		for (ParamExpression expression : this.expressions) {
			if (!expression.match(request)) {
				return null;
			}
		}
		return this;
	}

	/**
	 * Compare to another condition based on parameter expressions. A condition
	 * is considered to be a more specific match, if it has:
	 * <ol>
	 * <li>A greater number of expressions.
	 * <li>A greater number of non-negated expressions with a concrete value.
	 * </ol>
	 * <p>It is assumed that both instances have been obtained via
	 * {@link #getMatchingCondition(HttpServletRequest)} and each instance
	 * contains the matching parameter expressions only or is otherwise empty.
	 */
	@Override
	public int compareTo(ParamsRequestCondition other, HttpServletRequest request) {
		int result = other.expressions.size() - this.expressions.size();
		if (result != 0) {
			return result;
		}
		return (int) (getValueMatchCount(other.expressions) - getValueMatchCount(this.expressions));
	}

	private long getValueMatchCount(Set<ParamExpression> expressions) {
		long count = 0;
		for (ParamExpression e : expressions) {
			if (e.getValue() != null && !e.isNegated()) {
				count++;
			}
		}
		return count;
	}


	/**
	 * Parses and matches a single param expression to a request.
	 */
	static class ParamExpression extends AbstractNameValueExpression<String> {

		private final Set<String> namesToMatch = new HashSet<>(WebUtils.SUBMIT_IMAGE_SUFFIXES.length + 1);


		ParamExpression(String expression) {
			super(expression);
			this.namesToMatch.add(getName());
			for (String suffix : WebUtils.SUBMIT_IMAGE_SUFFIXES) {
				this.namesToMatch.add(getName() + suffix);
			}
		}

		@Override
		protected boolean isCaseSensitiveName() {
			return true;
		}

		@Override
		protected String parseValue(String valueExpression) {
			return valueExpression;
		}

		@Override
		protected boolean matchName(HttpServletRequest request) {
			for (String current : this.namesToMatch) {
				if (request.getParameterMap().get(current) != null) {
					return true;
				}
			}
			return request.getParameterMap().containsKey(this.name);
		}

		@Override
		protected boolean matchValue(HttpServletRequest request) {
			return ObjectUtils.nullSafeEquals(this.value, request.getParameter(this.name));
		}
	}

}
