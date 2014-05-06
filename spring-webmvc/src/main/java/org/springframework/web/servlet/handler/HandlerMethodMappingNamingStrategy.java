/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.handler;

import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

/**
 * A strategy for assigning a name to a controller method mapping.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface HandlerMethodMappingNamingStrategy<T> {

	/**
	 * Determine the name for the given HandlerMethod and mapping.
	 *
	 * @param handlerMethod the handler method
	 * @param mapping the mapping
	 *
	 * @return the name
	 */
	String getName(HandlerMethod handlerMethod, T mapping);

}
