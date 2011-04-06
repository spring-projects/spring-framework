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

package org.springframework.web.servlet.mvc.method.annotation.support;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.support.CookieValueMethodArgumentResolver;
import org.springframework.web.util.WebUtils;

/**
 * A {@link CookieValueMethodArgumentResolver} for Servlet environments.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletCookieValueMethodArgumentResolver extends CookieValueMethodArgumentResolver {

	public ServletCookieValueMethodArgumentResolver(ConfigurableBeanFactory beanFactory) {
		super(beanFactory);
	}

	@Override
	protected Object resolveNamedValueArgument(NativeWebRequest webRequest, 
											   MethodParameter parameter, 
											   String cookieName) throws Exception {
		HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		Cookie cookieValue = WebUtils.getCookie(servletRequest, cookieName);
		if (Cookie.class.isAssignableFrom(parameter.getParameterType())) {
			return cookieValue;
		}
		else if (cookieValue != null) {
			return getUrlPathHelper().decodeRequestString(servletRequest, cookieValue.getValue());
		}
		else {
			return null;
		}
	}
}
