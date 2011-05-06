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

package org.springframework.web.servlet.config.annotation;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.RequestToViewNameTranslator;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 * Helps with view controllers. View controllers provide a direct mapping between a URL path and view name. This is
 * useful when serving requests that don't require application-specific controller logic and can be forwarded
 * directly to a view for rendering.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ViewControllerConfigurer {

	private final Map<String, Controller> urlMap = new LinkedHashMap<String, Controller>();

	private int order = 1;

	/**
	 * Map the URL path to a view name derived by convention through the DispatcherServlet's
	 * {@link RequestToViewNameTranslator}.
	 * @return the same {@link ViewControllerConfigurer} instance for convenient chained method invocation
	 */
	public ViewControllerConfigurer mapViewNameByConvention(String urlPath) {
		return mapViewName(urlPath, null);
	}

	/**
	 * Map the URL path to the specified view name.
	 * @return the same {@link ViewControllerConfigurer} instance for convenient chained method invocation
	 */
	public ViewControllerConfigurer mapViewName(String urlPath, String viewName) {
		ParameterizableViewController controller = new ParameterizableViewController();
		controller.setViewName(viewName);
		urlMap.put(urlPath, controller);
		return this;
	}

	/**
	 * Specify the order in which to check view controller path mappings relative to other {@link HandlerMapping}
	 * instances in the Spring MVC web application context. The default value is 1.
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Get the order in which to check view controller path mappings relative to other {@link HandlerMapping}s.
	 */
	public int getOrder() {
		return order;
	}

	/**
	 * Return a {@link SimpleUrlHandlerMapping} with URL path to view controllers mappings.
	 */
	protected SimpleUrlHandlerMapping getHandlerMapping() {
		SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
		handlerMapping.setOrder(order);
		handlerMapping.setUrlMap(urlMap);
		return handlerMapping;
	}

}