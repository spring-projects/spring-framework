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
import java.util.List;

import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.resource.CachingResourceResolver;
import org.springframework.web.servlet.resource.CachingResourceTransformer;
import org.springframework.web.servlet.resource.ContentVersionStrategy;
import org.springframework.web.servlet.resource.CssLinkResourceTransformer;
import org.springframework.web.servlet.resource.FixedVersionStrategy;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceTransformer;
import org.springframework.web.servlet.resource.VersionResourceResolver;
import org.springframework.web.servlet.resource.VersionStrategy;

/**
 * Encapsulates information required to create a resource handlers.
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @author Brian Clozel
 *
 * @since 3.1
 */
public class ResourceHandlerRegistration {

	private static final String RESOURCE_CACHE_NAME = "spring-resourcehandler-cache";

	private final ResourceLoader resourceLoader;

	private final String[] pathPatterns;

	private final List<Resource> locations = new ArrayList<Resource>();

	private Integer cachePeriod;

	private List<ResourceResolver> customResolvers = new ArrayList<ResourceResolver>();

	private List<ResourceTransformer> customTransformers = new ArrayList<ResourceTransformer>();

	private boolean hasVersionResolver;

	private boolean hasCssLinkTransformer;

	private boolean isDevMode = false;

	private Cache resourceCache;


	/**
	 * Create a {@link ResourceHandlerRegistration} instance.
	 * @param resourceLoader a resource loader for turning a String location into a {@link Resource}
	 * @param pathPatterns one or more resource URL path patterns
	 */
	public ResourceHandlerRegistration(ResourceLoader resourceLoader, String... pathPatterns) {
		Assert.notEmpty(pathPatterns, "At least one path pattern is required for resource handling.");
		this.resourceLoader = resourceLoader;
		this.pathPatterns = pathPatterns;
	}


	/**
	 * Add one or more resource locations from which to serve static content. Each location must point to a valid
	 * directory. Multiple locations may be specified as a comma-separated list, and the locations will be checked
	 * for a given resource in the order specified.
	 * <p>For example, {{@code "/"}, {@code "classpath:/META-INF/public-web-resources/"}} allows resources to
	 * be served both from the web application root and from any JAR on the classpath that contains a
	 * {@code /META-INF/public-web-resources/} directory, with resources in the web application root taking precedence.
	 * @return the same {@link ResourceHandlerRegistration} instance for chained method invocation
	 */
	public ResourceHandlerRegistration addResourceLocations(String...resourceLocations) {
		for (String location : resourceLocations) {
			this.locations.add(resourceLoader.getResource(location));
		}
		return this;
	}

	/**
	 * Add a {@code ResourceResolver} to the chain, allowing to resolve server-side resources from
	 * HTTP requests.
	 *
	 * <p>{@link ResourceResolver}s are registered, in the following order:
	 * <ol>
	 *     <li>a {@link org.springframework.web.servlet.resource.CachingResourceResolver}
	 *     for caching the results of the next Resolvers; this resolver is only registered if you
	 *     did not provide your own instance of {@link CachingResourceResolver} at the beginning of the chain</li>
	 *     <li>all {@code ResourceResolver}s registered using this method, in the order of methods calls</li>
	 *     <li>a {@link VersionResourceResolver} if a versioning configuration has been applied with
	 *     {@code addVersionStrategy}, {@code addVersion}, etc.</li>
	 *     <li>a {@link PathResourceResolver} for resolving resources on the file system</li>
	 * </ol>
	 *
	 * @param resolver a {@link ResourceResolver} to add to the chain of resolvers
	 * @return the same {@link ResourceHandlerRegistration} instance for chained method invocation
	 * @see ResourceResolver
	 * @since 4.1
	 */
	public ResourceHandlerRegistration addResolver(ResourceResolver resolver) {
		Assert.notNull(resolver, "The provided ResourceResolver should not be null");
		this.customResolvers.add(resolver);
		if (resolver instanceof VersionResourceResolver) {
			this.hasVersionResolver = true;
		}
		return this;
	}

	/**
	 * Add a {@code ResourceTransformer} to the chain, allowing to transform the content
	 * of server-side resources when serving them to HTTP clients.
	 *
	 * <p>{@link ResourceTransformer}s are registered, in the following order:
	 * <ol>
	 *     <li>a {@link org.springframework.web.servlet.resource.CachingResourceTransformer}
	 *     for caching the results of the next Transformers; this transformer is only registered if you
	 *     did not provide your own instance of {@link CachingResourceTransformer} at the beginning of the chain</li>
	 *     <li>a {@link CssLinkResourceTransformer} for updating links within CSS files; this transformer
	 *     is only registered if a versioning configuration has been applied with {@code addVersionStrategy},
	 *     {@code addVersion}, etc</li>
	 *     <li>all {@code ResourceTransformer}s registered using this method, in the order of methods calls</li>
	 * </ol>
	 *
	 * @param transformer a {@link ResourceTransformer} to add to the chain of transformers
	 * @return the same {@link ResourceHandlerRegistration} instance for chained method invocation
	 * @see ResourceResolver
	 * @since 4.1
	 */
	public ResourceHandlerRegistration addTransformer(ResourceTransformer transformer) {
		Assert.notNull(transformer, "The provided ResourceTransformer should not be null");
		this.customTransformers.add(transformer);
		if (transformer instanceof CssLinkResourceTransformer) {
			this.hasCssLinkTransformer = true;
		}
		return this;
	}

	/**
	 * Disable automatic registration of caching Resolver/Transformer, thus disabling {@code Resource} caching
	 * if no caching Resolver/Transformer was manually registered.
	 * <p>Useful when updating static resources at runtime, i.e. during the development phase.</p>
	 * @return the same {@link ResourceHandlerRegistration} instance for chained method invocation
	 * @see ResourceResolver
	 * @see ResourceTransformer
	 * @since 4.1
	 */
	public ResourceHandlerRegistration enableDevMode() {
		this.isDevMode = true;
		return this;
	}

	/**
	 * Specify the cache period for the resources served by the resource handler, in seconds. The default is to not
	 * send any cache headers but to rely on last-modified timestamps only. Set to 0 in order to send cache headers
	 * that prevent caching, or to a positive number of seconds to send cache headers with the given max-age value.
	 * @param cachePeriod the time to cache resources in seconds
	 * @return the same {@link ResourceHandlerRegistration} instance for chained method invocation
	 */
	public ResourceHandlerRegistration setCachePeriod(Integer cachePeriod) {
		this.cachePeriod = cachePeriod;
		return this;
	}

	/**
	 * Returns the URL path patterns for the resource handler.
	 */
	protected String[] getPathPatterns() {
		return this.pathPatterns;
	}

	protected List<ResourceResolver> getResourceResolvers() {
		if (this.customResolvers.isEmpty()) {
			return null;
		}
		List<ResourceResolver> resolvers = new ArrayList<ResourceResolver>();
		ResourceResolver first = this.customResolvers.get(0);
		if (!ClassUtils.isAssignable(CachingResourceResolver.class, first.getClass()) && !this.isDevMode) {
			resolvers.add(new CachingResourceResolver(getDefaultResourceCache()));
		}
		resolvers.addAll(this.customResolvers);
		ResourceResolver last = this.customResolvers.get(this.customResolvers.size() - 1);
		if (!ClassUtils.isAssignable(PathResourceResolver.class, last.getClass())) {
			resolvers.add(new PathResourceResolver());
		}
		return resolvers;
	}

	protected List<ResourceTransformer> getResourceTransformers() {
		if (this.customTransformers.isEmpty()) {
			return null;
		}
		List<ResourceTransformer> transformers = new ArrayList<ResourceTransformer>();
		ResourceTransformer first = this.customTransformers.get(0);
		ResourceTransformer cachingTransformer = null;
		if (!this.isDevMode) {
			if (ClassUtils.isAssignable(CachingResourceTransformer.class, first.getClass())) {
				cachingTransformer = first;
			}
			else {
				cachingTransformer = new CachingResourceTransformer(getDefaultResourceCache());
				transformers.add(cachingTransformer);
			}
		}
		transformers.addAll(this.customTransformers);
		if (this.hasVersionResolver && !this.hasCssLinkTransformer) {
			transformers.add(cachingTransformer != null ? 1 : 0, new CssLinkResourceTransformer());
		}
		return transformers;
	}

	/**
	 * Returns a {@link ResourceHttpRequestHandler} instance.
	 */
	protected ResourceHttpRequestHandler getRequestHandler() {
		Assert.isTrue(!CollectionUtils.isEmpty(locations), "At least one location is required for resource handling.");
		ResourceHttpRequestHandler requestHandler = new ResourceHttpRequestHandler();
		List<ResourceResolver> resourceResolvers = getResourceResolvers();
		if (!CollectionUtils.isEmpty(resourceResolvers)) {
			requestHandler.setResourceResolvers(resourceResolvers);
		}
		List<ResourceTransformer> resourceTransformers = getResourceTransformers();
		if (!CollectionUtils.isEmpty(resourceTransformers)) {
			requestHandler.setResourceTransformers(resourceTransformers);
		}
		requestHandler.setLocations(this.locations);
		if (this.cachePeriod != null) {
			requestHandler.setCacheSeconds(this.cachePeriod);
		}
		return requestHandler;
	}

	/**
	 * Return a default instance of a {@code ConcurrentCacheMap} for
	 * caching resolved/transformed resources.
	 */
	private Cache getDefaultResourceCache() {
		if(this.resourceCache == null) {
			this.resourceCache = new ConcurrentMapCache(RESOURCE_CACHE_NAME);
		}
		return this.resourceCache;
	}

}
