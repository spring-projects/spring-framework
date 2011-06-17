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
import java.util.Iterator;

/**
 * Base class for {@link RequestCondition}s. 
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
abstract class RequestConditionSupport<This extends RequestConditionSupport<This>> implements RequestCondition<This> {
	
	/**
	 * Returns the individual expressions a request condition is composed of such as 
	 * URL patterns, HTTP request methods, parameter expressions, etc. 
	 */
	protected abstract Collection<?> getContent();
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o != null && getClass().equals(o.getClass())) {
			RequestConditionSupport<?> other = (RequestConditionSupport<?>) o;
			return getContent().equals(other.getContent());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getContent().hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("[");
		for (Iterator<?> iterator = getContent().iterator(); iterator.hasNext();) {
			Object expression = iterator.next();
			builder.append(expression.toString());
			if (iterator.hasNext()) {
				if (isLogicalConjunction()) {
					builder.append(" && ");
				}
				else {
					builder.append(" || ");
				}
			}
		}
		builder.append("]");
		return builder.toString();
	}

	/**
	 * Returns {@code true} if the individual expressions of the condition are combined via logical 
	 * conjunction (" && "); or {@code false} otherwise.
	 */
	protected abstract boolean isLogicalConjunction();

}
