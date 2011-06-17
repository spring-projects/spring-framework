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

import java.util.Collection;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

/**
 * Wraps and delegates operations to a custom {@link RequestCondition} whose type is not known and even its 
 * presence is not guaranteed ahead of time. The main purpose of this class is to ensure a type-safe and 
 * null-safe way of combining and comparing custom request conditions.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class CustomRequestCondition extends AbstractRequestCondition<CustomRequestCondition> {

	@SuppressWarnings("rawtypes")
	private final RequestCondition customCondition;
	
	/**
	 * Creates a {@link CustomRequestCondition} that wraps the given {@link RequestCondition} instance. 
	 * @param requestCondition the custom request condition to wrap
	 */
	public CustomRequestCondition(RequestCondition<?> requestCondition) {
		this.customCondition = requestCondition;
	}

	/**
	 * Creates an empty {@link CustomRequestCondition}.
	 */
	public CustomRequestCondition() {
		this(null);
	}

	public RequestCondition<?> getRequestCondition() {
		return customCondition;
	}

	@Override
	protected Collection<?> getContent() {
		return customCondition != null ? Collections.singleton(customCondition) : Collections.emptyList();
	}

	@Override
	protected String getToStringInfix() {
		return "";
	}

	/**
	 * Delegates the operation to the wrapped custom request conditions. May also return "this" instance
	 * if the "other" does not contain a custom request condition and vice versa.
	 */
	@SuppressWarnings("unchecked")
	public CustomRequestCondition combine(CustomRequestCondition other) {
		if (customCondition == null && other.customCondition == null) {
			return this;
		}
		else if (customCondition == null) {
			return other;
		}
		else if (other.customCondition == null) {
			return this;
		}
		else {
			assertCompatible(other);
			RequestCondition<?> combined = (RequestCondition<?>) customCondition.combine(other.customCondition);
			return new CustomRequestCondition(combined);
		}
	}

	private void assertCompatible(CustomRequestCondition other) {
		if (customCondition != null && other.customCondition != null) {
			Class<?> clazz = customCondition.getClass();
			Class<?> otherClazz = other.customCondition.getClass();
			if (!clazz.equals(otherClazz)) {
				throw new ClassCastException("Incompatible custom request conditions: " + clazz + ", " + otherClazz);
			}
		}
	}
	
	/**
	 * Delegates the operation to the wrapped custom request condition; or otherwise returns the same 
	 * instance if there is no custom request condition.
	 */
	public CustomRequestCondition getMatchingCondition(HttpServletRequest request) {
		if (customCondition == null) {
			return this;
		}
		RequestCondition<?> match = (RequestCondition<?>) customCondition.getMatchingCondition(request);
		return new CustomRequestCondition(match);
	}

	/**
	 * Delegates the operation to the wrapped custom request conditions after checking for the presence
	 * custom request conditions and asserting type safety. The presence of a custom request condition 
	 * in one instance but not the other will cause it to be selected, and vice versa.
	 */
	@SuppressWarnings("unchecked")
	public int compareTo(CustomRequestCondition other, HttpServletRequest request) {
		if (customCondition == null && other.customCondition == null) {
			return 0;
		}
		else if (customCondition == null) {
			return 1;
		}
		else if (other.customCondition == null) {
			return -1;
		}
		else {
			assertCompatible(other);
			return customCondition.compareTo(other.customCondition, request);
		}
	}
	
}
