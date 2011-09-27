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

import java.util.Collections;
import java.util.Map;

import javax.servlet.ServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.validation.DataBinder;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.annotation.support.ModelAttributeMethodProcessor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ServletRequestDataBinderFactory;

/**
 * A Servlet-specific {@link ModelAttributeMethodProcessor} that applies data
 * binding through a WebDataBinder of type {@link ServletRequestDataBinder}. 
 * 
 * <p>Adds a fall-back strategy to instantiate a model attribute from a 
 * URI template variable combined with type conversion, if the model attribute 
 * name matches to a URI template variable name.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletModelAttributeMethodProcessor extends ModelAttributeMethodProcessor {

	/**
	 * @param annotationNotRequired if {@code true}, any non-simple type 
	 * argument or return value is regarded as a model attribute even without 
	 * the presence of a {@code @ModelAttribute} annotation in which case the 
	 * attribute name is derived from the model attribute's type.
	 */
	public ServletModelAttributeMethodProcessor(boolean annotationNotRequired) {
		super(annotationNotRequired);
	}

	/**
	 * Add a fall-back strategy to instantiate the model attribute from a URI 
	 * template variable with type conversion, if the model attribute name 
	 * matches to a URI variable name.
	 */
	@Override
	protected Object createAttribute(String attributeName, 
									 MethodParameter parameter, 
									 WebDataBinderFactory binderFactory, 
									 NativeWebRequest request) throws Exception {
		
		Map<String, String> uriVariables = getUriTemplateVariables(request);
		
		if (uriVariables.containsKey(attributeName)) {
			DataBinder binder = binderFactory.createBinder(request, null, attributeName);
			return binder.convertIfNecessary(uriVariables.get(attributeName), parameter.getParameterType());
		}
		
		return super.createAttribute(attributeName, parameter, binderFactory, request);
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> getUriTemplateVariables(NativeWebRequest request) {
		
		Map<String, String> uriTemplateVars = 
			(Map<String, String>) request.getAttribute(
					HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
		
		return (uriTemplateVars != null) ? uriTemplateVars : Collections.<String, String>emptyMap();
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