/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.Collection;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * Abstract cache annotation tests (containing several reusable methods).
 *
 * @author Costin Leau
 * @author Chris Beams
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public abstract class AbstractCacheAnnotationTests {

	protected ConfigurableApplicationContext ctx;

	protected CacheableService<?> cs;

	protected CacheableService<?> ccs;

	protected CacheManager cm;


	/**
	 * @return a refreshed application context
	 */
	protected abstract ConfigurableApplicationContext getApplicationContext();


	@Before
	public void setup() {
		this.ctx = getApplicationContext();
		this.cs = ctx.getBean("service", CacheableService.class);
		this.ccs = ctx.getBean("classService", CacheableService.class);
		this.cm = ctx.getBean("cacheManager", CacheManager.class);

		Collection<String> cn = this.cm.getCacheNames();
		assertThat(cn.contains("testCache")).isTrue();
		assertThat(cn.contains("secondary")).isTrue();
		assertThat(cn.contains("primary")).isTrue();
	}

	@After
	public void close() {
		if (this.ctx != null) {
			this.ctx.close();
		}
	}


	public void testCacheable(CacheableService<?> service) throws Exception {
		Object o1 = new Object();

		Object r1 = service.cache(o1);
		Object r2 = service.cache(o1);
		Object r3 = service.cache(o1);

		assertThat(r2).isSameAs(r1);
		assertThat(r3).isSameAs(r1);
	}

	public void testCacheableNull(CacheableService<?> service) throws Exception {
		Object o1 = new Object();
		assertThat((Object) this.cm.getCache("testCache").get(o1)).isNull();

		Object r1 = service.cacheNull(o1);
		Object r2 = service.cacheNull(o1);
		Object r3 = service.cacheNull(o1);

		assertThat(r2).isSameAs(r1);
		assertThat(r3).isSameAs(r1);

		assertThat(this.cm.getCache("testCache").get(o1).get()).isEqualTo(r3);
		assertThat(r3).as("Cached value should be null").isNull();
	}

	public void testCacheableSync(CacheableService<?> service) throws Exception {
		Object o1 = new Object();

		Object r1 = service.cacheSync(o1);
		Object r2 = service.cacheSync(o1);
		Object r3 = service.cacheSync(o1);

		assertThat(r2).isSameAs(r1);
		assertThat(r3).isSameAs(r1);
	}

	public void testCacheableSyncNull(CacheableService<?> service) throws Exception {
		Object o1 = new Object();
		assertThat((Object) this.cm.getCache("testCache").get(o1)).isNull();

		Object r1 = service.cacheSyncNull(o1);
		Object r2 = service.cacheSyncNull(o1);
		Object r3 = service.cacheSyncNull(o1);

		assertThat(r2).isSameAs(r1);
		assertThat(r3).isSameAs(r1);

		assertThat(this.cm.getCache("testCache").get(o1).get()).isEqualTo(r3);
		assertThat(r3).as("Cached value should be null").isNull();
	}

	public void testEvict(CacheableService<?> service) throws Exception {
		Object o1 = new Object();

		Object r1 = service.cache(o1);
		Object r2 = service.cache(o1);

		assertThat(r2).isSameAs(r1);
		service.invalidate(o1);
		Object r3 = service.cache(o1);
		Object r4 = service.cache(o1);
		assertThat(r3).isNotSameAs(r1);
		assertThat(r4).isSameAs(r3);
	}

	public void testEvictEarly(CacheableService<?> service) throws Exception {
		Object o1 = new Object();

		Object r1 = service.cache(o1);
		Object r2 = service.cache(o1);

		assertThat(r2).isSameAs(r1);
		try {
			service.evictEarly(o1);
		}
		catch (RuntimeException ex) {
			// expected
		}

		Object r3 = service.cache(o1);
		Object r4 = service.cache(o1);
		assertThat(r3).isNotSameAs(r1);
		assertThat(r4).isSameAs(r3);
	}

	public void testEvictException(CacheableService<?> service) throws Exception {
		Object o1 = new Object();

		Object r1 = service.cache(o1);
		Object r2 = service.cache(o1);

		assertThat(r2).isSameAs(r1);
		try {
			service.evictWithException(o1);
		}
		catch (RuntimeException ex) {
			// expected
		}
		// exception occurred, eviction skipped, data should still be in the cache
		Object r3 = service.cache(o1);
		assertThat(r3).isSameAs(r1);
	}

	public void testEvictWKey(CacheableService<?> service) throws Exception {
		Object o1 = new Object();

		Object r1 = service.cache(o1);
		Object r2 = service.cache(o1);

		assertThat(r2).isSameAs(r1);
		service.evict(o1, null);
		Object r3 = service.cache(o1);
		Object r4 = service.cache(o1);
		assertThat(r3).isNotSameAs(r1);
		assertThat(r4).isSameAs(r3);
	}

	public void testEvictWKeyEarly(CacheableService<?> service) throws Exception {
		Object o1 = new Object();

		Object r1 = service.cache(o1);
		Object r2 = service.cache(o1);

		assertThat(r2).isSameAs(r1);

		try {
			service.invalidateEarly(o1, null);
		}
		catch (Exception ex) {
			// expected
		}
		Object r3 = service.cache(o1);
		Object r4 = service.cache(o1);
		assertThat(r3).isNotSameAs(r1);
		assertThat(r4).isSameAs(r3);
	}

	public void testEvictAll(CacheableService<?> service) throws Exception {
		Object o1 = new Object();

		Object r1 = service.cache(o1);
		Object r2 = service.cache(o1);

		Object o2 = new Object();
		Object r10 = service.cache(o2);

		assertThat(r2).isSameAs(r1);
		assertThat(r10).isNotSameAs(r1);
		service.evictAll(new Object());
		Cache cache = this.cm.getCache("testCache");
		assertThat((Object) cache.get(o1)).isNull();
		assertThat((Object) cache.get(o2)).isNull();

		Object r3 = service.cache(o1);
		Object r4 = service.cache(o1);
		assertThat(r3).isNotSameAs(r1);
		assertThat(r4).isSameAs(r3);
	}

	public void testConditionalExpression(CacheableService<?> service) throws Exception {
		Object r1 = service.conditional(4);
		Object r2 = service.conditional(4);

		assertThat(r2).isNotSameAs(r1);

		Object r3 = service.conditional(3);
		Object r4 = service.conditional(3);

		assertThat(r4).isSameAs(r3);
	}

	public void testConditionalExpressionSync(CacheableService<?> service) throws Exception {
		Object r1 = service.conditionalSync(4);
		Object r2 = service.conditionalSync(4);

		assertThat(r2).isNotSameAs(r1);

		Object r3 = service.conditionalSync(3);
		Object r4 = service.conditionalSync(3);

		assertThat(r4).isSameAs(r3);
	}

	public void testUnlessExpression(CacheableService<?> service) throws Exception {
		Cache cache = this.cm.getCache("testCache");
		cache.clear();
		service.unless(10);
		service.unless(11);
		assertThat(cache.get(10).get()).isEqualTo(10L);
		assertThat(cache.get(11)).isNull();
	}

	public void testKeyExpression(CacheableService<?> service) throws Exception {
		Object r1 = service.key(5, 1);
		Object r2 = service.key(5, 2);

		assertThat(r2).isSameAs(r1);

		Object r3 = service.key(1, 5);
		Object r4 = service.key(2, 5);

		assertThat(r4).isNotSameAs(r3);
	}

	public void testVarArgsKey(CacheableService<?> service) throws Exception {
		Object r1 = service.varArgsKey(1, 2, 3);
		Object r2 = service.varArgsKey(1, 2, 3);

		assertThat(r2).isSameAs(r1);

		Object r3 = service.varArgsKey(1, 2, 3);
		Object r4 = service.varArgsKey(1, 2);

		assertThat(r4).isNotSameAs(r3);
	}


	public void testNullValue(CacheableService<?> service) throws Exception {
		Object key = new Object();
		assertThat(service.nullValue(key)).isNull();
		int nr = service.nullInvocations().intValue();
		assertThat(service.nullValue(key)).isNull();
		assertThat(service.nullInvocations().intValue()).isEqualTo(nr);
		assertThat(service.nullValue(new Object())).isNull();
		assertThat(service.nullInvocations().intValue()).isEqualTo(nr + 1);
	}

	public void testMethodName(CacheableService<?> service, String keyName) throws Exception {
		Object key = new Object();
		Object r1 = service.name(key);
		assertThat(service.name(key)).isSameAs(r1);
		Cache cache = this.cm.getCache("testCache");
		// assert the method name is used
		assertThat(cache.get(keyName)).isNotNull();
	}

	public void testRootVars(CacheableService<?> service) {
		Object key = new Object();
		Object r1 = service.rootVars(key);
		assertThat(service.rootVars(key)).isSameAs(r1);
		Cache cache = this.cm.getCache("testCache");
		// assert the method name is used
		String expectedKey = "rootVarsrootVars" + AopProxyUtils.ultimateTargetClass(service) + service;
		assertThat(cache.get(expectedKey)).isNotNull();
	}

	public void testCheckedThrowable(CacheableService<?> service) throws Exception {
		String arg = UUID.randomUUID().toString();
		assertThatIOException().isThrownBy(() ->
				service.throwChecked(arg))
			.withMessage(arg);
	}

	public void testUncheckedThrowable(CacheableService<?> service) throws Exception {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				service.throwUnchecked(1L))
			.withMessage("1");
	}

	public void testCheckedThrowableSync(CacheableService<?> service) throws Exception {
		String arg = UUID.randomUUID().toString();
		assertThatIOException().isThrownBy(() ->
				service.throwCheckedSync(arg))
			.withMessage(arg);
	}

	public void testUncheckedThrowableSync(CacheableService<?> service) throws Exception {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				service.throwUncheckedSync(1L))
			.withMessage("1");
	}

	public void testNullArg(CacheableService<?> service) {
		Object r1 = service.cache(null);
		assertThat(service.cache(null)).isSameAs(r1);
	}

	public void testCacheUpdate(CacheableService<?> service) {
		Object o = new Object();
		Cache cache = this.cm.getCache("testCache");
		assertThat((Object) cache.get(o)).isNull();
		Object r1 = service.update(o);
		assertThat(cache.get(o).get()).isSameAs(r1);

		o = new Object();
		assertThat((Object) cache.get(o)).isNull();
		Object r2 = service.update(o);
		assertThat(cache.get(o).get()).isSameAs(r2);
	}

	public void testConditionalCacheUpdate(CacheableService<?> service) {
		Integer one = 1;
		Integer three = 3;

		Cache cache = this.cm.getCache("testCache");
		assertThat((int) Integer.valueOf(service.conditionalUpdate(one).toString())).isEqualTo((int) one);
		assertThat(cache.get(one)).isNull();

		assertThat((int) Integer.valueOf(service.conditionalUpdate(three).toString())).isEqualTo((int) three);
		assertThat((int) Integer.valueOf(cache.get(three).get().toString())).isEqualTo((int) three);
	}

	public void testMultiCache(CacheableService<?> service) {
		Object o1 = new Object();
		Object o2 = new Object();

		Cache primary = this.cm.getCache("primary");
		Cache secondary = this.cm.getCache("secondary");

		assertThat((Object) primary.get(o1)).isNull();
		assertThat((Object) secondary.get(o1)).isNull();
		Object r1 = service.multiCache(o1);
		assertThat(primary.get(o1).get()).isSameAs(r1);
		assertThat(secondary.get(o1).get()).isSameAs(r1);

		Object r2 = service.multiCache(o1);
		Object r3 = service.multiCache(o1);

		assertThat(r2).isSameAs(r1);
		assertThat(r3).isSameAs(r1);

		assertThat((Object) primary.get(o2)).isNull();
		assertThat((Object) secondary.get(o2)).isNull();
		Object r4 = service.multiCache(o2);
		assertThat(primary.get(o2).get()).isSameAs(r4);
		assertThat(secondary.get(o2).get()).isSameAs(r4);
	}

	public void testMultiEvict(CacheableService<?> service) {
		Object o1 = new Object();
		Object o2 = o1.toString() + "A";


		Object r1 = service.multiCache(o1);
		Object r2 = service.multiCache(o1);

		Cache primary = this.cm.getCache("primary");
		Cache secondary = this.cm.getCache("secondary");

		primary.put(o2, o2);
		assertThat(r2).isSameAs(r1);
		assertThat(primary.get(o1).get()).isSameAs(r1);
		assertThat(secondary.get(o1).get()).isSameAs(r1);

		service.multiEvict(o1);
		assertThat((Object) primary.get(o1)).isNull();
		assertThat((Object) secondary.get(o1)).isNull();
		assertThat((Object) primary.get(o2)).isNull();

		Object r3 = service.multiCache(o1);
		Object r4 = service.multiCache(o1);
		assertThat(r3).isNotSameAs(r1);
		assertThat(r4).isSameAs(r3);

		assertThat(primary.get(o1).get()).isSameAs(r3);
		assertThat(secondary.get(o1).get()).isSameAs(r4);
	}

	public void testMultiPut(CacheableService<?> service) {
		Object o = 1;

		Cache primary = this.cm.getCache("primary");
		Cache secondary = this.cm.getCache("secondary");

		assertThat((Object) primary.get(o)).isNull();
		assertThat((Object) secondary.get(o)).isNull();
		Object r1 = service.multiUpdate(o);
		assertThat(primary.get(o).get()).isSameAs(r1);
		assertThat(secondary.get(o).get()).isSameAs(r1);

		o = 2;
		assertThat((Object) primary.get(o)).isNull();
		assertThat((Object) secondary.get(o)).isNull();
		Object r2 = service.multiUpdate(o);
		assertThat(primary.get(o).get()).isSameAs(r2);
		assertThat(secondary.get(o).get()).isSameAs(r2);
	}

	public void testPutRefersToResult(CacheableService<?> service) throws Exception {
		Long id = Long.MIN_VALUE;
		TestEntity entity = new TestEntity();
		Cache primary = this.cm.getCache("primary");
		assertThat((Object) primary.get(id)).isNull();
		assertThat((Object) entity.getId()).isNull();
		service.putRefersToResult(entity);
		assertThat(primary.get(id).get()).isSameAs(entity);
	}

	public void testMultiCacheAndEvict(CacheableService<?> service) {
		String methodName = "multiCacheAndEvict";

		Cache primary = this.cm.getCache("primary");
		Cache secondary = this.cm.getCache("secondary");
		Object key = 1;

		secondary.put(key, key);

		assertThat(secondary.get(methodName)).isNull();
		assertThat(secondary.get(key).get()).isSameAs(key);

		Object r1 = service.multiCacheAndEvict(key);
		assertThat(service.multiCacheAndEvict(key)).isSameAs(r1);

		// assert the method name is used
		assertThat(primary.get(methodName).get()).isSameAs(r1);
		assertThat(secondary.get(methodName)).isNull();
		assertThat(secondary.get(key)).isNull();
	}

	public void testMultiConditionalCacheAndEvict(CacheableService<?> service) {
		Cache primary = this.cm.getCache("primary");
		Cache secondary = this.cm.getCache("secondary");
		Object key = 1;

		secondary.put(key, key);

		assertThat(primary.get(key)).isNull();
		assertThat(secondary.get(key).get()).isSameAs(key);

		Object r1 = service.multiConditionalCacheAndEvict(key);
		Object r3 = service.multiConditionalCacheAndEvict(key);

		assertThat(!r1.equals(r3)).isTrue();
		assertThat((Object) primary.get(key)).isNull();

		Object key2 = 3;
		Object r2 = service.multiConditionalCacheAndEvict(key2);
		assertThat(service.multiConditionalCacheAndEvict(key2)).isSameAs(r2);

		// assert the method name is used
		assertThat(primary.get(key2).get()).isSameAs(r2);
		assertThat(secondary.get(key2)).isNull();
	}

	@Test
	public void testCacheable() throws Exception {
		testCacheable(this.cs);
	}

	@Test
	public void testCacheableNull() throws Exception {
		testCacheableNull(this.cs);
	}

	@Test
	public void testCacheableSync() throws Exception {
		testCacheableSync(this.cs);
	}

	@Test
	public void testCacheableSyncNull() throws Exception {
		testCacheableSyncNull(this.cs);
	}

	@Test
	public void testInvalidate() throws Exception {
		testEvict(this.cs);
	}

	@Test
	public void testEarlyInvalidate() throws Exception {
		testEvictEarly(this.cs);
	}

	@Test
	public void testEvictWithException() throws Exception {
		testEvictException(this.cs);
	}

	@Test
	public void testEvictAll() throws Exception {
		testEvictAll(this.cs);
	}

	@Test
	public void testInvalidateWithKey() throws Exception {
		testEvictWKey(this.cs);
	}

	@Test
	public void testEarlyInvalidateWithKey() throws Exception {
		testEvictWKeyEarly(this.cs);
	}

	@Test
	public void testConditionalExpression() throws Exception {
		testConditionalExpression(this.cs);
	}

	@Test
	public void testConditionalExpressionSync() throws Exception {
		testConditionalExpressionSync(this.cs);
	}

	@Test
	public void testUnlessExpression() throws Exception {
		testUnlessExpression(this.cs);
	}

	@Test
	public void testClassCacheUnlessExpression() throws Exception {
		testUnlessExpression(this.cs);
	}

	@Test
	public void testKeyExpression() throws Exception {
		testKeyExpression(this.cs);
	}

	@Test
	public void testVarArgsKey() throws Exception {
		testVarArgsKey(this.cs);
	}

	@Test
	public void testClassCacheCacheable() throws Exception {
		testCacheable(this.ccs);
	}

	@Test
	public void testClassCacheInvalidate() throws Exception {
		testEvict(this.ccs);
	}

	@Test
	public void testClassEarlyInvalidate() throws Exception {
		testEvictEarly(this.ccs);
	}

	@Test
	public void testClassEvictAll() throws Exception {
		testEvictAll(this.ccs);
	}

	@Test
	public void testClassEvictWithException() throws Exception {
		testEvictException(this.ccs);
	}

	@Test
	public void testClassCacheInvalidateWKey() throws Exception {
		testEvictWKey(this.ccs);
	}

	@Test
	public void testClassEarlyInvalidateWithKey() throws Exception {
		testEvictWKeyEarly(this.ccs);
	}

	@Test
	public void testNullValue() throws Exception {
		testNullValue(this.cs);
	}

	@Test
	public void testClassNullValue() throws Exception {
		Object key = new Object();
		assertThat(this.ccs.nullValue(key)).isNull();
		int nr = this.ccs.nullInvocations().intValue();
		assertThat(this.ccs.nullValue(key)).isNull();
		assertThat(this.ccs.nullInvocations().intValue()).isEqualTo(nr);
		assertThat(this.ccs.nullValue(new Object())).isNull();
		// the check method is also cached
		assertThat(this.ccs.nullInvocations().intValue()).isEqualTo(nr);
		assertThat(AnnotatedClassCacheableService.nullInvocations.intValue()).isEqualTo(nr + 1);
	}

	@Test
	public void testMethodName() throws Exception {
		testMethodName(this.cs, "name");
	}

	@Test
	public void testClassMethodName() throws Exception {
		testMethodName(this.ccs, "nametestCache");
	}

	@Test
	public void testRootVars() throws Exception {
		testRootVars(this.cs);
	}

	@Test
	public void testClassRootVars() throws Exception {
		testRootVars(this.ccs);
	}

	@Test
	public void testCustomKeyGenerator() {
		Object param = new Object();
		Object r1 = this.cs.customKeyGenerator(param);
		assertThat(this.cs.customKeyGenerator(param)).isSameAs(r1);
		Cache cache = this.cm.getCache("testCache");
		// Checks that the custom keyGenerator was used
		Object expectedKey = SomeCustomKeyGenerator.generateKey("customKeyGenerator", param);
		assertThat(cache.get(expectedKey)).isNotNull();
	}

	@Test
	public void testUnknownCustomKeyGenerator() {
		Object param = new Object();
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				this.cs.unknownCustomKeyGenerator(param));
	}

	@Test
	public void testCustomCacheManager() {
		CacheManager customCm = this.ctx.getBean("customCacheManager", CacheManager.class);
		Object key = new Object();
		Object r1 = this.cs.customCacheManager(key);
		assertThat(this.cs.customCacheManager(key)).isSameAs(r1);

		Cache cache = customCm.getCache("testCache");
		assertThat(cache.get(key)).isNotNull();
	}

	@Test
	public void testUnknownCustomCacheManager() {
		Object param = new Object();
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				this.cs.unknownCustomCacheManager(param));
	}

	@Test
	public void testNullArg() throws Exception {
		testNullArg(this.cs);
	}

	@Test
	public void testClassNullArg() throws Exception {
		testNullArg(this.ccs);
	}

	@Test
	public void testCheckedException() throws Exception {
		testCheckedThrowable(this.cs);
	}

	@Test
	public void testClassCheckedException() throws Exception {
		testCheckedThrowable(this.ccs);
	}

	@Test
	public void testCheckedExceptionSync() throws Exception {
		testCheckedThrowableSync(this.cs);
	}

	@Test
	public void testClassCheckedExceptionSync() throws Exception {
		testCheckedThrowableSync(this.ccs);
	}

	@Test
	public void testUncheckedException() throws Exception {
		testUncheckedThrowable(this.cs);
	}

	@Test
	public void testClassUncheckedException() throws Exception {
		testUncheckedThrowable(this.ccs);
	}

	@Test
	public void testUncheckedExceptionSync() throws Exception {
		testUncheckedThrowableSync(this.cs);
	}

	@Test
	public void testClassUncheckedExceptionSync() throws Exception {
		testUncheckedThrowableSync(this.ccs);
	}

	@Test
	public void testUpdate() {
		testCacheUpdate(this.cs);
	}

	@Test
	public void testClassUpdate() {
		testCacheUpdate(this.ccs);
	}

	@Test
	public void testConditionalUpdate() {
		testConditionalCacheUpdate(this.cs);
	}

	@Test
	public void testClassConditionalUpdate() {
		testConditionalCacheUpdate(this.ccs);
	}

	@Test
	public void testMultiCache() {
		testMultiCache(this.cs);
	}

	@Test
	public void testClassMultiCache() {
		testMultiCache(this.ccs);
	}

	@Test
	public void testMultiEvict() {
		testMultiEvict(this.cs);
	}

	@Test
	public void testClassMultiEvict() {
		testMultiEvict(this.ccs);
	}

	@Test
	public void testMultiPut() {
		testMultiPut(this.cs);
	}

	@Test
	public void testClassMultiPut() {
		testMultiPut(this.ccs);
	}

	@Test
	public void testPutRefersToResult() throws Exception {
		testPutRefersToResult(this.cs);
	}

	@Test
	public void testClassPutRefersToResult() throws Exception {
		testPutRefersToResult(this.ccs);
	}

	@Test
	public void testMultiCacheAndEvict() {
		testMultiCacheAndEvict(this.cs);
	}

	@Test
	public void testClassMultiCacheAndEvict() {
		testMultiCacheAndEvict(this.ccs);
	}

	@Test
	public void testMultiConditionalCacheAndEvict() {
		testMultiConditionalCacheAndEvict(this.cs);
	}

	@Test
	public void testClassMultiConditionalCacheAndEvict() {
		testMultiConditionalCacheAndEvict(this.ccs);
	}

}
