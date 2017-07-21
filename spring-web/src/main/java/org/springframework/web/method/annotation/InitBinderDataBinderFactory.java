/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.method.annotation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
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
	public InitBinderDataBinderFactory(@Nullable List<InvocableHandlerMethod> binderMethods,
			@Nullable WebBindingInitializer initializer) {

		super(initializer);
		this.binderMethods = (binderMethods != null ? binderMethods : Collections.emptyList());
	}

	/**
	 * Initialize a WebDataBinder with {@code @InitBinder} methods.
	 * If the {@code @InitBinder} annotation specifies attributes names, it is
	 * invoked only if the names include the target object name.
	 * @throws Exception if one of the invoked @{@link InitBinder} methods fail.
	 */
	@Override
	public void initBinder(WebDataBinder binder, NativeWebRequest request) throws Exception {
		for (InvocableHandlerMethod binderMethod : this.binderMethods) {
			if (isBinderMethodApplicable(binderMethod, binder)) {
				Object returnValue = binderMethod.invokeForRequest(request, null, binder);
				if (returnValue != null) {
					throw new IllegalStateException(
							"@InitBinder methods should return void: " + binderMethod);
				}
			}
		}
	}

	/**
	 * Whether the given {@code @InitBinder} method should be used to initialize
	 * the given WebDataBinder instance. By default we check the attributes
	 * names of the annotation, if present.
	 */
	protected boolean isBinderMethodApplicable(HandlerMethod binderMethod, WebDataBinder binder) {
		InitBinder ann = binderMethod.getMethodAnnotation(InitBinder.class);
		Assert.state(ann != null, "No InitBinder annotation");
		Collection<String> names = Arrays.asList(ann.value());
		return (names.isEmpty() || names.contains(binder.getObjectName()));
	}

}
