/*
 * Copyright 2010-2011 the original author or authors.
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

package org.springframework.cache.config;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;

/**
 * Abstract annotation test (containing several reusable methods).
 *
 * @author Costin Leau
 * @author Chris Beams
 */
public abstract class AbstractAnnotationTests {

	protected ApplicationContext ctx;

	protected CacheableService<?> cs;

	protected CacheableService<?> ccs;

	protected CacheManager cm;

	/** @return a refreshed application context */
	protected abstract ApplicationContext getApplicationContext();

	@Before
	public void setup() {
		ctx = getApplicationContext();
		cs = ctx.getBean("service", CacheableService.class);
		ccs = ctx.getBean("classService", CacheableService.class);
		cm = ctx.getBean(CacheManager.class);
		Collection<String> cn = cm.getCacheNames();
		assertTrue(cn.contains("default"));
		assertTrue(cn.contains("secondary"));
		assertTrue(cn.contains("primary"));
	}

	public void testCacheable(CacheableService<?> service) throws Exception {
		Object o1 = new Object();

		Object r1 = service.cache(o1);
		Object r2 = service.cache(o1);
		Object r3 = service.cache(o1);

		assertSame(r1, r2);
		assertSame(r1, r3);
	}

	public void testEvict(CacheableService<?> service) throws Exception {
		Object o1 = new Object();

		Object r1 = service.cache(o1);
		Object r2 = service.cache(o1);

		assertSame(r1, r2);
		service.invalidate(o1);
		Object r3 = service.cache(o1);
		Object r4 = service.cache(o1);
		assertNotSame(r1, r3);
		assertSame(r3, r4);
	}

	public void testEvictEarly(CacheableService<?> service) throws Exception {
		Object o1 = new Object();

		Object r1 = service.cache(o1);
		Object r2 = service.cache(o1);

		assertSame(r1, r2);
		try {
			service.evictEarly(o1);
		} catch (RuntimeException ex) {
			// expected
		}

		Object r3 = service.cache(o1);
		Object r4 = service.cache(o1);
		assertNotSame(r1, r3);
		assertSame(r3, r4);
	}

	public void testEvictException(CacheableService<?> service) throws Exception {
		Object o1 = new Object();

		Object r1 = service.cache(o1);
		Object r2 = service.cache(o1);

		assertSame(r1, r2);
		try {
			service.evictWithException(o1);
		} catch (RuntimeException ex) {
			// expected
		}
		// exception occurred, eviction skipped, data should still be in the cache
		Object r3 = service.cache(o1);
		assertSame(r1, r3);
	}

	public void testEvictWKey(CacheableService<?> service) throws Exception {
		Object o1 = new Object();

		Object r1 = service.cache(o1);
		Object r2 = service.cache(o1);

		assertSame(r1, r2);
		service.evict(o1, null);
		Object r3 = service.cache(o1);
		Object r4 = service.cache(o1);
		assertNotSame(r1, r3);
		assertSame(r3, r4);
	}

	public void testEvictWKeyEarly(CacheableService<?> service) throws Exception {
		Object o1 = new Object();

		Object r1 = service.cache(o1);
		Object r2 = service.cache(o1);

		assertSame(r1, r2);

		try {
			service.invalidateEarly(o1, null);
		} catch (Exception ex) {
			// expected
		}
		Object r3 = service.cache(o1);
		Object r4 = service.cache(o1);
		assertNotSame(r1, r3);
		assertSame(r3, r4);
	}

	public void testEvictAll(CacheableService<?> service) throws Exception {
		Object o1 = new Object();

		Object r1 = service.cache(o1);
		Object r2 = service.cache(o1);

		Object o2 = new Object();
		Object r10 = service.cache(o2);

		assertSame(r1, r2);
		assertNotSame(r1, r10);
		service.evictAll(new Object());
		Cache cache = cm.getCache("default");
		assertNull(cache.get(o1));
		assertNull(cache.get(o2));

		Object r3 = service.cache(o1);
		Object r4 = service.cache(o1);
		assertNotSame(r1, r3);
		assertSame(r3, r4);
	}

	public void testConditionalExpression(CacheableService<?> service) throws Exception {
		Object r1 = service.conditional(4);
		Object r2 = service.conditional(4);

		assertNotSame(r1, r2);

		Object r3 = service.conditional(3);
		Object r4 = service.conditional(3);

		assertSame(r3, r4);
	}

	public void testKeyExpression(CacheableService<?> service) throws Exception {
		Object r1 = service.key(5, 1);
		Object r2 = service.key(5, 2);

		assertSame(r1, r2);

		Object r3 = service.key(1, 5);
		Object r4 = service.key(2, 5);

		assertNotSame(r3, r4);
	}

	public void testNullValue(CacheableService<?> service) throws Exception {
		Object key = new Object();
		assertNull(service.nullValue(key));
		int nr = service.nullInvocations().intValue();
		assertNull(service.nullValue(key));
		assertEquals(nr, service.nullInvocations().intValue());
		assertNull(service.nullValue(new Object()));
		assertEquals(nr + 1, service.nullInvocations().intValue());
	}

	public void testMethodName(CacheableService<?> service, String keyName) throws Exception {
		Object key = new Object();
		Object r1 = service.name(key);
		assertSame(r1, service.name(key));
		Cache cache = cm.getCache("default");
		// assert the method name is used
		assertNotNull(cache.get(keyName));
	}

	public void testRootVars(CacheableService<?> service) {
		Object key = new Object();
		Object r1 = service.rootVars(key);
		assertSame(r1, service.rootVars(key));
		Cache cache = cm.getCache("default");
		// assert the method name is used
		String expectedKey = "rootVarsrootVars" + AopProxyUtils.ultimateTargetClass(service) + service;
		assertNotNull(cache.get(expectedKey));
	}

	public void testCheckedThrowable(CacheableService<?> service) throws Exception {
		String arg = UUID.randomUUID().toString();
		try {
			service.throwChecked(arg);
			fail("Excepted exception");
		} catch (Exception ex) {
			assertEquals(arg, ex.getMessage());
		}
	}

	public void testUncheckedThrowable(CacheableService<?> service) throws Exception {
		try {
			service.throwUnchecked(Long.valueOf(1));
			fail("Excepted exception");
		} catch (RuntimeException ex) {
			assertTrue("Excepted different exception type and got " + ex.getClass(),
					ex instanceof UnsupportedOperationException);
			// expected
		}
	}

	public void testNullArg(CacheableService<?> service) {
		Object r1 = service.cache(null);
		assertSame(r1, service.cache(null));
	}

	public void testCacheUpdate(CacheableService<?> service) {
		Object o = new Object();
		Cache cache = cm.getCache("default");
		assertNull(cache.get(o));
		Object r1 = service.update(o);
		assertSame(r1, cache.get(o).get());

		o = new Object();
		assertNull(cache.get(o));
		Object r2 = service.update(o);
		assertSame(r2, cache.get(o).get());
	}

	public void testConditionalCacheUpdate(CacheableService<?> service) {
		Integer one = Integer.valueOf(1);
		Integer three = Integer.valueOf(3);

		Cache cache = cm.getCache("default");
		assertEquals(one, Integer.valueOf(service.conditionalUpdate(one).toString()));
		assertNull(cache.get(one));

		assertEquals(three, Integer.valueOf(service.conditionalUpdate(three).toString()));
		assertEquals(three, Integer.valueOf(cache.get(three).get().toString()));
	}

	public void testMultiCache(CacheableService<?> service) {
		Object o1 = new Object();
		Object o2 = new Object();

		Cache primary = cm.getCache("primary");
		Cache secondary = cm.getCache("secondary");

		assertNull(primary.get(o1));
		assertNull(secondary.get(o1));
		Object r1 = service.multiCache(o1);
		assertSame(r1, primary.get(o1).get());
		assertSame(r1, secondary.get(o1).get());

		Object r2 = service.multiCache(o1);
		Object r3 = service.multiCache(o1);

		assertSame(r1, r2);
		assertSame(r1, r3);

		assertNull(primary.get(o2));
		assertNull(secondary.get(o2));
		Object r4 = service.multiCache(o2);
		assertSame(r4, primary.get(o2).get());
		assertSame(r4, secondary.get(o2).get());
	}

	public void testMultiEvict(CacheableService<?> service) {
		Object o1 = new Object();
		Object o2 = o1.toString() + "A";


		Object r1 = service.multiCache(o1);
		Object r2 = service.multiCache(o1);

		Cache primary = cm.getCache("primary");
		Cache secondary = cm.getCache("secondary");

		primary.put(o2, o2);
		assertSame(r1, r2);
		assertSame(r1, primary.get(o1).get());
		assertSame(r1, secondary.get(o1).get());

		service.multiEvict(o1);
		assertNull(primary.get(o1));
		assertNull(secondary.get(o1));
		assertNull(primary.get(o2));

		Object r3 = service.multiCache(o1);
		Object r4 = service.multiCache(o1);
		assertNotSame(r1, r3);
		assertSame(r3, r4);

		assertSame(r3, primary.get(o1).get());
		assertSame(r4, secondary.get(o1).get());
	}

	public void testMultiPut(CacheableService<?> service) {
		Object o = Integer.valueOf(1);

		Cache primary = cm.getCache("primary");
		Cache secondary = cm.getCache("secondary");

		assertNull(primary.get(o));
		assertNull(secondary.get(o));
		Object r1 = service.multiUpdate(o);
		assertSame(r1, primary.get(o).get());
		assertSame(r1, secondary.get(o).get());

		o = Integer.valueOf(2);
		assertNull(primary.get(o));
		assertNull(secondary.get(o));
		Object r2 = service.multiUpdate(o);
		assertSame(r2, primary.get(o).get());
		assertSame(r2, secondary.get(o).get());
	}

	public void testMultiCacheAndEvict(CacheableService<?> service) {
		String methodName = "multiCacheAndEvict";

		Cache primary = cm.getCache("primary");
		Cache secondary = cm.getCache("secondary");
		Object key = Integer.valueOf(1);

		secondary.put(key, key);

		assertNull(secondary.get(methodName));
		assertSame(key, secondary.get(key).get());

		Object r1 = service.multiCacheAndEvict(key);
		assertSame(r1, service.multiCacheAndEvict(key));

		// assert the method name is used
		assertSame(r1, primary.get(methodName).get());
		assertNull(secondary.get(methodName));
		assertNull(secondary.get(key));
	}

	public void testMultiConditionalCacheAndEvict(CacheableService<?> service) {
		Cache primary = cm.getCache("primary");
		Cache secondary = cm.getCache("secondary");
		Object key = Integer.valueOf(1);

		secondary.put(key, key);

		assertNull(primary.get(key));
		assertSame(key, secondary.get(key).get());

		Object r1 = service.multiConditionalCacheAndEvict(key);
		Object r3 = service.multiConditionalCacheAndEvict(key);

		assertTrue(!r1.equals(r3));
		assertNull(primary.get(key));

		Object key2 = Integer.valueOf(3);
		Object r2 = service.multiConditionalCacheAndEvict(key2);
		assertSame(r2, service.multiConditionalCacheAndEvict(key2));

		// assert the method name is used
		assertSame(r2, primary.get(key2).get());
		assertNull(secondary.get(key2));
	}

	@Test
	public void testCacheable() throws Exception {
		testCacheable(cs);
	}

	@Test
	public void testInvalidate() throws Exception {
		testEvict(cs);
	}

	@Test
	public void testEarlyInvalidate() throws Exception {
		testEvictEarly(cs);
	}

	@Test
	public void testEvictWithException() throws Exception {
		testEvictException(cs);
	}

	@Test
	public void testEvictAll() throws Exception {
		testEvictAll(cs);
	}

	@Test
	public void testInvalidateWithKey() throws Exception {
		testEvictWKey(cs);
	}

	@Test
	public void testEarlyInvalidateWithKey() throws Exception {
		testEvictWKeyEarly(cs);
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
		testEvict(ccs);
	}

	@Test
	public void testClassEarlyInvalidate() throws Exception {
		testEvictEarly(ccs);
	}

	@Test
	public void testClassEvictAll() throws Exception {
		testEvictAll(ccs);
	}

	@Test
	public void testClassEvictWithException() throws Exception {
		testEvictException(ccs);
	}

	@Test
	public void testClassCacheInvalidateWKey() throws Exception {
		testEvictWKey(ccs);
	}

	@Test
	public void testClassEarlyInvalidateWithKey() throws Exception {
		testEvictWKeyEarly(ccs);
	}

	@Test
	public void testNullValue() throws Exception {
		testNullValue(cs);
	}

	@Test
	public void testClassNullValue() throws Exception {
		Object key = new Object();
		assertNull(ccs.nullValue(key));
		int nr = ccs.nullInvocations().intValue();
		assertNull(ccs.nullValue(key));
		assertEquals(nr, ccs.nullInvocations().intValue());
		assertNull(ccs.nullValue(new Object()));
		// the check method is also cached
		assertEquals(nr, ccs.nullInvocations().intValue());
		assertEquals(nr + 1, AnnotatedClassCacheableService.nullInvocations.intValue());
	}

	@Test
	public void testMethodName() throws Exception {
		testMethodName(cs, "name");
	}

	@Test
	public void testClassMethodName() throws Exception {
		testMethodName(ccs, "namedefault");
	}

	@Test
	public void testRootVars() throws Exception {
		testRootVars(cs);
	}

	@Test
	public void testClassRootVars() throws Exception {
		testRootVars(ccs);
	}

	@Test
	public void testNullArg() throws Exception {
		testNullArg(cs);
	}

	@Test
	public void testClassNullArg() throws Exception {
		testNullArg(ccs);
	}

	@Test
	public void testCheckedException() throws Exception {
		testCheckedThrowable(cs);
	}

	@Test
	public void testClassCheckedException() throws Exception {
		testCheckedThrowable(ccs);
	}

	@Test
	public void testUncheckedException() throws Exception {
		testUncheckedThrowable(cs);
	}

	@Test
	public void testClassUncheckedException() throws Exception {
		testUncheckedThrowable(ccs);
	}

	@Test
	public void testUpdate() {
		testCacheUpdate(cs);
	}

	@Test
	public void testClassUpdate() {
		testCacheUpdate(ccs);
	}

	@Test
	public void testConditionalUpdate() {
		testConditionalCacheUpdate(cs);
	}

	@Test
	public void testClassConditionalUpdate() {
		testConditionalCacheUpdate(ccs);
	}

	@Test
	public void testMultiCache() {
		testMultiCache(cs);
	}

	@Test
	public void testClassMultiCache() {
		testMultiCache(ccs);
	}

	@Test
	public void testMultiEvict() {
		testMultiEvict(cs);
	}

	@Test
	public void testClassMultiEvict() {
		testMultiEvict(ccs);
	}

	@Test
	public void testMultiPut() {
		testMultiPut(cs);
	}

	@Test
	public void testClassMultiPut() {
		testMultiPut(ccs);
	}

	@Test
	public void testMultiCacheAndEvict() {
		testMultiCacheAndEvict(cs);
	}

	@Test
	public void testClassMultiCacheAndEvict() {
		testMultiCacheAndEvict(ccs);
	}

	@Test
	public void testMultiConditionalCacheAndEvict() {
		testMultiConditionalCacheAndEvict(cs);
	}

	@Test
	public void testClassMultiConditionalCacheAndEvict() {
		testMultiConditionalCacheAndEvict(ccs);
	}
}