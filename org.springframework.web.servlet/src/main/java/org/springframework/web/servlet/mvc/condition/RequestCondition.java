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

import javax.servlet.http.HttpServletRequest;

/**
 * The contract for request conditions.
 * 
 * <p>Request conditions can be combined (e.g. type + method-level conditions), matched to a request, 
 * or compared to each other to determine if one matches the request better.
 * 
 * @param <T> The type of objects that this RequestCondition can be compared to and combined with.
 * 
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 3.1
 */
public interface RequestCondition<T> {

	/**
	 * Defines the rules for combining "this" condition (i.e. the current instance) with another condition.
	 * <p>Example: combine type- and method-level request mapping conditions. 
	 * 
	 * @returns a request condition instance that is the result of combining the two condition instances.
	 */
	T combine(T other);

	/**
	 * Checks if this condition matches the given request and returns a potentially new request condition 
	 * with content tailored to the current request. For example a condition with URL patterns might return 
	 * a new condition that contains matching patterns sorted with best matching patterns on top.
	 * 
	 * @return a condition instance in case of a match; or {@code null} if there is no match.
	 */
	T getMatchingCondition(HttpServletRequest request);

	/**
	 * Compares "this" condition (i.e. the current instance) with another condition in the context of a request. 
	 * <p>Note: it is assumed both instances have been obtained via {@link #getMatchingCondition(HttpServletRequest)} 
	 * to ensure they have content relevant to current request only.
	 */
	int compareTo(T other, HttpServletRequest request);
	
}
