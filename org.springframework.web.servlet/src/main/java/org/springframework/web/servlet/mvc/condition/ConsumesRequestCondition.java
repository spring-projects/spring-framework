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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition.HeaderExpression;

/**
 * A logical disjunction (' || ') request condition to match requests against consumable media type expressions.
 * 
 * <p>For details on the syntax of the expressions see {@link RequestMapping#consumes()}. If the condition is 
 * created with 0 consumable media type expressions, it matches to every request.
 * 
 * <p>This request condition is also capable of parsing header expressions specifically selecting 'Content-Type'
 * header expressions and converting them to consumable media type expressions.
 * 
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ConsumesRequestCondition extends AbstractRequestCondition<ConsumesRequestCondition> {

	private final List<ConsumeMediaTypeExpression> expressions;

	/**
	 * Creates a {@link ConsumesRequestCondition} with the given consumable media type expressions.
	 * @param consumes the expressions to parse; if 0, the condition matches to every request
	 */
	public ConsumesRequestCondition(String... consumes) {
		this(consumes, null);
	}

	/**
	 * Creates a {@link ConsumesRequestCondition} with the given header and consumes expressions.
	 * In addition to consume expressions, {@code "Content-Type"} header expressions are extracted 
	 * and treated as consumable media type expressions.
	 * @param consumes the consumes expressions to parse; if 0, the condition matches to all requests
	 * @param headers the header expression to parse; if 0, the condition matches to all requests
	 */
	public ConsumesRequestCondition(String[] consumes, String[] headers) {
		this(parseExpressions(consumes, headers));
	}

	/**
	 * Private constructor.
	 */
	private ConsumesRequestCondition(Collection<ConsumeMediaTypeExpression> expressions) {
		this.expressions = new ArrayList<ConsumeMediaTypeExpression>(expressions);
		Collections.sort(this.expressions);
	}

	private static Set<ConsumeMediaTypeExpression> parseExpressions(String[] consumes, String[] headers) {
		Set<ConsumeMediaTypeExpression> result = new LinkedHashSet<ConsumeMediaTypeExpression>();
		if (headers != null) {
			for (String header : headers) {
				HeaderExpression expr = new HeaderExpression(header);
				if ("Content-Type".equalsIgnoreCase(expr.name)) {
					for (MediaType mediaType : MediaType.parseMediaTypes(expr.value)) {
						result.add(new ConsumeMediaTypeExpression(mediaType, expr.isNegated));
					}
				}
			}
		}
		if (consumes != null) {
			for (String consume : consumes) {
				result.add(new ConsumeMediaTypeExpression(consume));
			}
		}
		return result;
	}

	/**
	 * Returns the consumable media types contained in all expressions of this condition.
	 */
	public Set<MediaType> getMediaTypes() {
		Set<MediaType> result = new LinkedHashSet<MediaType>();
		for (ConsumeMediaTypeExpression expression : expressions) {
			result.add(expression.getMediaType());
		}
		return result;
	}

	/**
	 * Returns true if this condition contains 0 consumable media type expressions.
	 */
	public boolean isEmpty() {
		return expressions.isEmpty();
	}

	@Override
	protected Collection<ConsumeMediaTypeExpression> getContent() {
		return expressions;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * Returns the "other" instance provided it contains expressions; returns "this" instance otherwise.
	 * In other words "other" takes precedence over "this" as long as it has expressions.
	 * <p>Example: method-level "consumes" overrides type-level "consumes" condition. 
	 */
	public ConsumesRequestCondition combine(ConsumesRequestCondition other) {
		return !other.expressions.isEmpty() ? other : this;
	}

	/**
	 * Checks if any of the consumable media type expressions match the given request and returns an 
	 * instance that is guaranteed to contain matching media type expressions only.
	 * 
	 * @param request the current request
	 * 
	 * @return the same instance if the condition contains no expressions; 
	 * 		or a new condition with matching expressions only; or {@code null} if no expressions match.
	 */
	public ConsumesRequestCondition getMatchingCondition(HttpServletRequest request) {
		if (isEmpty()) {
			return this;
		}
		Set<ConsumeMediaTypeExpression> result = new LinkedHashSet<ConsumeMediaTypeExpression>(expressions);
		for (Iterator<ConsumeMediaTypeExpression> iterator = result.iterator(); iterator.hasNext();) {
			ConsumeMediaTypeExpression expression = iterator.next();
			if (!expression.match(request)) {
				iterator.remove();
			}
		}
		return (result.isEmpty()) ? null : new ConsumesRequestCondition(result);
	}

	/**
	 * Returns:
	 * <ul>
	 * 	<li>0 if the two conditions have the same number of expressions
	 * 	<li>Less than 1 if "this" has more in number or more specific consumable media type expressions
	 * 	<li>Greater than 1 if "other" has more in number or more specific consumable media type expressions
	 * </ul>   
	 * 
	 * <p>It is assumed that both instances have been obtained via {@link #getMatchingCondition(HttpServletRequest)}
	 * and each instance contains the matching consumable media type expression only or is otherwise empty.
	 */
	public int compareTo(ConsumesRequestCondition other, HttpServletRequest request) {
		if (expressions.isEmpty() && other.expressions.isEmpty()) {
			return 0;
		}
		else if (expressions.isEmpty()) {
			return 1;
		}
		else if (other.expressions.isEmpty()) {
			return -1;
		}
		else {
			return expressions.get(0).compareTo(other.expressions.get(0));
		}
	}

	/**
	 * Parsing and request matching logic for consumable media type expressions. 
	 * @see RequestMapping#consumes()
	 */
	static class ConsumeMediaTypeExpression extends MediaTypeExpression {

		ConsumeMediaTypeExpression(String expression) {
			super(expression);
		}

		ConsumeMediaTypeExpression(MediaType mediaType, boolean negated) {
			super(mediaType, negated);
		}

		@Override
		protected boolean match(HttpServletRequest request, MediaType mediaType) {

			MediaType contentType = StringUtils.hasLength(request.getContentType()) ?
					MediaType.parseMediaType(request.getContentType()) :
					MediaType.APPLICATION_OCTET_STREAM	;
					
			return mediaType.includes(contentType);
		}
	}

}
