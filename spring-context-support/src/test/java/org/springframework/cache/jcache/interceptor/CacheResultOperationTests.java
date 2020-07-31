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

package org.springframework.cache.jcache.interceptor;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Set;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheResult;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

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

		assertThat(operation.getKeyGenerator()).isNotNull();
		assertThat(operation.getExceptionCacheResolver()).isNotNull();

		assertThat(operation.getExceptionCacheName()).isNull();
		assertThat(operation.getExceptionCacheResolver()).isEqualTo(defaultExceptionCacheResolver);

		CacheInvocationParameter[] allParameters = operation.getAllParameters(2L);
		assertThat(allParameters.length).isEqualTo(1);
		assertCacheInvocationParameter(allParameters[0], Long.class, 2L, 0);

		CacheInvocationParameter[] keyParameters = operation.getKeyParameters(2L);
		assertThat(keyParameters.length).isEqualTo(1);
		assertCacheInvocationParameter(keyParameters[0], Long.class, 2L, 0);
	}

	@Test
	public void multiParameterKey() {
		CacheMethodDetails<CacheResult> methodDetails = create(CacheResult.class,
				SampleObject.class, "multiKeysGet", Long.class, Boolean.class, String.class);
		CacheResultOperation operation = createDefaultOperation(methodDetails);

		CacheInvocationParameter[] keyParameters = operation.getKeyParameters(3L, Boolean.TRUE, "Foo");
		assertThat(keyParameters.length).isEqualTo(2);
		assertCacheInvocationParameter(keyParameters[0], Long.class, 3L, 0);
		assertCacheInvocationParameter(keyParameters[1], String.class, "Foo", 2);
	}

	@Test
	public void invokeWithWrongParameters() {
		CacheMethodDetails<CacheResult> methodDetails = create(CacheResult.class,
				SampleObject.class, "anotherSimpleGet", String.class, Long.class);
		CacheResultOperation operation = createDefaultOperation(methodDetails);

		// missing one argument
		assertThatIllegalStateException().isThrownBy(() ->
				operation.getAllParameters("bar"));
	}

	@Test
	public void tooManyKeyValues() {
		CacheMethodDetails<CacheResult> methodDetails = create(CacheResult.class,
				SampleObject.class, "anotherSimpleGet", String.class, Long.class);
		CacheResultOperation operation = createDefaultOperation(methodDetails);

		// missing one argument
		assertThatIllegalStateException().isThrownBy(() ->
				operation.getKeyParameters("bar"));
	}

	@Test
	public void annotatedGet() {
		CacheMethodDetails<CacheResult> methodDetails = create(CacheResult.class,
				SampleObject.class, "annotatedGet", Long.class, String.class);
		CacheResultOperation operation = createDefaultOperation(methodDetails);
		CacheInvocationParameter[] parameters = operation.getAllParameters(2L, "foo");

		Set<Annotation> firstParameterAnnotations = parameters[0].getAnnotations();
		assertThat(firstParameterAnnotations.size()).isEqualTo(1);
		assertThat(firstParameterAnnotations.iterator().next().annotationType()).isEqualTo(CacheKey.class);

		Set<Annotation> secondParameterAnnotations = parameters[1].getAnnotations();
		assertThat(secondParameterAnnotations.size()).isEqualTo(1);
		assertThat(secondParameterAnnotations.iterator().next().annotationType()).isEqualTo(Value.class);
	}

	@Test
	public void fullGetConfig() {
		CacheMethodDetails<CacheResult> methodDetails = create(CacheResult.class,
				SampleObject.class, "fullGetConfig", Long.class);
		CacheResultOperation operation = createDefaultOperation(methodDetails);
		assertThat(operation.isAlwaysInvoked()).isTrue();
		assertThat(operation.getExceptionTypeFilter()).isNotNull();
		assertThat(operation.getExceptionTypeFilter().match(IOException.class)).isTrue();
		assertThat(operation.getExceptionTypeFilter().match(NullPointerException.class)).isFalse();
	}

	private CacheResultOperation createDefaultOperation(CacheMethodDetails<CacheResult> methodDetails) {
		return new CacheResultOperation(methodDetails,
				defaultCacheResolver, defaultKeyGenerator, defaultCacheResolver);
	}

}
