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

package org.springframework.web.context.support;

import java.util.Map;
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.context.ServletContextAware;

/**
 * Exporter that takes Spring-defined objects and exposes them as
 * ServletContext attributes. Usually, bean references will be used
 * to export Spring-defined beans as ServletContext attributes.
 *
 * <p>Useful to make Spring-defined beans available to code that is
 * not aware of Spring at all, but rather just of the Servlet API.
 * Client code can then use plain ServletContext attribute lookups
 * to access those objects, despite them being defined in a Spring
 * application context.
 *
 * <p>Alternatively, consider using the WebApplicationContextUtils
 * class to access Spring-defined beans via the WebApplicationContext
 * interface. This makes client code aware of Spring API, of course.
 *
 * @author Juergen Hoeller
 * @since 1.1.4
 * @see javax.servlet.ServletContext#getAttribute
 * @see WebApplicationContextUtils#getWebApplicationContext
 */
public class ServletContextAttributeExporter implements ServletContextAware {

	protected final Log logger = LogFactory.getLog(getClass());

	private Map<String, Object> attributes;


	/**
	 * Set the ServletContext attributes to expose as key-value pairs.
	 * Each key will be considered a ServletContext attributes key,
	 * and each value will be used as corresponding attribute value.
	 * <p>Usually, you will use bean references for the values,
	 * to export Spring-defined beans as ServletContext attributes.
	 * Of course, it is also possible to define plain values to export.
	 */
	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		for (Map.Entry<String, Object> entry : attributes.entrySet()) {
			String attributeName = entry.getKey();
			if (logger.isWarnEnabled()) {
				if (servletContext.getAttribute(attributeName) != null) {
					logger.warn("Replacing existing ServletContext attribute with name '" + attributeName + "'");
				}
			}
			servletContext.setAttribute(attributeName, entry.getValue());
			if (logger.isInfoEnabled()) {
				logger.info("Exported ServletContext attribute with name '" + attributeName + "'");
			}
		}
	}

}
