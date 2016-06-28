/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.servlet.support;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.HandlerMapping;

/**
 * Additional interface that a {@link HandlerMapping} can implement to expose
 * a request matching API aligned with its internal request matching
 * configuration and implementation.
 *
 * @author Rossen Stoyanchev
 * @since 4.3.1
 * @see HandlerMappingIntrospector
 */
public interface MatchableHandlerMapping {

	/**
	 * Whether the given request matches the request criteria.
	 * @param request the current request
	 * @param pattern the pattern to match
	 * @return the result from request matching or {@code null}
	 */
	RequestMatchResult match(HttpServletRequest request, String pattern);

}
