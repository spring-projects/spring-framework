/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.method;

import java.lang.reflect.Method;
import java.util.Set;

import org.springframework.core.MethodIntrospector;
import org.springframework.util.ReflectionUtils.MethodFilter;

/**
 * Defines the algorithm for searching handler methods exhaustively including interfaces and parent
 * classes while also dealing with parameterized methods as well as interface and class-based proxies.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 * @deprecated as of Spring 4.2.3, in favor of the generalized and refined {@link MethodIntrospector}
 */
@Deprecated
public abstract class HandlerMethodSelector {

	/**
	 * Select handler methods for the given handler type.
	 * <p>Callers define handler methods of interest through the {@link MethodFilter} parameter.
	 * @param handlerType the handler type to search handler methods on
	 * @param handlerMethodFilter a {@link MethodFilter} to help recognize handler methods of interest
	 * @return the selected methods, or an empty set
	 * @see MethodIntrospector#selectMethods
	 */
	public static Set<Method> selectMethods(Class<?> handlerType, MethodFilter handlerMethodFilter) {
		return MethodIntrospector.selectMethods(handlerType, handlerMethodFilter);
	}

}
