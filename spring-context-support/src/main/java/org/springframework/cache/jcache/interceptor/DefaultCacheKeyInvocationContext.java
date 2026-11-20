/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKeyInvocationContext;

import org.jspecify.annotations.Nullable;

/**
 * The default {@link CacheKeyInvocationContext} implementation.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @param <A> the annotation type
 */
class DefaultCacheKeyInvocationContext<A extends Annotation> extends DefaultCacheInvocationContext<A>
		implements CacheKeyInvocationContext<A> {

	private final CacheInvocationParameter[] keyParameters;

	private final @Nullable CacheInvocationParameter valueParameter;


	public DefaultCacheKeyInvocationContext(AbstractJCacheKeyOperation<A> operation, Object target, @Nullable Object[] args) {
		super(operation, target, args);
		this.keyParameters = operation.getKeyParameters(args);
		if (operation instanceof CachePutOperation cachePutOperation) {
			this.valueParameter = cachePutOperation.getValueParameter(args);
		}
		else {
			this.valueParameter = null;
		}
	}


	@Override
	public CacheInvocationParameter[] getKeyParameters() {
		return this.keyParameters.clone();
	}

	@Override
	public @Nullable CacheInvocationParameter getValueParameter() {
		return this.valueParameter;
	}

}
