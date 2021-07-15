/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;

/**
 * Stores registrations of resource handlers for serving static resources such
 * as images, css files and others through Spring MVC including setting cache
 * headers optimized for efficient loading in a web browser. Resources can be
 * served out of locations under web application root, from the classpath, and
 * others.
 *
 * <p>To create a resource handler, use {@link #addResourceHandler(String...)}
 * providing the URL path patterns for which the handler should be invoked to
 * serve static resources (e.g. {@code "/resources/**"}).
 *
 * <p>Then use additional methods on the returned
 * {@link ResourceHandlerRegistration} to add one or more locations from which
 * to serve static content from (e.g. {{@code "/"},
 * {@code "classpath:/META-INF/public-web-resources/"}}) or to specify a cache
 * period for served resources.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see DefaultServletHandlerConfigurer
 */
public class ResourceHandlerRegistry {

	private final ServletContext servletContext;

	private final ApplicationContext applicationContext;

	@Nullable
	private final ContentNegotiationManager contentNegotiationManager;

	@Nullable
	private final UrlPathHelper pathHelper;

	private final List<ResourceHandlerRegistration> registrations = new ArrayList<>();

	private int order = Ordered.LOWEST_PRECEDENCE - 1;


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
			@Nullable ContentNegotiationManager contentNegotiationManager) {

		this(applicationContext, servletContext, contentNegotiationManager, null);
	}

	/**
	 * A variant of
	 * {@link #ResourceHandlerRegistry(ApplicationContext, ServletContext, ContentNegotiationManager)}
	 * that also accepts the {@link UrlPathHelper} used for mapping requests to static resources.
	 * @since 4.3.13
	 */
	public ResourceHandlerRegistry(ApplicationContext applicationContext, ServletContext servletContext,
			@Nullable ContentNegotiationManager contentNegotiationManager, @Nullable UrlPathHelper pathHelper) {

		Assert.notNull(applicationContext, "ApplicationContext is required");
		this.applicationContext = applicationContext;
		this.servletContext = servletContext;
		this.contentNegotiationManager = contentNegotiationManager;
		this.pathHelper = pathHelper;
	}


	/**
	 * Add a resource handler to serve static resources. The handler is invoked
	 * for requests that match one of the specified URL path patterns.
	 * <p>Patterns such as {@code "/static/**"} or {@code "/css/{filename:\\w+\\.css}"}
	 * are supported.
	 * <p>For pattern syntax see {@link PathPattern} when parsed patterns
	 * are {@link PathMatchConfigurer#setPatternParser enabled} or
	 * {@link AntPathMatcher} otherwise. The syntax is largely the same with
	 * {@link PathPattern} more tailored for web usage and more efficient.
	 */
	public ResourceHandlerRegistration addResourceHandler(String... pathPatterns) {
		ResourceHandlerRegistration registration = new ResourceHandlerRegistration(pathPatterns);
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
	 * Specify the order to use for resource handling relative to other {@link HandlerMapping HandlerMappings}
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
	@Nullable
	protected AbstractHandlerMapping getHandlerMapping() {
		if (this.registrations.isEmpty()) {
			return null;
		}
		Map<String, HttpRequestHandler> urlMap = new LinkedHashMap<>();
		for (ResourceHandlerRegistration registration : this.registrations) {
			ResourceHttpRequestHandler handler = getRequestHandler(registration);
			for (String pathPattern : registration.getPathPatterns()) {
				urlMap.put(pathPattern, handler);
			}
		}
		return new SimpleUrlHandlerMapping(urlMap, this.order);
	}

	@SuppressWarnings("deprecation")
	private ResourceHttpRequestHandler getRequestHandler(ResourceHandlerRegistration registration) {
		ResourceHttpRequestHandler handler = registration.getRequestHandler();
		if (this.pathHelper != null) {
			handler.setUrlPathHelper(this.pathHelper);
		}
		if (this.contentNegotiationManager != null) {
			handler.setContentNegotiationManager(this.contentNegotiationManager);
		}
		handler.setServletContext(this.servletContext);
		handler.setApplicationContext(this.applicationContext);
		try {
			handler.afterPropertiesSet();
		}
		catch (Throwable ex) {
			throw new BeanInitializationException("Failed to init ResourceHttpRequestHandler", ex);
		}
		return handler;
	}

}
