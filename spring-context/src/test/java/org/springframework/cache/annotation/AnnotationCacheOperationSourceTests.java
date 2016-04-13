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
import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;

import org.springframework.cache.interceptor.CacheEvictOperation;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CacheableOperation;

import static org.junit.Assert.*;

/**
 * @author Costin Leau
 */
public class AnnotationCacheOperationSourceTests {

	private final AnnotationCacheOperationSource source = new AnnotationCacheOperationSource();


	@Test
	public void testSingularAnnotation() throws Exception {
		Collection<CacheOperation> ops = getOps("singular");
		assertEquals(1, ops.size());
		assertTrue(ops.iterator().next() instanceof CacheableOperation);
	}

	@Test
	public void testMultipleAnnotation() throws Exception {
		Collection<CacheOperation> ops = getOps("multiple");
		assertEquals(2, ops.size());
		Iterator<CacheOperation> it = ops.iterator();
		assertTrue(it.next() instanceof CacheableOperation);
		assertTrue(it.next() instanceof CacheEvictOperation);
	}

	@Test
	public void testCaching() throws Exception {
		Collection<CacheOperation> ops = getOps("caching");
		assertEquals(2, ops.size());
		Iterator<CacheOperation> it = ops.iterator();
		assertTrue(it.next() instanceof CacheableOperation);
		assertTrue(it.next() instanceof CacheEvictOperation);
	}

	@Test
	public void testEmptyCaching() throws Exception {
		Collection<CacheOperation> ops = getOps("emptyCaching");
		assertTrue(ops.isEmpty());
	}

	@Test
	public void testSingularStereotype() throws Exception {
		Collection<CacheOperation> ops = getOps("singleStereotype");
		assertEquals(1, ops.size());
		assertTrue(ops.iterator().next() instanceof CacheEvictOperation);
	}

	@Test
	public void testMultipleStereotypes() throws Exception {
		Collection<CacheOperation> ops = getOps("multipleStereotype");
		assertEquals(3, ops.size());
		Iterator<CacheOperation> it = ops.iterator();
		assertTrue(it.next() instanceof CacheableOperation);
		CacheOperation next = it.next();
		assertTrue(next instanceof CacheEvictOperation);
		assertTrue(next.getCacheNames().contains("foo"));
		next = it.next();
		assertTrue(next instanceof CacheEvictOperation);
		assertTrue(next.getCacheNames().contains("bar"));
	}


	private Collection<CacheOperation> getOps(String name) throws Exception {
		Method method = AnnotatedClass.class.getMethod(name);
		return source.getCacheOperations(method, AnnotatedClass.class);
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

		@EvictFoo
		public void singleStereotype() {
		}

		@EvictFoo
		@CacheableFoo
		@EvictBar
		public void multipleStereotype() {
		}

		@Caching(cacheable = { @Cacheable(value = "test", key = "a"), @Cacheable(value = "test", key = "b") })
		public void multipleCaching() {
		}
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@Cacheable("foo")
	public @interface CacheableFoo {
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

}
