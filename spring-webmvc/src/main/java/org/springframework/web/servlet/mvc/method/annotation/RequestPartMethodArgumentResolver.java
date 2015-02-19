/*
 * Copyright 2002-2015 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.support.RequestPartServletServerHttpRequest;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.util.WebUtils;

/**
 * Resolves the following method arguments:
 * <ul>
 * <li>Annotated with {@code @RequestPart}
 * <li>Of type {@link MultipartFile} in conjunction with Spring's {@link MultipartResolver} abstraction
 * <li>Of type {@code javax.servlet.http.Part} in conjunction with Servlet 3.0 multipart requests
 * </ul>
 *
 * <p>When a parameter is annotated with {@code @RequestPart}, the content of the part is
 * passed through an {@link HttpMessageConverter} to resolve the method argument with the
 * 'Content-Type' of the request part in mind. This is analogous to what @{@link RequestBody}
 * does to resolve an argument based on the content of a regular request.
 *
 * <p>When a parameter is not annotated or the name of the part is not specified,
 * it is derived from the name of the method argument.
 *
 * <p>Automatic validation may be applied if the argument is annotated with
 * {@code @javax.validation.Valid}. In case of validation failure, a
 * {@link MethodArgumentNotValidException} is raised and a 400 response status
 * code returned if {@link DefaultHandlerExceptionResolver} is configured.
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
	 * <li>annotated with {@code @RequestPart}
	 * <li>of type {@link MultipartFile} unless annotated with {@code @RequestParam}
	 * <li>of type {@code javax.servlet.http.Part} unless annotated with {@code @RequestParam}
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

	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {

		HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
		assertIsMultipartRequest(servletRequest);

		MultipartHttpServletRequest multipartRequest =
				WebUtils.getNativeRequest(servletRequest, MultipartHttpServletRequest.class);

		String partName = getPartName(parameter);
		Object arg;

		if (MultipartFile.class.equals(parameter.getParameterType())) {
			Assert.notNull(multipartRequest, "Expected MultipartHttpServletRequest: is a MultipartResolver configured?");
			arg = multipartRequest.getFile(partName);
		}
		else if (isMultipartFileCollection(parameter)) {
			Assert.notNull(multipartRequest, "Expected MultipartHttpServletRequest: is a MultipartResolver configured?");
			arg = multipartRequest.getFiles(partName);
		}
		else if ("javax.servlet.http.Part".equals(parameter.getParameterType().getName())) {
			arg = servletRequest.getPart(partName);
		}
		else {
			try {
				HttpInputMessage inputMessage = new RequestPartServletServerHttpRequest(servletRequest, partName);
				arg = readWithMessageConverters(inputMessage, parameter, parameter.getParameterType());
				WebDataBinder binder = binderFactory.createBinder(request, arg, partName);
				if (arg != null) {
					validate(binder, parameter);
				}
				mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + partName, binder.getBindingResult());
			}
			catch (MissingServletRequestPartException ex) {
				// handled below
				arg = null;
			}
		}

		RequestPart ann = parameter.getParameterAnnotation(RequestPart.class);
		boolean isRequired = (ann == null || ann.required());

		if (arg == null && isRequired) {
			throw new MissingServletRequestPartException(partName);
		}

		return arg;
	}

	private static void assertIsMultipartRequest(HttpServletRequest request) {
		String contentType = request.getContentType();
		if (contentType == null || !contentType.toLowerCase().startsWith("multipart/")) {
			throw new MultipartException("The current request is not a multipart request");
		}
	}

	private String getPartName(MethodParameter methodParam) {
		RequestPart ann = methodParam.getParameterAnnotation(RequestPart.class);
		String partName = (ann != null ? ann.value() : "");
		if (partName.length() == 0) {
			partName = methodParam.getParameterName();
			if (partName == null) {
				throw new IllegalArgumentException("Request part name for argument type [" +
						methodParam.getNestedParameterType().getName() +
						"] not specified, and parameter name information not found in class file either.");
			}
		}
		return partName;
	}

	private boolean isMultipartFileCollection(MethodParameter methodParam) {
		Class<?> paramType = methodParam.getParameterType();
		if (Collection.class.equals(paramType) || List.class.isAssignableFrom(paramType)){
			Class<?> valueType = GenericCollectionTypeResolver.getCollectionParameterType(methodParam);
			if (MultipartFile.class.equals(valueType)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Validate the request part if applicable.
	 * <p>The default implementation checks for {@code @javax.validation.Valid},
	 * Spring's {@link org.springframework.validation.annotation.Validated},
	 * and custom annotations whose name starts with "Valid".
	 * @param binder the DataBinder to be used
	 * @param methodParam the method parameter
	 * @throws MethodArgumentNotValidException in case of a binding error which
	 * is meant to be fatal (i.e. without a declared {@link Errors} parameter)
	 */
	private void validate(WebDataBinder binder, MethodParameter methodParam) throws MethodArgumentNotValidException {
		Annotation[] annotations = methodParam.getParameterAnnotations();
		for (Annotation ann : annotations) {
			if (ann.annotationType().getSimpleName().startsWith("Valid")) {
				Object hints = AnnotationUtils.getValue(ann);
				binder.validate(hints instanceof Object[] ? (Object[]) hints : new Object[] {hints});
				BindingResult bindingResult = binder.getBindingResult();
				if (bindingResult.hasErrors() && isBindExceptionRequired(binder, methodParam)) {
					throw new MethodArgumentNotValidException(methodParam, bindingResult);
				}
				break;
			}
		}
	}

	/**
	 * Whether to raise a fatal bind exception on validation errors.
	 * @param binder the data binder used to perform data binding
	 * @param methodParam the method argument
	 * @return {@code true} if the next method argument is not of type {@link Errors}
	 */
	private boolean isBindExceptionRequired(WebDataBinder binder, MethodParameter methodParam) {
		int i = methodParam.getParameterIndex();
		Class<?>[] paramTypes = methodParam.getMethod().getParameterTypes();
		boolean hasBindingResult = (paramTypes.length > (i + 1) && Errors.class.isAssignableFrom(paramTypes[i + 1]));
		return !hasBindingResult;
	}

}
