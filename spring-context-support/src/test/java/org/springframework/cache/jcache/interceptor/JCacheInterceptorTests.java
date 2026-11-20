/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.jcache.interceptor;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.NamedCacheResolver;
import org.springframework.cache.jcache.AbstractJCacheTests;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Stephane Nicoll
 */
class JCacheInterceptorTests extends AbstractJCacheTests {

	private final CacheOperationInvoker dummyInvoker = new DummyInvoker(null);

	@Test
	void severalCachesNotSupported() {
		JCacheInterceptor interceptor = createInterceptor(createOperationSource(
				cacheManager, new NamedCacheResolver(cacheManager, "default", "simpleCache"),
				defaultExceptionCacheResolver, defaultKeyGenerator));

		AnnotatedJCacheableService service = new AnnotatedJCacheableService(cacheManager.getCache("default"));
		Method m = ReflectionUtils.findMethod(AnnotatedJCacheableService.class, "cache", String.class);

		assertThatIllegalStateException().isThrownBy(() ->
				interceptor.execute(dummyInvoker, service, m, new Object[] {"myId"}))
			.withMessageContaining("JSR-107 only supports a single cache");
	}

	@Test
	void noCacheCouldBeResolved() {
		JCacheInterceptor interceptor = createInterceptor(createOperationSource(
				cacheManager, new NamedCacheResolver(cacheManager), // Returns empty list
				defaultExceptionCacheResolver, defaultKeyGenerator));

		AnnotatedJCacheableService service = new AnnotatedJCacheableService(cacheManager.getCache("default"));
		Method m = ReflectionUtils.findMethod(AnnotatedJCacheableService.class, "cache", String.class);
		assertThatIllegalStateException().isThrownBy(() ->
				interceptor.execute(dummyInvoker, service, m, new Object[] {"myId"}))
			.withMessageContaining("Cache could not have been resolved for");
	}

	@Test
	void cacheManagerMandatoryIfCacheResolverNotSet() {
		assertThatIllegalStateException().isThrownBy(() ->
				createOperationSource(null, null, null, defaultKeyGenerator));
	}

	@Test
	void cacheManagerOptionalIfCacheResolversSet() {
		createOperationSource(null, defaultCacheResolver, defaultExceptionCacheResolver, defaultKeyGenerator);
	}

	@Test
	void cacheResultReturnsProperType() {
		JCacheInterceptor interceptor = createInterceptor(createOperationSource(
				cacheManager, defaultCacheResolver, defaultExceptionCacheResolver, defaultKeyGenerator));

		AnnotatedJCacheableService service = new AnnotatedJCacheableService(cacheManager.getCache("default"));
		Method method = ReflectionUtils.findMethod(AnnotatedJCacheableService.class, "cache", String.class);

		CacheOperationInvoker invoker = new DummyInvoker(0L);
		Object execute = interceptor.execute(invoker, service, method, new Object[] {"myId"});
		assertThat(execute).as("result cannot be null.").isNotNull();
		assertThat(execute.getClass()).as("Wrong result type").isEqualTo(Long.class);
		assertThat(execute).as("Wrong result").isEqualTo(0L);
	}

	protected JCacheOperationSource createOperationSource(CacheManager cacheManager,
			CacheResolver cacheResolver, CacheResolver exceptionCacheResolver, KeyGenerator keyGenerator) {

		DefaultJCacheOperationSource source = new DefaultJCacheOperationSource();
		source.setCacheManager(cacheManager);
		source.setCacheResolver(cacheResolver);
		source.setExceptionCacheResolver(exceptionCacheResolver);
		source.setKeyGenerator(keyGenerator);
		source.setBeanFactory(new StaticListableBeanFactory());
		source.afterSingletonsInstantiated();
		return source;
	}


	protected JCacheInterceptor createInterceptor(JCacheOperationSource source) {
		JCacheInterceptor interceptor = new JCacheInterceptor();
		interceptor.setCacheOperationSource(source);
		interceptor.afterPropertiesSet();
		return interceptor;
	}


	private static class DummyInvoker implements CacheOperationInvoker {

		private final Object result;

		private DummyInvoker(Object result) {
			this.result = result;
		}

		@Override
		public Object invoke() throws ThrowableWrapper {
			return result;
		}
	}

}
