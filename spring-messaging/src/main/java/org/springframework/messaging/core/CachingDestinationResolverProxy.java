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

package org.springframework.messaging.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * {@link DestinationResolver} implementation that proxies a target DestinationResolver,
 * caching its {@link #resolveDestination} results. Such caching is particularly useful
 * if the destination resolving process is expensive (e.g. the destination has to be
 * resolved through an external system) and the resolution results are stable anyway.
 *
 * @author Agim Emruli
 * @author Juergen Hoeller
 * @since 4.1
 * @see DestinationResolver#resolveDestination
 */
public class CachingDestinationResolverProxy<D> implements DestinationResolver<D>, InitializingBean {

	private final Map<String, D> resolvedDestinationCache = new ConcurrentHashMap<String, D>();

	private DestinationResolver<D> targetDestinationResolver;


	/**
	 * Create a new CachingDestinationResolverProxy, setting the target DestinationResolver
	 * through the {@link #setTargetDestinationResolver} bean property.
	 */
	public CachingDestinationResolverProxy() {
	}

	/**
	 * Create a new CachingDestinationResolverProxy using the given target
	 * DestinationResolver to actually resolve destinations.
	 * @param targetDestinationResolver the target DestinationResolver to delegate to
	 */
	public CachingDestinationResolverProxy(DestinationResolver<D> targetDestinationResolver) {
		Assert.notNull(targetDestinationResolver, "Target DestinationResolver must not be null");
		this.targetDestinationResolver = targetDestinationResolver;
	}


	/**
	 * Set the target DestinationResolver to delegate to.
	 */
	public void setTargetDestinationResolver(DestinationResolver<D> targetDestinationResolver) {
		this.targetDestinationResolver = targetDestinationResolver;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.targetDestinationResolver == null) {
			throw new IllegalArgumentException("Property 'targetDestinationResolver' is required");
		}
	}


	/**
	 * Resolves and caches destinations if successfully resolved by the target
	 * DestinationResolver implementation.
	 * @param name the destination name to be resolved
	 * @return the currently resolved destination or an already cached destination
	 * @throws DestinationResolutionException if the target DestinationResolver
	 * reports an error during destination resolution
	 */
	@Override
	public D resolveDestination(String name) throws DestinationResolutionException {
		D destination = this.resolvedDestinationCache.get(name);
		if (destination == null) {
			destination = this.targetDestinationResolver.resolveDestination(name);
			this.resolvedDestinationCache.put(name, destination);
		}
		return destination;
	}

}
