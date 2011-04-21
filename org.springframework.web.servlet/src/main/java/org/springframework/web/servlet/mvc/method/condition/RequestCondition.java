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
 * Defines the contract for conditions that must be met before an incoming request matches a {@link
 * org.springframework.web.servlet.mvc.method.annotation.RequestMappingInfo RequestKey}.
 *
 * <p>Implementations of this interface are created by the {@link RequestConditionFactory}.
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @see RequestConditionFactory
 * @since 3.1
 */
public interface RequestCondition extends Comparable<RequestCondition> {

	/**
	 * Indicates whether this condition matches against the given servlet request.
	 *
	 * @param request the request
	 * @return {@code true} if this condition matches the request; {@code false} otherwise
	 */
	boolean match(HttpServletRequest request);

}
