/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.method.annotation;

import java.util.Collections;
import java.util.List;

import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.InvocableHandlerMethod;

/**
 * Adds initialization to a WebDataBinder via {@code @InitBinder} methods.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class InitBinderDataBinderFactory extends DefaultDataBinderFactory {

	private final List<InvocableHandlerMethod> binderMethods;


	/**
	 * Create a new InitBinderDataBinderFactory instance.
	 * @param binderMethods {@code @InitBinder} methods
	 * @param initializer for global data binder initialization
	 */
	public InitBinderDataBinderFactory(List<InvocableHandlerMethod> binderMethods, WebBindingInitializer initializer) {
		super(initializer);
		this.binderMethods = (binderMethods != null ? binderMethods : Collections.<InvocableHandlerMethod>emptyList());
	}


	/**
	 * Initialize a WebDataBinder with {@code @InitBinder} methods.
	 * <p>If the {@code @InitBinder} annotation specifies attributes names,
	 * it is invoked only if the names include the target object name.
	 * @throws Exception if one of the invoked @{@link InitBinder} methods fails
	 * @see #isBinderMethodApplicable
	 */
	@Override
	public void initBinder(WebDataBinder dataBinder, NativeWebRequest request) throws Exception {
		for (InvocableHandlerMethod binderMethod : this.binderMethods) {
			if (isBinderMethodApplicable(binderMethod, dataBinder)) {
				Object returnValue = binderMethod.invokeForRequest(request, null, dataBinder);
				if (returnValue != null) {
					throw new IllegalStateException(
							"@InitBinder methods must not return a value (should be void): " + binderMethod);
				}
			}
		}
	}

	/**
	 * Determine whether the given {@code @InitBinder} method should be used
	 * to initialize the given {@link WebDataBinder} instance. By default we
	 * check the specified attribute names in the annotation value, if any.
	 */
	protected boolean isBinderMethodApplicable(HandlerMethod initBinderMethod, WebDataBinder dataBinder) {
		InitBinder ann = initBinderMethod.getMethodAnnotation(InitBinder.class);
		String[] names = ann.value();
		return (ObjectUtils.isEmpty(names) || ObjectUtils.containsElement(names, dataBinder.getObjectName()));
	}

}
