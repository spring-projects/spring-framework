/*
 * Copyright 2002-2016 the original author or authors.
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

import org.junit.Assert;
import org.junit.Test;

import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

/**
 * @author Costin Leau
 * @author Chris Beams
 */
public class CacheAdviceNamespaceTests extends AbstractCacheAnnotationTests {

	@Override
	protected ConfigurableApplicationContext getApplicationContext() {
		return new GenericXmlApplicationContext(
				"/org/springframework/cache/config/cache-advice.xml");
	}

	@Test
	public void testKeyStrategy() throws Exception {
		CacheInterceptor bean = this.ctx.getBean("cacheAdviceClass", CacheInterceptor.class);
		Assert.assertSame(this.ctx.getBean("keyGenerator"), bean.getKeyGenerator());
	}

}
