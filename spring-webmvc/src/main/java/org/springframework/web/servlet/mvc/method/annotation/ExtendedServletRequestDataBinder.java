/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletRequest;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Subclass of {@link ServletRequestDataBinder} that adds URI template variables
 * to the values used for data binding.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ExtendedServletRequestDataBinder extends ServletRequestDataBinder {

	/**
	 * Create a new instance, with default object name.
	 * @param target the target object to bind onto (or {@code null}
	 * if the binder is just used to convert a plain parameter value)
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public ExtendedServletRequestDataBinder(Object target) {
		super(target);
	}

	/**
	 * Create a new instance.
	 * @param target the target object to bind onto (or {@code null}
	 * if the binder is just used to convert a plain parameter value)
	 * @param objectName the name of the target object
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public ExtendedServletRequestDataBinder(Object target, String objectName) {
		super(target, objectName);
	}

	/**
	 * Merge URI variables into the property values to use for data binding.
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected void addBindValues(MutablePropertyValues mpvs, ServletRequest request) {
		String attr = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		Map<String, String> uriVars = (Map<String, String>) request.getAttribute(attr);
		if (uriVars != null) {
			for (Entry<String, String> entry : uriVars.entrySet()) {
				if (mpvs.contains(entry.getKey())) {
					logger.warn("Skipping URI variable '" + entry.getKey()
							+ "' since the request contains a bind value with the same name.");
				}
				else {
					mpvs.addPropertyValue(entry.getKey(), entry.getValue());
				}
			}
		}
	}

}
