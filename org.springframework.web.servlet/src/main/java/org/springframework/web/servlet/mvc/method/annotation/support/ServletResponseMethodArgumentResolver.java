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
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

/**
 * Resolves response-related method argument values of types:
 * <ul>
 * <li>{@link ServletResponse}
 * <li>{@link OutputStream}
 * <li>{@link Writer}
 * </ul>
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletResponseMethodArgumentResolver implements HandlerMethodArgumentResolver {

	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> paramType = parameter.getParameterType();
		return ServletResponse.class.isAssignableFrom(paramType)
				|| OutputStream.class.isAssignableFrom(paramType) 
				|| Writer.class.isAssignableFrom(paramType);
	}

	/**
	 * {@inheritDoc}
	 * <p>Sets the {@link ModelAndViewContainer#setResolveView(boolean)} flag to {@code false} to indicate
	 * that the method signature provides access to the response. If subsequently the underlying method  
	 * returns {@code null}, view resolution will be bypassed.
	 * @see ServletInvocableHandlerMethod#invokeAndHandle(NativeWebRequest, ModelAndViewContainer, Object...)
	 */
	public Object resolveArgument(MethodParameter parameter,
								  ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest, 
								  WebDataBinderFactory binderFactory) throws IOException {
		
		mavContainer.setResolveView(false);

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
		else {
			// should not happen
			throw new UnsupportedOperationException();
		}
	}
	
}
