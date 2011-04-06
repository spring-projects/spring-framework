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

package org.springframework.web.bind.support;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * A {@link WebDataBinderFactory} that creates {@link WebDataBinder} and initializes them 
 * with a {@link WebBindingInitializer}.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class DefaultDataBinderFactory implements WebDataBinderFactory {

	private final WebBindingInitializer bindingInitializer;

	/**
	 * Create {@link DefaultDataBinderFactory} instance.
	 * @param bindingInitializer a {@link WebBindingInitializer} to initialize new data binder instances with
	 */
	public DefaultDataBinderFactory(WebBindingInitializer bindingInitializer) {
		this.bindingInitializer = bindingInitializer;
	}

	/**
	 * Create a new {@link WebDataBinder} for the given target object and initialize it through 
	 * a {@link WebBindingInitializer}. 
	 */
	public WebDataBinder createBinder(NativeWebRequest webRequest, Object target, String objectName) throws Exception {
		WebDataBinder dataBinder = createBinderInstance(target, objectName);

		if (bindingInitializer != null) {
			this.bindingInitializer.initBinder(dataBinder, webRequest);
		}

		return dataBinder;
	}

	/**
	 * Create a {@link WebDataBinder} instance.
	 * @param target the object to create a data binder for, or {@code null} if creating a binder for a simple type
	 * @param objectName the name of the target object 
	 */
	protected WebDataBinder createBinderInstance(Object target, String objectName) {
		return new WebRequestDataBinder(target, objectName);
	}

}
