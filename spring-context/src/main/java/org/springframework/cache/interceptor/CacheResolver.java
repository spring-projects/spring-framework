/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.interceptor;

import java.util.Collection;

import org.springframework.cache.Cache;

/**
 * Determine the {@link Cache} instance(s) to use for an intercepted method invocation.
 *
 * <p>Implementations must be thread-safe.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
@FunctionalInterface
public interface CacheResolver {

	/**
	 * Return the cache(s) to use for the specified invocation.
	 * @param context the context of the particular invocation
	 * @return the cache(s) to use (never {@code null})
	 * @throws IllegalStateException if cache resolution failed
	 */
	Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context);

}
