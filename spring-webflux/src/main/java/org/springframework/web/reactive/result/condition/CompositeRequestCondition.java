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

package org.springframework.web.reactive.result.condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * Implements the {@link RequestCondition} contract by delegating to multiple
 * {@code RequestCondition} types and using a logical conjunction ({@code ' && '}) to
 * ensure all conditions match a given request.
 *
 * <p>When {@code CompositeRequestCondition} instances are combined or compared
 * is expected that (a) they contain the same number of conditions and (b)
 * conditions at the same index are of the same type. It is acceptable to
 * provide {@code null} conditions or no conditions at all to the constructor.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class CompositeRequestCondition extends AbstractRequestCondition<CompositeRequestCondition> {

	private final RequestConditionHolder[] requestConditions;


	/**
	 * Create an instance with 0 or more {@code RequestCondition} types. It is
	 * important to create {@code CompositeRequestCondition} instances with the
	 * same number of conditions so they may be compared and combined.
	 * It is acceptable to provide {@code null} conditions.
	 */
	public CompositeRequestCondition(RequestCondition<?>... requestConditions) {
		this.requestConditions = wrap(requestConditions);
	}

	private CompositeRequestCondition(RequestConditionHolder[] requestConditions) {
		this.requestConditions = requestConditions;
	}


	private RequestConditionHolder[] wrap(RequestCondition<?>... rawConditions) {
		RequestConditionHolder[] wrappedConditions = new RequestConditionHolder[rawConditions.length];
		for (int i = 0; i < rawConditions.length; i++) {
			wrappedConditions[i] = new RequestConditionHolder(rawConditions[i]);
		}
		return wrappedConditions;
	}

	/**
	 * Whether this instance contains 0 conditions or not.
	 */
	@Override
	public boolean isEmpty() {
		return ObjectUtils.isEmpty(this.requestConditions);
	}

	/**
	 * Return the underlying conditions, possibly empty but never {@code null}.
	 */
	public List<@Nullable RequestCondition<?>> getConditions() {
		return unwrap();
	}

	private List<@Nullable RequestCondition<?>> unwrap() {
		List<@Nullable RequestCondition<?>> result = new ArrayList<>();
		for (RequestConditionHolder holder : this.requestConditions) {
			result.add(holder.getCondition());
		}
		return result;
	}

	@Override
	protected Collection<? extends @Nullable Object> getContent() {
		return (!isEmpty() ? getConditions() : Collections.emptyList());
	}

	@Override
	protected String getToStringInfix() {
		return " && ";
	}

	private int getLength() {
		return this.requestConditions.length;
	}

	/**
	 * If one instance is empty, return the other.
	 * <p>If both instances have conditions, combine the individual conditions
	 * after ensuring they are of the same type and number.
	 */
	@Override
	public CompositeRequestCondition combine(CompositeRequestCondition other) {
		if (isEmpty() && other.isEmpty()) {
			return this;
		}
		else if (other.isEmpty()) {
			return this;
		}
		else if (isEmpty()) {
			return other;
		}
		else {
			assertNumberOfConditions(other);
			RequestConditionHolder[] combinedConditions = new RequestConditionHolder[getLength()];
			for (int i = 0; i < getLength(); i++) {
				combinedConditions[i] = this.requestConditions[i].combine(other.requestConditions[i]);
			}
			return new CompositeRequestCondition(combinedConditions);
		}
	}

	private void assertNumberOfConditions(CompositeRequestCondition other) {
		Assert.isTrue(getLength() == other.getLength(),
				() -> "Cannot combine CompositeRequestConditions with a different number of conditions. " +
				ObjectUtils.nullSafeToString(this.requestConditions) + " and " +
				ObjectUtils.nullSafeToString(other.requestConditions));
	}

	/**
	 * Delegate to <em>all</em> contained conditions to match the request and return the
	 * resulting "matching" condition instances.
	 * <p>An empty {@code CompositeRequestCondition} matches to all requests.
	 */
	@Override
	public @Nullable CompositeRequestCondition getMatchingCondition(ServerWebExchange exchange) {
		if (isEmpty()) {
			return this;
		}
		RequestConditionHolder[] matchingConditions = new RequestConditionHolder[getLength()];
		for (int i = 0; i < getLength(); i++) {
			RequestConditionHolder matchingCondition = this.requestConditions[i].getMatchingCondition(exchange);
			if (matchingCondition == null) {
				return null;
			}
			matchingConditions[i] = matchingCondition;
		}
		return new CompositeRequestCondition(matchingConditions);
	}

	/**
	 * If one instance is empty, the other "wins". If both instances have
	 * conditions, compare them in the order in which they were provided.
	 */
	@Override
	public int compareTo(CompositeRequestCondition other, ServerWebExchange exchange) {
		if (isEmpty() && other.isEmpty()) {
			return 0;
		}
		else if (isEmpty()) {
			return 1;
		}
		else if (other.isEmpty()) {
			return -1;
		}
		else {
			assertNumberOfConditions(other);
			for (int i = 0; i < getLength(); i++) {
				int result = this.requestConditions[i].compareTo(other.requestConditions[i], exchange);
				if (result != 0) {
					return result;
				}
			}
			return 0;
		}
	}

}
