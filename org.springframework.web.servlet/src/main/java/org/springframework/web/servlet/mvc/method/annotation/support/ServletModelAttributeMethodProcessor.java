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

import java.beans.PropertyEditor;
import java.util.Map;

import javax.servlet.ServletRequest;

import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.validation.DataBinder;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.annotation.support.ModelAttributeMethodProcessor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * A Servlet-specific {@link ModelAttributeMethodProcessor} variant with the following further benefits:
 * <ul>
 * 	<li>Casts the data binder down to {@link ServletRequestDataBinder} prior to invoking bind on it
 * 	<li>Attempts to instantiate the model attribute using a path variable and type conversion
 * </ul>
 *  that casts
 * instance to {@link ServletRequestDataBinder} prior to invoking data binding.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletModelAttributeMethodProcessor extends ModelAttributeMethodProcessor {

	/**
	 * @param useDefaultResolution in default resolution mode a method argument that isn't a simple type, as
	 * defined in {@link BeanUtils#isSimpleProperty(Class)}, is treated as a model attribute even if it doesn't
	 * have an @{@link ModelAttribute} annotation with its name derived from the model attribute type.
	 */
	public ServletModelAttributeMethodProcessor(boolean useDefaultResolution) {
		super(useDefaultResolution);
	}

	/**
	 * Instantiates the model attribute by trying to match the model attribute name to a path variable.
	 * If a match is found an attempt is made to convert the String path variable to the expected 
	 * method parameter type through a registered {@link Converter} or {@link PropertyEditor}.
	 * If this fails the call is delegated back to the parent for default constructor instantiation.   
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected Object createAttribute(String attributeName, 
									 MethodParameter parameter, 
									 WebDataBinderFactory binderFactory, 
									 NativeWebRequest request) throws Exception {
		Map<String, String> uriTemplateVars = 
			(Map<String, String>) request.getAttribute(
					HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

		if (uriTemplateVars != null && uriTemplateVars.containsKey(attributeName)) {
			try {
				String var = uriTemplateVars.get(attributeName);
				DataBinder binder = binderFactory.createBinder(request, null, attributeName);
				return binder.convertIfNecessary(var, parameter.getParameterType());

			} catch (Exception exception) {
				logger.info("Model attribute '" + attributeName + "' matches to a URI template variable name. "
						+ "The URI template variable however couldn't converted to a model attribute instance: "
						+ exception.getMessage());
			}
		}
		
		return super.createAttribute(attributeName, parameter, binderFactory, request);
	}

	/**
	 * {@inheritDoc}
	 * <p>This implementation downcasts to {@link ServletRequestDataBinder} before invoking the bind operation.
	 */
	@Override
	protected void bindRequestParameters(WebDataBinder binder, NativeWebRequest request) {
		ServletRequest servletRequest = request.getNativeRequest(ServletRequest.class);
		ServletRequestDataBinder servletBinder = (ServletRequestDataBinder) binder;
		servletBinder.bind(servletRequest);
	}

}