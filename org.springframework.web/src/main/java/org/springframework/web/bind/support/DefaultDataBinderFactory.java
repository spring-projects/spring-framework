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
 * Creates a {@link WebRequestDataBinder} and initializes it through a {@link WebBindingInitializer}.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class DefaultDataBinderFactory implements WebDataBinderFactory {

	private final WebBindingInitializer initializer;

	/**
	 * Create {@link DefaultDataBinderFactory} instance.
	 * @param initializer a global initializer to initialize new data binder instances with
	 */
	public DefaultDataBinderFactory(WebBindingInitializer initializer) {
		this.initializer = initializer;
	}

	/**
	 * Create a new {@link WebDataBinder} for the given target object and initialize it through 
	 * a {@link WebBindingInitializer}. 
	 */
	public final WebDataBinder createBinder(NativeWebRequest webRequest, Object target, String objectName) throws Exception {
		WebDataBinder dataBinder = createBinderInstance(target, objectName, webRequest);

		if (initializer != null) {
			this.initializer.initBinder(dataBinder, webRequest);
		}

		initBinder(dataBinder, webRequest);
		
		return dataBinder;
	}

	/**
	 * Extension hook that subclasses can use to create a data binder of a specific type. 
	 * The default implementation creates a {@link WebRequestDataBinder}.
	 * @param target the data binding target object; or {@code null} for type conversion on simple objects.
	 * @param objectName the name of the target object 
	 * @param webRequest the current request
	 */
	protected WebDataBinder createBinderInstance(Object target, String objectName, NativeWebRequest webRequest) {
		return new WebRequestDataBinder(target, objectName);
	}

	/**
	 * Extension hook that subclasses can override to initialize further the data binder.
	 * Will be invoked after the data binder is initialized through the {@link WebBindingInitializer}. 
	 * @param dataBinder the data binder instance to customize
	 * @param webRequest the current request
	 * @throws Exception if initialization fails
	 */
	protected void initBinder(WebDataBinder dataBinder, NativeWebRequest webRequest) throws Exception {
	}

}
