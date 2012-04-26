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

import java.util.Collections;
import java.util.Map;

import javax.servlet.ServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.annotation.ModelAttributeMethodProcessor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * A Servlet-specific {@link org.springframework.web.method.annotation.ModelAttributeMethodProcessor} that applies data
 * binding through a WebDataBinder of type {@link ServletRequestDataBinder}. 
 * 
 * <p>Also adds a fall-back strategy to instantiate the model attribute from a
 * URI template variable or from a request parameter if the name matches the
 * model attribute name and there is an appropriate type conversion strategy.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletModelAttributeMethodProcessor extends ModelAttributeMethodProcessor {

	/**
	 * @param annotationNotRequired if "true", non-simple method arguments and
	 * return values are considered model attributes with or without a 
	 * {@code @ModelAttribute} annotation.
	 */
	public ServletModelAttributeMethodProcessor(boolean annotationNotRequired) {
		super(annotationNotRequired);
	}

	/**
	 * Instantiate the model attribute from a URI template variable or from a
	 * request parameter if the name matches to the model attribute name and
	 * if there is an appropriate type conversion strategy. If none of these
	 * are true delegate back to the base class.
	 * @see #createAttributeFromRequestValue(String, String, MethodParameter, WebDataBinderFactory, NativeWebRequest)
	 */
	@Override
	protected final Object createAttribute(String attributeName, 
										   MethodParameter parameter, 
										   WebDataBinderFactory binderFactory, 
										   NativeWebRequest request) throws Exception {

		String value = getRequestValueForAttribute(attributeName, request);
		if (value != null) {
			Object attribute = createAttributeFromRequestValue(value, attributeName, parameter, binderFactory, request);
			if (attribute != null) {
				return attribute;
			}
		}

		return super.createAttribute(attributeName, parameter, binderFactory, request);
	}
	
	/**
	 * Obtain a value from the request that may be used to instantiate the
	 * model attribute through type conversion from String to the target type. 
	 * <p>The default implementation looks for the attribute name to match
	 * a URI variable first and then a request parameter.
	 * @param attributeName the model attribute name
	 * @param request the current request
	 * @return the request value to try to convert or {@code null}
	 */
	protected String getRequestValueForAttribute(String attributeName, NativeWebRequest request) {
		Map<String, String> variables = getUriTemplateVariables(request);
		if (StringUtils.hasText(variables.get(attributeName))) {
			return variables.get(attributeName);
		}
		else if (StringUtils.hasText(request.getParameter(attributeName))) {
			return request.getParameter(attributeName);
		}
		else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	protected final Map<String, String> getUriTemplateVariables(NativeWebRequest request) {
		Map<String, String> variables = 
			(Map<String, String>) request.getAttribute(
					HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
		return (variables != null) ? variables : Collections.<String, String>emptyMap();
	}

	/**
	 * Create a model attribute from a String request value (e.g. URI template
	 * variable, request parameter) using type conversion.
	 * <p>The default implementation converts only if there a registered 
	 * {@link Converter} that can perform the conversion.
	 * @param sourceValue the source value to create the model attribute from
	 * @param attributeName the name of the attribute, never {@code null}
	 * @param parameter the method parameter
	 * @param binderFactory for creating WebDataBinder instance
	 * @param request the current request
	 * @return the created model attribute, or {@code null}
	 * @throws Exception
	 */
	protected Object createAttributeFromRequestValue(String sourceValue,
												 String attributeName, 
												 MethodParameter parameter, 
												 WebDataBinderFactory binderFactory, 
												 NativeWebRequest request) throws Exception {
		DataBinder binder = binderFactory.createBinder(request, null, attributeName);
		ConversionService conversionService = binder.getConversionService();
		if (conversionService != null) {
			TypeDescriptor source = TypeDescriptor.valueOf(String.class);
			TypeDescriptor target = new TypeDescriptor(parameter);
			if (conversionService.canConvert(source, target)) {
				return binder.convertIfNecessary(sourceValue, parameter.getParameterType(), parameter);
			}
		}
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 * <p>Downcast {@link WebDataBinder} to {@link ServletRequestDataBinder} before binding.
	 * @see ServletRequestDataBinderFactory
	 */
	@Override
	protected void bindRequestParameters(WebDataBinder binder, NativeWebRequest request) {
		ServletRequest servletRequest = request.getNativeRequest(ServletRequest.class);
		ServletRequestDataBinder servletBinder = (ServletRequestDataBinder) binder;
		servletBinder.bind(servletRequest);
	}

}