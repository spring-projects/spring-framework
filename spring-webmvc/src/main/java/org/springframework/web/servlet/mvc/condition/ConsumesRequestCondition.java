/*
 * Copyright 2002-2012 the original author or authors.
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
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition.HeaderExpression;

/**
 * A logical disjunction (' || ') request condition to match a request's
 * 'Content-Type' header to a list of media type expressions. Two kinds of
 * media type expressions are supported, which are described in
 * {@link RequestMapping#consumes()} and {@link RequestMapping#headers()}
 * where the header name is 'Content-Type'. Regardless of which syntax is
 * used, the semantics are the same.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class ConsumesRequestCondition extends AbstractRequestCondition<ConsumesRequestCondition> {

	private final List<ConsumeMediaTypeExpression> expressions;

	/**
	 * Creates a new instance from 0 or more "consumes" expressions.
	 * @param consumes expressions with the syntax described in
	 * {@link RequestMapping#consumes()}; if 0 expressions are provided,
	 * the condition will match to every request.
	 */
	public ConsumesRequestCondition(String... consumes) {
		this(consumes, null);
	}

	/**
	 * Creates a new instance with "consumes" and "header" expressions.
	 * "Header" expressions where the header name is not 'Content-Type' or have
	 * no header value defined are ignored. If 0 expressions are provided in
	 * total, the condition will match to every request
	 * @param consumes as described in {@link RequestMapping#consumes()}
	 * @param headers as described in {@link RequestMapping#headers()}
	 */
	public ConsumesRequestCondition(String[] consumes, String[] headers) {
		this(parseExpressions(consumes, headers));
	}

	/**
	 * Private constructor accepting parsed media type expressions.
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
	 * Return the contained MediaType expressions.
	 */
	public Set<MediaTypeExpression> getExpressions() {
		return new LinkedHashSet<MediaTypeExpression>(this.expressions);
	}

	/**
	 * Returns the media types for this condition excluding negated expressions.
	 */
	public Set<MediaType> getConsumableMediaTypes() {
		Set<MediaType> result = new LinkedHashSet<MediaType>();
		for (ConsumeMediaTypeExpression expression : this.expressions) {
			if (!expression.isNegated()) {
				result.add(expression.getMediaType());
			}
		}
		return result;
	}

	/**
	 * Whether the condition has any media type expressions.
	 */
	public boolean isEmpty() {
		return this.expressions.isEmpty();
	}

	@Override
	protected Collection<ConsumeMediaTypeExpression> getContent() {
		return this.expressions;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * Returns the "other" instance if it has any expressions; returns "this"
	 * instance otherwise. Practically that means a method-level "consumes"
	 * overrides a type-level "consumes" condition.
	 */
	public ConsumesRequestCondition combine(ConsumesRequestCondition other) {
		return !other.expressions.isEmpty() ? other : this;
	}

	/**
	 * Checks if any of the contained media type expressions match the given
	 * request 'Content-Type' header and returns an instance that is guaranteed
	 * to contain matching expressions only. The match is performed via
	 * {@link MediaType#includes(MediaType)}.
	 *
	 * @param request the current request
	 *
	 * @return the same instance if the condition contains no expressions;
	 * 		or a new condition with matching expressions only;
	 * 		or {@code null} if no expressions match.
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
	 * 	<li>Less than 0 if "this" has more or more specific media type expressions
	 * 	<li>Greater than 0 if "other" has more or more specific media type expressions
	 * </ul>
	 *
	 * <p>It is assumed that both instances have been obtained via
	 * {@link #getMatchingCondition(HttpServletRequest)} and each instance contains
	 * the matching consumable media type expression only or is otherwise empty.
	 */
	public int compareTo(ConsumesRequestCondition other, HttpServletRequest request) {
		if (this.expressions.isEmpty() && other.expressions.isEmpty()) {
			return 0;
		}
		else if (this.expressions.isEmpty()) {
			return 1;
		}
		else if (other.expressions.isEmpty()) {
			return -1;
		}
		else {
			return this.expressions.get(0).compareTo(other.expressions.get(0));
		}
	}

	/**
	 * Parses and matches a single media type expression to a request's 'Content-Type' header.
	 */
	static class ConsumeMediaTypeExpression extends AbstractMediaTypeExpression {

		ConsumeMediaTypeExpression(String expression) {
			super(expression);
		}

		ConsumeMediaTypeExpression(MediaType mediaType, boolean negated) {
			super(mediaType, negated);
		}

		@Override
		protected boolean matchMediaType(HttpServletRequest request) throws HttpMediaTypeNotSupportedException {
			try {
				MediaType contentType = StringUtils.hasLength(request.getContentType()) ?
						MediaType.parseMediaType(request.getContentType()) :
						MediaType.APPLICATION_OCTET_STREAM;
						return getMediaType().includes(contentType);
			}
			catch (IllegalArgumentException ex) {
				throw new HttpMediaTypeNotSupportedException(
						"Can't parse Content-Type [" + request.getContentType() + "]: " + ex.getMessage());
			}
		}
	}

}
