/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cache.config;

import org.junit.jupiter.api.Test;

import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.context.testfixture.cache.AbstractCacheAnnotationTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Costin Leau
 * @author Chris Beams
 * @author Stephane Nicoll
 */
class AnnotationNamespaceDrivenTests extends AbstractCacheAnnotationTests {

	@Override
	protected ConfigurableApplicationContext getApplicationContext() {
		return new GenericXmlApplicationContext(
				"/org/springframework/cache/config/annotationDrivenCacheNamespace.xml");
	}

	@Test
	void testKeyStrategy() {
		CacheInterceptor ci = this.ctx.getBean(
				"org.springframework.cache.interceptor.CacheInterceptor#0", CacheInterceptor.class);
		assertThat(ci.getKeyGenerator()).isSameAs(this.ctx.getBean("keyGenerator"));
	}

	@Test
	void cacheResolver() {
		ConfigurableApplicationContext context = new GenericXmlApplicationContext(
				"/org/springframework/cache/config/annotationDrivenCacheNamespace-resolver.xml");

		CacheInterceptor ci = context.getBean(CacheInterceptor.class);
		assertThat(ci.getCacheResolver()).isSameAs(context.getBean("cacheResolver"));
		context.close();
	}

	@Test
	void bothSetOnlyResolverIsUsed() {
		ConfigurableApplicationContext context = new GenericXmlApplicationContext(
				"/org/springframework/cache/config/annotationDrivenCacheNamespace-manager-resolver.xml");

		CacheInterceptor ci = context.getBean(CacheInterceptor.class);
		assertThat(ci.getCacheResolver()).isSameAs(context.getBean("cacheResolver"));
		context.close();
	}

	@Test
	void testCacheErrorHandler() {
		CacheInterceptor ci = this.ctx.getBean(
				"org.springframework.cache.interceptor.CacheInterceptor#0", CacheInterceptor.class);
		assertThat(ci.getErrorHandler()).isSameAs(this.ctx.getBean("errorHandler", CacheErrorHandler.class));
	}

}
