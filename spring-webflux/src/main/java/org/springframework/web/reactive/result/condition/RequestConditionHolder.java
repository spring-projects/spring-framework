/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;

import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;

/**
 * A holder for a {@link RequestCondition} useful when the type of the request
 * condition is not known ahead of time, e.g. custom condition. Since this
 * class is also an implementation of {@code RequestCondition}, effectively it
 * decorates the held request condition and allows it to be combined and compared
 * with other request conditions in a type and null safe way.
 *
 * <p>When two {@code RequestConditionHolder} instances are combined or compared
 * with each other, it is expected the conditions they hold are of the same type.
 * If they are not, a {@link ClassCastException} is raised.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class RequestConditionHolder extends AbstractRequestCondition<RequestConditionHolder> {

	@Nullable
	private final RequestCondition<Object> condition;


	/**
	 * Create a new holder to wrap the given request condition.
	 * @param requestCondition the condition to hold (may be {@code null})
	 */
	@SuppressWarnings("unchecked")
	public RequestConditionHolder(@Nullable RequestCondition<?> requestCondition) {
		this.condition = (RequestCondition<Object>) requestCondition;
	}


	/**
	 * Return the held request condition, or {@code null} if not holding one.
	 */
	@Nullable
	public RequestCondition<?> getCondition() {
		return this.condition;
	}

	@Override
	protected Collection<?> getContent() {
		return (this.condition != null ? Collections.singleton(this.condition) : Collections.emptyList());
	}

	@Override
	protected String getToStringInfix() {
		return " ";
	}

	/**
	 * Combine the request conditions held by the two RequestConditionHolder
	 * instances after making sure the conditions are of the same type.
	 * Or if one holder is empty, the other holder is returned.
	 */
	@Override
	public RequestConditionHolder combine(RequestConditionHolder other) {
		if (this.condition == null && other.condition == null) {
			return this;
		}
		else if (this.condition == null) {
			return other;
		}
		else if (other.condition == null) {
			return this;
		}
		else {
			assertEqualConditionTypes(this.condition, other.condition);
			RequestCondition<?> combined = (RequestCondition<?>) this.condition.combine(other.condition);
			return new RequestConditionHolder(combined);
		}
	}

	/**
	 * Get the matching condition for the held request condition wrap it in a
	 * new RequestConditionHolder instance. Or otherwise if this is an empty
	 * holder, return the same holder instance.
	 */
	@Override
	@Nullable
	public RequestConditionHolder getMatchingCondition(ServerWebExchange exchange) {
		if (this.condition == null) {
			return this;
		}
		RequestCondition<?> match = (RequestCondition<?>) this.condition.getMatchingCondition(exchange);
		return (match != null ? new RequestConditionHolder(match) : null);
	}

	/**
	 * Compare the request conditions held by the two RequestConditionHolder
	 * instances after making sure the conditions are of the same type.
	 * Or if one holder is empty, the other holder is preferred.
	 */
	@Override
	public int compareTo(RequestConditionHolder other, ServerWebExchange exchange) {
		if (this.condition == null && other.condition == null) {
			return 0;
		}
		else if (this.condition == null) {
			return 1;
		}
		else if (other.condition == null) {
			return -1;
		}
		else {
			assertEqualConditionTypes(this.condition, other.condition);
			return this.condition.compareTo(other.condition, exchange);
		}
	}

	/**
	 * Ensure the held request conditions are of the same type.
	 */
	private void assertEqualConditionTypes(RequestCondition<?> cond1, RequestCondition<?> cond2) {
		Class<?> clazz = cond1.getClass();
		Class<?> otherClazz = cond2.getClass();
		if (!clazz.equals(otherClazz)) {
			throw new ClassCastException("Incompatible request conditions: " + clazz + " vs " + otherClazz);
		}
	}

}
