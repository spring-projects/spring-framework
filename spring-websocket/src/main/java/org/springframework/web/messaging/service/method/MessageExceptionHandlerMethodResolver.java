/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.messaging.service.method;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.messaging.annotation.MessageExceptionHandler;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MessageExceptionHandlerMethodResolver extends ExceptionHandlerMethodResolver {


	public MessageExceptionHandlerMethodResolver(Class<?> handlerType) {
		super(handlerType);
	}


	@Override
	protected MethodFilter getExceptionHandlerMethods() {
		return MESSAGE_EXCEPTION_HANDLER_METHODS;
	}

	@Override
	protected void detectAnnotationExceptionMappings(Method method, List<Class<? extends Throwable>> result) {
		MessageExceptionHandler annotation = AnnotationUtils.findAnnotation(method, MessageExceptionHandler.class);
		result.addAll(Arrays.asList(annotation.value()));
	}


	/**
	 * A filter for selecting {@code @ExceptionHandler} methods.
	 */
	public final static MethodFilter MESSAGE_EXCEPTION_HANDLER_METHODS = new MethodFilter() {

		@Override
		public boolean matches(Method method) {
			return AnnotationUtils.findAnnotation(method, MessageExceptionHandler.class) != null;
		}
	};
}
