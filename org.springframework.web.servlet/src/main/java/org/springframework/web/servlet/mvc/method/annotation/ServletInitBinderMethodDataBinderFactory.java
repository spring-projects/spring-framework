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

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.List;

import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.method.annotation.InitBinderMethodDataBinderFactory;
import org.springframework.web.method.support.InvocableHandlerMethod;

/**
 * An {@link InitBinderMethodDataBinderFactory} for Servlet environments. 
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletInitBinderMethodDataBinderFactory extends InitBinderMethodDataBinderFactory {

	/**
	 * Create an {@link ServletInitBinderMethodDataBinderFactory} instance.
	 * @param initBinderMethods init binder methods to use to initialize new data binders.  
	 * @param bindingInitializer a WebBindingInitializer to use to initialize created data binder instances.
	 */
	public ServletInitBinderMethodDataBinderFactory(List<InvocableHandlerMethod> initBinderMethods,
													WebBindingInitializer bindingInitializer) {
		super(initBinderMethods, bindingInitializer);
	}

	/**
	 * {@inheritDoc} creates a Servlet data binder.
	 */
	@Override
	protected WebDataBinder createBinderInstance(Object target, String objectName) {
		return new ServletRequestDataBinder(target, objectName);
	}

}
