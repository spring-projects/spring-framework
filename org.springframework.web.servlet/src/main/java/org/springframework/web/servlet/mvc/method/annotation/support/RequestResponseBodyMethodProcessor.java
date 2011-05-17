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
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves method arguments annotated with @{@link RequestBody}. Handles return values from methods annotated with 
 * {@link ResponseBody}.
 * 
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class RequestResponseBodyMethodProcessor extends AbstractMessageConverterMethodProcessor {

	public RequestResponseBodyMethodProcessor(List<HttpMessageConverter<?>> messageConverters) {
		super(messageConverters);
	}

	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(RequestBody.class);
	}

	public boolean supportsReturnType(MethodParameter returnType) {
		return returnType.getMethodAnnotation(ResponseBody.class) != null;
	}

	public Object resolveArgument(MethodParameter parameter,
								  ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest,
								  WebDataBinderFactory binderFactory)
			throws IOException, HttpMediaTypeNotSupportedException {
		return readWithMessageConverters(webRequest, parameter, parameter.getParameterType());
	}

	public void handleReturnValue(Object returnValue,
								  MethodParameter returnType,
								  ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest) throws IOException, HttpMediaTypeNotAcceptableException {
		mavContainer.setResolveView(false);
		if (returnValue != null) {
			writeWithMessageConverters(returnValue, returnType, createInputMessage(webRequest),
					createOutputMessage(webRequest));
		}
	}

	@Override
	protected HttpOutputMessage createOutputMessage(NativeWebRequest webRequest) {
		HttpServletResponse servletResponse = (HttpServletResponse) webRequest.getNativeResponse();
		return new ServletServerHttpResponse(servletResponse);
	}

}