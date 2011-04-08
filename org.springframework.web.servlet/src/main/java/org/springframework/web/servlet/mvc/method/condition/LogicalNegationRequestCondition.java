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

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.Assert;

/**
 * {@link RequestCondition} implementation that represents a logical NOT (i.e. !).
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
class LogicalNegationRequestCondition extends AbstractRequestCondition {

	private final RequestCondition requestCondition;

	LogicalNegationRequestCondition(RequestCondition requestCondition) {
		Assert.notNull(requestCondition, "'requestCondition' must not be null");
		this.requestCondition = requestCondition;
	}

	@Override
	protected int getSpecificity() {
		if (requestCondition instanceof AbstractRequestCondition) {
			return ((AbstractRequestCondition) requestCondition).getSpecificity();
		}
		else {
			return 0;
		}
	}

	public boolean match(HttpServletRequest request) {
		return !requestCondition.match(request);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o != null && o instanceof LogicalNegationRequestCondition) {
			LogicalNegationRequestCondition other = (LogicalNegationRequestCondition) o;
			return this.requestCondition.equals(other.requestCondition);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return requestCondition.hashCode();
	}

	@Override
	public String toString() {
		return "!(" + requestCondition.toString() + ")";
	}
}
