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

import org.springframework.util.Assert;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Caching {@link org.springframework.messaging.core.DestinationResolver} implementation
 * that decorates existing DestinationResolver instances. This decorator is particularly
 * useful if the destination resolving process is expensive (e.g. destination has to
 * be resolved externally).
 *
 * @author Agim Emruli
 * @since 4.1
 */
public class CachingDestinationResolver<D> implements DestinationResolver<D> {

    private final ConcurrentHashMap<String, D> resolvedDestinationsCacheMap =
            new ConcurrentHashMap<String, D>();

    private final DestinationResolver<D> destinationResolverDelegate;

    /**
     * Creates a new instance using the underlying target DestinationResolver to
     * resolve destinations
     * @param destinationResolverDelegate the delegate DestinationResolver
     */
    public CachingDestinationResolver(DestinationResolver<D> destinationResolverDelegate) {
        Assert.notNull(destinationResolverDelegate);
        this.destinationResolverDelegate = destinationResolverDelegate;
    }

    /**
     *
     * Resolves and caches destinations if sucessfully resolved by the underlying
     * DestinationResolver implementation. Only stores the resolved destination
     * if it is resolved to a non-null destination by the underlying DestinationResolver.
     *
     * @param name the destination name to be resolved
     * @return the current resolved destination, an already cached destination or
     * {@code null} if no destination could be resolved
     * @throws DestinationResolutionException if the underlying DestinationResolver
     * reports an error during destionation resolution
     */
    @Override
    public D resolveDestination(String name) throws DestinationResolutionException {
        D destination;
        if (this.resolvedDestinationsCacheMap.containsKey(name)) {
            destination = this.resolvedDestinationsCacheMap.get(name);
        } else {
            destination = this.destinationResolverDelegate.resolveDestination(name);
            if (destination != null) {
                this.resolvedDestinationsCacheMap.putIfAbsent(name, destination);
            }
        }
        return destination;
    }
}