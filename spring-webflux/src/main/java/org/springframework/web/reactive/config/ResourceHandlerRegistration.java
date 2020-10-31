/*
 * Copyright 2002-2016 the original author or authors.
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
import java.util.List;

import org.springframework.cache.Cache;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.CacheControl;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.resource.ResourceWebHandler;

/**
 * Assist with creating and configuring a static resources handler.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ResourceHandlerRegistration {

	private final ResourceLoader resourceLoader;

	private final String[] pathPatterns;

	private final List<String> locationValues = new ArrayList<>();

	@Nullable
	private CacheControl cacheControl;

	@Nullable
	private ResourceChainRegistration resourceChainRegistration;

	private boolean useLastModified = true;


	/**
	 * Create a {@link ResourceHandlerRegistration} instance.
	 * @param resourceLoader a resource loader for turning a String location
	 * into a {@link Resource}
	 * @param pathPatterns one or more resource URL path patterns
	 */
	public ResourceHandlerRegistration(ResourceLoader resourceLoader, String... pathPatterns) {
		Assert.notNull(resourceLoader, "ResourceLoader is required");
		Assert.notEmpty(pathPatterns, "At least one path pattern is required for resource handling");
		this.resourceLoader = resourceLoader;
		this.pathPatterns = pathPatterns;
	}


	/**
	 * Add one or more resource locations from which to serve static content.
	 * Each location must point to a valid directory. Multiple locations may
	 * be specified as a comma-separated list, and the locations will be checked
	 * for a given resource in the order specified.
	 *
	 * <p>For example, {{@code "/"},
	 * {@code "classpath:/META-INF/public-web-resources/"}} allows resources to
	 * be served both from the web application root and from any JAR on the
	 * classpath that contains a {@code /META-INF/public-web-resources/} directory,
	 * with resources in the web application root taking precedence.
	 * @return the same {@link ResourceHandlerRegistration} instance, for
	 * chained method invocation
	 */
	public ResourceHandlerRegistration addResourceLocations(String... resourceLocations) {
		this.locationValues.addAll(Arrays.asList(resourceLocations));
		return this;
	}

	/**
	 * Specify the {@link CacheControl} which should be used
	 * by the resource handler.
	 * @param cacheControl the CacheControl configuration to use
	 * @return the same {@link ResourceHandlerRegistration} instance, for
	 * chained method invocation
	 */
	public ResourceHandlerRegistration setCacheControl(CacheControl cacheControl) {
		this.cacheControl = cacheControl;
		return this;
	}

	/**
	 * Set whether the {@link Resource#lastModified()} information should be used to drive HTTP responses.
	 * <p>This configuration is set to {@code true} by default.
	 * @param useLastModified whether the "last modified" resource information should be used.
	 * @return the same {@link ResourceHandlerRegistration} instance, for chained method invocation
	 * @since 5.3
	 */
	public ResourceHandlerRegistration setUseLastModified(boolean useLastModified) {
		this.useLastModified = useLastModified;
		return this;
	}

	/**
	 * Configure a chain of resource resolvers and transformers to use. This
	 * can be useful, for example, to apply a version strategy to resource URLs.
	 * <p>If this method is not invoked, by default only a simple
	 * {@code PathResourceResolver} is used in order to match URL paths to
	 * resources under the configured locations.
	 * @param cacheResources whether to cache the result of resource resolution;
	 * setting this to "true" is recommended for production (and "false" for
	 * development, especially when applying a version strategy)
	 * @return the same {@link ResourceHandlerRegistration} instance, for
	 * chained method invocation
	 */
	public ResourceChainRegistration resourceChain(boolean cacheResources) {
		this.resourceChainRegistration = new ResourceChainRegistration(cacheResources);
		return this.resourceChainRegistration;
	}

	/**
	 * Configure a chain of resource resolvers and transformers to use. This
	 * can be useful, for example, to apply a version strategy to resource URLs.
	 * <p>If this method is not invoked, by default only a simple
	 * {@code PathResourceResolver} is used in order to match URL paths to
	 * resources under the configured locations.
	 * @param cacheResources whether to cache the result of resource resolution;
	 * setting this to "true" is recommended for production (and "false" for
	 * development, especially when applying a version strategy
	 * @param cache the cache to use for storing resolved and transformed resources;
	 * by default a {@link org.springframework.cache.concurrent.ConcurrentMapCache}
	 * is used. Since Resources aren't serializable and can be dependent on the
	 * application host, one should not use a distributed cache but rather an
	 * in-memory cache.
	 * @return the same {@link ResourceHandlerRegistration} instance, for chained method invocation
	 */
	public ResourceChainRegistration resourceChain(boolean cacheResources, Cache cache) {
		this.resourceChainRegistration = new ResourceChainRegistration(cacheResources, cache);
		return this.resourceChainRegistration;
	}

	/**
	 * Returns the URL path patterns for the resource handler.
	 */
	protected String[] getPathPatterns() {
		return this.pathPatterns;
	}

	/**
	 * Returns a {@link ResourceWebHandler} instance.
	 */
	protected ResourceWebHandler getRequestHandler() {
		ResourceWebHandler handler = new ResourceWebHandler();
		handler.setLocationValues(this.locationValues);
		handler.setResourceLoader(this.resourceLoader);
		if (this.resourceChainRegistration != null) {
			handler.setResourceResolvers(this.resourceChainRegistration.getResourceResolvers());
			handler.setResourceTransformers(this.resourceChainRegistration.getResourceTransformers());
		}
		if (this.cacheControl != null) {
			handler.setCacheControl(this.cacheControl);
		}
		handler.setUseLastModified(this.useLastModified);
		return handler;
	}

}
