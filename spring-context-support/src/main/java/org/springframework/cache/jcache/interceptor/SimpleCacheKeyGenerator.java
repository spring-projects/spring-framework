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

import java.lang.annotation.Annotation;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheKeyInvocationContext;
import javax.cache.annotation.GeneratedCacheKey;

/**
 * A JSR-107 compliant key generator. Uses only the parameters that have been annotated
 * with {@link javax.cache.annotation.CacheKey} or all of them if none are set, except
 * the {@link javax.cache.annotation.CacheValue} one.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see javax.cache.annotation.CacheKeyInvocationContext#getKeyParameters()
 */
public class SimpleCacheKeyGenerator implements CacheKeyGenerator {

	@Override
	public GeneratedCacheKey generateCacheKey(CacheKeyInvocationContext<? extends Annotation> context) {
		CacheInvocationParameter[] keyParameters = context.getKeyParameters();
		final Object[] parameters = new Object[keyParameters.length];
		for (int i = 0; i < keyParameters.length; i++) {
			parameters[i] = keyParameters[i].getValue();
		}
		return new SimpleGeneratedCacheKey(parameters);
	}

}
