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
import java.util.Collection;
import java.util.List;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.InvocableHandlerMethod;

/**
 * Adds data binder initialization through the invocation of @{@link InitBinder} methods.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class InitBinderDataBinderFactory extends DefaultDataBinderFactory {

	private final List<InvocableHandlerMethod> binderMethods;
	
	/**
	 * Create a new instance.
	 * @param binderMethods {@link InitBinder} methods to initialize new data binder instances with
	 * @param initializer a global initializer to initialize new data binder instances with
	 */
	public InitBinderDataBinderFactory(List<InvocableHandlerMethod> binderMethods, WebBindingInitializer initializer) {
		super(initializer);
		this.binderMethods = (binderMethods != null) ? binderMethods : new ArrayList<InvocableHandlerMethod>();
	}

	/**
	 * Initializes the given data binder through the invocation of @{@link InitBinder} methods.
	 * An @{@link InitBinder} method that defines names via {@link InitBinder#value()} will 
	 * not be invoked unless one of the names matches the target object name.
	 * @see InitBinder#value()
	 * @throws Exception if one of the invoked @{@link InitBinder} methods fail.
	 */
	@Override
	public void initBinder(WebDataBinder binder, NativeWebRequest request) throws Exception {
		for (InvocableHandlerMethod binderMethod : this.binderMethods) {
			if (!isBinderMethodApplicable(binderMethod, binder)) {
				continue;
			}
			Object returnValue = binderMethod.invokeForRequest(request, null, binder);
			if (returnValue != null) {
				throw new IllegalStateException("This @InitBinder method does not return void: " + binderMethod);
			}
		}
	}

	/**
	 * Returns {@code true} if the given @{@link InitBinder} method should be invoked to initialize
	 * the given {@link WebDataBinder} instance. This implementations returns {@code true} if 
	 * the @{@link InitBinder} annotation on the method does not define any names or if one of the
	 * names it defines names matches the target object name.
	 */
	protected boolean isBinderMethodApplicable(HandlerMethod binderMethod, WebDataBinder binder) {
		InitBinder annot = binderMethod.getMethodAnnotation(InitBinder.class);
		Collection<String> names = Arrays.asList(annot.value());
		return (names.size() == 0 || names.contains(binder.getObjectName()));
	}
	
}
