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

package org.springframework.web.method.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.InvocableHandlerMethod;

/**
 * A specialization of {@link DefaultDataBinderFactory} that further initializes {@link WebDataBinder} instances
 * by invoking {@link InitBinder} methods.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class InitBinderDataBinderFactory extends DefaultDataBinderFactory {

	private final List<InvocableHandlerMethod> initBinderMethods;
	
	/**
	 * Create an {@code InitBinderDataBinderFactory} instance with the given {@link InitBinder} methods.
	 * @param binderMethods {@link InitBinder} methods to use to invoke to initialize new data binder instances
	 * @param bindingInitializer a {@link WebBindingInitializer} to initialize new data binder instances with
	 */
	public InitBinderDataBinderFactory(List<InvocableHandlerMethod> binderMethods,
									   WebBindingInitializer bindingInitializer) {
		super(bindingInitializer);
		this.initBinderMethods = (binderMethods != null) ? binderMethods : new ArrayList<InvocableHandlerMethod>();
	}

	/**
	 * Create a {@link WebDataBinder} for the given object and initialize it by calling {@link InitBinder} methods.
	 * Only methods with an {@link InitBinder} annotation value that doesn't list attributes names or methods with
	 * an {@link InitBinder} annotation value that matches the target object name are invoked.
	 * @see InitBinder#value()
	 */
	@Override
	public WebDataBinder createBinder(NativeWebRequest request, Object target, String objectName) throws Exception {
		WebDataBinder dataBinder = super.createBinder(request, target, objectName);

		for (InvocableHandlerMethod binderMethod : this.initBinderMethods) {
			InitBinder annot = binderMethod.getMethodAnnotation(InitBinder.class);
			Set<String> attributeNames = new HashSet<String>(Arrays.asList(annot.value()));
			
			if (attributeNames.size() == 0 || attributeNames.contains(objectName)) {
				Object returnValue = binderMethod.invokeForRequest(request, null, dataBinder);
				
				if (returnValue != null) {
					throw new IllegalStateException("InitBinder methods must not have a return value: " + binderMethod);
				}
			}
		}
	
		return dataBinder;
	}
	
}