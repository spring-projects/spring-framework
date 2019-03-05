/*
 * Copyright 2002-2019 the original author or authors.
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
package org.springframework.messaging.handler.invocation;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.MethodIntrospector;
import org.springframework.util.ReflectionUtils;

/**
 * Sub-class for {@link AbstractExceptionHandlerMethodResolver} for testing.
 * @author Rossen Stoyanchev
 */
public class TestExceptionResolver extends AbstractExceptionHandlerMethodResolver {

	private final static ReflectionUtils.MethodFilter EXCEPTION_HANDLER_METHOD_FILTER =
			method -> method.getName().matches("handle[\\w]*Exception");


	public TestExceptionResolver(Class<?> handlerType) {
		super(initExceptionMappings(handlerType));
	}

	private static Map<Class<? extends Throwable>, Method> initExceptionMappings(Class<?> handlerType) {
		Map<Class<? extends Throwable>, Method> result = new HashMap<>();
		for (Method method : MethodIntrospector.selectMethods(handlerType, EXCEPTION_HANDLER_METHOD_FILTER)) {
			for (Class<? extends Throwable> exception : getExceptionsFromMethodSignature(method)) {
				result.put(exception, method);
			}
		}
		return result;
	}

}
