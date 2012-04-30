/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.web.servlet.handler;

import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * {@link ServletWebRequest} subclass that is aware of
 * {@link org.springframework.web.servlet.DispatcherServlet}'s
 * request context, such as the Locale determined by the configured
 * {@link org.springframework.web.servlet.LocaleResolver}.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #getLocale()
 * @see org.springframework.web.servlet.LocaleResolver
 */
public class DispatcherServletWebRequest extends ServletWebRequest {

	/**
	 * Create a new DispatcherServletWebRequest instance for the given request.
	 * @param request current HTTP request
	 */
	public DispatcherServletWebRequest(HttpServletRequest request) {
		super(request);
	}

	/**
	 * Create a new DispatcherServletWebRequest instance for the given request and response.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 */
	public DispatcherServletWebRequest(HttpServletRequest request, HttpServletResponse response) {
		super(request, response);
	}

	@Override
	public Locale getLocale() {
		return RequestContextUtils.getLocale(getRequest());
	}

}
