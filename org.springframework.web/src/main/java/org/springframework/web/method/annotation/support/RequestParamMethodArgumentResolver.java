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

package org.springframework.web.method.annotation.support;

import java.beans.PropertyEditor;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;

/**
 * Resolves method arguments annotated with @{@link RequestParam}. 
 * 
 * <p>If the method parameter type is {@link Map}, the request parameter name is resolved and then converted 
 * to a {@link Map} via type conversion assuming a suitable {@link PropertyEditor} or {@link Converter} is 
 * registered. Alternatively, see {@link RequestParamMapMethodArgumentResolver} for access to all request 
 * parameters in a {@link Map}. 
 * 
 * <p>If this class is created with default resolution mode on, simple types not annotated 
 * with @{@link RequestParam} are also treated as request parameters with the parameter name based 
 * on the method argument name. See the class constructor for more details.
 * 
 * <p>A {@link WebDataBinder} is invoked to apply type conversion to resolved request header values that 
 * don't yet match the method parameter type.
 * 
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see RequestParamMapMethodArgumentResolver
 */
public class RequestParamMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	private final boolean useDefaultResolution;

	/**
	 * @param beanFactory a bean factory to use for resolving  ${...} placeholder and #{...} SpEL expressions 
	 * in default values, or {@code null} if default values are not expected to contain expressions
	 * @param useDefaultResolution in default resolution mode a method argument that is a simple type, as
	 * defined in {@link BeanUtils#isSimpleProperty(Class)}, is treated as a request parameter even if it doesn't have
	 * an @{@link RequestParam} annotation, the request parameter name is derived from the method parameter name.
	 */
	public RequestParamMethodArgumentResolver(ConfigurableBeanFactory beanFactory, 
											  boolean useDefaultResolution) {
		super(beanFactory);
		this.useDefaultResolution = useDefaultResolution;
	}

	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> paramType = parameter.getParameterType();
		RequestParam requestParamAnnot = parameter.getParameterAnnotation(RequestParam.class);
		if (requestParamAnnot != null) {
			if (Map.class.isAssignableFrom(paramType)) {
				return StringUtils.hasText(requestParamAnnot.value());
			}
			return true;
		}
		else if (this.useDefaultResolution) {
			return BeanUtils.isSimpleProperty(paramType);
		}
		else {
			return false;
		}
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		RequestParam annotation = parameter.getParameterAnnotation(RequestParam.class);
		return (annotation != null) ? 
				new RequestParamNamedValueInfo(annotation) : 
				new RequestParamNamedValueInfo();
	}

	@Override
	protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest webRequest) throws Exception {
		MultipartRequest multipartRequest = webRequest.getNativeRequest(MultipartRequest.class);
		if (multipartRequest != null) {
			List<MultipartFile> files = multipartRequest.getFiles(name);
			if (!files.isEmpty()) {
				return (files.size() == 1 ? files.get(0) : files);
			}
		}

		String[] paramValues = webRequest.getParameterValues(name);
		if (paramValues != null) {
			return paramValues.length == 1 ? paramValues[0] : paramValues;
		}
		else {
			return null;
		}
	}

	@Override
	protected void handleMissingValue(String paramName, MethodParameter parameter) throws ServletException {
		throw new MissingServletRequestParameterException(paramName, parameter.getParameterType().getSimpleName());
	}

	private class RequestParamNamedValueInfo extends NamedValueInfo {

		private RequestParamNamedValueInfo() {
			super("", true, ValueConstants.DEFAULT_NONE);
		}
		
		private RequestParamNamedValueInfo(RequestParam annotation) {
			super(annotation.value(), annotation.required(), annotation.defaultValue());
		}
	}
}