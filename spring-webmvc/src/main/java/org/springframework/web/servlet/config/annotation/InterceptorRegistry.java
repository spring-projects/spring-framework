/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;

/**
 * Helps with configuring a list of mapped interceptors.
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 *
 * @since 3.1
 */
public class InterceptorRegistry {

	private final List<InterceptorRegistration> registrations = new ArrayList<>();

	/**
	 * Adds the provided {@link HandlerInterceptor}.
	 * @param interceptor the interceptor to add
	 * @return An {@link InterceptorRegistration} that allows you optionally configure the
	 * registered interceptor further for example adding URL patterns it should apply to.
	 */
	public InterceptorRegistration addInterceptor(HandlerInterceptor interceptor) {
		InterceptorRegistration registration = new InterceptorRegistration(interceptor);
		registrations.add(registration);
		return registration;
	}

	/**
	 * Adds the provided {@link WebRequestInterceptor}.
	 * @param interceptor the interceptor to add
	 * @return An {@link InterceptorRegistration} that allows you optionally configure the
	 * registered interceptor further for example adding URL patterns it should apply to.
	 */
	public InterceptorRegistration addWebRequestInterceptor(WebRequestInterceptor interceptor) {
		WebRequestHandlerInterceptorAdapter adapted = new WebRequestHandlerInterceptorAdapter(interceptor);
		InterceptorRegistration registration = new InterceptorRegistration(adapted);
		registrations.add(registration);
		return registration;
	}

	/**
	 * Returns all registered interceptors.
	 */
	protected List<Object> getInterceptors() {
		List<Object> interceptors = new ArrayList<>();
		for (InterceptorRegistration registration : registrations) {
			interceptors.add(registration.getInterceptor());
		}
		return interceptors ;
	}

}
