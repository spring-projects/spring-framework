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
import java.lang.annotation.Annotation;
import java.util.List;

import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.validation.Errors;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

/**
 * Resolves method arguments annotated with @{@link RequestBody} and handles return values from methods 
 * annotated with {@link ResponseBody}. 
 * 
 * <p>An @{@link RequestBody} method argument will be validated if annotated with {@code @Valid}. 
 * In case of validation failure, a {@link RequestBodyNotValidException} is thrown and handled 
 * automatically in {@link DefaultHandlerExceptionResolver}. 
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
								  WebDataBinderFactory binderFactory) throws Exception {
		Object arg = readWithMessageConverters(webRequest, parameter, parameter.getParameterType());
		if (isValidationApplicable(arg, parameter)) {
			String name = Conventions.getVariableNameForParameter(parameter);
			WebDataBinder binder = binderFactory.createBinder(webRequest, arg, name);
			binder.validate();
			Errors errors = binder.getBindingResult();
			if (errors.hasErrors()) {
				throw new RequestBodyNotValidException(errors);
			}
		}
		return arg;
	}

	/**
	 * Whether to validate the given @{@link RequestBody} method argument. The default implementation checks 
	 * if the parameter is also annotated with {@code @Valid}.
	 * @param argumentValue the validation candidate
	 * @param parameter the method argument declaring the validation candidate
	 * @return {@code true} if validation should be invoked, {@code false} otherwise.
	 */
	protected boolean isValidationApplicable(Object argumentValue, MethodParameter parameter) {
		Annotation[] annotations = parameter.getParameterAnnotations();
		for (Annotation annot : annotations) {
			if ("Valid".equals(annot.annotationType().getSimpleName())) {
				return true;
			}
		}
		return false;
	}
	
	public void handleReturnValue(Object returnValue,
								  MethodParameter returnType,
								  ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest) throws IOException, HttpMediaTypeNotAcceptableException {
		mavContainer.setResolveView(false);
		if (returnValue != null) {
			writeWithMessageConverters(returnValue, returnType, webRequest);
		}
	}

}