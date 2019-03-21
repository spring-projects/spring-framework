/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.cache.jcache.config;

import org.junit.Test;

import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.jcache.interceptor.DefaultJCacheOperationSource;
import org.springframework.cache.jcache.interceptor.JCacheInterceptor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

import static org.junit.Assert.*;

/**
 * @author Stephane Nicoll
 */
public class JCacheNamespaceDrivenTests extends AbstractJCacheAnnotationTests {

	@Override
	protected ApplicationContext getApplicationContext() {
		return new GenericXmlApplicationContext(
				"/org/springframework/cache/jcache/config/jCacheNamespaceDriven.xml");
	}

	@Test
	public void cacheResolver() {
		ConfigurableApplicationContext context = new GenericXmlApplicationContext(
				"/org/springframework/cache/jcache/config/jCacheNamespaceDriven-resolver.xml");

		DefaultJCacheOperationSource ci = context.getBean(DefaultJCacheOperationSource.class);
		assertSame(context.getBean("cacheResolver"), ci.getCacheResolver());
		context.close();
	}

	@Test
	public void testCacheErrorHandler() {
		JCacheInterceptor ci = ctx.getBean(JCacheInterceptor.class);
		assertSame(ctx.getBean("errorHandler", CacheErrorHandler.class), ci.getErrorHandler());
	}

}
