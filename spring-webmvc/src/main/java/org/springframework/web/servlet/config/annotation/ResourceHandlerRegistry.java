/*
 * Copyright 2002-2016 the original author or authors.
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

/**
 * Stores registrations of resource handlers for serving static resources such as images, css files and others
 * through Spring MVC including setting cache headers optimized for efficient loading in a web browser.
 * Resources can be served out of locations under web application root, from the classpath, and others.
 *
 * <p>To create a resource handler, use {@link #addResourceHandler(String...)} providing the URL path patterns
 * for which the handler should be invoked to serve static resources (e.g. {@code "/resources/**"}).
 *
 * <p>Then use additional methods on the returned {@link ResourceHandlerRegistration} to add one or more
 * locations from which to serve static content from (e.g. {{@code "/"},
 * {@code "classpath:/META-INF/public-web-resources/"}}) or to specify a cache period for served resources.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see DefaultServletHandlerConfigurer
 */
public class ResourceHandlerRegistry {

	private final ServletContext servletContext;

	private final ApplicationContext applicationContext;

	private final ContentNegotiationManager contentNegotiationManager;

	private final List<ResourceHandlerRegistration> registrations = new ArrayList<>();

	private int order = Integer.MAX_VALUE -1;


	/**
	 * Create a new resource handler registry for the given application context.
	 * @param applicationContext the Spring application context
	 * @param servletContext the corresponding Servlet context
	 */
	public ResourceHandlerRegistry(ApplicationContext applicationContext, ServletContext servletContext) {
		this(applicationContext, servletContext, null);
	}

	/**
	 * Create a new resource handler registry for the given application context.
	 * @param applicationContext the Spring application context
	 * @param servletContext the corresponding Servlet context
	 * @param contentNegotiationManager the content negotiation manager to use
	 * @since 4.3
	 */
	public ResourceHandlerRegistry(ApplicationContext applicationContext, ServletContext servletContext,
			ContentNegotiationManager contentNegotiationManager) {

		Assert.notNull(applicationContext, "ApplicationContext is required");
		this.applicationContext = applicationContext;
		this.servletContext = servletContext;
		this.contentNegotiationManager = contentNegotiationManager;
	}


	/**
	 * Add a resource handler for serving static resources based on the specified URL path
	 * patterns. The handler will be invoked for every incoming request that matches to
	 * one of the specified path patterns.
	 * @return A {@link ResourceHandlerRegistration} to use to further configure the
	 * registered resource handler
	 */
	public ResourceHandlerRegistration addResourceHandler(String... pathPatterns) {
		ResourceHandlerRegistration registration =
				new ResourceHandlerRegistration(this.applicationContext, pathPatterns);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * Whether a resource handler has already been registered for the given path pattern.
	 */
	public boolean hasMappingForPattern(String pathPattern) {
		for (ResourceHandlerRegistration registration : this.registrations) {
			if (Arrays.asList(registration.getPathPatterns()).contains(pathPattern)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Specify the order to use for resource handling relative to other {@link HandlerMapping}s
	 * configured in the Spring MVC application context.
	 * <p>The default value used is {@code Integer.MAX_VALUE-1}.
	 */
	public ResourceHandlerRegistry setOrder(int order) {
		this.order = order;
		return this;
	}

	/**
	 * Return a handler mapping with the mapped resource handlers; or {@code null} in case
	 * of no registrations.
	 */
	protected AbstractHandlerMapping getHandlerMapping() {
		if (this.registrations.isEmpty()) {
			return null;
		}

		Map<String, HttpRequestHandler> urlMap = new LinkedHashMap<>();
		for (ResourceHandlerRegistration registration : this.registrations) {
			for (String pathPattern : registration.getPathPatterns()) {
				ResourceHttpRequestHandler handler = registration.getRequestHandler();
				handler.setServletContext(this.servletContext);
				handler.setApplicationContext(this.applicationContext);
				handler.setContentNegotiationManager(this.contentNegotiationManager);
				try {
					handler.afterPropertiesSet();
				}
				catch (Exception ex) {
					throw new BeanInitializationException("Failed to init ResourceHttpRequestHandler", ex);
				}
				urlMap.put(pathPattern, handler);
			}
		}

		SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
		handlerMapping.setOrder(order);
		handlerMapping.setUrlMap(urlMap);
		return handlerMapping;
	}

}
