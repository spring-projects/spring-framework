/*
 * Copyright 2002-2016 the original author or authors.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.cache.interceptor.CacheEvictOperation;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CacheableOperation;
import org.springframework.core.annotation.AliasFor;
import org.springframework.util.ReflectionUtils;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
public class AnnotationCacheOperationSourceTests {

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	private AnnotationCacheOperationSource source = new AnnotationCacheOperationSource();


	private Collection<CacheOperation> getOps(Class<?> target, String name, int expectedNumberOfOperations) {
		Collection<CacheOperation> result = getOps(target, name);
		assertEquals("Wrong number of operation(s) for '" + name + "'", expectedNumberOfOperations, result.size());
		return result;
	}

	private Collection<CacheOperation> getOps(Class<?> target, String name) {
		Method method = ReflectionUtils.findMethod(target, name);
		return source.getCacheOperations(method, target);
	}

	@Test
	public void singularAnnotation() throws Exception {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "singular", 1);
		assertTrue(ops.iterator().next() instanceof CacheableOperation);
	}

	@Test
	public void multipleAnnotation() throws Exception {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "multiple", 2);
		Iterator<CacheOperation> it = ops.iterator();
		assertTrue(it.next() instanceof CacheableOperation);
		assertTrue(it.next() instanceof CacheEvictOperation);
	}

	@Test
	public void caching() throws Exception {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "caching", 2);
		Iterator<CacheOperation> it = ops.iterator();
		assertTrue(it.next() instanceof CacheableOperation);
		assertTrue(it.next() instanceof CacheEvictOperation);
	}

	@Test
	public void singularStereotype() throws Exception {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "singleStereotype", 1);
		assertTrue(ops.iterator().next() instanceof CacheEvictOperation);
	}

	@Test
	public void multipleStereotypes() throws Exception {
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
	public void singleComposedAnnotation() throws Exception {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "singleComposed", 2);
		Iterator<CacheOperation> it = ops.iterator();

		CacheOperation cacheOperation = it.next();
		assertThat(cacheOperation, instanceOf(CacheableOperation.class));
		assertThat(cacheOperation.getCacheNames(), equalTo(Collections.singleton("directly declared")));
		assertThat(cacheOperation.getKey(), equalTo(""));

		cacheOperation = it.next();
		assertThat(cacheOperation, instanceOf(CacheableOperation.class));
		assertThat(cacheOperation.getCacheNames(), equalTo(Collections.singleton("composedCache")));
		assertThat(cacheOperation.getKey(), equalTo("composedKey"));
	}

	@Test
	public void multipleComposedAnnotations() throws Exception {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "multipleComposed", 4);
		Iterator<CacheOperation> it = ops.iterator();

		CacheOperation cacheOperation = it.next();
		assertThat(cacheOperation, instanceOf(CacheableOperation.class));
		assertThat(cacheOperation.getCacheNames(), equalTo(Collections.singleton("directly declared")));
		assertThat(cacheOperation.getKey(), equalTo(""));

		cacheOperation = it.next();
		assertThat(cacheOperation, instanceOf(CacheableOperation.class));
		assertThat(cacheOperation.getCacheNames(), equalTo(Collections.singleton("composedCache")));
		assertThat(cacheOperation.getKey(), equalTo("composedKey"));

		cacheOperation = it.next();
		assertThat(cacheOperation, instanceOf(CacheableOperation.class));
		assertThat(cacheOperation.getCacheNames(), equalTo(Collections.singleton("foo")));
		assertThat(cacheOperation.getKey(), equalTo(""));

		cacheOperation = it.next();
		assertThat(cacheOperation, instanceOf(CacheEvictOperation.class));
		assertThat(cacheOperation.getCacheNames(), equalTo(Collections.singleton("composedCacheEvict")));
		assertThat(cacheOperation.getKey(), equalTo("composedEvictionKey"));
	}

	@Test
	public void customKeyGenerator() {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "customKeyGenerator", 1);
		CacheOperation cacheOperation = ops.iterator().next();
		assertEquals("Custom key generator not set", "custom", cacheOperation.getKeyGenerator());
	}

	@Test
	public void customKeyGeneratorInherited() {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "customKeyGeneratorInherited", 1);
		CacheOperation cacheOperation = ops.iterator().next();
		assertEquals("Custom key generator not set", "custom", cacheOperation.getKeyGenerator());
	}

	@Test
	public void keyAndKeyGeneratorCannotBeSetTogether() {
		exception.expect(IllegalStateException.class);
		getOps(AnnotatedClass.class, "invalidKeyAndKeyGeneratorSet");
	}

	@Test
	public void customCacheManager() {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "customCacheManager", 1);
		CacheOperation cacheOperation = ops.iterator().next();
		assertEquals("Custom cache manager not set", "custom", cacheOperation.getCacheManager());
	}

	@Test
	public void customCacheManagerInherited() {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "customCacheManagerInherited", 1);
		CacheOperation cacheOperation = ops.iterator().next();
		assertEquals("Custom cache manager not set", "custom", cacheOperation.getCacheManager());
	}

	@Test
	public void customCacheResolver() {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "customCacheResolver", 1);
		CacheOperation cacheOperation = ops.iterator().next();
		assertEquals("Custom cache resolver not set", "custom", cacheOperation.getCacheResolver());
	}

	@Test
	public void customCacheResolverInherited() {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "customCacheResolverInherited", 1);
		CacheOperation cacheOperation = ops.iterator().next();
		assertEquals("Custom cache resolver not set", "custom", cacheOperation.getCacheResolver());
	}

	@Test
	public void cacheResolverAndCacheManagerCannotBeSetTogether() {
		exception.expect(IllegalStateException.class);
		getOps(AnnotatedClass.class, "invalidCacheResolverAndCacheManagerSet");
	}

	@Test
	public void fullClassLevelWithCustomCacheName() {
		Collection<CacheOperation> ops = getOps(AnnotatedClassWithFullDefault.class, "methodLevelCacheName", 1);
		CacheOperation cacheOperation = ops.iterator().next();
		assertSharedConfig(cacheOperation, "classKeyGenerator", "", "classCacheResolver", "custom");
	}

	@Test
	public void fullClassLevelWithCustomKeyManager() {
		Collection<CacheOperation> ops = getOps(AnnotatedClassWithFullDefault.class, "methodLevelKeyGenerator", 1);
		CacheOperation cacheOperation = ops.iterator().next();
		assertSharedConfig(cacheOperation, "custom", "", "classCacheResolver" , "classCacheName");
	}

	@Test
	public void fullClassLevelWithCustomCacheManager() {
		Collection<CacheOperation> ops = getOps(AnnotatedClassWithFullDefault.class, "methodLevelCacheManager", 1);
		CacheOperation cacheOperation = ops.iterator().next();
		assertSharedConfig(cacheOperation, "classKeyGenerator", "custom", "", "classCacheName");
	}

	@Test
	public void fullClassLevelWithCustomCacheResolver() {
		Collection<CacheOperation> ops = getOps(AnnotatedClassWithFullDefault.class, "methodLevelCacheResolver", 1);
		CacheOperation cacheOperation = ops.iterator().next();
		assertSharedConfig(cacheOperation, "classKeyGenerator", "", "custom" , "classCacheName");
	}

	@Test
	public void validateNoCacheIsValid() {
		// Valid as a CacheResolver might return the cache names to use with other info
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "noCacheNameSpecified");
		CacheOperation cacheOperation = ops.iterator().next();
		assertNotNull("cache names set must not be null", cacheOperation.getCacheNames());
		assertEquals("no cache names specified", 0, cacheOperation.getCacheNames().size());
	}

	@Test
	public void customClassLevelWithCustomCacheName() {
		Collection<CacheOperation> ops = getOps(AnnotatedClassWithCustomDefault.class, "methodLevelCacheName", 1);
		CacheOperation cacheOperation = ops.iterator().next();
		assertSharedConfig(cacheOperation, "classKeyGenerator", "", "classCacheResolver", "custom");
	}

	@Test
	public void severalCacheConfigUseClosest() {
		Collection<CacheOperation> ops = getOps(MultipleCacheConfig.class, "multipleCacheConfig");
		CacheOperation cacheOperation = ops.iterator().next();
		assertSharedConfig(cacheOperation, "", "", "", "myCache");
	}

	@Test
	public void partialClassLevelWithCustomCacheManager() {
		Collection<CacheOperation> ops = getOps(AnnotatedClassWithSomeDefault.class, "methodLevelCacheManager", 1);
		CacheOperation cacheOperation = ops.iterator().next();
		assertSharedConfig(cacheOperation, "classKeyGenerator", "custom", "", "classCacheName");
	}

	@Test
	public void partialClassLevelWithCustomCacheResolver() {
		Collection<CacheOperation> ops = getOps(AnnotatedClassWithSomeDefault.class, "methodLevelCacheResolver", 1);
		CacheOperation cacheOperation = ops.iterator().next();
		assertSharedConfig(cacheOperation, "classKeyGenerator", "", "custom", "classCacheName");
	}

	@Test
	public void partialClassLevelWithNoCustomization() {
		Collection<CacheOperation> ops = getOps(AnnotatedClassWithSomeDefault.class, "noCustomization", 1);
		CacheOperation cacheOperation = ops.iterator().next();
		assertSharedConfig(cacheOperation, "classKeyGenerator", "classCacheManager", "", "classCacheName");
	}

	private void assertSharedConfig(CacheOperation actual, String keyGenerator, String cacheManager,
									String cacheResolver, String... cacheNames) {
		assertEquals("Wrong key manager",  keyGenerator, actual.getKeyGenerator());
		assertEquals("Wrong cache manager", cacheManager, actual.getCacheManager());
		assertEquals("Wrong cache resolver", cacheResolver, actual.getCacheResolver());
		assertEquals("Wrong number of cache names", cacheNames.length, actual.getCacheNames().size());
		Arrays.stream(cacheNames).forEach(
			cacheName -> assertTrue("Cache '" + cacheName + "' not found in " + actual.getCacheNames(),
				actual.getCacheNames().contains(cacheName)));
	}

	private static class AnnotatedClass {

		@Cacheable("test")
		public void singular() {
		}

		@CacheEvict("test")
		@Cacheable("test")
		public void multiple() {
		}

		@Caching(cacheable = @Cacheable("test"), evict = @CacheEvict("test"))
		public void caching() {
		}

		@Cacheable(cacheNames = "test", keyGenerator = "custom")
		public void customKeyGenerator() {
		}

		@Cacheable(cacheNames = "test", cacheManager = "custom")
		public void customCacheManager() {
		}

		@Cacheable(cacheNames = "test", cacheResolver = "custom")
		public void customCacheResolver() {
		}

		@EvictFoo
		public void singleStereotype() {
		}

		@EvictFoo
		@CacheableFoo
		@EvictBar
		public void multipleStereotype() {
		}

		@Cacheable("directly declared")
		@ComposedCacheable(cacheNames = "composedCache", key = "composedKey")
		public void singleComposed() {
		}

		@Cacheable("directly declared")
		@ComposedCacheable(cacheNames = "composedCache", key = "composedKey")
		@CacheableFoo
		@ComposedCacheEvict(cacheNames = "composedCacheEvict", key = "composedEvictionKey")
		public void multipleComposed() {
		}

		@Caching(cacheable = { @Cacheable(cacheNames = "test", key = "a"), @Cacheable(cacheNames = "test", key = "b") })
		public void multipleCaching() {
		}

		@CacheableFooCustomKeyGenerator
		public void customKeyGeneratorInherited() {
		}

		@Cacheable(cacheNames = "test", key = "#root.methodName", keyGenerator = "custom")
		public void invalidKeyAndKeyGeneratorSet() {
		}

		@CacheableFooCustomCacheManager
		public void customCacheManagerInherited() {
		}

		@CacheableFooCustomCacheResolver
		public void customCacheResolverInherited() {
		}

		@Cacheable(cacheNames = "test", cacheManager = "custom", cacheResolver = "custom")
		public void invalidCacheResolverAndCacheManagerSet() {
		}

		@Cacheable // cache name can be inherited from CacheConfig. There's none here
		public void noCacheNameSpecified() {
		}
	}

	@CacheConfig(cacheNames = "classCacheName",
			keyGenerator = "classKeyGenerator",
			cacheManager = "classCacheManager", cacheResolver = "classCacheResolver")
	private static class AnnotatedClassWithFullDefault {

		@Cacheable("custom")
		public void methodLevelCacheName() {
		}

		@Cacheable(keyGenerator = "custom")
		public void methodLevelKeyGenerator() {
		}

		@Cacheable(cacheManager = "custom")
		public void methodLevelCacheManager() {
		}

		@Cacheable(cacheResolver = "custom")
		public void methodLevelCacheResolver() {
		}
	}

	@CacheConfigFoo
	private static class AnnotatedClassWithCustomDefault {

		@Cacheable("custom")
		public void methodLevelCacheName() {
		}
	}

	@CacheConfig(cacheNames = "classCacheName",
			keyGenerator = "classKeyGenerator",
			cacheManager = "classCacheManager")
	private static class AnnotatedClassWithSomeDefault {

		@Cacheable(cacheManager = "custom")
		public void methodLevelCacheManager() {
		}

		@Cacheable(cacheResolver = "custom")
		public void methodLevelCacheResolver() {
		}

		@Cacheable
		public void noCustomization() {
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
	@Cacheable(cacheNames = "foo", keyGenerator = "custom")
	public @interface CacheableFooCustomKeyGenerator {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@Cacheable(cacheNames = "foo", cacheManager = "custom")
	public @interface CacheableFooCustomCacheManager {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@Cacheable(cacheNames = "foo", cacheResolver = "custom")
	public @interface CacheableFooCustomCacheResolver {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@CacheEvict("foo")
	public @interface EvictFoo {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@CacheEvict("bar")
	public @interface EvictBar {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@CacheConfig(keyGenerator = "classKeyGenerator", cacheManager = "classCacheManager", cacheResolver = "classCacheResolver")
	public @interface CacheConfigFoo {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Cacheable(cacheNames = "shadowed cache name", key = "shadowed key")
	@interface ComposedCacheable {

		@AliasFor(annotation = Cacheable.class)
		String[] value() default {};

		@AliasFor(annotation = Cacheable.class)
		String[] cacheNames() default {};

		@AliasFor(annotation = Cacheable.class)
		String key() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD, ElementType.TYPE })
	@CacheEvict(cacheNames = "shadowed cache name", key = "shadowed key")
	@interface ComposedCacheEvict {

		@AliasFor(annotation = CacheEvict.class)
		String[] value() default {};

		@AliasFor(annotation = CacheEvict.class)
		String[] cacheNames() default {};

		@AliasFor(annotation = CacheEvict.class)
		String key() default "";
	}

}