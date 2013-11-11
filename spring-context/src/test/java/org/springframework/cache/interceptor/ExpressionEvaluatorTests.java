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

package org.springframework.cache.interceptor;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.ReflectionUtils;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author Costin Leau
 * @author Phillip Webb
 * @author Sam Brannen
 */
public class ExpressionEvaluatorTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private ExpressionEvaluator eval = new ExpressionEvaluator();

	private AnnotationCacheOperationSource source = new AnnotationCacheOperationSource();

	private Collection<CacheOperation> getOps(String name) {
		Method method = ReflectionUtils.findMethod(AnnotatedClass.class, name, Object.class, Object.class);
		return source.getCacheOperations(method, AnnotatedClass.class);
	}

	@Test
	public void testMultipleCachingSource() throws Exception {
		Collection<CacheOperation> ops = getOps("multipleCaching");
		assertEquals(2, ops.size());
		Iterator<CacheOperation> it = ops.iterator();
		CacheOperation next = it.next();
		assertTrue(next instanceof CacheableOperation);
		assertTrue(next.getCacheNames().contains("test"));
		assertEquals("#a", next.getKey());
		next = it.next();
		assertTrue(next instanceof CacheableOperation);
		assertTrue(next.getCacheNames().contains("test"));
		assertEquals("#b", next.getKey());
	}

	@Test
	public void testMultipleCachingEval() throws Exception {
		AnnotatedClass target = new AnnotatedClass();
		Method method = ReflectionUtils.findMethod(AnnotatedClass.class, "multipleCaching", Object.class,
				Object.class);
		Object[] args = new Object[] { new Object(), new Object() };
		Collection<ConcurrentMapCache> caches = Collections.singleton(new ConcurrentMapCache("test"));

		EvaluationContext evalCtx = eval.createEvaluationContext(caches, method, args, target, target.getClass());
		Collection<CacheOperation> ops = getOps("multipleCaching");

		Iterator<CacheOperation> it = ops.iterator();

		Object keyA = eval.key(it.next().getKey(), method, evalCtx);
		Object keyB = eval.key(it.next().getKey(), method, evalCtx);

		assertEquals(args[0], keyA);
		assertEquals(args[1], keyB);
	}

	@Test
	public void withReturnValue() throws Exception {
		EvaluationContext context = createEvaluationContext("theResult");
		Object value = new SpelExpressionParser().parseExpression("#result").getValue(context);
		assertThat(value, equalTo((Object) "theResult"));
	}

	@Test
	public void withNullReturn() throws Exception {
		EvaluationContext context = createEvaluationContext(null);
		Object value = new SpelExpressionParser().parseExpression("#result").getValue(context);
		assertThat(value, nullValue());
	}

	@Test
	public void withoutReturnValue() throws Exception {
		EvaluationContext context = createEvaluationContext(ExpressionEvaluator.NO_RESULT);
		Object value = new SpelExpressionParser().parseExpression("#result").getValue(context);
		assertThat(value, nullValue());
	}

	private EvaluationContext createEvaluationContext(Object result) {
		AnnotatedClass target = new AnnotatedClass();
		Method method = ReflectionUtils.findMethod(AnnotatedClass.class, "multipleCaching", Object.class,
				Object.class);
		Object[] args = new Object[] { new Object(), new Object() };
		Collection<ConcurrentMapCache> caches = Collections.singleton(new ConcurrentMapCache("test"));
		EvaluationContext context = eval.createEvaluationContext(caches, method, args, target, target.getClass(), result);
		return context;
	}


	private static class AnnotatedClass {

		@Caching(cacheable = { @Cacheable(value = "test", key = "#a"), @Cacheable(value = "test", key = "#b") })
		public void multipleCaching(Object a, Object b) {
		}
	}

}
