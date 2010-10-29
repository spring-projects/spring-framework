/*
 * Copyright 2010 the original author or authors.
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
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;
import org.springframework.cache.config.CacheableService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Abstract annotation test (containing several reusable methods).
 * @author Costin Leau
 */
public abstract class AbstractAnnotationTest {

	protected ApplicationContext ctx;

	protected CacheableService cs;

	protected CacheableService ccs;

	protected abstract String getConfig();

	@Before
	public void setup() {
		ctx = new ClassPathXmlApplicationContext(getConfig());
		cs = ctx.getBean("service", CacheableService.class);
		ccs = ctx.getBean("classService", CacheableService.class);
	}

	public void testCacheable(CacheableService service) throws Exception {
		Object o1 = new Object();
		Object o2 = new Object();

		Object r1 = service.cache(o1);
		Object r2 = service.cache(o1);
		Object r3 = service.cache(o1);

		assertSame(r1, r2);
		assertSame(r1, r3);
	}

	public void testInvalidate(CacheableService service) throws Exception {
		Object o1 = new Object();
		Object o2 = new Object();

		Object r1 = service.cache(o1);
		Object r2 = service.cache(o1);

		assertSame(r1, r2);
		service.invalidate(o1);
		Object r3 = service.cache(o1);
		Object r4 = service.cache(o1);
		assertNotSame(r1, r3);
		assertSame(r3, r4);
	}

	public void testConditionalExpression(CacheableService service) throws Exception {
		Object r1 = service.conditional(4);
		Object r2 = service.conditional(4);

		assertNotSame(r1, r2);

		Object r3 = service.conditional(3);
		Object r4 = service.conditional(3);

		assertSame(r3, r4);
	}

	public void testKeyExpression(CacheableService service) throws Exception {
		Object r1 = service.key(5, 1);
		Object r2 = service.key(5, 2);

		assertSame(r1, r2);

		Object r3 = service.key(1, 5);
		Object r4 = service.key(2, 5);

		assertNotSame(r3, r4);
	}

	@Test
	public void testCacheable() throws Exception {
		testCacheable(cs);
	}

	@Test
	public void testInvalidate() throws Exception {
		testInvalidate(cs);
	}

	@Test
	public void testConditionalExpression() throws Exception {
		testConditionalExpression(cs);
	}

	@Test
	public void testKeyExpression() throws Exception {
		testKeyExpression(cs);
	}

	@Test
	public void testClassCacheCacheable() throws Exception {
		testCacheable(ccs);
	}

	@Test
	public void testClassCacheInvalidate() throws Exception {
		testInvalidate(ccs);
	}

}