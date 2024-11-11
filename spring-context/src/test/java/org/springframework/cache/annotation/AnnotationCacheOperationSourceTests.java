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

package org.springframework.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.cache.interceptor.CacheEvictOperation;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CacheableOperation;
import org.springframework.core.annotation.AliasFor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
class AnnotationCacheOperationSourceTests {

	private final AnnotationCacheOperationSource source = new AnnotationCacheOperationSource();


	@Test
	void singularAnnotation() {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "singular", 1);
		assertThat(ops).singleElement().satisfies(cacheOperation(CacheableOperation.class, "test"));
	}

	@Test
	void multipleAnnotation() {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "multiple", 2);
		assertThat(ops).satisfiesExactly(cacheOperation(CacheableOperation.class),
				cacheOperation(CacheEvictOperation.class));
	}

	@Test
	void caching() {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "caching", 2);
		assertThat(ops).satisfiesExactly(cacheOperation(CacheableOperation.class),
				cacheOperation(CacheEvictOperation.class));
	}

	@Test
	void emptyCaching() {
		getOps(AnnotatedClass.class, "emptyCaching", 0);
	}

	@Test
	void singularStereotype() {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "singleStereotype", 1);
		assertThat(ops).satisfiesExactly(cacheOperation(CacheEvictOperation.class));
	}

	@Test
	void multipleStereotypes() {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "multipleStereotype", 3);
		assertThat(ops).satisfiesExactly(cacheOperation(CacheableOperation.class),
				cacheOperation(CacheEvictOperation.class, "foo"),
				cacheOperation(CacheEvictOperation.class, "bar")
		);
	}

	@Test
	void singleComposedAnnotation() {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "singleComposed", 2);
		assertThat(ops).satisfiesExactly(
				zero -> {
					assertThat(zero).satisfies(cacheOperation(CacheOperation.class, "directly declared"));
					assertThat(zero.getKey()).isEmpty();
				},
				first -> {
					assertThat(first).satisfies(cacheOperation(CacheOperation.class, "composedCache"));
					assertThat(first.getKey()).isEqualTo("composedKey");
				}
		);
	}

	@Test
	void multipleComposedAnnotations() {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "multipleComposed", 4);
		assertThat(ops).satisfiesExactly(
				zero -> {
					assertThat(zero).satisfies(cacheOperation(CacheOperation.class, "directly declared"));
					assertThat(zero.getKey()).isEmpty();
				},
				first -> {
					assertThat(first).satisfies(cacheOperation(CacheOperation.class, "composedCache"));
					assertThat(first.getKey()).isEqualTo("composedKey");
				},
				two -> {
					assertThat(two).satisfies(cacheOperation(CacheOperation.class, "foo"));
					assertThat(two.getKey()).isEmpty();
				},
				three -> {
					assertThat(three).satisfies(cacheOperation(CacheEvictOperation.class, "composedCacheEvict"));
					assertThat(three.getKey()).isEqualTo("composedEvictionKey");
				}
		);
	}

	@Test
	void customKeyGenerator() {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "customKeyGenerator", 1);
		assertThat(ops).singleElement().satisfies(cacheOperation ->
				assertThat(cacheOperation.getKeyGenerator()).isEqualTo("custom"));

	}

	@Test
	void customKeyGeneratorInherited() {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "customKeyGeneratorInherited", 1);
		assertThat(ops).singleElement().satisfies(cacheOperation ->
				assertThat(cacheOperation.getKeyGenerator()).isEqualTo("custom"));
	}

	@Test
	void keyAndKeyGeneratorCannotBeSetTogether() {
		assertThatIllegalStateException().isThrownBy(() ->
				getOps(AnnotatedClass.class, "invalidKeyAndKeyGeneratorSet"));
	}

	@Test
	void customCacheManager() {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "customCacheManager", 1);
		assertThat(ops).singleElement().satisfies(cacheOperation ->
				assertThat(cacheOperation.getCacheManager()).isEqualTo("custom"));
	}

	@Test
	void customCacheManagerInherited() {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "customCacheManagerInherited", 1);
		assertThat(ops).singleElement().satisfies(cacheOperation ->
				assertThat(cacheOperation.getCacheManager()).isEqualTo("custom"));
	}

	@Test
	void customCacheResolver() {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "customCacheResolver", 1);
		assertThat(ops).singleElement().satisfies(cacheOperation ->
				assertThat(cacheOperation.getCacheResolver()).isEqualTo("custom"));
	}

	@Test
	void customCacheResolverInherited() {
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "customCacheResolverInherited", 1);
		assertThat(ops).singleElement().satisfies(cacheOperation ->
				assertThat(cacheOperation.getCacheResolver()).isEqualTo("custom"));
	}

	@Test
	void cacheResolverAndCacheManagerCannotBeSetTogether() {
		assertThatIllegalStateException().isThrownBy(() ->
				getOps(AnnotatedClass.class, "invalidCacheResolverAndCacheManagerSet"));
	}

	@Test
	void fullClassLevelWithCustomCacheName() {
		Collection<CacheOperation> ops = getOps(AnnotatedClassWithFullDefault.class, "methodLevelCacheName", 1);
		assertThat(ops).singleElement().satisfies(hasSharedConfig(
				"classKeyGenerator", "", "classCacheResolver", "custom"));
	}

	@Test
	void fullClassLevelWithCustomKeyManager() {
		Collection<CacheOperation> ops = getOps(AnnotatedClassWithFullDefault.class, "methodLevelKeyGenerator", 1);
		assertThat(ops).singleElement().satisfies(hasSharedConfig(
				"custom", "", "classCacheResolver" , "classCacheName"));
	}

	@Test
	void fullClassLevelWithCustomCacheManager() {
		Collection<CacheOperation> ops = getOps(AnnotatedClassWithFullDefault.class, "methodLevelCacheManager", 1);
		assertThat(ops).singleElement().satisfies(hasSharedConfig(
				"classKeyGenerator", "custom", "", "classCacheName"));
	}

	@Test
	void fullClassLevelWithCustomCacheResolver() {
		Collection<CacheOperation> ops = getOps(AnnotatedClassWithFullDefault.class, "methodLevelCacheResolver", 1);
		assertThat(ops).singleElement().satisfies(hasSharedConfig(
				"classKeyGenerator", "", "custom" , "classCacheName"));
	}

	@Test
	void validateNoCacheIsValid() {
		// Valid as a CacheResolver might return the cache names to use with other info
		Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "noCacheNameSpecified");
		assertThat(ops).singleElement().satisfies(cacheOperation ->
				assertThat(cacheOperation.getCacheNames()).isEmpty());

	}

	@Test
	void customClassLevelWithCustomCacheName() {
		Collection<CacheOperation> ops = getOps(AnnotatedClassWithCustomDefault.class, "methodLevelCacheName", 1);
		assertThat(ops).singleElement().satisfies(hasSharedConfig(
				"classKeyGenerator", "", "classCacheResolver", "custom"));
	}

	@Test
	void severalCacheConfigUseClosest() {
		Collection<CacheOperation> ops = getOps(MultipleCacheConfig.class, "multipleCacheConfig");
		assertThat(ops).singleElement().satisfies(hasSharedConfig("", "", "", "myCache"));
	}

	@Test
	void cacheConfigFromInterface() {
		Collection<CacheOperation> ops = getOps(InterfaceCacheConfig.class, "interfaceCacheConfig");
		assertThat(ops).singleElement().satisfies(hasSharedConfig("", "", "", "myCache"));
	}

	@Test
	void cacheAnnotationOverride() {
		Collection<CacheOperation> ops = getOps(InterfaceCacheConfig.class, "interfaceCacheableOverride");
		assertThat(ops).singleElement().satisfies(cacheOperation(CacheableOperation.class));
	}

	@Test
	void partialClassLevelWithCustomCacheManager() {
		Collection<CacheOperation> ops = getOps(AnnotatedClassWithSomeDefault.class, "methodLevelCacheManager", 1);
		assertThat(ops).singleElement().satisfies(hasSharedConfig(
				"classKeyGenerator", "custom", "", "classCacheName"));
	}

	@Test
	void partialClassLevelWithCustomCacheResolver() {
		Collection<CacheOperation> ops = getOps(AnnotatedClassWithSomeDefault.class, "methodLevelCacheResolver", 1);
		assertThat(ops).singleElement().satisfies(hasSharedConfig(
				"classKeyGenerator", "", "custom", "classCacheName"));
	}

	@Test
	void partialClassLevelWithNoCustomization() {
		Collection<CacheOperation> ops = getOps(AnnotatedClassWithSomeDefault.class, "noCustomization", 1);
		assertThat(ops).singleElement().satisfies(hasSharedConfig(
				"classKeyGenerator", "classCacheManager", "", "classCacheName"));
	}

	private Consumer<CacheOperation> cacheOperation(Class<? extends CacheOperation> type, String... cacheNames) {
		return candidate -> {
			assertThat(candidate).isInstanceOf(type);
			assertThat(candidate.getCacheNames()).containsExactly(cacheNames);
		};
	}

	private Consumer<CacheOperation> cacheOperation(Class<? extends CacheOperation> type) {
		return candidate -> assertThat(candidate).isInstanceOf(type);
	}

	private Collection<CacheOperation> getOps(Class<?> target, String name, int expectedNumberOfOperations) {
		Collection<CacheOperation> result = getOps(target, name);
		assertThat(result).as("Wrong number of operation(s) for '" + name + "'").hasSize(expectedNumberOfOperations);
		return result;
	}

	private Collection<CacheOperation> getOps(Class<?> target, String name) {
		try {
			Method method = target.getMethod(name);
			return this.source.getCacheOperations(method, target);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private Consumer<CacheOperation> hasSharedConfig(String keyGenerator, String cacheManager,
			String cacheResolver, String... cacheNames) {
		return actual -> {
			assertThat(actual.getKeyGenerator()).isEqualTo(keyGenerator);
			assertThat(actual.getCacheManager()).isEqualTo(cacheManager);
			assertThat(actual.getCacheResolver()).isEqualTo(cacheResolver);
			assertThat(actual.getCacheNames()).hasSameSizeAs(cacheNames);
			assertThat(actual.getCacheNames()).containsExactly(cacheNames);
		};
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

		@Caching
		public void emptyCaching() {
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
	@CacheConfig(cacheNames = "myCache")  // multiple sources
	private static class MultipleCacheConfig {

		@Cacheable
		public void multipleCacheConfig() {
		}
	}


	@CacheConfig(cacheNames = "myCache")
	private interface CacheConfigIfc {

		@Cacheable
		void interfaceCacheConfig();

		@CachePut
		void interfaceCacheableOverride();
	}


	private static class InterfaceCacheConfig implements CacheConfigIfc {

		@Override
		public void interfaceCacheConfig() {
		}

		@Override
		@Cacheable
		public void interfaceCacheableOverride() {
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
	@CacheConfig(keyGenerator = "classKeyGenerator",
			cacheManager = "classCacheManager",
			cacheResolver = "classCacheResolver")
	public @interface CacheConfigFoo {
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.TYPE})
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
	@Target({ElementType.METHOD, ElementType.TYPE})
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
