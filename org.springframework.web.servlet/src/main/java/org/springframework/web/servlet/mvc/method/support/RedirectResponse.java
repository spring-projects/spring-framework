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

package org.springframework.web.servlet.mvc.method.support;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Provides annotated controller methods with convenience methods for setting
 * up response with a view name that will result in a redirect.
 * 
 * <p>An instance of this class is obtained via {@link ResponseContext#redirect}.
 *
 * @author Keith Donald
 * @author Rossen Stoyanchev
 * 
 * @since 3.1
 */
public class RedirectResponse {
	
	private final NativeWebRequest webRequest;

	private final ModelAndViewContainer mavContainer;
	
	RedirectResponse(NativeWebRequest webRequest, ModelAndViewContainer mavContainer) {
		this.webRequest = webRequest;
		this.mavContainer = mavContainer;
	}

	/**
	 * Add a URI template variable to use to expand the URI template into a URL.
	 * <p><strong>Note:</strong> URI template variables from the current 
	 * request are automatically used when expanding the redirect URI template. 
	 * They don't need to be added explicitly here.
	 */
	public RedirectResponse uriVariable(String name, Object value) {
		this.mavContainer.addAttribute(name, value);
		return this;
	}

	/**
	 * Add a URI template variable to use to expand the URI template into a URL.
	 * The name of the variable is selected using a
	 * {@link org.springframework.core.Conventions#getVariableName generated name}.
	 * <p><strong>Note:</strong> URI template variables from the current 
	 * request are automatically used when expanding the redirect URI template. 
	 * They don't need to be added explicitly here.
	 */
	public RedirectResponse uriVariable(Object value) {
		this.mavContainer.addAttribute(value);
		return this;
	}

	/**
	 * Add a query parameter to append to the redirect URL.
	 */
	public RedirectResponse queryParam(String name, Object value) {
		this.mavContainer.addAttribute(name, value);
		return this;
	}

	/**
	 * Add a query parameter to append to the redirect URL.
	 * The name of the parameter is selected using a
	 * {@link org.springframework.core.Conventions#getVariableName generated name}.
	 */
	public RedirectResponse queryParam(Object value) {
		this.mavContainer.addAttribute(value);
		return this;
	}

	/**
	 * Add a flash attribute to save and make available in the model of the 
	 * target controller method after the redirect.
	 */
	public RedirectResponse flashAttribute(String name, Object value) {
		getFlashMap().addAttribute(name, value);
		return this;
	}

	/**
	 * Add a flash attribute to save and make available in the model of the 
	 * target controller method after the redirect.
	 * The name of the attribute is selected using a
	 * {@link org.springframework.core.Conventions#getVariableName generated name}.
	 */
	public RedirectResponse flashAttribute(Object value) {
		getFlashMap().addAttribute(value);
		return this;
	}

	private FlashMap getFlashMap() {
		HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		return RequestContextUtils.getFlashMap(servletRequest);
	}

}
