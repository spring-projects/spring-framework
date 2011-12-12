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

package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;

import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
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

	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(RequestBody.class);
	}

	public boolean supportsReturnType(MethodParameter returnType) {
		return returnType.getMethodAnnotation(ResponseBody.class) != null;
	}

	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		Object arg = readWithMessageConverters(webRequest, parameter, parameter.getParameterType());
		Annotation[] annotations = parameter.getParameterAnnotations();
		for (Annotation annot : annotations) {
			if (annot.annotationType().getSimpleName().startsWith("Valid")) {
				String name = Conventions.getVariableNameForParameter(parameter);
				WebDataBinder binder = binderFactory.createBinder(webRequest, arg, name);
				Object hints = AnnotationUtils.getValue(annot);
				binder.validate(hints instanceof Object[] ? (Object[]) hints : new Object[] {hints});
				BindingResult bindingResult = binder.getBindingResult();
				if (bindingResult.hasErrors()) {
					throw new MethodArgumentNotValidException(parameter, bindingResult);
				}
			}
		}
		return arg;
	}

	public void handleReturnValue(Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest)
			throws IOException, HttpMediaTypeNotAcceptableException {

		mavContainer.setRequestHandled(true);
		if (returnValue != null) {
			writeWithMessageConverters(returnValue, returnType, webRequest);
		}
	}

}