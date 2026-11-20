/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.contextsupport.testfixture.cache;

import java.lang.annotation.Annotation;

import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheResolver;
import javax.cache.annotation.CacheResolverFactory;
import javax.cache.annotation.CacheResult;

/**
 * @author Stephane Nicoll
 */
public class TestableCacheResolverFactory implements CacheResolverFactory {

	@Override
	public CacheResolver getCacheResolver(CacheMethodDetails<? extends Annotation> cacheMethodDetails) {
		return new TestableCacheResolver();
	}

	@Override
	public CacheResolver getExceptionCacheResolver(CacheMethodDetails<CacheResult> cacheMethodDetails) {
		return new TestableCacheResolver();
	}

}
