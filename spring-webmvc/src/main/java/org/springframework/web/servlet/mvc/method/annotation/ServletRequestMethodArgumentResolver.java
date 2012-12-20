/*
 * Copyright 2002-2012 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Locale;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Resolves request-related method argument values of the following types:
 * <ul>
 * <li>{@link WebRequest}
 * <li>{@link ServletRequest}
 * <li>{@link MultipartRequest}
 * <li>{@link HttpSession}
 * <li>{@link Principal}
 * <li>{@link Locale}
 * <li>{@link InputStream}
 * <li>{@link Reader}
 * </ul>
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletRequestMethodArgumentResolver implements HandlerMethodArgumentResolver {

	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> paramType = parameter.getParameterType();
		return WebRequest.class.isAssignableFrom(paramType) ||
				ServletRequest.class.isAssignableFrom(paramType) ||
				MultipartRequest.class.isAssignableFrom(paramType) ||
				HttpSession.class.isAssignableFrom(paramType) ||
				Principal.class.isAssignableFrom(paramType) ||
				Locale.class.equals(paramType) ||
				InputStream.class.isAssignableFrom(paramType) ||
				Reader.class.isAssignableFrom(paramType);
	}

	public Object resolveArgument(
			MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory)
			throws IOException {

		Class<?> paramType = parameter.getParameterType();
		if (WebRequest.class.isAssignableFrom(paramType)) {
			return webRequest;
		}

		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		if (ServletRequest.class.isAssignableFrom(paramType) || MultipartRequest.class.isAssignableFrom(paramType)) {
			Object nativeRequest = webRequest.getNativeRequest(paramType);
			if (nativeRequest == null) {
				throw new IllegalStateException(
						"Current request is not of type [" + paramType.getName() + "]: " + request);
			}
			return nativeRequest;
		}
		else if (HttpSession.class.isAssignableFrom(paramType)) {
			return request.getSession();
		}
		else if (Principal.class.isAssignableFrom(paramType)) {
			return request.getUserPrincipal();
		}
		else if (Locale.class.equals(paramType)) {
			return RequestContextUtils.getLocale(request);
		}
		else if (InputStream.class.isAssignableFrom(paramType)) {
			return request.getInputStream();
		}
		else if (Reader.class.isAssignableFrom(paramType)) {
			return request.getReader();
		}
		else {
			// should never happen..
			Method method = parameter.getMethod();
			throw new UnsupportedOperationException("Unknown parameter type: " + paramType + " in method: " + method);
		}
	}

}
