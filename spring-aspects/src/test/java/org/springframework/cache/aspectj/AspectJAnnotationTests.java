/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cache.aspectj;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.config.AbstractAnnotationTests;
import org.springframework.cache.config.CacheableService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;


/**
 * @author Costin Leau
 */
public class AspectJAnnotationTests extends AbstractAnnotationTests {


	@Override
	protected ApplicationContext getApplicationContext() {
		return new GenericXmlApplicationContext("/org/springframework/cache/config/annotation-cache-aspectj.xml");
	}

	@Test
	public void testKeyStrategy() throws Exception {
		AnnotationCacheAspect aspect = ctx.getBean("org.springframework.cache.config.internalCacheAspect", AnnotationCacheAspect.class);
		Assert.assertSame(ctx.getBean("keyGenerator"), aspect.getKeyGenerator());
	}

	@Override
	public void testMultiEvict(CacheableService<?> service) {
		Object o1 = new Object();

		Object r1 = service.multiCache(o1);
		Object r2 = service.multiCache(o1);

		Cache primary = cm.getCache("primary");
		Cache secondary = cm.getCache("secondary");

		assertSame(r1, r2);
		assertSame(r1, primary.get(o1).get());
		assertSame(r1, secondary.get(o1).get());

		service.multiEvict(o1);
		assertNull(primary.get(o1));
		assertNull(secondary.get(o1));

		Object r3 = service.multiCache(o1);
		Object r4 = service.multiCache(o1);
		assertNotSame(r1, r3);
		assertSame(r3, r4);

		assertSame(r3, primary.get(o1).get());
		assertSame(r4, secondary.get(o1).get());
	}
}
