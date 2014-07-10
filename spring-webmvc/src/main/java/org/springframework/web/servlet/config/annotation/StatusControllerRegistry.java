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

import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helps with configuring a list of status controllers.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class StatusControllerRegistry {

	private final List<AbstractStatusControllerRegistration> registrations = new ArrayList<AbstractStatusControllerRegistration>();

	private int order = -1;

	/**
	 * Add a new status controller for status code other than 3xx.
	 */
	public StatusControllerRegistration addStatusController(String urlPath, HttpStatus statusCode) {
		Assert.isTrue(!statusCode.is3xxRedirection(), "Status code can not be a 3xx redirection");
		StatusControllerRegistration registration = new StatusControllerRegistration(urlPath, statusCode);
		registrations.add(registration);
		return registration;
	}

	/**
	 * Add a new status controller for 3xx status code (redirects).
	 */
	public RedirectStatusControllerRegistration addStatusController(String urlPath,
			HttpStatus statusCode, String redirectPath) {
		Assert.isTrue(statusCode.is3xxRedirection(), "Status code must be a 3xx redirection");
		RedirectStatusControllerRegistration registration = new RedirectStatusControllerRegistration(urlPath, statusCode, redirectPath);
		registrations.add(registration);
		return registration;
	}

	/**
	 * Specify the order to use for StatusControllers mappings relative to other {@link HandlerMapping}s
	 * configured in the Spring MVC application context. The default value for status controllers is -1,
	 * which is 1 lower than the value used for annotated controllers.
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Returns a handler mapping with the mapped StatusControllers; or {@code null} in case of no registrations.
	 */
	protected AbstractHandlerMapping getHandlerMapping() {
		if (registrations.isEmpty()) {
			return null;
		}

		Map<String, Object> urlMap = new LinkedHashMap<String, Object>();
		for (AbstractStatusControllerRegistration registration : registrations) {
			urlMap.put(registration.getUrlPath(), registration.getStatusController());
		}

		SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
		handlerMapping.setOrder(order);
		handlerMapping.setUrlMap(urlMap);
		return handlerMapping;
	}
}
