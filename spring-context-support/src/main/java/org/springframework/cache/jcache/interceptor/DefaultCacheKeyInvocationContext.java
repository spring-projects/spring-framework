/*
 * Copyright 2002-2014 the original author or authors.
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

import java.lang.annotation.Annotation;
import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKeyInvocationContext;

/**
 * The default {@link CacheKeyInvocationContext} implementation.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
class DefaultCacheKeyInvocationContext<A extends Annotation>
		extends DefaultCacheInvocationContext<A> implements CacheKeyInvocationContext<A> {

	private final CacheInvocationParameter[] keyParameters;

	private final CacheInvocationParameter valueParameter;

	public DefaultCacheKeyInvocationContext(AbstractJCacheKeyOperation<A> operation,
			Object target, Object[] args) {
		super(operation, target, args);
		this.keyParameters = operation.getKeyParameters(args);
		if (operation instanceof CachePutOperation) {
			this.valueParameter = ((CachePutOperation) operation).getValueParameter(args);
		}
		else {
			this.valueParameter = null;
		}
	}

	@Override
	public CacheInvocationParameter[] getKeyParameters() {
		return keyParameters.clone();
	}

	@Override
	public CacheInvocationParameter getValueParameter() {
		return valueParameter;
	}

}
