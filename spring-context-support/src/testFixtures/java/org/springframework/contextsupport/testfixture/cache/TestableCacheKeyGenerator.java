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

package org.springframework.contextsupport.testfixture.cache;

import java.lang.annotation.Annotation;

import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheKeyInvocationContext;
import javax.cache.annotation.GeneratedCacheKey;

import org.springframework.cache.interceptor.SimpleKey;

/**
 * A simple test key generator that only takes the first key arguments into
 * account. To be used with a multi parameters key to validate it has been
 * used properly.
 *
 * @author Stephane Nicoll
 */
public class TestableCacheKeyGenerator implements CacheKeyGenerator {

	@Override
	public GeneratedCacheKey generateCacheKey(CacheKeyInvocationContext<? extends Annotation> context) {
		return new SimpleGeneratedCacheKey(context.getKeyParameters()[0]);
	}


	@SuppressWarnings("serial")
	private static class SimpleGeneratedCacheKey extends SimpleKey implements GeneratedCacheKey {

		public SimpleGeneratedCacheKey(Object... elements) {
			super(elements);
		}

	}

}
