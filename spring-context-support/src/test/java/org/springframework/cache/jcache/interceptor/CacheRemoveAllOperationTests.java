/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheRemoveAll;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
class CacheRemoveAllOperationTests extends AbstractCacheOperationTests<CacheRemoveAllOperation> {

	@Override
	protected CacheRemoveAllOperation createSimpleOperation() {
		CacheMethodDetails<CacheRemoveAll> methodDetails = create(CacheRemoveAll.class,
				SampleObject.class, "simpleRemoveAll");

		return new CacheRemoveAllOperation(methodDetails, defaultCacheResolver);
	}

	@Test
	void simpleRemoveAll() {
		CacheRemoveAllOperation operation = createSimpleOperation();

		CacheInvocationParameter[] allParameters = operation.getAllParameters();
		assertThat(allParameters).isEmpty();
	}

}
