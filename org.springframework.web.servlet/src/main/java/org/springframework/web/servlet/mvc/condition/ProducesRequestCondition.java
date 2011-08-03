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
 * A logical disjunction (' || ') request condition to match a request's 'Accept' header
 * to a list of media type expressions. Two kinds of media type expressions are 
 * supported, which are described in {@link RequestMapping#produces()} and
 * {@link RequestMapping#headers()} where the header name is 'Accept'. 
 * Regardless of which syntax is used, the semantics are the same.
 * 
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class ProducesRequestCondition extends AbstractRequestCondition<ProducesRequestCondition> {

	private final List<ProduceMediaTypeExpression> expressions;

	/**
	 * Creates a new instance from 0 or more "produces" expressions.
	 * @param produces expressions with the syntax described in {@link RequestMapping#produces()}
	 * 		if 0 expressions are provided, the condition matches to every request
	 */
	public ProducesRequestCondition(String... produces) {
		this(parseExpressions(produces, null));
	}
	
	/**
	 * Creates a new instance with "produces" and "header" expressions. "Header" expressions 
	 * where the header name is not 'Accept' or have no header value defined are ignored.
	 * If 0 expressions are provided in total, the condition matches to every request
	 * @param produces expressions with the syntax described in {@link RequestMapping#produces()}
	 * @param headers expressions with the syntax described in {@link RequestMapping#headers()}
	 */
	public ProducesRequestCondition(String[] produces, String[] headers) {
		this(parseExpressions(produces, headers));
	}

	/**
	 * Private constructor accepting parsed media type expressions.
	 */
	private ProducesRequestCondition(Collection<ProduceMediaTypeExpression> expressions) {
		this.expressions = new ArrayList<ProduceMediaTypeExpression>(expressions);
		Collections.sort(this.expressions);
	}

	private static Set<ProduceMediaTypeExpression> parseExpressions(String[] produces, String[] headers) {
		Set<ProduceMediaTypeExpression> result = new LinkedHashSet<ProduceMediaTypeExpression>();
		if (headers != null) {
			for (String header : headers) {
				HeaderExpression expr = new HeaderExpression(header);
				if ("Accept".equalsIgnoreCase(expr.name)) {
					for( MediaType mediaType : MediaType.parseMediaTypes(expr.value)) {
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
	 * Returns the media types for this condition.
	 */
	public Set<MediaType> getMediaTypes() {
		Set<MediaType> result = new LinkedHashSet<MediaType>();
		for (ProduceMediaTypeExpression expression : expressions) {
			result.add(expression.getMediaType());
		}
		return result;
	}

	/**
	 * Whether the condition has any media type expressions.
	 */
	public boolean isEmpty() {
		return expressions.isEmpty();
	}

	@Override
	protected Collection<ProduceMediaTypeExpression> getContent() {
		return expressions;
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
	public ProducesRequestCondition combine(ProducesRequestCondition other) {
		return !other.expressions.isEmpty() ? other : this;
	}

	/**
	 * Checks if any of the contained media type expressions match the given 
	 * request 'Content-Type' header and returns an instance that is guaranteed 
	 * to contain matching expressions only. The match is performed via
	 * {@link MediaType#isCompatibleWith(MediaType)}.
	 * 
	 * @param request the current request
	 * 
	 * @return the same instance if there are no expressions; 
	 * 		or a new condition with matching expressions; 
	 * 		or {@code null} if no expressions match.
	 */
	public ProducesRequestCondition getMatchingCondition(HttpServletRequest request) {
		if (isEmpty()) {
			return this;
		}
		Set<ProduceMediaTypeExpression> result = new LinkedHashSet<ProduceMediaTypeExpression>(expressions);
		for (Iterator<ProduceMediaTypeExpression> iterator = result.iterator(); iterator.hasNext();) {
			ProduceMediaTypeExpression expression = iterator.next();
			if (!expression.match(request)) {
				iterator.remove();
			}
		}
		return (result.isEmpty()) ? null : new ProducesRequestCondition(result);
	}

	/**
	 * Compares this and another Produces condition as follows:
	 * 
	 * <ol>
	 * 	<li>Sorts the request 'Accept' header media types by quality value via
	 * 	{@link MediaType#sortByQualityValue(List)} and iterates over the sorted types.
	 * 	<li>Compares the sorted request media types against the media types of each 
	 * 	Produces condition via {@link MediaType#includes(MediaType)}. 
	 * 	<li>A "produces" condition with a matching media type listed earlier wins.
	 * 	<li>If both conditions have a matching media type at the same index, the
	 * 	media types are further compared by specificity and quality.
	 * </ol>
	 * 
	 * <p>If a request media type is {@link MediaType#ALL} or if there is no 'Accept'
	 * header, and therefore both conditions match, preference is given to one 
	 * Produces condition if it is empty and the other one is not.
	 * 
	 * <p>It is assumed that both instances have been obtained via 
	 * {@link #getMatchingCondition(HttpServletRequest)} and each instance 
	 * contains the matching producible media type expression only or 
	 * is otherwise empty.
	 */
	public int compareTo(ProducesRequestCondition other, HttpServletRequest request) {
		String acceptHeader = request.getHeader("Accept");
		List<MediaType> acceptedMediaTypes = MediaType.parseMediaTypes(acceptHeader);
		MediaType.sortByQualityValue(acceptedMediaTypes);

		for (MediaType acceptedMediaType : acceptedMediaTypes) {
			if (acceptedMediaType.equals(MediaType.ALL)) {
				if (isOneEmptyButNotBoth(other)) {
					return this.isEmpty() ? -1 : 1;
				}
			}
			int thisIndex = this.indexOfMediaType(acceptedMediaType);
			int otherIndex = other.indexOfMediaType(acceptedMediaType);
			if (thisIndex != otherIndex) {
				return otherIndex - thisIndex;
			} else if (thisIndex != -1 && otherIndex != -1) {
				ProduceMediaTypeExpression thisExpr = this.expressions.get(thisIndex);
				ProduceMediaTypeExpression otherExpr = other.expressions.get(otherIndex);
				int result = thisExpr.compareTo(otherExpr);
				if (result != 0) {
					return result;
				}
			}
		}
		
		if (acceptedMediaTypes.isEmpty()) {
			if (isOneEmptyButNotBoth(other)) {
				return this.isEmpty() ? -1 : 1;
			}
		}
		
		return 0;
	}

	private int indexOfMediaType(MediaType mediaType) {
		for (int i = 0; i < expressions.size(); i++) {
			if (mediaType.includes(expressions.get(i).getMediaType())) {
				return i;
			}
		}
		return -1;
	}

	private boolean isOneEmptyButNotBoth(ProducesRequestCondition other) {
		return ((this.isEmpty() || other.isEmpty()) && (this.expressions.size() != other.expressions.size()));
	}
	
	/**
	 * Parses and matches a single media type expression to a request's 'Accept' header. 
	 */
	static class ProduceMediaTypeExpression extends MediaTypeExpression {
		
		ProduceMediaTypeExpression(MediaType mediaType, boolean negated) {
			super(mediaType, negated);
		}

		ProduceMediaTypeExpression(String expression) {
			super(expression);
		}

		@Override
		protected boolean matchMediaType(HttpServletRequest request) {
			List<MediaType> acceptedMediaTypes = getAcceptedMediaTypes(request);
			for (MediaType acceptedMediaType : acceptedMediaTypes) {
				if (getMediaType().isCompatibleWith(acceptedMediaType)) {
					return true;
				}
			}
			return false;
		}

		private List<MediaType> getAcceptedMediaTypes(HttpServletRequest request) {
			String acceptHeader = request.getHeader("Accept");
			if (StringUtils.hasLength(acceptHeader)) {
				return MediaType.parseMediaTypes(acceptHeader);
			}
			else {
				return Collections.singletonList(MediaType.ALL);
			}
		}
	}

}
