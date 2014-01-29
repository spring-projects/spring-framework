/*
 * Copyright 2011-2014 the original author or authors.
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

package org.springframework.cache.annotation;

import static org.junit.Assert.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.cache.interceptor.CacheEvictOperation;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CacheableOperation;
import org.springframework.util.ReflectionUtils;

/**
 * @author Costin Leau
 * @author Stephane Nicoll
 */
public class AnnotationCacheOperationSourceTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private AnnotationCacheOperationSource source = new AnnotationCacheOperationSource();

	private Collection<CacheOperation> getOps(Class<?> target, String name,
											  int expectedNumberOfOperations) {
		Collection<CacheOperation> result = getOps(target, name);
		assertEquals("Wrong number of operation(s) for '"+name+"'",
				expectedNumberOfOperations, result.size());
		return result;
	}

	private Collection<CacheOperation> getOps(Class<?> target, String name) {
		Method method = ReflectionUtils.findMethod(target, name);
		return source.getCacheOperations(method, target);
	}

	@Test
	public void testSingularAnnotation() throws Exception {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "singular", 1);
		assertTrue(ops.iterator().next() instanceof CacheableOperation);
	}

	@Test
	public void testMultipleAnnotation() throws Exception {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "multiple", 2);
		Iterator<CacheOperation> it = ops.iterator();
		assertTrue(it.next() instanceof CacheableOperation);
		assertTrue(it.next() instanceof CacheEvictOperation);
	}

	@Test
	public void testCaching() throws Exception {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "caching", 2);
		Iterator<CacheOperation> it = ops.iterator();
		assertTrue(it.next() instanceof CacheableOperation);
		assertTrue(it.next() instanceof CacheEvictOperation);
	}

	@Test
	public void testSingularStereotype() throws Exception {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "singleStereotype", 1);
		assertTrue(ops.iterator().next() instanceof CacheEvictOperation);
	}

	@Test
	public void testMultipleStereotypes() throws Exception {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "multipleStereotype", 3);
		Iterator<CacheOperation> it = ops.iterator();
		assertTrue(it.next() instanceof CacheableOperation);
		CacheOperation next = it.next();
		assertTrue(next instanceof CacheEvictOperation);
		assertTrue(next.getCacheNames().contains("foo"));
		next = it.next();
		assertTrue(next instanceof CacheEvictOperation);
		assertTrue(next.getCacheNames().contains("bar"));
	}

	@Test
	public void testCustomKeyGenerator() {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "customKeyGenerator", 1);
		CacheOperation cacheOperation = ops.iterator().next();
		assertEquals("Custom key generator not set", "custom", cacheOperation.getKeyGenerator());
	}

	@Test
	public void testCustomKeyGeneratorInherited() {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "customKeyGeneratorInherited", 1);
		CacheOperation cacheOperation = ops.iterator().next();
		assertEquals("Custom key generator not set", "custom", cacheOperation.getKeyGenerator());
	}

	@Test
	public void testKeyAndKeyGeneratorCannotBeSetTogether() {
		try {
			getOps(AnnotatedClass.class, "invalidKeyAndKeyGeneratorSet");
			fail("Should have failed to parse @Cacheable annotation");
		} catch (IllegalStateException e) {
			// expected
		}
	}

	@Test
	public void testCustomCacheManager() {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "customCacheManager", 1);
		CacheOperation cacheOperation = ops.iterator().next();
		assertEquals("Custom cache manager not set", "custom", cacheOperation.getCacheManager());
	}

	@Test
	public void testCustomCacheManagerInherited() {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "customCacheManagerInherited", 1);
		CacheOperation cacheOperation = ops.iterator().next();
		assertEquals("Custom cache manager not set", "custom", cacheOperation.getCacheManager());
	}

	@Test
	public void fullClassLevelWithCustomKeyManager() {
		Collection<CacheOperation> ops = getOps(AnnotatedClassWithFullDefault.class, "methodLevelKeyGenerator", 1);
		CacheOperation cacheOperation = ops.iterator().next();
		assertSharedConfig(cacheOperation, "classCacheManager", "custom", "classCacheName");
	}

	@Test
	public void fullClassLevelWithCustomCacheManager() {
		Collection<CacheOperation> ops = getOps(AnnotatedClassWithFullDefault.class, "methodLevelCacheManager", 1);
		CacheOperation cacheOperation = ops.iterator().next();
		assertSharedConfig(cacheOperation, "custom", "classKeyGenerator", "classCacheName");
	}

	@Test
	public void fullClassLevelWithCustomCacheName() {
		Collection<CacheOperation> ops = getOps(AnnotatedClassWithFullDefault.class, "methodLevelCacheName", 1);
		CacheOperation cacheOperation = ops.iterator().next();
		assertSharedConfig(cacheOperation, "classCacheManager", "classKeyGenerator", "custom");
	}

	@Test
	public void validateAtLeastOneCacheNameMustBeSet() {
		thrown.expect(IllegalStateException.class);
		getOps(AnnotatedClass.class, "noCacheNameSpecified");
	}

	@Test
	public void customClassLevelWithCustomCacheName() {
		Collection<CacheOperation> ops = getOps(AnnotatedClassWithCustomDefault.class, "methodLevelCacheName", 1);
		CacheOperation cacheOperation = ops.iterator().next();
		assertSharedConfig(cacheOperation, "classCacheManager", "classKeyGenerator", "custom");
	}

	@Test
	public void severalCacheConfigUseClosest() {
		Collection<CacheOperation> ops = getOps(MultipleCacheConfig.class, "multipleCacheConfig");
		CacheOperation cacheOperation = ops.iterator().next();
		assertSharedConfig(cacheOperation, "", "", "myCache");
	}

	private void assertSharedConfig(CacheOperation actual, String cacheManager,
									String keyGenerator, String... cacheNames) {
		assertEquals("Wrong cache manager", cacheManager, actual.getCacheManager());
		assertEquals("Wrong key manager",  keyGenerator, actual.getKeyGenerator());
		for (String cacheName : cacheNames) {
			assertTrue("Cache '"+cacheName+"' not found (got "+actual.getCacheNames(),
					actual.getCacheNames().contains(cacheName));
		}
		assertEquals("Wrong number of cache name(s)", cacheNames.length, actual.getCacheNames().size());
	}

	private static class AnnotatedClass {
		@Cacheable("test")
		public void singular() {
		}

		@CacheEvict("test")
		@Cacheable("test")
		public void multiple() {
		}

		@Caching(cacheable = {@Cacheable("test")}, evict = {@CacheEvict("test")})
		public void caching() {
		}

		@Cacheable(value = "test", keyGenerator = "custom")
		public void customKeyGenerator() {
		}

		@Cacheable(value = "test", cacheManager = "custom")
		public void customCacheManager() {
		}

		@EvictFoo
		public void singleStereotype() {
		}

		@EvictFoo
		@CacheableFoo
		@EvictBar
		public void multipleStereotype() {
		}

		@Caching(cacheable = {@Cacheable(value = "test", key = "a"), @Cacheable(value = "test", key = "b")})
		public void multipleCaching() {
		}

		@CacheableFooCustomKeyGenerator
		public void customKeyGeneratorInherited() {
		}

		@Cacheable(value = "test", key = "#root.methodName", keyGenerator = "custom")
		public void invalidKeyAndKeyGeneratorSet() {
		}

		@CacheableFooCustomCacheManager
		public void customCacheManagerInherited() {
		}

		@Cacheable // cache name can be inherited from CacheConfig. There's none here
		public void noCacheNameSpecified() {
		}
	}

	@CacheConfig(cacheNames = "classCacheName",
			cacheManager = "classCacheManager", keyGenerator = "classKeyGenerator")
	private static class AnnotatedClassWithFullDefault {

		@Cacheable(keyGenerator = "custom")
		public void methodLevelKeyGenerator() {
		}

		@Cacheable(cacheManager = "custom")
		public void methodLevelCacheManager() {
		}

		@Cacheable("custom")
		public void methodLevelCacheName() {
		}
	}

	@CacheConfigFoo
	private static class AnnotatedClassWithCustomDefault {

		@Cacheable("custom")
		public void methodLevelCacheName() {
		}
	}

	@CacheConfigFoo
	@CacheConfig(cacheNames = "myCache") // multiple sources
	private static class MultipleCacheConfig {

		@Cacheable
		public void multipleCacheConfig() {
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@Cacheable("foo")
	public @interface CacheableFoo {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@Cacheable(value = "foo", keyGenerator = "custom")
	public @interface CacheableFooCustomKeyGenerator {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@Cacheable(value = "foo", cacheManager = "custom")
	public @interface CacheableFooCustomCacheManager {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@CacheEvict(value = "foo")
	public @interface EvictFoo {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@CacheEvict(value = "bar")
	public @interface EvictBar {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@CacheConfig(cacheManager = "classCacheManager", keyGenerator = "classKeyGenerator")
	public @interface CacheConfigFoo {
	}
}