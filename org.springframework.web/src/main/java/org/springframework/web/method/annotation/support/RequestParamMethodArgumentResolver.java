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

import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;

/**
 * Implementation of {@link HandlerMethodArgumentResolver} that supports arguments annotated with
 * {@link RequestParam @RequestParam}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestParamMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	private final boolean resolveParamsWithoutAnnotations;

	/**
	 * Creates a {@link RequestParamMethodArgumentResolver} instance.
	 * 
	 * @param beanFactory the bean factory to use for resolving default value expressions
	 * @param resolveParamsWithoutAnnotations enable default resolution mode in which parameters without
	 * 		annotations that are simple types (see {@link BeanUtils#isSimpleProperty(Class)})  
	 * 		are also treated as model attributes with a default name based on the method argument name.
	 */
	public RequestParamMethodArgumentResolver(ConfigurableBeanFactory beanFactory, 
											  boolean resolveParamsWithoutAnnotations) {
		super(beanFactory);
		this.resolveParamsWithoutAnnotations = resolveParamsWithoutAnnotations;
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
		else if (this.resolveParamsWithoutAnnotations && !parameter.hasParameterAnnotations()) {
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
	protected Object resolveNamedValueArgument(NativeWebRequest webRequest,
											   MethodParameter parameter,
											   String paramName) throws Exception {
		MultipartRequest multipartRequest = webRequest.getNativeRequest(MultipartRequest.class);
		if (multipartRequest != null) {
			List<MultipartFile> files = multipartRequest.getFiles(paramName);
			if (!files.isEmpty()) {
				return (files.size() == 1 ? files.get(0) : files);
			}
		}

		String[] paramValues = webRequest.getParameterValues(paramName);
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
