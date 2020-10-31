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

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CachePut;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Stephane Nicoll
 */
public class CachePutOperationTests extends AbstractCacheOperationTests<CachePutOperation> {

	@Override
	protected CachePutOperation createSimpleOperation() {
		CacheMethodDetails<CachePut> methodDetails = create(CachePut.class,
				SampleObject.class, "simplePut", Long.class, SampleObject.class);
		return createDefaultOperation(methodDetails);
	}

	@Test
	public void simplePut() {
		CachePutOperation operation = createSimpleOperation();

		CacheInvocationParameter[] allParameters = operation.getAllParameters(2L, sampleInstance);
		assertThat(allParameters.length).isEqualTo(2);
		assertCacheInvocationParameter(allParameters[0], Long.class, 2L, 0);
		assertCacheInvocationParameter(allParameters[1], SampleObject.class, sampleInstance, 1);

		CacheInvocationParameter valueParameter = operation.getValueParameter(2L, sampleInstance);
		assertThat(valueParameter).isNotNull();
		assertCacheInvocationParameter(valueParameter, SampleObject.class, sampleInstance, 1);
	}

	@Test
	public void noCacheValue() {
		CacheMethodDetails<CachePut> methodDetails = create(CachePut.class,
				SampleObject.class, "noCacheValue", Long.class);

		assertThatIllegalArgumentException().isThrownBy(() ->
				createDefaultOperation(methodDetails));
	}

	@Test
	public void multiCacheValues() {
		CacheMethodDetails<CachePut> methodDetails = create(CachePut.class,
				SampleObject.class, "multiCacheValues", Long.class, SampleObject.class, SampleObject.class);

		assertThatIllegalArgumentException().isThrownBy(() ->
				createDefaultOperation(methodDetails));
	}

	@Test
	public void invokeWithWrongParameters() {
		CachePutOperation operation = createSimpleOperation();

		assertThatIllegalStateException().isThrownBy(() ->
				operation.getValueParameter(2L));
	}

	@Test
	public void fullPutConfig() {
		CacheMethodDetails<CachePut> methodDetails = create(CachePut.class,
				SampleObject.class, "fullPutConfig", Long.class, SampleObject.class);
		CachePutOperation operation = createDefaultOperation(methodDetails);
		assertThat(operation.isEarlyPut()).isTrue();
		assertThat(operation.getExceptionTypeFilter()).isNotNull();
		assertThat(operation.getExceptionTypeFilter().match(IOException.class)).isTrue();
		assertThat(operation.getExceptionTypeFilter().match(NullPointerException.class)).isFalse();
	}

	private CachePutOperation createDefaultOperation(CacheMethodDetails<CachePut> methodDetails) {
		return new CachePutOperation(methodDetails, defaultCacheResolver, defaultKeyGenerator);
	}

}
