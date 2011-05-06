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

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

/**
 * Helps with configuring a handler for serving static resources such as images, css files and others through
 * Spring MVC including setting cache headers optimized for efficient loading in a web browser. Resources can
 * be served out of locations under web application root, from the classpath, and others.
 *
 * <p>To configure resource handling, use {@link #addPathMappings(String...)} to add one or more URL path patterns
 * within the current Servlet context, to use for serving resources from the handler, such as {@code "/resources/**"}.
 *
 * <p>Then use {@link #addResourceLocations(String...)} to add one or more locations from which to serve
 * static content. For example, {{@code "/"}, {@code "classpath:/META-INF/public-web-resources/"}} allows resources
 * to be served both from the web application root and from any JAR on the classpath that contains a
 * {@code /META-INF/public-web-resources/} directory, with resources in the web application root taking precedence.
 *
 * <p>Optionally use {@link #setCachePeriod(Integer)} to specify the cache period for the resources served by the
 * handler and {@link #setOrder(int)} to set the order in which to serve requests relative to other
 * {@link HandlerMapping} instances in the Spring MVC web application context.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 *
 * @see DefaultServletHandlerConfigurer
 */
public class ResourceConfigurer {

	private final List<String> pathPatterns = new ArrayList<String>();

	private final List<Resource> locations = new ArrayList<Resource>();

	private Integer cachePeriod;

	private int order = Integer.MAX_VALUE -1;

	private final ServletContext servletContext;

	private final ApplicationContext applicationContext;

	public ResourceConfigurer(ApplicationContext applicationContext, ServletContext servletContext) {
		Assert.notNull(applicationContext, "ApplicationContext is required");
		this.applicationContext = applicationContext;
		this.servletContext = servletContext;
	}

	/**
	 * Add a URL path pattern within the current Servlet context to use for serving static resources
	 * using the Spring MVC {@link ResourceHttpRequestHandler}, for example {@code "/resources/**"}.
	 * @return the same {@link ResourceConfigurer} instance for chained method invocation
	 */
	public ResourceConfigurer addPathMapping(String pathPattern) {
		return addPathMappings(pathPattern);
	}

	/**
	 * Add several URL path patterns within the current Servlet context to use for serving static resources
	 * using the Spring MVC {@link ResourceHttpRequestHandler}, for example {@code "/resources/**"}.
	 * @return the same {@link ResourceConfigurer} instance for chained method invocation
	 */
	public ResourceConfigurer addPathMappings(String...pathPatterns) {
		for (String path : pathPatterns) {
			this.pathPatterns.add(path);
		}
		return this;
	}

	/**
	 * Add resource location from which to serve static content. The location must point to a valid
	 * directory. <p>For example, a value of {@code "/"} will allow resources to be served both from the web
	 * application root. Also see {@link #addResourceLocations(String...)} for mapping several resource locations.
	 * @return the same {@link ResourceConfigurer} instance for chained method invocation
	 */
	public ResourceConfigurer addResourceLocation(String resourceLocation) {
		return addResourceLocations(resourceLocation);
	}

	/**
	 * Add one or more resource locations from which to serve static content. Each location must point to a valid
	 * directory. Multiple locations may be specified as a comma-separated list, and the locations will be checked
	 * for a given resource in the order specified.
	 * <p>For example, {{@code "/"}, {@code "classpath:/META-INF/public-web-resources/"}} allows resources to
	 * be served both from the web application root and from any JAR on the classpath that contains a
	 * {@code /META-INF/public-web-resources/} directory, with resources in the web application root taking precedence.
	 * @return the same {@link ResourceConfigurer} instance for chained method invocation
	 */
	public ResourceConfigurer addResourceLocations(String...resourceLocations) {
		for (String location : resourceLocations) {
			this.locations.add(applicationContext.getResource(location));
		}
		return this;
	}

	/**
	 * Specify the cache period for the resources served by the resource handler, in seconds. The default is to not
	 * send any cache headers but to rely on last-modified timestamps only. Set to 0 in order to send cache headers
	 * that prevent caching, or to a positive number of seconds to send cache headers with the given max-age value.
	 * @param cachePeriod the time to cache resources in seconds
	 * @return the same {@link ResourceConfigurer} instance for chained method invocation
	 */
	public ResourceConfigurer setCachePeriod(Integer cachePeriod) {
		this.cachePeriod = cachePeriod;
		return this;
	}

	/**
	 * Get the cache period for static resources served by the resource handler.
	 */
	public Integer getCachePeriod() {
		return cachePeriod;
	}

	/**
	 * Specify the order in which to serve static resources relative to other {@link HandlerMapping} instances in the
	 * Spring MVC web application context. The default value is {@code Integer.MAX_VALUE-1}.
	 */
	public ResourceConfigurer setOrder(int order) {
		this.order = order;
		return this;
	}

	/**
	 * Get the order in which to serve static resources relative other {@link HandlerMapping} instances.
	 * @return the same {@link ResourceConfigurer} instance for chained method invocation
	 */
	public Integer getOrder() {
		return order;
	}

	/**
	 * Return a {@link SimpleUrlHandlerMapping} with a {@link ResourceHttpRequestHandler} mapped to one or more
	 * URL path patterns. If the no path patterns were specified, the HandlerMapping returned contains an empty map.
	 */
	protected SimpleUrlHandlerMapping getHandlerMapping() {
		SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
		handlerMapping.setOrder(order);
		handlerMapping.setUrlMap(getUrlMap());
		return handlerMapping;
	}

	private Map<String, HttpRequestHandler> getUrlMap() {
		Map<String, HttpRequestHandler> urlMap = new LinkedHashMap<String, HttpRequestHandler>();
		if (!pathPatterns.isEmpty()) {
			ResourceHttpRequestHandler requestHandler = createRequestHandler();
			for (String pathPattern : pathPatterns) {
				urlMap.put(pathPattern, requestHandler);
			}
		}
		return urlMap;
	}

	/**
	 * Create a {@link ResourceHttpRequestHandler} instance.
	 */
	protected ResourceHttpRequestHandler createRequestHandler() {
		Assert.isTrue(!CollectionUtils.isEmpty(locations), "Path patterns specified but not resource locations.");
		ResourceHttpRequestHandler requestHandler = new ResourceHttpRequestHandler();
		requestHandler.setApplicationContext(applicationContext);
		requestHandler.setServletContext(servletContext);
		requestHandler.setLocations(locations);
		if (cachePeriod != null) {
			requestHandler.setCacheSeconds(cachePeriod);
		}
		return requestHandler;
	}

}