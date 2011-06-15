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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

/**
 * Stores registrations of view controllers. A view controller does nothing more than return a specified 
 * view name. It saves you from having to write a controller when you want to forward the request straight 
 * through to a view such as a JSP.
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @since 3.1
 */
public class ViewControllerRegistry {

	private final List<ViewControllerRegistration> registrations = new ArrayList<ViewControllerRegistration>();
	
	private int order = 1;

	public ViewControllerRegistration addViewController(String urlPath) {
		ViewControllerRegistration registration = new ViewControllerRegistration(urlPath);
		registrations.add(registration);
		return registration;
	}
	
	/**
	 * Specify the order to use for ViewControllers mappings relative to other {@link HandlerMapping}s 
	 * configured in the Spring MVC application context. The default value for view controllers is 1, 
	 * which is 1 higher than the value used for annotated controllers.
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Returns a handler mapping with the mapped ViewControllers; or {@code null} in case of no registrations.
	 */
	protected AbstractHandlerMapping getHandlerMapping() {
		if (registrations.isEmpty()) {
			return null;
		}

		Map<String, Object> urlMap = new LinkedHashMap<String, Object>();
		for (ViewControllerRegistration registration : registrations) {
			urlMap.put(registration.getUrlPath(), registration.getViewController());
		}
		
		SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
		handlerMapping.setOrder(order);
		handlerMapping.setUrlMap(urlMap);
		return handlerMapping;
	}
	
}