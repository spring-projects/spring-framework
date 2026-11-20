/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.contextsupport.testfixture.cache;

import java.lang.annotation.Annotation;

import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheKeyInvocationContext;
import javax.cache.annotation.GeneratedCacheKey;

import org.springframework.cache.interceptor.SimpleKey;

/**
 * A simple test key generator that only takes the first key arguments into
 * account. To be used with a multi parameters key to validate it has been
 * used properly.
 *
 * @author Stephane Nicoll
 */
public class TestableCacheKeyGenerator implements CacheKeyGenerator {

	@Override
	public GeneratedCacheKey generateCacheKey(CacheKeyInvocationContext<? extends Annotation> context) {
		return new SimpleGeneratedCacheKey(context.getKeyParameters()[0]);
	}


	@SuppressWarnings("serial")
	private static class SimpleGeneratedCacheKey extends SimpleKey implements GeneratedCacheKey {

		public SimpleGeneratedCacheKey(Object... elements) {
			super(elements);
		}

	}

}
