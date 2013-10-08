/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.servlet.resource;

import org.springframework.util.Assert;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * An interceptor that exposes the
 * {@link PublicResourceUrlProvider ResourceUrlPathTranslator}
 * instance it is configured with as a request attribute.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class PublicResourceUrlProviderExposingInterceptor extends HandlerInterceptorAdapter {

	/**
	 * Name of request attribute that holds
	 * {@link PublicResourceUrlProvider ResourceUrlPathTranslator}.
	 */
	public static final String RESOURCE_URL_PROVIDER_ATTR = PublicResourceUrlProvider.class.getName().toString();


	private final PublicResourceUrlProvider resourceUrlProvider;


	public PublicResourceUrlProviderExposingInterceptor(PublicResourceUrlProvider resourceUrlProvider) {
		Assert.notNull(resourceUrlProvider, "'resourceUrlProvider' is required");
		this.resourceUrlProvider = resourceUrlProvider;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
			Object handler) throws Exception {

		request.setAttribute(RESOURCE_URL_PROVIDER_ATTR, this.resourceUrlProvider);
		return true;
	}

}
