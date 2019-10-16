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

package org.springframework.cache;

import org.junit.jupiter.api.Test;

import org.springframework.cache.support.AbstractValueAdaptingCache;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Stephane Nicoll
 */
public abstract class AbstractValueAdaptingCacheTests<T extends AbstractValueAdaptingCache>
		extends AbstractCacheTests<T>  {

	protected final static String CACHE_NAME_NO_NULL = "testCacheNoNull";

	protected abstract T getCache(boolean allowNull);

	@Test
	public void testCachePutNullValueAllowNullFalse() {
		T cache = getCache(false);
		String key = createRandomKey();
		assertThatIllegalArgumentException().isThrownBy(() ->
				cache.put(key, null))
			.withMessageContaining(CACHE_NAME_NO_NULL)
			.withMessageContaining("is configured to not allow null values but null was provided");
	}

}
