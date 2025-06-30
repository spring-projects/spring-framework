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

package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.cache.Cache;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

/**
 * Encapsulates information required to create a resource handler.
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @author Brian Clozel
 * @since 3.1
 */
public class ResourceHandlerRegistration {

	private final String[] pathPatterns;

	private final List<String> locationValues = new ArrayList<>();

	private final List<Resource> locationsResources = new ArrayList<>();

	private @Nullable Integer cachePeriod;

	private @Nullable CacheControl cacheControl;

	private @Nullable ResourceChainRegistration resourceChainRegistration;

	private boolean useLastModified = true;

	private @Nullable Function<Resource, String> etagGenerator;

	private boolean optimizeLocations = false;


	/**
	 * Create a {@link ResourceHandlerRegistration} instance.
	 * @param pathPatterns one or more resource URL path patterns
	 */
	public ResourceHandlerRegistration(String... pathPatterns) {
		Assert.notEmpty(pathPatterns, "At least one path pattern is required for resource handling.");
		this.pathPatterns = pathPatterns;
	}


	/**
	 * Add one or more resource locations from which to serve static content.
	 * Each location must point to a valid directory. Multiple locations may
	 * be specified as a comma-separated list, and the locations will be checked
	 * for a given resource in the order specified.
	 * <p>For example, {{@code "/"}, {@code "classpath:/META-INF/public-web-resources/"}}
	 * allows resources to be served both from the web application root and
	 * from any JAR on the classpath that contains a
	 * {@code /META-INF/public-web-resources/} directory, with resources in the
	 * web application root taking precedence.
	 * <p>For {@link org.springframework.core.io.UrlResource URL-based resources}
	 * (for example, files, HTTP URLs, etc) this method supports a special prefix to
	 * indicate the charset associated with the URL so that relative paths
	 * appended to it can be encoded correctly, for example,
	 * {@code [charset=Windows-31J]https://example.org/path}.
	 * @return the same {@link ResourceHandlerRegistration} instance, for
	 * chained method invocation
	 */
	public ResourceHandlerRegistration addResourceLocations(String... locations) {
		this.locationValues.addAll(Arrays.asList(locations));
		return this;
	}

	/**
	 * Configure locations to serve static resources from based on pre-resolved
	 * {@code Resource} references.
	 * @param locations the resource locations to use
	 * @return the same {@link ResourceHandlerRegistration} instance, for
	 * chained method invocation
	 * @since 5.3.3
	 */
	public ResourceHandlerRegistration addResourceLocations(Resource... locations) {
		this.locationsResources.addAll(Arrays.asList(locations));
		return this;
	}

	/**
	 * Specify the cache period for the resources served by the resource handler, in seconds. The default is to not
	 * send any cache headers but to rely on last-modified timestamps only. Set to 0 in order to send cache headers
	 * that prevent caching, or to a positive number of seconds to send cache headers with the given max-age value.
	 * @param cachePeriod the time to cache resources in seconds
	 * @return the same {@link ResourceHandlerRegistration} instance, for chained method invocation
	 */
	public ResourceHandlerRegistration setCachePeriod(Integer cachePeriod) {
		this.cachePeriod = cachePeriod;
		return this;
	}

	/**
	 * Specify the {@link org.springframework.http.CacheControl} which should be used
	 * by the resource handler.
	 * <p>Setting a custom value here will override the configuration set with {@link #setCachePeriod}.
	 * @param cacheControl the CacheControl configuration to use
	 * @return the same {@link ResourceHandlerRegistration} instance, for chained method invocation
	 * @since 4.2
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
	 * @see ResourceHttpRequestHandler#setUseLastModified
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
	 * @see ResourceHttpRequestHandler#setEtagGenerator(Function)
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
	 * @see ResourceHttpRequestHandler#setOptimizeLocations
	 */
	public ResourceHandlerRegistration setOptimizeLocations(boolean optimizeLocations) {
		this.optimizeLocations = optimizeLocations;
		return this;
	}

	/**
	 * Configure a chain of resource resolvers and transformers to use. This
	 * can be useful, for example, to apply a version strategy to resource URLs.
	 * <p>If this method is not invoked, by default only a simple
	 * {@link PathResourceResolver} is used in order to match URL paths to
	 * resources under the configured locations.
	 * @param cacheResources whether to cache the result of resource resolution;
	 * setting this to "true" is recommended for production (and "false" for
	 * development, especially when applying a version strategy)
	 * @return the same {@link ResourceHandlerRegistration} instance, for chained method invocation
	 * @since 4.1
	 */
	public ResourceChainRegistration resourceChain(boolean cacheResources) {
		this.resourceChainRegistration = new ResourceChainRegistration(cacheResources);
		return this.resourceChainRegistration;
	}

	/**
	 * Configure a chain of resource resolvers and transformers to use. This
	 * can be useful, for example, to apply a version strategy to resource URLs.
	 * <p>If this method is not invoked, by default only a simple
	 * {@link PathResourceResolver} is used in order to match URL paths to
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
	 * @since 4.1
	 */
	public ResourceChainRegistration resourceChain(boolean cacheResources, Cache cache) {
		this.resourceChainRegistration = new ResourceChainRegistration(cacheResources, cache);
		return this.resourceChainRegistration;
	}


	/**
	 * Return the URL path patterns for the resource handler.
	 */
	protected String[] getPathPatterns() {
		return this.pathPatterns;
	}

	/**
	 * Return a {@link ResourceHttpRequestHandler} instance.
	 */
	protected ResourceHttpRequestHandler getRequestHandler() {
		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		if (this.resourceChainRegistration != null) {
			handler.setResourceResolvers(this.resourceChainRegistration.getResourceResolvers());
			handler.setResourceTransformers(this.resourceChainRegistration.getResourceTransformers());
		}
		handler.setLocationValues(this.locationValues);
		handler.setLocations(this.locationsResources);
		if (this.cacheControl != null) {
			handler.setCacheControl(this.cacheControl);
		}
		else if (this.cachePeriod != null) {
			handler.setCacheSeconds(this.cachePeriod);
		}
		handler.setUseLastModified(this.useLastModified);
		handler.setEtagGenerator(this.etagGenerator);
		handler.setOptimizeLocations(this.optimizeLocations);
		return handler;
	}

}
