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

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheRemoveAll;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Stephane Nicoll
 */
public class CacheRemoveAllOperationTests extends AbstractCacheOperationTests<CacheRemoveAllOperation> {

	@Override
	protected CacheRemoveAllOperation createSimpleOperation() {
		CacheMethodDetails<CacheRemoveAll> methodDetails = create(CacheRemoveAll.class,
				SampleObject.class, "simpleRemoveAll");

		return new CacheRemoveAllOperation(methodDetails, defaultCacheResolver);
	}

	@Test
	public void simpleRemoveAll() {
		CacheRemoveAllOperation operation = createSimpleOperation();

		CacheInvocationParameter[] allParameters = operation.getAllParameters();
		assertEquals(0, allParameters.length);
	}

}
