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

import java.util.List;
import java.util.Map;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.annotation.InitBinderDataBinderFactory;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Creates a WebDataBinder of type {@link ServletRequestDataBinder} that can 
 * also use URI template variables values for data binding purposes.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletRequestDataBinderFactory extends InitBinderDataBinderFactory {

	/**
	 * Create a new instance.
	 * @param binderMethods one or more {@code @InitBinder} methods
	 * @param initializer provides global data binder initialization
	 */
	public ServletRequestDataBinderFactory(List<InvocableHandlerMethod> binderMethods, 
										   WebBindingInitializer initializer) {
		super(binderMethods, initializer);
	}
	
	/**
	 * Create a WebDataBinder of type {@link ServletRequestDataBinder} that can 
	 * also use URI template variables values for data binding purposes.
	 */
	@Override
	protected WebDataBinder createBinderInstance(Object target, String objectName, final NativeWebRequest request) {
		return new ServletRequestDataBinder(target, objectName) {
			@Override
			protected void doBind(MutablePropertyValues mpvs) {
				mergeUriTemplateVariables(mpvs, request);
				super.doBind(mpvs);
			}
		};
	}

	/**
	 * Merge URI variable values into the given PropertyValues.
	 * @param mpvs the PropertyValues to add to
	 * @param request the current request
	 */
	@SuppressWarnings("unchecked")
	protected final void mergeUriTemplateVariables(MutablePropertyValues mpvs, NativeWebRequest request) {

		Map<String, String> uriTemplateVars = 
			(Map<String, String>) request.getAttribute(
					HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
		
		if (uriTemplateVars != null){
			for (String variableName : uriTemplateVars.keySet()) {
				if (!mpvs.contains(variableName)) {
					mpvs.addPropertyValue(variableName, uriTemplateVars.get(variableName));
				}
			}
		}
	}

}
