package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheKeyInvocationContext;
import javax.cache.annotation.GeneratedCacheKey;

/**
 * A JSR-107 compliant key generator. Uses only the parameters that have been annotated
 * with {@link javax.cache.annotation.CacheKey} or all of them if none are set, except
 * the {@link javax.cache.annotation.CacheValue} one.
 *
 * @author Stephane Nicoll
 * @see 4.1
 * @see javax.cache.annotation.CacheKeyInvocationContext#getKeyParameters()
 */
public class SimpleCacheKeyGenerator implements CacheKeyGenerator {

	@Override
	public GeneratedCacheKey generateCacheKey(CacheKeyInvocationContext<? extends Annotation> context) {
		CacheInvocationParameter[] keyParameters = context.getKeyParameters();
		final Object[] parameters = new Object[keyParameters.length];
		for (int i = 0; i < keyParameters.length; i++) {
			parameters[i] = keyParameters[i].getValue();
		}
		return new SimpleGeneratedCacheKey(parameters);
	}

}
