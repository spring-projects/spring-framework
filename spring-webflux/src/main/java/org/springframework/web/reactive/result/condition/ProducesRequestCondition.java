/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.reactive.accept.HeaderContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

/**
 * A logical disjunction (' || ') request condition to match a request's 'Accept' header
 * to a list of media type expressions. Two kinds of media type expressions are
 * supported, which are described in {@link RequestMapping#produces()} and
 * {@link RequestMapping#headers()} where the header name is 'Accept'.
 * Regardless of which syntax is used, the semantics are the same.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class ProducesRequestCondition extends AbstractRequestCondition<ProducesRequestCondition> {

	private static final ProducesRequestCondition EMPTY_CONDITION = new ProducesRequestCondition();


	private final List<ProduceMediaTypeExpression> mediaTypeAllList =
			Collections.singletonList(new ProduceMediaTypeExpression(MediaType.ALL_VALUE));

	private final List<ProduceMediaTypeExpression> expressions;

	private final RequestedContentTypeResolver contentTypeResolver;


	/**
	 * Creates a new instance from "produces" expressions. If 0 expressions
	 * are provided in total, this condition will match to any request.
	 * @param produces expressions with syntax defined by {@link RequestMapping#produces()}
	 */
	public ProducesRequestCondition(String... produces) {
		this(produces, null);
	}

	/**
	 * Creates a new instance with "produces" and "header" expressions. "Header"
	 * expressions where the header name is not 'Accept' or have no header value
	 * defined are ignored. If 0 expressions are provided in total, this condition
	 * will match to any request.
	 * @param produces expressions with syntax defined by {@link RequestMapping#produces()}
	 * @param headers expressions with syntax defined by {@link RequestMapping#headers()}
	 */
	public ProducesRequestCondition(String[] produces, String[] headers) {
		this(produces, headers, null);
	}

	/**
	 * Same as {@link #ProducesRequestCondition(String[], String[])} but also
	 * accepting a {@link ContentNegotiationManager}.
	 * @param produces expressions with syntax defined by {@link RequestMapping#produces()}
	 * @param headers expressions with syntax defined by {@link RequestMapping#headers()}
	 * @param resolver used to determine requested content type
	 */
	public ProducesRequestCondition(String[] produces, String[] headers, RequestedContentTypeResolver resolver) {
		this.expressions = new ArrayList<>(parseExpressions(produces, headers));
		Collections.sort(this.expressions);
		this.contentTypeResolver = (resolver != null ? resolver : new HeaderContentTypeResolver());
	}

	/**
	 * Private constructor with already parsed media type expressions.
	 */
	private ProducesRequestCondition(Collection<ProduceMediaTypeExpression> expressions,
			RequestedContentTypeResolver resolver) {

		this.expressions = new ArrayList<>(expressions);
		Collections.sort(this.expressions);
		this.contentTypeResolver = (resolver != null ? resolver : new RequestedContentTypeResolverBuilder().build());
	}


	private Set<ProduceMediaTypeExpression> parseExpressions(String[] produces, String[] headers) {
		Set<ProduceMediaTypeExpression> result = new LinkedHashSet<>();
		if (headers != null) {
			for (String header : headers) {
				HeadersRequestCondition.HeaderExpression expr = new HeadersRequestCondition.HeaderExpression(header);
				if ("Accept".equalsIgnoreCase(expr.name)) {
					for (MediaType mediaType : MediaType.parseMediaTypes(expr.value)) {
						result.add(new ProduceMediaTypeExpression(mediaType, expr.isNegated));
					}
				}
			}
		}
		if (produces != null) {
			for (String produce : produces) {
				result.add(new ProduceMediaTypeExpression(produce));
			}
		}
		return result;
	}

	/**
	 * Return the contained "produces" expressions.
	 */
	public Set<MediaTypeExpression> getExpressions() {
		return new LinkedHashSet<>(this.expressions);
	}

	/**
	 * Return the contained producible media types excluding negated expressions.
	 */
	public Set<MediaType> getProducibleMediaTypes() {
		Set<MediaType> result = new LinkedHashSet<>();
		for (ProduceMediaTypeExpression expression : this.expressions) {
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
	protected List<ProduceMediaTypeExpression> getContent() {
		return this.expressions;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * Returns the "other" instance if it has any expressions; returns "this"
	 * instance otherwise. Practically that means a method-level "produces"
	 * overrides a type-level "produces" condition.
	 */
	@Override
	public ProducesRequestCondition combine(ProducesRequestCondition other) {
		return (!other.expressions.isEmpty() ? other : this);
	}

	/**
	 * Checks if any of the contained media type expressions match the given
	 * request 'Content-Type' header and returns an instance that is guaranteed
	 * to contain matching expressions only. The match is performed via
	 * {@link MediaType#isCompatibleWith(MediaType)}.
	 * @param exchange the current exchange
	 * @return the same instance if there are no expressions;
	 * or a new condition with matching expressions;
	 * or {@code null} if no expressions match.
	 */
	@Override
	@Nullable
	public ProducesRequestCondition getMatchingCondition(ServerWebExchange exchange) {
		if (CorsUtils.isPreFlightRequest(exchange.getRequest())) {
			return EMPTY_CONDITION;
		}
		if (isEmpty()) {
			return this;
		}
		Set<ProduceMediaTypeExpression> result = new LinkedHashSet<>(this.expressions);
		result.removeIf(expression -> !expression.match(exchange));
		if (!result.isEmpty()) {
			return new ProducesRequestCondition(result, this.contentTypeResolver);
		}
		else {
			try {
				if (MediaType.ALL.isPresentIn(getAcceptedMediaTypes(exchange))) {
					return EMPTY_CONDITION;
				}
			}
			catch (NotAcceptableStatusException | UnsupportedMediaTypeStatusException ex) {
				// Ignore
			}
		}
		return null;
	}

	/**
	 * Compares this and another "produces" condition as follows:
	 * <ol>
	 * <li>Sort 'Accept' header media types by quality value via
	 * {@link MediaType#sortByQualityValue(List)} and iterate the list.
	 * <li>Get the first index of matching media types in each "produces"
	 * condition first matching with {@link MediaType#equals(Object)} and
	 * then with {@link MediaType#includes(MediaType)}.
	 * <li>If a lower index is found, the condition at that index wins.
	 * <li>If both indexes are equal, the media types at the index are
	 * compared further with {@link MediaType#SPECIFICITY_COMPARATOR}.
	 * </ol>
	 * <p>It is assumed that both instances have been obtained via
	 * {@link #getMatchingCondition(ServerWebExchange)} and each instance
	 * contains the matching producible media type expression only or
	 * is otherwise empty.
	 */
	@Override
	public int compareTo(ProducesRequestCondition other, ServerWebExchange exchange) {
		try {
			List<MediaType> acceptedMediaTypes = getAcceptedMediaTypes(exchange);
			for (MediaType acceptedMediaType : acceptedMediaTypes) {
				int thisIndex = this.indexOfEqualMediaType(acceptedMediaType);
				int otherIndex = other.indexOfEqualMediaType(acceptedMediaType);
				int result = compareMatchingMediaTypes(this, thisIndex, other, otherIndex);
				if (result != 0) {
					return result;
				}
				thisIndex = this.indexOfIncludedMediaType(acceptedMediaType);
				otherIndex = other.indexOfIncludedMediaType(acceptedMediaType);
				result = compareMatchingMediaTypes(this, thisIndex, other, otherIndex);
				if (result != 0) {
					return result;
				}
			}
			return 0;
		}
		catch (NotAcceptableStatusException ex) {
			// should never happen
			throw new IllegalStateException("Cannot compare without having any requested media types", ex);
		}
	}

	private List<MediaType> getAcceptedMediaTypes(ServerWebExchange exchange) throws NotAcceptableStatusException {
		return this.contentTypeResolver.resolveMediaTypes(exchange);
	}

	private int indexOfEqualMediaType(MediaType mediaType) {
		for (int i = 0; i < getExpressionsToCompare().size(); i++) {
			MediaType currentMediaType = getExpressionsToCompare().get(i).getMediaType();
			if (mediaType.getType().equalsIgnoreCase(currentMediaType.getType()) &&
					mediaType.getSubtype().equalsIgnoreCase(currentMediaType.getSubtype())) {
				return i;
			}
		}
		return -1;
	}

	private int indexOfIncludedMediaType(MediaType mediaType) {
		for (int i = 0; i < getExpressionsToCompare().size(); i++) {
			if (mediaType.includes(getExpressionsToCompare().get(i).getMediaType())) {
				return i;
			}
		}
		return -1;
	}

	private int compareMatchingMediaTypes(ProducesRequestCondition condition1, int index1,
			ProducesRequestCondition condition2, int index2) {

		int result = 0;
		if (index1 != index2) {
			result = index2 - index1;
		}
		else if (index1 != -1) {
			ProduceMediaTypeExpression expr1 = condition1.getExpressionsToCompare().get(index1);
			ProduceMediaTypeExpression expr2 = condition2.getExpressionsToCompare().get(index2);
			result = expr1.compareTo(expr2);
			result = (result != 0 ? result : expr1.getMediaType().compareTo(expr2.getMediaType()));
		}
		return result;
	}

	/**
	 * Return the contained "produces" expressions or if that's empty, a list
	 * with a {@value MediaType#ALL_VALUE} expression.
	 */
	private List<ProduceMediaTypeExpression> getExpressionsToCompare() {
		return (this.expressions.isEmpty() ? this.mediaTypeAllList  : this.expressions);
	}


	/**
	 * Parses and matches a single media type expression to a request's 'Accept' header.
	 */
	class ProduceMediaTypeExpression extends AbstractMediaTypeExpression {

		ProduceMediaTypeExpression(MediaType mediaType, boolean negated) {
			super(mediaType, negated);
		}

		ProduceMediaTypeExpression(String expression) {
			super(expression);
		}

		@Override
		protected boolean matchMediaType(ServerWebExchange exchange) throws NotAcceptableStatusException {
			List<MediaType> acceptedMediaTypes = getAcceptedMediaTypes(exchange);
			for (MediaType acceptedMediaType : acceptedMediaTypes) {
				if (getMediaType().isCompatibleWith(acceptedMediaType)) {
					return true;
				}
			}
			return false;
		}
	}

}
