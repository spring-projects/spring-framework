/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.reactive.result.condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

/**
 * A logical disjunction (' || ') request condition to match a request's
 * 'Content-Type' header to a list of media type expressions. Two kinds of
 * media type expressions are supported, which are described in
 * {@link RequestMapping#consumes()} and {@link RequestMapping#headers()}
 * where the header name is 'Content-Type'. Regardless of which syntax is
 * used, the semantics are the same.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class ConsumesRequestCondition extends AbstractRequestCondition<ConsumesRequestCondition> {

	private static final ConsumesRequestCondition EMPTY_CONDITION = new ConsumesRequestCondition();


	private final List<ConsumeMediaTypeExpression> expressions;

	private boolean bodyRequired = true;


	/**
	 * Creates a new instance from 0 or more "consumes" expressions.
	 * @param consumes expressions with the syntax described in
	 * {@link RequestMapping#consumes()}; if 0 expressions are provided,
	 * the condition will match to every request
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
		this.expressions = new ArrayList<>(parseExpressions(consumes, headers));
		if (this.expressions.size() > 1) {
			Collections.sort(this.expressions);
		}
	}

	/**
	 * Private constructor for internal when creating matching conditions.
	 * Note the expressions List is neither sorted nor deep copied.
	 */
	private ConsumesRequestCondition(List<ConsumeMediaTypeExpression> expressions) {
		this.expressions = expressions;
	}


	private static Set<ConsumeMediaTypeExpression> parseExpressions(String[] consumes, String[] headers) {
		Set<ConsumeMediaTypeExpression> result = new LinkedHashSet<>();
		if (headers != null) {
			for (String header : headers) {
				HeadersRequestCondition.HeaderExpression expr = new HeadersRequestCondition.HeaderExpression(header);
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
		return new LinkedHashSet<>(this.expressions);
	}

	/**
	 * Returns the media types for this condition excluding negated expressions.
	 */
	public Set<MediaType> getConsumableMediaTypes() {
		Set<MediaType> result = new LinkedHashSet<>();
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
	@Override
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
	 * Whether this condition should expect requests to have a body.
	 * <p>By default this is set to {@code true} in which case it is assumed a
	 * request body is required and this condition matches to the "Content-Type"
	 * header or falls back on "Content-Type: application/octet-stream".
	 * <p>If set to {@code false}, and the request does not have a body, then this
	 * condition matches automatically, i.e. without checking expressions.
	 * @param bodyRequired whether requests are expected to have a body
	 * @since 5.2
	 */
	public void setBodyRequired(boolean bodyRequired) {
		this.bodyRequired = bodyRequired;
	}

	/**
	 * Return the setting for {@link #setBodyRequired(boolean)}.
	 * @since 5.2
	 */
	public boolean isBodyRequired() {
		return this.bodyRequired;
	}


	/**
	 * Returns the "other" instance if it has any expressions; returns "this"
	 * instance otherwise. Practically that means a method-level "consumes"
	 * overrides a type-level "consumes" condition.
	 */
	@Override
	public ConsumesRequestCondition combine(ConsumesRequestCondition other) {
		return (!other.expressions.isEmpty() ? other : this);
	}

	/**
	 * Checks if any of the contained media type expressions match the given
	 * request 'Content-Type' header and returns an instance that is guaranteed
	 * to contain matching expressions only. The match is performed via
	 * {@link MediaType#includes(MediaType)}.
	 * @param exchange the current exchange
	 * @return the same instance if the condition contains no expressions;
	 * or a new condition with matching expressions only;
	 * or {@code null} if no expressions match.
	 */
	@Override
	public ConsumesRequestCondition getMatchingCondition(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		if (CorsUtils.isPreFlightRequest(request)) {
			return EMPTY_CONDITION;
		}
		if (isEmpty()) {
			return this;
		}
		if (!hasBody(request) && !this.bodyRequired) {
			return EMPTY_CONDITION;
		}
		List<ConsumeMediaTypeExpression> result = getMatchingExpressions(exchange);
		return !CollectionUtils.isEmpty(result) ? new ConsumesRequestCondition(result) : null;
	}

	private boolean hasBody(ServerHttpRequest request) {
		String contentLength = request.getHeaders().getFirst(HttpHeaders.CONTENT_LENGTH);
		String transferEncoding = request.getHeaders().getFirst(HttpHeaders.TRANSFER_ENCODING);
		return StringUtils.hasText(transferEncoding) ||
				(StringUtils.hasText(contentLength) && !contentLength.trim().equals("0"));
	}

	@Nullable
	private List<ConsumeMediaTypeExpression> getMatchingExpressions(ServerWebExchange exchange) {
		List<ConsumeMediaTypeExpression> result = null;
		for (ConsumeMediaTypeExpression expression : this.expressions) {
			if (expression.match(exchange)) {
				result = result != null ? result : new ArrayList<>();
				result.add(expression);
			}
		}
		return result;
	}

	/**
	 * Returns:
	 * <ul>
	 * <li>0 if the two conditions have the same number of expressions
	 * <li>Less than 0 if "this" has more or more specific media type expressions
	 * <li>Greater than 0 if "other" has more or more specific media type expressions
	 * </ul>
	 * <p>It is assumed that both instances have been obtained via
	 * {@link #getMatchingCondition(ServerWebExchange)} and each instance contains
	 * the matching consumable media type expression only or is otherwise empty.
	 */
	@Override
	public int compareTo(ConsumesRequestCondition other, ServerWebExchange exchange) {
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
		protected boolean matchMediaType(ServerWebExchange exchange) throws UnsupportedMediaTypeStatusException {
			try {
				MediaType contentType = exchange.getRequest().getHeaders().getContentType();
				contentType = (contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM);
				return getMediaType().includes(contentType);
			}
			catch (InvalidMediaTypeException ex) {
				throw new UnsupportedMediaTypeStatusException("Can't parse Content-Type [" +
						exchange.getRequest().getHeaders().getFirst("Content-Type") +
						"]: " + ex.getMessage());
			}
		}
	}

}
