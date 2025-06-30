/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.InitBinderDataBinderFactory;
import org.springframework.web.method.support.InvocableHandlerMethod;

/**
 * Creates a {@code ServletRequestDataBinder}.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletRequestDataBinderFactory extends InitBinderDataBinderFactory {

	/**
	 * Create a new instance.
	 * @param binderMethods one or more {@code @InitBinder} methods
	 * @param initializer provides global data binder initialization
	 */
	public ServletRequestDataBinderFactory(@Nullable List<InvocableHandlerMethod> binderMethods,
			@Nullable WebBindingInitializer initializer) {

		super(binderMethods, initializer);
	}

	/**
	 * Returns an instance of {@link ExtendedServletRequestDataBinder}.
	 */
	@Override
	protected ServletRequestDataBinder createBinderInstance(
			@Nullable Object target, String objectName, NativeWebRequest request) throws Exception {

		return new ExtendedServletRequestDataBinder(target, objectName);
	}

}
