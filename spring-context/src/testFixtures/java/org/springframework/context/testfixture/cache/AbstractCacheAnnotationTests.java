/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.context.testfixture.cache;

import java.util.Collection;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.testfixture.cache.beans.AnnotatedClassCacheableService;
import org.springframework.context.testfixture.cache.beans.CacheableService;
import org.springframework.context.testfixture.cache.beans.TestEntity;

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


	@BeforeEach
	public void setup() {
		this.ctx = getApplicationContext();
		this.cs = ctx.getBean("service", CacheableService.class);
		this.ccs = ctx.getBean("classService", CacheableService.class);
		this.cm = ctx.getBean("cacheManager", CacheManager.class);

		Collection<String> cn = this.cm.getCacheNames();
		assertThat(cn).containsOnly("testCache", "secondary", "primary");
	}

	@AfterEach
	public void close() {
		if (this.ctx != null) {
			this.ctx.close();
		}
	}


	protected void testCacheable(CacheableService<?> service) {
		Object o1 = new Object();

		Object r1 = service.cache(o1);
		Object r2 = service.cache(o1);
		Object r3 = service.cache(o1);

		assertThat(r2).isSameAs(r1);
		assertThat(r3).isSameAs(r1);
	}

	protected void testCacheableNull(CacheableService<?> service) {
		Object o1 = new Object();
		assertThat(this.cm.getCache("testCache").get(o1)).isNull();

		Object r1 = service.cacheNull(o1);
		Object r2 = service.cacheNull(o1);
		Object r3 = service.cacheNull(o1);

		assertThat(r2).isSameAs(r1);
		assertThat(r3).isSameAs(r1);

		assertThat(this.cm.getCache("testCache")).as("testCache").isNotNull();
		assertThat(this.cm.getCache("testCache").get(o1)).as("cached object").isNotNull();
		assertThat(this.cm.getCache("testCache").get(o1).get()).isEqualTo(r3);
		assertThat(r3).as("Cached value should be null").isNull();
	}

	protected void testCacheableSync(CacheableService<?> service) {
		Object o1 = new Object();

		Object r1 = service.cacheSync(o1);
		Object r2 = service.cacheSync(o1);
		Object r3 = service.cacheSync(o1);

		assertThat(r2).isSameAs(r1);
		assertThat(r3).isSameAs(r1);
	}

	protected void testCacheableSyncNull(CacheableService<?> service) {
		Object o1 = new Object();
		assertThat(this.cm.getCache("testCache").get(o1)).isNull();

		Object r1 = service.cacheSyncNull(o1);
		Object r2 = service.cacheSyncNull(o1);
		Object r3 = service.cacheSyncNull(o1);

		assertThat(r2).isSameAs(r1);
		assertThat(r3).isSameAs(r1);

		assertThat(this.cm.getCache("testCache").get(o1).get()).isEqualTo(r3);
		assertThat(r3).as("Cached value should be null").isNull();
	}

	protected void testEvict(CacheableService<?> service, boolean successExpected) {
		Cache cache = this.cm.getCache("testCache");

		Object o1 = new Object();
		cache.putIfAbsent(o1, -1L);
		Object r1 = service.cache(o1);

		service.evict(o1, null);
		if (successExpected) {
			assertThat(cache.get(o1)).isNull();
		}
		else {
			assertThat(cache.get(o1)).isNotNull();
		}

		Object r2 = service.cache(o1);
		if (successExpected) {
			assertThat(r2).isNotSameAs(r1);
		}
		else {
			assertThat(r2).isSameAs(r1);
		}
	}

	protected void testEvictEarly(CacheableService<?> service) {
		Cache cache = this.cm.getCache("testCache");

		Object o1 = new Object();
		cache.putIfAbsent(o1, -1L);
		Object r1 = service.cache(o1);

		try {
			service.evictEarly(o1);
		}
		catch (RuntimeException ex) {
			// expected
		}
		assertThat(cache.get(o1)).isNull();

		Object r2 = service.cache(o1);
		assertThat(r2).isNotSameAs(r1);
	}

	protected void testEvictException(CacheableService<?> service) {
		Object o1 = new Object();
		Object r1 = service.cache(o1);

		try {
			service.evictWithException(o1);
		}
		catch (RuntimeException ex) {
			// expected
		}
		// exception occurred, eviction skipped, data should still be in the cache
		Object r2 = service.cache(o1);
		assertThat(r2).isSameAs(r1);
	}

	protected void testEvictWithKey(CacheableService<?> service) {
		Object o1 = new Object();
		Object r1 = service.cache(o1);

		service.evict(o1, null);
		Object r2 = service.cache(o1);
		assertThat(r2).isNotSameAs(r1);
	}

	protected void testEvictWithKeyEarly(CacheableService<?> service) {
		Object o1 = new Object();
		Object r1 = service.cache(o1);

		try {
			service.evictEarly(o1);
		}
		catch (Exception ex) {
			// expected
		}
		Object r2 = service.cache(o1);
		assertThat(r2).isNotSameAs(r1);
	}

	protected void testEvictAll(CacheableService<?> service, boolean successExpected) {
		Cache cache = this.cm.getCache("testCache");

		Object o1 = new Object();
		Object o2 = new Object();
		cache.putIfAbsent(o1, -1L);
		cache.putIfAbsent(o2, -2L);

		Object r1 = service.cache(o1);
		Object r2 = service.cache(o2);
		assertThat(r2).isNotSameAs(r1);

		service.evictAll(new Object());
		if (successExpected) {
			assertThat(cache.get(o1)).isNull();
			assertThat(cache.get(o2)).isNull();
		}
		else {
			assertThat(cache.get(o1)).isNotNull();
			assertThat(cache.get(o2)).isNotNull();
		}

		Object r3 = service.cache(o1);
		Object r4 = service.cache(o2);
		if (successExpected) {
			assertThat(r3).isNotSameAs(r1);
			assertThat(r4).isNotSameAs(r2);
		}
		else {
			assertThat(r3).isSameAs(r1);
			assertThat(r4).isSameAs(r2);
		}
	}

	protected void testEvictAllEarly(CacheableService<?> service) {
		Cache cache = this.cm.getCache("testCache");

		Object o1 = new Object();
		Object o2 = new Object();
		cache.putIfAbsent(o1, -1L);
		cache.putIfAbsent(o2, -2L);

		Object r1 = service.cache(o1);
		Object r2 = service.cache(o2);
		assertThat(r2).isNotSameAs(r1);

		try {
			service.evictAllEarly(new Object());
		}
		catch (Exception ex) {
			// expected
		}
		assertThat(cache.get(o1)).isNull();
		assertThat(cache.get(o2)).isNull();

		Object r3 = service.cache(o1);
		Object r4 = service.cache(o2);
		assertThat(r3).isNotSameAs(r1);
		assertThat(r4).isNotSameAs(r2);
	}

	protected void testConditionalExpression(CacheableService<?> service) {
		Object r1 = service.conditional(4);
		Object r2 = service.conditional(4);

		assertThat(r2).isNotSameAs(r1);

		Object r3 = service.conditional(3);
		Object r4 = service.conditional(3);

		assertThat(r4).isSameAs(r3);
	}

	protected void testConditionalExpressionSync(CacheableService<?> service) {
		Object r1 = service.conditionalSync(4);
		Object r2 = service.conditionalSync(4);

		assertThat(r2).isNotSameAs(r1);

		Object r3 = service.conditionalSync(3);
		Object r4 = service.conditionalSync(3);

		assertThat(r4).isSameAs(r3);
	}

	protected void testUnlessExpression(CacheableService<?> service) {
		Cache cache = this.cm.getCache("testCache");
		cache.clear();
		service.unless(10);
		service.unless(11);
		assertThat(cache.get(10).get()).isEqualTo(10L);
		assertThat(cache.get(11)).isNull();
	}

	protected void testKeyExpression(CacheableService<?> service) {
		Object r1 = service.key(5, 1);
		Object r2 = service.key(5, 2);

		assertThat(r2).isSameAs(r1);

		Object r3 = service.key(1, 5);
		Object r4 = service.key(2, 5);

		assertThat(r4).isNotSameAs(r3);
	}

	protected void testVarArgsKey(CacheableService<?> service) {
		Object r1 = service.varArgsKey(1, 2, 3);
		Object r2 = service.varArgsKey(1, 2, 3);

		assertThat(r2).isSameAs(r1);

		Object r3 = service.varArgsKey(1, 2, 3);
		Object r4 = service.varArgsKey(1, 2);

		assertThat(r4).isNotSameAs(r3);
	}

	protected void testNullValue(CacheableService<?> service) {
		Object key = new Object();
		assertThat(service.nullValue(key)).isNull();
		int nr = service.nullInvocations().intValue();
		assertThat(service.nullValue(key)).isNull();
		assertThat(service.nullInvocations().intValue()).isEqualTo(nr);
		assertThat(service.nullValue(new Object())).isNull();
		assertThat(service.nullInvocations().intValue()).isEqualTo(nr + 1);
	}

	protected void testMethodName(CacheableService<?> service, String keyName) {
		Object key = new Object();
		Object r1 = service.name(key);
		assertThat(service.name(key)).isSameAs(r1);
		Cache cache = this.cm.getCache("testCache");
		// assert the method name is used
		assertThat(cache.get(keyName)).isNotNull();
	}

	protected void testRootVars(CacheableService<?> service) {
		Object key = new Object();
		Object r1 = service.rootVars(key);
		assertThat(service.rootVars(key)).isSameAs(r1);
		Cache cache = this.cm.getCache("testCache");
		// assert the method name is used
		String expectedKey = "rootVarsrootVars" + AopProxyUtils.ultimateTargetClass(service) + service;
		assertThat(cache.get(expectedKey)).isNotNull();
	}

	protected void testCheckedThrowable(CacheableService<?> service) {
		String arg = UUID.randomUUID().toString();
		assertThatIOException().isThrownBy(() ->
				service.throwChecked(arg))
			.withMessage(arg);
	}

	protected void testUncheckedThrowable(CacheableService<?> service) {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				service.throwUnchecked(1L))
			.withMessage("1");
	}

	protected void testCheckedThrowableSync(CacheableService<?> service) {
		String arg = UUID.randomUUID().toString();
		assertThatIOException().isThrownBy(() ->
				service.throwCheckedSync(arg))
			.withMessage(arg);
	}

	protected void testUncheckedThrowableSync(CacheableService<?> service) {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				service.throwUncheckedSync(1L))
			.withMessage("1");
	}

	protected void testNullArg(CacheableService<?> service) {
		Object r1 = service.cache(null);
		assertThat(service.cache(null)).isSameAs(r1);
	}

	protected void testCacheUpdate(CacheableService<?> service) {
		Object o = new Object();
		Cache cache = this.cm.getCache("testCache");
		assertThat(cache.get(o)).isNull();
		Object r1 = service.update(o);
		assertThat(cache.get(o).get()).isSameAs(r1);

		o = new Object();
		assertThat(cache.get(o)).isNull();
		Object r2 = service.update(o);
		assertThat(cache.get(o).get()).isSameAs(r2);
	}

	protected void testConditionalCacheUpdate(CacheableService<?> service) {
		int one = 1;
		int three = 3;

		Cache cache = this.cm.getCache("testCache");
		assertThat(Integer.parseInt(service.conditionalUpdate(one).toString())).isEqualTo(one);
		assertThat(cache.get(one)).isNull();

		assertThat(Integer.parseInt(service.conditionalUpdate(three).toString())).isEqualTo(three);
		assertThat(Integer.parseInt(cache.get(three).get().toString())).isEqualTo(three);
	}

	protected void testMultiCache(CacheableService<?> service) {
		Object o1 = new Object();
		Object o2 = new Object();

		Cache primary = this.cm.getCache("primary");
		Cache secondary = this.cm.getCache("secondary");

		assertThat(primary.get(o1)).isNull();
		assertThat(secondary.get(o1)).isNull();
		Object r1 = service.multiCache(o1);
		assertThat(primary.get(o1).get()).isSameAs(r1);
		assertThat(secondary.get(o1).get()).isSameAs(r1);

		Object r2 = service.multiCache(o1);
		Object r3 = service.multiCache(o1);

		assertThat(r2).isSameAs(r1);
		assertThat(r3).isSameAs(r1);

		assertThat(primary.get(o2)).isNull();
		assertThat(secondary.get(o2)).isNull();
		Object r4 = service.multiCache(o2);
		assertThat(primary.get(o2).get()).isSameAs(r4);
		assertThat(secondary.get(o2).get()).isSameAs(r4);
	}

	protected void testMultiEvict(CacheableService<?> service) {
		Object o1 = new Object();
		Object o2 = o1 + "A";


		Object r1 = service.multiCache(o1);
		Object r2 = service.multiCache(o1);

		Cache primary = this.cm.getCache("primary");
		Cache secondary = this.cm.getCache("secondary");

		primary.put(o2, o2);
		assertThat(r2).isSameAs(r1);
		assertThat(primary.get(o1).get()).isSameAs(r1);
		assertThat(secondary.get(o1).get()).isSameAs(r1);

		service.multiEvict(o1);
		assertThat(primary.get(o1)).isNull();
		assertThat(secondary.get(o1)).isNull();
		assertThat(primary.get(o2)).isNull();

		Object r3 = service.multiCache(o1);
		Object r4 = service.multiCache(o1);
		assertThat(r3).isNotSameAs(r1);
		assertThat(r4).isSameAs(r3);

		assertThat(primary.get(o1).get()).isSameAs(r3);
		assertThat(secondary.get(o1).get()).isSameAs(r4);
	}

	protected void testMultiPut(CacheableService<?> service) {
		Object o = 1;

		Cache primary = this.cm.getCache("primary");
		Cache secondary = this.cm.getCache("secondary");

		assertThat(primary.get(o)).isNull();
		assertThat(secondary.get(o)).isNull();
		Object r1 = service.multiUpdate(o);
		assertThat(primary.get(o).get()).isSameAs(r1);
		assertThat(secondary.get(o).get()).isSameAs(r1);

		o = 2;
		assertThat(primary.get(o)).isNull();
		assertThat(secondary.get(o)).isNull();
		Object r2 = service.multiUpdate(o);
		assertThat(primary.get(o).get()).isSameAs(r2);
		assertThat(secondary.get(o).get()).isSameAs(r2);
	}

	protected void testPutRefersToResult(CacheableService<?> service) {
		Long id = Long.MIN_VALUE;
		TestEntity entity = new TestEntity();
		Cache primary = this.cm.getCache("primary");
		assertThat(primary.get(id)).isNull();
		assertThat(entity.getId()).isNull();
		service.putRefersToResult(entity);
		assertThat(primary.get(id).get()).isSameAs(entity);
	}

	protected void testPutRefersToResultWithUnless(CacheableService<?> service) {
		Long id = 42L;
		TestEntity entity = new TestEntity();
		entity.setId(id);
		Cache primary = this.cm.getCache("primary");
		assertThat(primary.get(id)).isNull();
		assertThat(service.putEvaluatesUnlessBeforeKey(entity)).isNotNull();
		assertThat(primary.get(id).get()).isSameAs(entity);
	}

	protected void testPutEvaluatesUnlessBeforeKey(CacheableService<?> service) {
		Long id = Long.MIN_VALUE; // return null
		TestEntity entity = new TestEntity();
		entity.setId(id);
		Cache primary = this.cm.getCache("primary");
		assertThat(primary.get(id)).isNull();
		assertThat(service.putEvaluatesUnlessBeforeKey(entity)).isNull();
		assertThat(primary.get(id)).isNull();
	}

	protected void testMultiCacheAndEvict(CacheableService<?> service) {
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

	protected void testMultiConditionalCacheAndEvict(CacheableService<?> service) {
		Cache primary = this.cm.getCache("primary");
		Cache secondary = this.cm.getCache("secondary");
		Object key = 1;

		secondary.put(key, key);

		assertThat(primary.get(key)).isNull();
		assertThat(secondary.get(key).get()).isSameAs(key);

		Object r1 = service.multiConditionalCacheAndEvict(key);
		Object r3 = service.multiConditionalCacheAndEvict(key);

		assertThat(r1).isNotEqualTo(r3);
		assertThat(primary.get(key)).isNull();

		Object key2 = 3;
		Object r2 = service.multiConditionalCacheAndEvict(key2);
		assertThat(service.multiConditionalCacheAndEvict(key2)).isSameAs(r2);

		// assert the method name is used
		assertThat(primary.get(key2).get()).isSameAs(r2);
		assertThat(secondary.get(key2)).isNull();
	}

	@Test
	protected void testCacheable() {
		testCacheable(this.cs);
	}

	@Test
	protected void testCacheableNull() {
		testCacheableNull(this.cs);
	}

	@Test
	protected void testCacheableSync() {
		testCacheableSync(this.cs);
	}

	@Test
	protected void testCacheableSyncNull() {
		testCacheableSyncNull(this.cs);
	}

	@Test
	protected void testEvict() {
		testEvict(this.cs, true);
	}

	@Test
	protected void testEvictEarly() {
		testEvictEarly(this.cs);
	}

	@Test
	protected void testEvictWithException() {
		testEvictException(this.cs);
	}

	@Test
	protected void testEvictAll() {
		testEvictAll(this.cs, true);
	}

	@Test
	protected void testEvictAllEarly() {
		testEvictAllEarly(this.cs);
	}

	@Test
	protected void testEvictWithKey() {
		testEvictWithKey(this.cs);
	}

	@Test
	protected void testEvictWithKeyEarly() {
		testEvictWithKeyEarly(this.cs);
	}

	@Test
	protected void testConditionalExpression() {
		testConditionalExpression(this.cs);
	}

	@Test
	protected void testConditionalExpressionSync() {
		testConditionalExpressionSync(this.cs);
	}

	@Test
	protected void testUnlessExpression() {
		testUnlessExpression(this.cs);
	}

	@Test
	protected void testClassCacheUnlessExpression() {
		testUnlessExpression(this.cs);
	}

	@Test
	protected void testKeyExpression() {
		testKeyExpression(this.cs);
	}

	@Test
	protected void testVarArgsKey() {
		testVarArgsKey(this.cs);
	}

	@Test
	protected void testClassCacheCacheable() {
		testCacheable(this.ccs);
	}

	@Test
	protected void testClassCacheEvict() {
		testEvict(this.ccs, true);
	}

	@Test
	protected void testClassEvictEarly() {
		testEvictEarly(this.ccs);
	}

	@Test
	protected void testClassEvictAll() {
		testEvictAll(this.ccs, true);
	}

	@Test
	protected void testClassEvictWithException() {
		testEvictException(this.ccs);
	}

	@Test
	protected void testClassCacheEvictWithWKey() {
		testEvictWithKey(this.ccs);
	}

	@Test
	protected void testClassEvictWithKeyEarly() {
		testEvictWithKeyEarly(this.ccs);
	}

	@Test
	protected void testNullValue() {
		testNullValue(this.cs);
	}

	@Test
	protected void testClassNullValue() {
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
	protected void testMethodName() {
		testMethodName(this.cs, "name");
	}

	@Test
	protected void testClassMethodName() {
		testMethodName(this.ccs, "nametestCache");
	}

	@Test
	protected void testRootVars() {
		testRootVars(this.cs);
	}

	@Test
	protected void testClassRootVars() {
		testRootVars(this.ccs);
	}

	@Test
	protected void testCustomKeyGenerator() {
		Object param = new Object();
		Object r1 = this.cs.customKeyGenerator(param);
		assertThat(this.cs.customKeyGenerator(param)).isSameAs(r1);
		Cache cache = this.cm.getCache("testCache");
		// Checks that the custom keyGenerator was used
		Object expectedKey = SomeCustomKeyGenerator.generateKey("customKeyGenerator", param);
		assertThat(cache.get(expectedKey)).isNotNull();
	}

	@Test
	protected void testUnknownCustomKeyGenerator() {
		Object param = new Object();
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				this.cs.unknownCustomKeyGenerator(param));
	}

	@Test
	protected void testCustomCacheManager() {
		CacheManager customCm = this.ctx.getBean("customCacheManager", CacheManager.class);
		Object key = new Object();
		Object r1 = this.cs.customCacheManager(key);
		assertThat(this.cs.customCacheManager(key)).isSameAs(r1);

		Cache cache = customCm.getCache("testCache");
		assertThat(cache.get(key)).isNotNull();
	}

	@Test
	protected void testUnknownCustomCacheManager() {
		Object param = new Object();
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				this.cs.unknownCustomCacheManager(param));
	}

	@Test
	protected void testNullArg() {
		testNullArg(this.cs);
	}

	@Test
	protected void testClassNullArg() {
		testNullArg(this.ccs);
	}

	@Test
	protected void testCheckedException() {
		testCheckedThrowable(this.cs);
	}

	@Test
	protected void testClassCheckedException() {
		testCheckedThrowable(this.ccs);
	}

	@Test
	protected void testCheckedExceptionSync() {
		testCheckedThrowableSync(this.cs);
	}

	@Test
	protected void testClassCheckedExceptionSync() {
		testCheckedThrowableSync(this.ccs);
	}

	@Test
	protected void testUncheckedException() {
		testUncheckedThrowable(this.cs);
	}

	@Test
	protected void testClassUncheckedException() {
		testUncheckedThrowable(this.ccs);
	}

	@Test
	protected void testUncheckedExceptionSync() {
		testUncheckedThrowableSync(this.cs);
	}

	@Test
	protected void testClassUncheckedExceptionSync() {
		testUncheckedThrowableSync(this.ccs);
	}

	@Test
	protected void testUpdate() {
		testCacheUpdate(this.cs);
	}

	@Test
	protected void testClassUpdate() {
		testCacheUpdate(this.ccs);
	}

	@Test
	protected void testConditionalUpdate() {
		testConditionalCacheUpdate(this.cs);
	}

	@Test
	protected void testClassConditionalUpdate() {
		testConditionalCacheUpdate(this.ccs);
	}

	@Test
	protected void testMultiCache() {
		testMultiCache(this.cs);
	}

	@Test
	protected void testClassMultiCache() {
		testMultiCache(this.ccs);
	}

	@Test
	protected void testMultiEvict() {
		testMultiEvict(this.cs);
	}

	@Test
	protected void testClassMultiEvict() {
		testMultiEvict(this.ccs);
	}

	@Test
	protected void testMultiPut() {
		testMultiPut(this.cs);
	}

	@Test
	protected void testClassMultiPut() {
		testMultiPut(this.ccs);
	}

	@Test
	protected void testPutRefersToResult() {
		testPutRefersToResult(this.cs);
	}

	@Test
	protected void testPutRefersToResultWithUnless() {
		testPutRefersToResultWithUnless(this.cs);
	}

	@Test
	protected void testPutEvaluatesUnlessBeforeKey() {
		testPutEvaluatesUnlessBeforeKey(this.cs);
	}

	@Test
	protected void testClassPutRefersToResult() {
		testPutRefersToResult(this.ccs);
	}

	@Test
	protected void testClassPutRefersToResultWithUnless(){
		testPutRefersToResultWithUnless(this.ccs);
	}

	@Test
	protected void testClassPutEvaluatesUnlessBeforeKey(){
		testPutEvaluatesUnlessBeforeKey(this.ccs);
	}

	@Test
	protected void testMultiCacheAndEvict() {
		testMultiCacheAndEvict(this.cs);
	}

	@Test
	protected void testClassMultiCacheAndEvict() {
		testMultiCacheAndEvict(this.ccs);
	}

	@Test
	protected void testMultiConditionalCacheAndEvict() {
		testMultiConditionalCacheAndEvict(this.cs);
	}

	@Test
	protected void testClassMultiConditionalCacheAndEvict() {
		testMultiConditionalCacheAndEvict(this.ccs);
	}

}
