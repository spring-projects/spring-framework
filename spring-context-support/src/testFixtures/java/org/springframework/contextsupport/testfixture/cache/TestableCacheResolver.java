/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.contextsupport.testfixture.cache;

import java.lang.annotation.Annotation;

import javax.cache.Cache;
import javax.cache.annotation.CacheInvocationContext;
import javax.cache.annotation.CacheResolver;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Stephane Nicoll
 */
public class TestableCacheResolver implements CacheResolver {

	@Override
	public <K, V> Cache<K, V> resolveCache(CacheInvocationContext<? extends Annotation> cacheInvocationContext) {
		String cacheName = cacheInvocationContext.getCacheName();
		@SuppressWarnings("unchecked")
		Cache<K, V> mock = mock();
		given(mock.getName()).willReturn(cacheName);
		return mock;
	}

}
