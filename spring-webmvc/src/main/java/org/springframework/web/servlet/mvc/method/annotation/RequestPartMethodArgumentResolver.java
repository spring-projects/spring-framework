/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.UsesJava8;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
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
 * @author Brian Clozel
 * @since 3.1
 */
public class RequestPartMethodArgumentResolver extends AbstractMessageConverterMethodArgumentResolver {

	/**
	 * Basic constructor with converters only.
	 */
	public RequestPartMethodArgumentResolver(List<HttpMessageConverter<?>> messageConverters) {
		super(messageConverters);
	}

	/**
	 * Constructor with converters and {@code Request~} and
	 * {@code ResponseBodyAdvice}.
	 */
	public RequestPartMethodArgumentResolver(List<HttpMessageConverter<?>> messageConverters,
			List<Object> requestResponseBodyAdvice) {

		super(messageConverters, requestResponseBodyAdvice);
	}


	/**
	 * Supports the following:
	 * <ul>
	 * <li>annotated with {@code @RequestPart}
	 * <li>of type {@link MultipartFile} unless annotated with {@code @RequestParam}
	 * <li>of type {@code javax.servlet.http.Part} unless annotated with {@code @RequestParam}
	 * </ul>
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		if (parameter.hasParameterAnnotation(RequestPart.class)) {
			return true;
		}
		else {
			if (parameter.hasParameterAnnotation(RequestParam.class)){
				return false;
			}
			else if (MultipartFile.class == parameter.getParameterType()) {
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

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {

		HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
		assertIsMultipartRequest(servletRequest);
		MultipartHttpServletRequest multipartRequest =
				WebUtils.getNativeRequest(servletRequest, MultipartHttpServletRequest.class);

		String partName = getPartName(parameter);
		Class<?> paramType = parameter.getParameterType();
		boolean optional = paramType.getName().equals("java.util.Optional");
		if (optional) {
			parameter = new MethodParameter(parameter);
			parameter.increaseNestingLevel();
			paramType = parameter.getNestedParameterType();
		}

		Object arg;

		if (MultipartFile.class == paramType) {
			Assert.notNull(multipartRequest, "Expected MultipartHttpServletRequest: is a MultipartResolver configured?");
			arg = multipartRequest.getFile(partName);
		}
		else if (isMultipartFileCollection(parameter)) {
			Assert.notNull(multipartRequest, "Expected MultipartHttpServletRequest: is a MultipartResolver configured?");
			arg = multipartRequest.getFiles(partName);
		}
		else if (isMultipartFileArray(parameter)) {
			Assert.notNull(multipartRequest, "Expected MultipartHttpServletRequest: is a MultipartResolver configured?");
			List<MultipartFile> files = multipartRequest.getFiles(partName);
			arg = files.toArray(new MultipartFile[files.size()]);
		}
		else if ("javax.servlet.http.Part".equals(paramType.getName())) {
			assertIsMultipartRequest(servletRequest);
			arg = servletRequest.getPart(partName);
		}
		else if (isPartCollection(parameter)) {
			assertIsMultipartRequest(servletRequest);
			arg = new ArrayList<Object>(servletRequest.getParts());
		}
		else if (isPartArray(parameter)) {
			assertIsMultipartRequest(servletRequest);
			arg = RequestPartResolver.resolvePart(servletRequest);
		}
		else {
			try {
				HttpInputMessage inputMessage = new RequestPartServletServerHttpRequest(servletRequest, partName);
				arg = readWithMessageConverters(inputMessage, parameter, parameter.getNestedGenericParameterType());
				WebDataBinder binder = binderFactory.createBinder(request, arg, partName);
				if (arg != null) {
					validateIfApplicable(binder, parameter);
					if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
						throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
					}
				}
				mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + partName, binder.getBindingResult());
			}
			catch (MissingServletRequestPartException ex) {
				// handled below
				arg = null;
			}
		}

		RequestPart requestPart = parameter.getParameterAnnotation(RequestPart.class);
		boolean isRequired = ((requestPart == null || requestPart.required()) && !optional);

		if (arg == null && isRequired) {
			throw new MissingServletRequestPartException(partName);
		}
		if (optional) {
			arg = OptionalResolver.resolveValue(arg);
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
		RequestPart requestPart = methodParam.getParameterAnnotation(RequestPart.class);
		String partName = (requestPart != null ? requestPart.name() : "");
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
		Class<?> collectionType = getCollectionParameterType(methodParam);
		return MultipartFile.class == collectionType;
	}

	private boolean isMultipartFileArray(MethodParameter methodParam) {
		Class<?> paramType = methodParam.getNestedParameterType().getComponentType();
		return MultipartFile.class == paramType;
	}

	private boolean isPartCollection(MethodParameter methodParam) {
		Class<?> collectionType = getCollectionParameterType(methodParam);
		return (collectionType != null && "javax.servlet.http.Part".equals(collectionType.getName()));
	}

	private boolean isPartArray(MethodParameter methodParam) {
		Class<?> paramType = methodParam.getNestedParameterType().getComponentType();
		return (paramType != null && "javax.servlet.http.Part".equals(paramType.getName()));
	}

	private Class<?> getCollectionParameterType(MethodParameter methodParam) {
		Class<?> paramType = methodParam.getNestedParameterType();
		if (Collection.class == paramType || List.class.isAssignableFrom(paramType)){
			Class<?> valueType = GenericCollectionTypeResolver.getCollectionParameterType(methodParam);
			if (valueType != null) {
				return valueType;
			}
		}
		return null;
	}


	/**
	 * Inner class to avoid hard-coded dependency on Servlet 3.0 Part type...
	 */
	private static class RequestPartResolver {

		public static Object resolvePart(HttpServletRequest servletRequest) throws Exception {
			Collection<Part> parts = servletRequest.getParts();
			return parts.toArray(new Part[parts.size()]);
		}
	}


	/**
	 * Inner class to avoid hard-coded dependency on Java 8 Optional type...
	 */
	@UsesJava8
	private static class OptionalResolver {

		public static Object resolveValue(Object value) {
			return Optional.ofNullable(value);
		}
	}

}
