/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.jcache.config;

import org.junit.jupiter.api.Test;

import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.jcache.interceptor.DefaultJCacheOperationSource;
import org.springframework.cache.jcache.interceptor.JCacheInterceptor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.contextsupport.testfixture.jcache.AbstractJCacheAnnotationTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
class JCacheNamespaceDrivenTests extends AbstractJCacheAnnotationTests {

	@Override
	protected ApplicationContext getApplicationContext() {
		return new GenericXmlApplicationContext(
				"/org/springframework/cache/jcache/config/jCacheNamespaceDriven.xml");
	}

	@Test
	void cacheResolver() {
		ConfigurableApplicationContext context = new GenericXmlApplicationContext(
				"/org/springframework/cache/jcache/config/jCacheNamespaceDriven-resolver.xml");

		DefaultJCacheOperationSource ci = context.getBean(DefaultJCacheOperationSource.class);
		assertThat(ci.getCacheResolver()).isSameAs(context.getBean("cacheResolver"));
		context.close();
	}

	@Test
	void cacheErrorHandler() {
		JCacheInterceptor ci = ctx.getBean(JCacheInterceptor.class);
		assertThat(ci.getErrorHandler()).isSameAs(ctx.getBean("errorHandler", CacheErrorHandler.class));
	}

}
