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
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.support.MethodArgumentNotValidException;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.RequestPartServletServerHttpRequest;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.util.WebUtils;

/**
 * Resolves the following method arguments:
 * <ul>
 * 	<li>Arguments annotated with @{@link RequestPart}.
 * 	<li>Arguments of type {@link MultipartFile} in conjunction with Spring's 
 * 		{@link MultipartResolver} abstraction.
 * 	<li>Arguments of type {@code javax.servlet.http.Part} in conjunction 
 * 		with Servlet 3.0 multipart requests.
 * </ul>
 * 
 * <p>When a parameter is annotated with @{@link RequestPart} the content of the 
 * part is passed through an {@link HttpMessageConverter} to resolve the method 
 * argument with the 'Content-Type' of the request part in mind. This is 
 * analogous to what @{@link RequestBody} does to resolve an argument based on
 * the content of a non-multipart request.
 * 
 * <p>When a parameter is not annotated or the name of the part is not specified, 
 * it is derived from the name of the method argument.
 * 
 * <p>Automatic validation can be applied to a @{@link RequestPart} method argument 
 * through the use of {@code @Valid}. In case of validation failure, a 
 * {@link RequestPartNotValidException} is thrown and handled automatically through
 * the {@link DefaultHandlerExceptionResolver}. 
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class RequestPartMethodArgumentResolver extends AbstractMessageConverterMethodArgumentResolver {

	public RequestPartMethodArgumentResolver(List<HttpMessageConverter<?>> messageConverters) {
		super(messageConverters);
	}

	/**
	 * Supports the following:
	 * <ul>
	 * 	<li>@RequestPart-annotated method arguments.
	 * 	<li>Arguments of type {@link MultipartFile} unless annotated with {@link RequestParam}.
	 * 	<li>Arguments of type {@code javax.servlet.http.Part} unless annotated with {@link RequestParam}.
	 * </ul>
	 */
	public boolean supportsParameter(MethodParameter parameter) {
		if (parameter.hasParameterAnnotation(RequestPart.class)) {
			return true;
		}
		else {
			if (parameter.hasParameterAnnotation(RequestParam.class)){
				return false;
			}
			else if (MultipartFile.class.equals(parameter.getParameterType())) {
				return true;
			}
			else if ("javax.servlet.http.Part".equals(parameter.getParameterType().getName())) {
				return true;
			}
			else {
				return false;
			}
		}
	}

	public Object resolveArgument(MethodParameter parameter, 
								  ModelAndViewContainer mavContainer,
								  NativeWebRequest request, 
								  WebDataBinderFactory binderFactory) throws Exception {

		String partName = getPartName(parameter);
		Object arg;

		HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
		MultipartHttpServletRequest multipartRequest = 
			WebUtils.getNativeRequest(servletRequest, MultipartHttpServletRequest.class);

		if (MultipartFile.class.equals(parameter.getParameterType())) {
			assertMultipartRequest(multipartRequest, request);
			arg = multipartRequest.getFile(partName);
		}
		else if (isMultipartFileCollection(parameter)) {
			assertMultipartRequest(multipartRequest, request);
			arg = multipartRequest.getFiles(partName);
		}
		else if ("javax.servlet.http.Part".equals(parameter.getParameterType().getName())) {
			arg = servletRequest.getPart(partName);
		}
		else {
			HttpInputMessage inputMessage = new RequestPartServletServerHttpRequest(servletRequest, partName);
			arg = readWithMessageConverters(inputMessage, parameter, parameter.getParameterType());
			if (isValidationApplicable(arg, parameter)) {
				WebDataBinder binder = binderFactory.createBinder(request, arg, partName);
				binder.validate();
				BindingResult bindingResult = binder.getBindingResult();
				if (bindingResult.hasErrors()) {
					throw new MethodArgumentNotValidException(parameter, bindingResult);
				}
			}
		}

		if (arg == null) {
			handleMissingValue(partName, parameter);
		}
		
		return arg;
	}

	private String getPartName(MethodParameter parameter) {
		RequestPart annot = parameter.getParameterAnnotation(RequestPart.class);
		String partName = (annot != null) ? annot.value() : "";
		if (partName.length() == 0) {
			partName = parameter.getParameterName();
			Assert.notNull(partName, "Request part name for argument type [" + parameter.getParameterType().getName()
					+ "] not available, and parameter name information not found in class file either.");
		}
		return partName;
	}

	private void assertMultipartRequest(MultipartHttpServletRequest multipartRequest, NativeWebRequest request) {
		if (multipartRequest == null) {
			throw new IllegalStateException("Current request is not of type [" + MultipartRequest.class.getName()
					+ "]: " + request + ". Do you have a MultipartResolver configured?");
		}
	}
	
	private boolean isMultipartFileCollection(MethodParameter parameter) {
		Class<?> paramType = parameter.getParameterType();
		if (Collection.class.equals(paramType) || List.class.isAssignableFrom(paramType)){
			Class<?> valueType = GenericCollectionTypeResolver.getCollectionParameterType(parameter);
			if (valueType != null && valueType.equals(MultipartFile.class)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Invoked if the resolved argument value is {@code null}. The default implementation raises 
	 * a {@link ServletRequestBindingException} if the method parameter is required.
	 * @param partName the name used to look up the request part
	 * @param param the method argument
	 */
	protected void handleMissingValue(String partName, MethodParameter param) throws ServletRequestBindingException {
		RequestPart annot = param.getParameterAnnotation(RequestPart.class);
		boolean isRequired = (annot != null) ? annot.required() : true;
		if (isRequired) {
			String paramType = param.getParameterType().getName();
			throw new ServletRequestBindingException(
					"Missing request part '" + partName + "' for method parameter type [" + paramType + "]");
		}
	}
	
	/**
	 * Whether to validate the given @{@link RequestPart} method argument. 
	 * The default implementation return {@code true} if the argument value is not {@code null} 
	 * and the method parameter is annotated with {@code @Valid}.
	 * @param argumentValue the validation candidate
	 * @param parameter the method argument declaring the validation candidate
	 * @return {@code true} if validation should be invoked, {@code false} otherwise.
	 */
	protected boolean isValidationApplicable(Object argumentValue, MethodParameter parameter) {
		if (argumentValue == null) {
			return false;
		}
		else {
			Annotation[] annotations = parameter.getParameterAnnotations();
			for (Annotation annot : annotations) {
				if ("Valid".equals(annot.annotationType().getSimpleName())) {
					return true;
				}
			}
			return false;
		}
	}

}
