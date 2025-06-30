/*
 * Copyright 2002-present the original author or authors.
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
