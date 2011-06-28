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

import java.lang.annotation.Annotation;
import java.util.List;

import javax.servlet.ServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.multipart.RequestPartServletServerHttpRequest;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.util.WebUtils;

/**
 * Resolves method arguments annotated with @{@link RequestPart} expecting the request to be a 
 * {@link MultipartHttpServletRequest} and binding the method argument to a specific part of the multipart request.
 * The name of the part is derived either from the {@link RequestPart} annotation or from the name of the method
 * argument as a fallback.
 * 
 * <p>An @{@link RequestPart} method argument will be validated if annotated with {@code @Valid}. In case of 
 * validation failure, a {@link RequestPartNotValidException} is thrown and can be handled automatically through
 * the {@link DefaultHandlerExceptionResolver}. A {@link Validator} can be configured globally in XML configuration 
 * with the Spring MVC namespace or in Java-based configuration with @{@link EnableWebMvc}.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class RequestPartMethodArgumentResolver extends AbstractMessageConverterMethodArgumentResolver {

	public RequestPartMethodArgumentResolver(List<HttpMessageConverter<?>> messageConverters) {
		super(messageConverters);
	}

	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(RequestPart.class);
	}

	public Object resolveArgument(MethodParameter parameter, 
								  ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest, 
								  WebDataBinderFactory binderFactory) throws Exception {

		ServletRequest servletRequest = webRequest.getNativeRequest(ServletRequest.class);
		MultipartHttpServletRequest multipartServletRequest = 
			WebUtils.getNativeRequest(servletRequest, MultipartHttpServletRequest.class);
		if (multipartServletRequest == null) {
			throw new IllegalStateException(
					"Current request is not of type " + MultipartRequest.class.getName());
		}
		
		String partName = getPartName(parameter);
		HttpInputMessage inputMessage = new RequestPartServletServerHttpRequest(multipartServletRequest, partName);

		Object arg = readWithMessageConverters(inputMessage, parameter, parameter.getParameterType());
		
		if (isValidationApplicable(arg, parameter)) {
			WebDataBinder binder = binderFactory.createBinder(webRequest, arg, partName);
			binder.validate();
			Errors errors = binder.getBindingResult();
			if (errors.hasErrors()) {
				throw new RequestPartNotValidException(errors);
			}
		}
		
		return arg;
	}

	private String getPartName(MethodParameter parameter) {
		RequestPart annot = parameter.getParameterAnnotation(RequestPart.class);
		String partName = annot.value();
		if (partName.length() == 0) {
			partName = parameter.getParameterName();
			Assert.notNull(partName, "Request part name for argument type [" + parameter.getParameterType().getName()
					+ "] not available, and parameter name information not found in class file either.");
		}
		return partName;
	}

	/**
	 * Whether to validate the given @{@link RequestPart} method argument. The default implementation checks 
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

}
