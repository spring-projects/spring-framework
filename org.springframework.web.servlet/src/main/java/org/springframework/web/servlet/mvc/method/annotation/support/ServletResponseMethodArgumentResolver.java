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
import java.io.OutputStream;
import java.io.Writer;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.MethodParameter;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

/**
 * Implementation of {@link HandlerMethodArgumentResolver} that supports {@link ServletResponse} and related arguments.
 *
 * @author Arjen Poutsma
 */
public class ServletResponseMethodArgumentResolver implements HandlerMethodArgumentResolver {

	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> parameterType = parameter.getParameterType();
		return ServletResponse.class.isAssignableFrom(parameterType) ||
				OutputStream.class.isAssignableFrom(parameterType) || Writer.class.isAssignableFrom(parameterType);
	}

	public boolean usesResponseArgument(MethodParameter parameter) {
		return true;
	}

	public Object resolveArgument(MethodParameter parameter,
								  ModelMap model,
								  NativeWebRequest webRequest, 
								  WebDataBinderFactory binderFactory) throws IOException {
		HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
		Class<?> parameterType = parameter.getParameterType();

		if (ServletResponse.class.isAssignableFrom(parameterType)) {
			Object nativeResponse = webRequest.getNativeResponse(parameterType);
			if (nativeResponse == null) {
				throw new IllegalStateException(
						"Current response is not of type [" + parameterType.getName() + "]: " + response);
			}
			return nativeResponse;
		}
		else if (OutputStream.class.isAssignableFrom(parameterType)) {
			return response.getOutputStream();
		}
		else if (Writer.class.isAssignableFrom(parameterType)) {
			return response.getWriter();
		}
		// should not happen
		throw new UnsupportedOperationException();
	}
}
