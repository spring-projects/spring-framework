/*
 * Copyright 2002-present the original author or authors.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.cache.Cache;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.util.Assert;
import org.springframework.web.reactive.resource.ResourceWebHandler;
import org.springframework.web.server.ServerWebExchange;

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

	private @Nullable CacheControl cacheControl;

	private @Nullable ResourceChainRegistration resourceChainRegistration;

	private boolean useLastModified = true;

	private @Nullable Function<Resource, String> etagGenerator;

	private boolean optimizeLocations = false;

	private @Nullable Map<String, MediaType> mediaTypes;



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
	 * <p>Each location must point to a valid directory. Multiple locations may
	 * be specified as a comma-separated list, and the locations will be checked
	 * for a given resource in the order specified.
	 * <p>For example, {@code "/", "classpath:/META-INF/public-web-resources/"}
	 * allows resources to be served both from the web application root and from
	 * any JAR on the classpath that contains a {@code /META-INF/public-web-resources/}
	 * directory, with resources in the web application root taking precedence.
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
	 * @param useLastModified whether the "last modified" resource information should be used
	 * @return the same {@link ResourceHandlerRegistration} instance, for chained method invocation
	 * @since 5.3
	 * @see ResourceWebHandler#setUseLastModified
	 */
	public ResourceHandlerRegistration setUseLastModified(boolean useLastModified) {
		this.useLastModified = useLastModified;
		return this;
	}


	/**
	 * Configure a generator function that will be used to create the ETag information,
	 * given a {@link Resource} that is about to be written to the response.
	 * <p>This function should return a String that will be used as an argument in
	 * {@link ServerWebExchange#checkNotModified(String)}, or {@code null} if no value
	 * can be generated for the given resource.
	 * @param etagGenerator the HTTP ETag generator function to use.
	 * @since 6.1
	 * @see ResourceWebHandler#setEtagGenerator(Function)
	 */
	public ResourceHandlerRegistration setEtagGenerator(@Nullable Function<Resource, String> etagGenerator) {
		this.etagGenerator = etagGenerator;
		return this;
	}

	/**
	 * Set whether to optimize the specified locations through an existence check on startup,
	 * filtering non-existing directories upfront so that they do not have to be checked
	 * on every resource access.
	 * <p>The default is {@code false}, for defensiveness against zip files without directory
	 * entries which are unable to expose the existence of a directory upfront. Switch this flag to
	 * {@code true} for optimized access in case of a consistent jar layout with directory entries.
	 * @param optimizeLocations whether to optimize the locations through an existence check on startup
	 * @return the same {@link ResourceHandlerRegistration} instance, for chained method invocation
	 * @since 5.3.13
	 * @see ResourceWebHandler#setOptimizeLocations
	 */
	public ResourceHandlerRegistration setOptimizeLocations(boolean optimizeLocations) {
		this.optimizeLocations = optimizeLocations;
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
	 * Add mappings between file extensions extracted from the filename of static
	 * {@link Resource}s and the media types to use for the response.
	 * <p>Use of this method is typically not necessary since mappings can be
	 * also determined via {@link MediaTypeFactory#getMediaType(Resource)}.
	 * @param mediaTypes media type mappings
	 * @since 5.3.2
	 */
	public void setMediaTypes(Map<String, MediaType> mediaTypes) {
		if (this.mediaTypes == null) {
			this.mediaTypes = new HashMap<>(mediaTypes.size());
		}
		this.mediaTypes.clear();
		this.mediaTypes.putAll(mediaTypes);
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
		handler.setResourceLoader(this.resourceLoader);
		handler.setLocationValues(this.locationValues);
		if (this.resourceChainRegistration != null) {
			handler.setResourceResolvers(this.resourceChainRegistration.getResourceResolvers());
			handler.setResourceTransformers(this.resourceChainRegistration.getResourceTransformers());
		}
		if (this.cacheControl != null) {
			handler.setCacheControl(this.cacheControl);
		}
		handler.setUseLastModified(this.useLastModified);
		handler.setEtagGenerator(this.etagGenerator);
		handler.setOptimizeLocations(this.optimizeLocations);
		if (this.mediaTypes != null) {
			handler.setMediaTypes(this.mediaTypes);
		}
		return handler;
	}

}
