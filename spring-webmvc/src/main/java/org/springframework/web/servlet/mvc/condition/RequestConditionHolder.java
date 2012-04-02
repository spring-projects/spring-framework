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

import java.util.Collection;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;


/**
 * A holder for a {@link RequestCondition} useful when the type of the held
 * request condition is not known ahead of time - e.g. custom condition.
 *
 * <p>An implementation of {@code RequestCondition} itself, a
 * {@code RequestConditionHolder} decorates the held request condition allowing
 * it to be combined and compared with other custom request conditions while
 * ensuring type and null safety.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class RequestConditionHolder extends AbstractRequestCondition<RequestConditionHolder> {

	@SuppressWarnings("rawtypes")
	private final RequestCondition condition;

	/**
	 * Create a new holder to wrap the given request condition.
	 * @param requestCondition the condition to hold, may be {@code null}
	 */
	public RequestConditionHolder(RequestCondition<?> requestCondition) {
		this.condition = requestCondition;
	}

	/**
	 * Return the held request condition, or {@code null} if not holding one.
	 */
	public RequestCondition<?> getCondition() {
		return condition;
	}

	@Override
	protected Collection<?> getContent() {
		return condition != null ? Collections.singleton(condition) : Collections.emptyList();
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
	@SuppressWarnings("unchecked")
	public RequestConditionHolder combine(RequestConditionHolder other) {
		if (condition == null && other.condition == null) {
			return this;
		}
		else if (condition == null) {
			return other;
		}
		else if (other.condition == null) {
			return this;
		}
		else {
			assertIsCompatible(other);
			RequestCondition<?> combined = (RequestCondition<?>) condition.combine(other.condition);
			return new RequestConditionHolder(combined);
		}
	}

	/**
	 * Ensure the held request conditions are of the same type.
	 */
	private void assertIsCompatible(RequestConditionHolder other) {
		Class<?> clazz = condition.getClass();
		Class<?> otherClazz = other.condition.getClass();
		if (!clazz.equals(otherClazz)) {
			throw new ClassCastException("Incompatible request conditions: " + clazz + " and " + otherClazz);
		}
	}

	/**
	 * Get the matching condition for the held request condition wrap it in a
	 * new RequestConditionHolder instance. Or otherwise if this is an empty
	 * holder, return the same holder instance.
	 */
	public RequestConditionHolder getMatchingCondition(HttpServletRequest request) {
		if (condition == null) {
			return this;
		}
		RequestCondition<?> match = (RequestCondition<?>) condition.getMatchingCondition(request);
		return (match != null) ? new RequestConditionHolder(match) : null;
	}

	/**
	 * Compare the request conditions held by the two RequestConditionHolder
	 * instances after making sure the conditions are of the same type.
	 * Or if one holder is empty, the other holder is preferred.
	 */
	@SuppressWarnings("unchecked")
	public int compareTo(RequestConditionHolder other, HttpServletRequest request) {
		if (condition == null && other.condition == null) {
			return 0;
		}
		else if (condition == null) {
			return 1;
		}
		else if (other.condition == null) {
			return -1;
		}
		else {
			assertIsCompatible(other);
			return condition.compareTo(other.condition, request);
		}
	}

}
