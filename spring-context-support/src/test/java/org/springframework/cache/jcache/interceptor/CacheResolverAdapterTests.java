/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;

import javax.cache.annotation.CacheInvocationContext;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheResolver;
import javax.cache.annotation.CacheResult;

import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.jcache.AbstractJCacheTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Stephane Nicoll
 */
class CacheResolverAdapterTests extends AbstractJCacheTests {

	@Test
	void resolveSimpleCache() throws Exception {
		DefaultCacheInvocationContext<?> dummyContext = createDummyContext();
		CacheResolverAdapter adapter = new CacheResolverAdapter(getCacheResolver(dummyContext, "testCache"));
		Collection<? extends Cache> caches = adapter.resolveCaches(dummyContext);
		assertThat(caches).isNotNull();
		assertThat(caches).hasSize(1);
		assertThat(caches.iterator().next().getName()).isEqualTo("testCache");
	}

	@Test
	void resolveUnknownCache() throws Exception {
		DefaultCacheInvocationContext<?> dummyContext = createDummyContext();
		CacheResolverAdapter adapter = new CacheResolverAdapter(getCacheResolver(dummyContext, null));

		assertThatIllegalStateException().isThrownBy(() ->
				adapter.resolveCaches(dummyContext));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected CacheResolver getCacheResolver(CacheInvocationContext<? extends Annotation> context, String cacheName) {
		CacheResolver cacheResolver = mock();
		javax.cache.Cache cache;
		if (cacheName == null) {
			cache = null;
		}
		else {
			cache = mock();
			given(cache.getName()).willReturn(cacheName);
		}
		given(cacheResolver.resolveCache(context)).willReturn(cache);
		return cacheResolver;
	}

	protected DefaultCacheInvocationContext<?> createDummyContext() throws Exception {
		Method method = Sample.class.getMethod("get", String.class);
		CacheResult cacheAnnotation = method.getAnnotation(CacheResult.class);
		CacheMethodDetails<CacheResult> methodDetails =
				new DefaultCacheMethodDetails<>(method, cacheAnnotation, "test");
		CacheResultOperation operation = new CacheResultOperation(methodDetails,
				defaultCacheResolver, defaultKeyGenerator, defaultExceptionCacheResolver);
		return new DefaultCacheInvocationContext<>(operation, new Sample(), new Object[] {"id"});
	}


	static class Sample {

		@CacheResult
		public Object get(String id) {
			return null;
		}
	}

}
