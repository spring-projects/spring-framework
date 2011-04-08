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

/**
 * Request header name-value condition.
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @see org.springframework.web.bind.annotation.RequestMapping#headers()
 * @since 3.1
 */
class HeaderRequestCondition extends AbstractNameValueCondition<String> {

	public HeaderRequestCondition(String expression) {
		super(expression);
	}

	@Override
	protected String parseValue(String valueExpression) {
		return valueExpression;
	}

	@Override
	protected boolean matchName(HttpServletRequest request) {
		return request.getHeader(name) != null;
	}

	@Override
	protected boolean matchValue(HttpServletRequest request) {
		return value.equals(request.getHeader(name));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj != null && obj instanceof HeaderRequestCondition) {
			HeaderRequestCondition other = (HeaderRequestCondition) obj;
			return ((this.name.equalsIgnoreCase(other.name)) &&
					(this.value != null ? this.value.equals(other.value) : other.value == null) &&
					this.isNegated == other.isNegated);
		}
		return false;
	}

}
