/*
 * Copyright 2002-2014 the original author or authors.
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
 * Enables the registration of view controllers that have no logic other than to
 * return the view name they're configured with. This is an alternative to
 * writing a controller manually to do the same.
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @since 3.1
 */
public class ViewControllerRegistry {

	private final List<ViewControllerRegistration> registrations = new ArrayList<ViewControllerRegistration>();

	private int order = 1;


	/**
	 * Register a view controller mapped to the given URL path or URL path pattern.
	 */
	public ViewControllerRegistration addViewController(String urlPath) {
		ViewControllerRegistration registration = new ViewControllerRegistration(urlPath);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * Specify the order to use for the {@code HandlerMapping} used to map view
	 * controllers relative to other handler mappings configured in Spring MVC.
	 * <p>By default this is set to 1, i.e. right after annotated controllers,
	 * which are ordered at 0.
	 */
	public void setOrder(int order) {
		this.order = order;
	}


	/**
	 * Return the {@code HandlerMapping} that contains the registered view
	 * controller mappings, or {@code null} for no registrations.
	 */
	protected AbstractHandlerMapping getHandlerMapping() {
		if (this.registrations.isEmpty()) {
			return null;
		}
		Map<String, Object> urlMap = new LinkedHashMap<String, Object>();
		for (ViewControllerRegistration registration : this.registrations) {
			urlMap.put(registration.getUrlPath(), registration.getViewController());
		}
		SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
		handlerMapping.setOrder(this.order);
		handlerMapping.setUrlMap(urlMap);
		return handlerMapping;
	}

}
