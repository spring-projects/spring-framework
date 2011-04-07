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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.security.Principal;
import java.util.Locale;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Implementation of {@link HandlerMethodArgumentResolver} that supports {@link ServletRequest} and related arguments.
 *
 * @author Arjen Poutsma
 */
public class ServletRequestMethodArgumentResolver implements HandlerMethodArgumentResolver {

	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> parameterType = parameter.getParameterType();
		return ServletRequest.class.isAssignableFrom(parameterType) ||
				MultipartRequest.class.isAssignableFrom(parameterType) ||
				HttpSession.class.isAssignableFrom(parameterType) || Principal.class.isAssignableFrom(parameterType) ||
				Locale.class.equals(parameterType) || InputStream.class.isAssignableFrom(parameterType) ||
				Reader.class.isAssignableFrom(parameterType);
	}

	public boolean usesResponseArgument(MethodParameter parameter) {
		return false;
	}

	public Object resolveArgument(MethodParameter parameter,
								  ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest, 
								  WebDataBinderFactory binderFactory) throws IOException {
		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		Class<?> parameterType = parameter.getParameterType();

		if (ServletRequest.class.isAssignableFrom(parameterType) ||
				MultipartRequest.class.isAssignableFrom(parameterType)) {
			Object nativeRequest = webRequest.getNativeRequest(parameterType);
			if (nativeRequest == null) {
				throw new IllegalStateException(
						"Current request is not of type [" + parameterType.getName() + "]: " + request);
			}
			return nativeRequest;
		}
		else if (HttpSession.class.isAssignableFrom(parameterType)) {
			return request.getSession();
		}
		else if (Principal.class.isAssignableFrom(parameterType)) {
			return request.getUserPrincipal();
		}
		else if (Locale.class.equals(parameterType)) {
			return RequestContextUtils.getLocale(request);
		}
		else if (InputStream.class.isAssignableFrom(parameterType)) {
			return request.getInputStream();
		}
		else if (Reader.class.isAssignableFrom(parameterType)) {
			return request.getReader();
		}
		// should not happen
		throw new UnsupportedOperationException();
	}
}
