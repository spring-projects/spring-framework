/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.reactive.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.core.Ordered;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.handler.AbstractUrlHandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.resource.ResourceTransformer;
import org.springframework.web.reactive.resource.ResourceTransformerSupport;
import org.springframework.web.reactive.resource.ResourceUrlProvider;
import org.springframework.web.reactive.resource.ResourceWebHandler;
import org.springframework.web.server.WebHandler;

/**
 * Stores registrations of resource handlers for serving static resources such
 * as images, css files and others through Spring WebFlux including setting cache
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
 * @author Brian Clozel
 * @since 5.0
 */
public class ResourceHandlerRegistry {

	private final ResourceLoader resourceLoader;

	private final List<ResourceHandlerRegistration> registrations = new ArrayList<>();

	private int order = Ordered.LOWEST_PRECEDENCE - 1;

	@Nullable
	private ResourceUrlProvider resourceUrlProvider;


	/**
	 * Create a new resource handler registry for the given resource loader
	 * (typically an application context).
	 * @param resourceLoader the resource loader to use
	 */
	public ResourceHandlerRegistry(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Configure the {@link ResourceUrlProvider} that can be used by
	 * {@link org.springframework.web.reactive.resource.ResourceTransformer} instances.
	 * @param resourceUrlProvider the resource URL provider to use
	 * @since 5.0.11
	 */
	public void setResourceUrlProvider(@Nullable ResourceUrlProvider resourceUrlProvider) {
		this.resourceUrlProvider = resourceUrlProvider;
	}



	/**
	 * Add a resource handler for serving static resources based on the specified
	 * URL path patterns. The handler will be invoked for every incoming request
	 * that matches to one of the specified path patterns.
	 * <p>Patterns like {@code "/static/**"} or {@code "/css/{filename:\\w+\\.css}"}
	 * are allowed. See {@link org.springframework.web.util.pattern.PathPattern}
	 * for more details on the syntax.
	 * @return a {@link ResourceHandlerRegistration} to use to further configure
	 * the registered resource handler
	 */
	public ResourceHandlerRegistration addResourceHandler(String... patterns) {
		ResourceHandlerRegistration registration = new ResourceHandlerRegistration(this.resourceLoader, patterns);
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
	 * Specify the order to use for resource handling relative to other
	 * {@code HandlerMapping}s configured in the Spring configuration.
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
	protected AbstractUrlHandlerMapping getHandlerMapping() {
		if (this.registrations.isEmpty()) {
			return null;
		}
		Map<String, WebHandler> urlMap = new LinkedHashMap<>();
		for (ResourceHandlerRegistration registration : this.registrations) {
			ResourceWebHandler handler = getRequestHandler(registration);
			for (String pathPattern : registration.getPathPatterns()) {
				urlMap.put(pathPattern, handler);
			}
		}
		return new SimpleUrlHandlerMapping(urlMap, this.order);
	}

	private ResourceWebHandler getRequestHandler(ResourceHandlerRegistration registration) {
		ResourceWebHandler handler = registration.getRequestHandler();
		for (ResourceTransformer transformer : handler.getResourceTransformers()) {
			if (transformer instanceof ResourceTransformerSupport resourceTransformerSupport) {
				resourceTransformerSupport.setResourceUrlProvider(this.resourceUrlProvider);
			}
		}
		try {
			handler.afterPropertiesSet();
		}
		catch (Throwable ex) {
			throw new BeanInitializationException("Failed to init ResourceHttpRequestHandler", ex);
		}
		return handler;
	}

}
