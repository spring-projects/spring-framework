/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.method.annotation;

import java.beans.PropertyEditor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.UriComponentsContributor;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.WebUtils;

/**
 * Resolves method arguments annotated with @{@link RequestParam}, arguments of
 * type {@link MultipartFile} in conjunction with Spring's {@link MultipartResolver}
 * abstraction, and arguments of type {@code javax.servlet.http.Part} in conjunction
 * with Servlet 3.0 multipart requests. This resolver can also be created in default
 * resolution mode in which simple types (int, long, etc.) not annotated
 * with @{@link RequestParam} are also treated as request parameters with the
 * parameter name derived from the argument name.
 *
 * <p>If the method parameter type is {@link Map}, the name specified in the
 * annotation is used to resolve the request parameter String value. The value is
 * then converted to a {@link Map} via type conversion assuming a suitable
 * {@link Converter} or {@link PropertyEditor} has been registered.
 * Or if a request parameter name is not specified the
 * {@link RequestParamMapMethodArgumentResolver} is used instead to provide
 * access to all request parameters in the form of a map.
 *
 * <p>A {@link WebDataBinder} is invoked to apply type conversion to resolved request
 * header values that don't yet match the method parameter type.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 3.1
 * @see RequestParamMapMethodArgumentResolver
 */
public class RequestParamMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver
		implements UriComponentsContributor {

	private static final TypeDescriptor STRING_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(String.class);

	private final boolean useDefaultResolution;


	/**
	 * @param useDefaultResolution in default resolution mode a method argument
	 * that is a simple type, as defined in {@link BeanUtils#isSimpleProperty},
	 * is treated as a request parameter even if it it isn't annotated, the
	 * request parameter name is derived from the method parameter name.
	 */
	public RequestParamMethodArgumentResolver(boolean useDefaultResolution) {
		this.useDefaultResolution = useDefaultResolution;
	}

	/**
	 * @param beanFactory a bean factory used for resolving  ${...} placeholder
	 * and #{...} SpEL expressions in default values, or {@code null} if default
	 * values are not expected to contain expressions
	 * @param useDefaultResolution in default resolution mode a method argument
	 * that is a simple type, as defined in {@link BeanUtils#isSimpleProperty},
	 * is treated as a request parameter even if it it isn't annotated, the
	 * request parameter name is derived from the method parameter name.
	 */
	public RequestParamMethodArgumentResolver(ConfigurableBeanFactory beanFactory, boolean useDefaultResolution) {
		super(beanFactory);
		this.useDefaultResolution = useDefaultResolution;
	}


	/**
	 * Supports the following:
	 * <ul>
	 * <li>@RequestParam-annotated method arguments.
	 * This excludes {@link Map} params where the annotation doesn't
	 * specify a name.	See {@link RequestParamMapMethodArgumentResolver}
	 * instead for such params.
	 * <li>Arguments of type {@link MultipartFile}
	 * unless annotated with @{@link RequestPart}.
	 * <li>Arguments of type {@code javax.servlet.http.Part}
	 * unless annotated with @{@link RequestPart}.
	 * <li>In default resolution mode, simple type arguments
	 * even if not with @{@link RequestParam}.
	 * </ul>
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> paramType = parameter.getParameterType();
		if (parameter.hasParameterAnnotation(RequestParam.class)) {
			if (Map.class.isAssignableFrom(paramType)) {
				String paramName = parameter.getParameterAnnotation(RequestParam.class).value();
				return StringUtils.hasText(paramName);
			}
			else {
				return true;
			}
		}
		else {
			if (parameter.hasParameterAnnotation(RequestPart.class)) {
				return false;
			}
			else if (MultipartFile.class.equals(paramType) || "javax.servlet.http.Part".equals(paramType.getName())) {
				return true;
			}
			else if (this.useDefaultResolution) {
				return BeanUtils.isSimpleProperty(paramType);
			}
			else {
				return false;
			}
		}
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		RequestParam ann = parameter.getParameterAnnotation(RequestParam.class);
		return (ann != null ? new RequestParamNamedValueInfo(ann) : new RequestParamNamedValueInfo());
	}

	@Override
	protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest webRequest) throws Exception {
		HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		MultipartHttpServletRequest multipartRequest =
				WebUtils.getNativeRequest(servletRequest, MultipartHttpServletRequest.class);
		Object arg;

		if (MultipartFile.class.equals(parameter.getParameterType())) {
			assertIsMultipartRequest(servletRequest);
			Assert.notNull(multipartRequest, "Expected MultipartHttpServletRequest: is a MultipartResolver configured?");
			arg = multipartRequest.getFile(name);
		}
		else if (isMultipartFileCollection(parameter)) {
			assertIsMultipartRequest(servletRequest);
			Assert.notNull(multipartRequest, "Expected MultipartHttpServletRequest: is a MultipartResolver configured?");
			arg = multipartRequest.getFiles(name);
		}
		else if (isMultipartFileArray(parameter)) {
			assertIsMultipartRequest(servletRequest);
			Assert.notNull(multipartRequest, "Expected MultipartHttpServletRequest: is a MultipartResolver configured?");
			List<MultipartFile> multipartFiles = multipartRequest.getFiles(name);
			arg = multipartFiles.toArray(new MultipartFile[multipartFiles.size()]);
		}
		else if ("javax.servlet.http.Part".equals(parameter.getParameterType().getName())) {
			assertIsMultipartRequest(servletRequest);
			arg = servletRequest.getPart(name);
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
			arg = null;
			if (multipartRequest != null) {
				List<MultipartFile> files = multipartRequest.getFiles(name);
				if (!files.isEmpty()) {
					arg = (files.size() == 1 ? files.get(0) : files);
				}
			}
			if (arg == null) {
				String[] paramValues = webRequest.getParameterValues(name);
				if (paramValues != null) {
					arg = paramValues.length == 1 ? paramValues[0] : paramValues;
				}
			}
		}

		return arg;
	}

	private void assertIsMultipartRequest(HttpServletRequest request) {
		String contentType = request.getContentType();
		if (contentType == null || !contentType.toLowerCase().startsWith("multipart/")) {
			throw new MultipartException("The current request is not a multipart request");
		}
	}

	private boolean isMultipartFileCollection(MethodParameter parameter) {
		Class<?> collectionType = getCollectionParameterType(parameter);
		return ((collectionType != null) && collectionType.equals(MultipartFile.class));
	}

	private boolean isPartCollection(MethodParameter parameter) {
		Class<?> collectionType = getCollectionParameterType(parameter);
		return ((collectionType != null) && "javax.servlet.http.Part".equals(collectionType.getName()));
	}

	private boolean isPartArray(MethodParameter parameter) {
		Class<?> paramType = parameter.getParameterType().getComponentType();
		return ((paramType != null) && "javax.servlet.http.Part".equals(paramType.getName()));
	}

	private boolean isMultipartFileArray(MethodParameter parameter) {
		Class<?> paramType = parameter.getParameterType().getComponentType();
		return ((paramType != null) && MultipartFile.class.equals(paramType));
	}

	private Class<?> getCollectionParameterType(MethodParameter parameter) {
		Class<?> paramType = parameter.getParameterType();
		if (Collection.class.equals(paramType) || List.class.isAssignableFrom(paramType)){
			Class<?> valueType = GenericCollectionTypeResolver.getCollectionParameterType(parameter);
			if (valueType != null) {
				return valueType;
			}
		}
		return null;
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletException {
		throw new MissingServletRequestParameterException(name, parameter.getParameterType().getSimpleName());
	}

	@Override
	public void contributeMethodArgument(MethodParameter parameter, Object value,
			UriComponentsBuilder builder, Map<String, Object> uriVariables, ConversionService conversionService) {

		Class<?> paramType = parameter.getParameterType();
		if (Map.class.isAssignableFrom(paramType) || MultipartFile.class.equals(paramType) ||
				"javax.servlet.http.Part".equals(paramType.getName())) {
			return;
		}

		RequestParam ann = parameter.getParameterAnnotation(RequestParam.class);
		String name = (ann == null || StringUtils.isEmpty(ann.value()) ? parameter.getParameterName() : ann.value());

		if (value == null) {
			builder.queryParam(name);
		}
		else if (value instanceof Collection) {
			for (Object element : (Collection<?>) value) {
				element = formatUriValue(conversionService, TypeDescriptor.nested(parameter, 1), element);
				builder.queryParam(name, element);
			}
		}
		else {
			builder.queryParam(name, formatUriValue(conversionService, new TypeDescriptor(parameter), value));
		}
	}

	protected String formatUriValue(ConversionService cs, TypeDescriptor sourceType, Object value) {
		return (cs != null ? (String) cs.convert(value, sourceType, STRING_TYPE_DESCRIPTOR) : null);
	}


	private static class RequestParamNamedValueInfo extends NamedValueInfo {

		public RequestParamNamedValueInfo() {
			super("", false, ValueConstants.DEFAULT_NONE);
		}

		public RequestParamNamedValueInfo(RequestParam annotation) {
			super(annotation.value(), annotation.required(), annotation.defaultValue());
		}
	}


	private static class RequestPartResolver {

		public static Object resolvePart(HttpServletRequest servletRequest) throws Exception {
			return servletRequest.getParts().toArray(new Part[servletRequest.getParts().size()]);
		}
	}

}
