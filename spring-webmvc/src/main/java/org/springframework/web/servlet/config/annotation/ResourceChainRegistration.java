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
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.resource.CachingResourceResolver;
import org.springframework.web.servlet.resource.CachingResourceTransformer;
import org.springframework.web.servlet.resource.CssLinkResourceTransformer;
import org.springframework.web.servlet.resource.LiteWebJarsResourceResolver;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceTransformer;
import org.springframework.web.servlet.resource.VersionResourceResolver;

/**
 * Assists with the registration of resource resolvers and transformers.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class ResourceChainRegistration {

	private static final String DEFAULT_CACHE_NAME = "spring-resource-chain-cache";

	private static final boolean webJarsPresent = ClassUtils.isPresent(
			"org.webjars.WebJarVersionLocator", ResourceChainRegistration.class.getClassLoader());


	private final List<ResourceResolver> resolvers = new ArrayList<>(4);

	private final List<ResourceTransformer> transformers = new ArrayList<>(4);

	private boolean hasVersionResolver;

	private boolean hasPathResolver;

	private boolean hasCssLinkTransformer;

	private boolean hasWebjarsResolver;


	public ResourceChainRegistration(boolean cacheResources) {
		this(cacheResources, (cacheResources ? new ConcurrentMapCache(DEFAULT_CACHE_NAME) : null));
	}

	@SuppressWarnings("NullAway") // Dataflow analysis limitation
	public ResourceChainRegistration(boolean cacheResources, @Nullable Cache cache) {
		Assert.isTrue(!cacheResources || cache != null, "'cache' is required when cacheResources=true");
		if (cacheResources) {
			this.resolvers.add(new CachingResourceResolver(cache));
			this.transformers.add(new CachingResourceTransformer(cache));
		}
	}


	/**
	 * Add a resource resolver to the chain.
	 * @param resolver the resolver to add
	 * @return the current instance for chained method invocation
	 */
	@SuppressWarnings("removal")
	public ResourceChainRegistration addResolver(ResourceResolver resolver) {
		Assert.notNull(resolver, "The provided ResourceResolver should not be null");
		this.resolvers.add(resolver);
		if (resolver instanceof VersionResourceResolver) {
			this.hasVersionResolver = true;
		}
		else if (resolver instanceof PathResourceResolver) {
			this.hasPathResolver = true;
		}
		else if (resolver instanceof LiteWebJarsResourceResolver) {
			this.hasWebjarsResolver = true;
		}
		return this;
	}

	/**
	 * Add a resource transformer to the chain.
	 * @param transformer the transformer to add
	 * @return the current instance for chained method invocation
	 */
	public ResourceChainRegistration addTransformer(ResourceTransformer transformer) {
		Assert.notNull(transformer, "The provided ResourceTransformer should not be null");
		this.transformers.add(transformer);
		if (transformer instanceof CssLinkResourceTransformer) {
			this.hasCssLinkTransformer = true;
		}
		return this;
	}

	protected List<ResourceResolver> getResourceResolvers() {
		if (!this.hasPathResolver) {
			List<ResourceResolver> result = new ArrayList<>(this.resolvers);
			if (webJarsPresent && !this.hasWebjarsResolver) {
				result.add(new LiteWebJarsResourceResolver());
			}
			result.add(new PathResourceResolver());
			return result;
		}
		return this.resolvers;
	}

	protected List<ResourceTransformer> getResourceTransformers() {
		if (this.hasVersionResolver && !this.hasCssLinkTransformer) {
			List<ResourceTransformer> result = new ArrayList<>(this.transformers);
			boolean hasTransformers = !this.transformers.isEmpty();
			boolean hasCaching = hasTransformers && this.transformers.get(0) instanceof CachingResourceTransformer;
			result.add(hasCaching ? 1 : 0, new CssLinkResourceTransformer());
			return result;
		}
		return this.transformers;
	}

}
