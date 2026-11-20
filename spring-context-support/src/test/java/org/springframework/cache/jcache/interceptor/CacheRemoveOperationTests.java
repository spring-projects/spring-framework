/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheRemove;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
class CacheRemoveOperationTests extends AbstractCacheOperationTests<CacheRemoveOperation> {

	@Override
	protected CacheRemoveOperation createSimpleOperation() {
		CacheMethodDetails<CacheRemove> methodDetails = create(CacheRemove.class,
				SampleObject.class, "simpleRemove", Long.class);

		return new CacheRemoveOperation(methodDetails, defaultCacheResolver, defaultKeyGenerator);
	}

	@Test
	void simpleRemove() {
		CacheRemoveOperation operation = createSimpleOperation();

		CacheInvocationParameter[] allParameters = operation.getAllParameters(2L);
		assertThat(allParameters).hasSize(1);
		assertCacheInvocationParameter(allParameters[0], Long.class, 2L, 0);
	}

}
