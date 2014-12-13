/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.cache.jcache.interceptor;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Set;
import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheResult;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Value;

import static org.junit.Assert.*;

/**
 * @author Stephane Nicoll
 */
public class CacheResultOperationTests extends AbstractCacheOperationTests<CacheResultOperation> {

	@Override
	protected CacheResultOperation createSimpleOperation() {
		CacheMethodDetails<CacheResult> methodDetails = create(CacheResult.class,
				SampleObject.class, "simpleGet", Long.class);

		return new CacheResultOperation(methodDetails, defaultCacheResolver, defaultKeyGenerator,
				defaultExceptionCacheResolver);
	}

	@Test
	public void simpleGet() {
		CacheResultOperation operation = createSimpleOperation();

		assertNotNull(operation.getKeyGenerator());
		assertNotNull(operation.getExceptionCacheResolver());

		assertNull(operation.getExceptionCacheName());
		assertEquals(defaultExceptionCacheResolver, operation.getExceptionCacheResolver());

		CacheInvocationParameter[] allParameters = operation.getAllParameters(2L);
		assertEquals(1, allParameters.length);
		assertCacheInvocationParameter(allParameters[0], Long.class, 2L, 0);

		CacheInvocationParameter[] keyParameters = operation.getKeyParameters(2L);
		assertEquals(1, keyParameters.length);
		assertCacheInvocationParameter(keyParameters[0], Long.class, 2L, 0);
	}

	@Test
	public void multiParameterKey() {
		CacheMethodDetails<CacheResult> methodDetails = create(CacheResult.class,
				SampleObject.class, "multiKeysGet", Long.class, Boolean.class, String.class);
		CacheResultOperation operation = createDefaultOperation(methodDetails);

		CacheInvocationParameter[] keyParameters = operation.getKeyParameters(3L, Boolean.TRUE, "Foo");
		assertEquals(2, keyParameters.length);
		assertCacheInvocationParameter(keyParameters[0], Long.class, 3L, 0);
		assertCacheInvocationParameter(keyParameters[1], String.class, "Foo", 2);
	}

	@Test
	public void invokeWithWrongParameters() {
		CacheMethodDetails<CacheResult> methodDetails = create(CacheResult.class,
				SampleObject.class, "anotherSimpleGet", String.class, Long.class);
		CacheResultOperation operation = createDefaultOperation(methodDetails);

		thrown.expect(IllegalStateException.class);
		operation.getAllParameters("bar"); // missing one argument
	}

	@Test
	public void tooManyKeyValues() {
		CacheMethodDetails<CacheResult> methodDetails = create(CacheResult.class,
				SampleObject.class, "anotherSimpleGet", String.class, Long.class);
		CacheResultOperation operation = createDefaultOperation(methodDetails);

		thrown.expect(IllegalStateException.class);
		operation.getKeyParameters("bar"); // missing one argument
	}

	@Test
	public void annotatedGet() {
		CacheMethodDetails<CacheResult> methodDetails = create(CacheResult.class,
				SampleObject.class, "annotatedGet", Long.class, String.class);
		CacheResultOperation operation = createDefaultOperation(methodDetails);
		CacheInvocationParameter[] parameters = operation.getAllParameters(2L, "foo");

		Set<Annotation> firstParameterAnnotations = parameters[0].getAnnotations();
		assertEquals(1, firstParameterAnnotations.size());
		assertEquals(CacheKey.class, firstParameterAnnotations.iterator().next().annotationType());

		Set<Annotation> secondParameterAnnotations = parameters[1].getAnnotations();
		assertEquals(1, secondParameterAnnotations.size());
		assertEquals(Value.class, secondParameterAnnotations.iterator().next().annotationType());
	}

	@Test
	public void fullGetConfig() {
		CacheMethodDetails<CacheResult> methodDetails = create(CacheResult.class,
				SampleObject.class, "fullGetConfig", Long.class);
		CacheResultOperation operation = createDefaultOperation(methodDetails);
		assertTrue(operation.isAlwaysInvoked());
		assertNotNull(operation.getExceptionTypeFilter());
		assertTrue(operation.getExceptionTypeFilter().match(IOException.class));
		assertFalse(operation.getExceptionTypeFilter().match(NullPointerException.class));
	}

	private CacheResultOperation createDefaultOperation(CacheMethodDetails<CacheResult> methodDetails) {
		return new CacheResultOperation(methodDetails,
				defaultCacheResolver, defaultKeyGenerator, defaultCacheResolver);
	}

}
