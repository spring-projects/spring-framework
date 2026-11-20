/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Composite {@link CacheOperationSource} implementation that iterates
 * over a given array of {@code CacheOperationSource} instances.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.1
 */
@SuppressWarnings("serial")
public class CompositeCacheOperationSource implements CacheOperationSource, Serializable {

	private final CacheOperationSource[] cacheOperationSources;


	/**
	 * Create a new CompositeCacheOperationSource for the given sources.
	 * @param cacheOperationSources the CacheOperationSource instances to combine
	 */
	public CompositeCacheOperationSource(CacheOperationSource... cacheOperationSources) {
		Assert.notEmpty(cacheOperationSources, "CacheOperationSource array must not be empty");
		this.cacheOperationSources = cacheOperationSources;
	}

	/**
	 * Return the {@code CacheOperationSource} instances that this
	 * {@code CompositeCacheOperationSource} combines.
	 */
	public final CacheOperationSource[] getCacheOperationSources() {
		return this.cacheOperationSources;
	}


	@Override
	public boolean isCandidateClass(Class<?> targetClass) {
		for (CacheOperationSource source : this.cacheOperationSources) {
			if (source.isCandidateClass(targetClass)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasCacheOperations(Method method, @Nullable Class<?> targetClass) {
		for (CacheOperationSource source : this.cacheOperationSources) {
			if (source.hasCacheOperations(method, targetClass)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public @Nullable Collection<CacheOperation> getCacheOperations(Method method, @Nullable Class<?> targetClass) {
		Collection<CacheOperation> ops = null;
		for (CacheOperationSource source : this.cacheOperationSources) {
			Collection<CacheOperation> cacheOperations = source.getCacheOperations(method, targetClass);
			if (cacheOperations != null) {
				if (ops == null) {
					ops = new ArrayList<>();
				}
				ops.addAll(cacheOperations);
			}
		}
		return ops;
	}

}
