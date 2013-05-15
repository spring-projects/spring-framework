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
import java.io.PushbackInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

/**
 * Resolves method arguments annotated with {@code @RequestBody} and handles
 * return values from methods annotated with {@code @ResponseBody} by reading
 * and writing to the body of the request or response with an
 * {@link HttpMessageConverter}.
 *
 * <p>An {@code @RequestBody} method argument is also validated if it is
 * annotated with {@code @javax.validation.Valid}. In case of validation
 * failure, {@link MethodArgumentNotValidException} is raised and results
 * in a 400 response status code if {@link DefaultHandlerExceptionResolver}
 * is configured.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class RequestResponseBodyMethodProcessor extends AbstractMessageConverterMethodProcessor {

	public RequestResponseBodyMethodProcessor(List<HttpMessageConverter<?>> messageConverters) {
		super(messageConverters);
	}

	public RequestResponseBodyMethodProcessor(List<HttpMessageConverter<?>> messageConverters,
			ContentNegotiationManager contentNegotiationManager) {

		super(messageConverters, contentNegotiationManager);
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(RequestBody.class);
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return returnType.getMethodAnnotation(ResponseBody.class) != null;
	}

	/**
	 * {@inheritDoc}
	 * @throws MethodArgumentNotValidException if validation fails
	 * @throws HttpMessageNotReadableException if {@link RequestBody#required()}
	 * 	is {@code true} and there is no body content or if there is no suitable
	 * 	converter to read the content with.
	 */
	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		Object argument = readWithMessageConverters(webRequest, parameter, parameter.getGenericParameterType());

		String name = Conventions.getVariableNameForParameter(parameter);
		WebDataBinder binder = binderFactory.createBinder(webRequest, argument, name);

		if (argument != null) {
			validate(binder, parameter);
		}

		mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());

		return argument;
	}

	private void validate(WebDataBinder binder, MethodParameter parameter) throws Exception, MethodArgumentNotValidException {

		Annotation[] annotations = parameter.getParameterAnnotations();
		for (Annotation annot : annotations) {
			if (annot.annotationType().getSimpleName().startsWith("Valid")) {
				Object hints = AnnotationUtils.getValue(annot);
				binder.validate(hints instanceof Object[] ? (Object[]) hints : new Object[] {hints});
				BindingResult bindingResult = binder.getBindingResult();
				if (bindingResult.hasErrors()) {
					if (isBindExceptionRequired(binder, parameter)) {
						throw new MethodArgumentNotValidException(parameter, bindingResult);
					}
				}
				break;
			}
		}
	}

	/**
	 * Whether to raise a {@link MethodArgumentNotValidException} on validation errors.
	 * @param binder the data binder used to perform data binding
	 * @param parameter the method argument
	 * @return {@code true} if the next method argument is not of type {@link Errors}.
	 */
	private boolean isBindExceptionRequired(WebDataBinder binder, MethodParameter parameter) {
		int i = parameter.getParameterIndex();
		Class<?>[] paramTypes = parameter.getMethod().getParameterTypes();
		boolean hasBindingResult = (paramTypes.length > (i + 1) && Errors.class.isAssignableFrom(paramTypes[i + 1]));

		return !hasBindingResult;
	}

	@Override
	protected <T> Object readWithMessageConverters(NativeWebRequest webRequest,
			MethodParameter methodParam,  Type paramType) throws IOException, HttpMediaTypeNotSupportedException {

		final HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		HttpInputMessage inputMessage = new ServletServerHttpRequest(servletRequest);

		RequestBody annot = methodParam.getParameterAnnotation(RequestBody.class);
		if (!annot.required()) {
			InputStream inputStream = inputMessage.getBody();
			if (inputStream == null) {
				return null;
			}
			else if (inputStream.markSupported()) {
				inputStream.mark(1);
				if (inputStream.read() == -1) {
					return null;
				}
				inputStream.reset();
			}
			else {
				final PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream);
				int b = pushbackInputStream.read();
				if (b == -1) {
					return null;
				}
				else {
					pushbackInputStream.unread(b);
				}
				inputMessage = new ServletServerHttpRequest(servletRequest) {
					@Override
					public InputStream getBody() throws IOException {
						// Form POST should not get here
						return pushbackInputStream;
					}
				};
			}
		}

		return super.readWithMessageConverters(inputMessage, methodParam, paramType);
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest)
			throws IOException, HttpMediaTypeNotAcceptableException {

		mavContainer.setRequestHandled(true);
		if (returnValue != null) {
			writeWithMessageConverters(returnValue, returnType, webRequest);
		}
	}

}
