/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
class CachePutOperationTests extends AbstractCacheOperationTests<CachePutOperation> {

	@Override
	protected CachePutOperation createSimpleOperation() {
		CacheMethodDetails<CachePut> methodDetails = create(CachePut.class,
				SampleObject.class, "simplePut", Long.class, SampleObject.class);
		return createDefaultOperation(methodDetails);
	}

	@Test
	void simplePut() {
		CachePutOperation operation = createSimpleOperation();

		CacheInvocationParameter[] allParameters = operation.getAllParameters(2L, sampleInstance);
		assertThat(allParameters).hasSize(2);
		assertCacheInvocationParameter(allParameters[0], Long.class, 2L, 0);
		assertCacheInvocationParameter(allParameters[1], SampleObject.class, sampleInstance, 1);

		CacheInvocationParameter valueParameter = operation.getValueParameter(2L, sampleInstance);
		assertThat(valueParameter).isNotNull();
		assertCacheInvocationParameter(valueParameter, SampleObject.class, sampleInstance, 1);
	}

	@Test
	void noCacheValue() {
		CacheMethodDetails<CachePut> methodDetails = create(CachePut.class,
				SampleObject.class, "noCacheValue", Long.class);

		assertThatIllegalArgumentException().isThrownBy(() ->
				createDefaultOperation(methodDetails));
	}

	@Test
	void multiCacheValues() {
		CacheMethodDetails<CachePut> methodDetails = create(CachePut.class,
				SampleObject.class, "multiCacheValues", Long.class, SampleObject.class, SampleObject.class);

		assertThatIllegalArgumentException().isThrownBy(() ->
				createDefaultOperation(methodDetails));
	}

	@Test
	void invokeWithWrongParameters() {
		CachePutOperation operation = createSimpleOperation();

		assertThatIllegalStateException().isThrownBy(() ->
				operation.getValueParameter(2L));
	}

	@Test
	void fullPutConfig() {
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
