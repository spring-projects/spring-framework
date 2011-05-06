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
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.method.annotation.InitBinderMethodDataBinderFactory;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

/**
 * An {@link InitBinderMethodDataBinderFactory} variation instantiating a data binder of type
 * {@link ServletRequestDataBinder} and further extending it with the ability to add URI template variables
 * to the values used in data binding.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletInitBinderMethodDataBinderFactory extends InitBinderMethodDataBinderFactory {

	/**
	 * Create an {@link ServletInitBinderMethodDataBinderFactory} instance.
	 * @param initBinderMethods init binder methods to use to initialize new data binders.
	 * @param bindingInitializer a WebBindingInitializer to use to initialize created data binder instances.
	 */
	public ServletInitBinderMethodDataBinderFactory(List<InvocableHandlerMethod> initBinderMethods,
													WebBindingInitializer bindingInitializer) {
		super(initBinderMethods, bindingInitializer);
	}

	/**
	 * {@inheritDoc}
	 * <p>This method creates a {@link ServletRequestDataBinder} instance that also adds URI template variables to
	 * the values used in data binding.
	 * <p>Subclasses wishing to override this method to provide their own ServletRequestDataBinder type can use the
	 * {@link #addUriTemplateVariables(MutablePropertyValues)} method to include URI template variables as follows:
	 * <pre>
	 * return new CustomServletRequestDataBinder(target, objectName) {
	 *    protected void doBind(MutablePropertyValues mpvs) {
	 *        addUriTemplateVariables(mpvs);
	 *        super.doBind(mpvs);
	 *    }
	 * };
	 * </pre>
	 */
	@Override
	protected WebDataBinder createBinderInstance(Object target, String objectName) {
		return new ServletRequestDataBinder(target, objectName) {

			protected void doBind(MutablePropertyValues mpvs) {
				addUriTemplateVariables(mpvs);
				super.doBind(mpvs);
			}
		};
	}

	/**
	 * Adds URI template variables to the given property values.
	 * @param mpvs the PropertyValues to add URI template variables to
	 */
	@SuppressWarnings("unchecked")
	protected void addUriTemplateVariables(MutablePropertyValues mpvs) {
		RequestAttributes requestAttrs = RequestContextHolder.getRequestAttributes();
		if (requestAttrs != null) {
			String key = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
			int scope = RequestAttributes.SCOPE_REQUEST;
			Map<String, String> uriTemplateVars = (Map<String, String>) requestAttrs.getAttribute(key, scope);
			mpvs.addPropertyValues(uriTemplateVars);
		}
	}

}