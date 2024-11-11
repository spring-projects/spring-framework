/*
 * Copyright 2002-2024 the original author or authors.
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

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * A {@code Pointcut} that matches if the underlying {@link JCacheOperationSource}
 * has an operation for a given method.
 *
 * @author Juergen Hoeller
 * @since 6.2
 */
@SuppressWarnings("serial")
final class JCacheOperationSourcePointcut extends StaticMethodMatcherPointcut implements Serializable {

	@Nullable
	private JCacheOperationSource cacheOperationSource;


	public JCacheOperationSourcePointcut() {
		setClassFilter(new JCacheOperationSourceClassFilter());
	}


	public void setCacheOperationSource(@Nullable JCacheOperationSource cacheOperationSource) {
		this.cacheOperationSource = cacheOperationSource;
	}

	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		return (this.cacheOperationSource == null ||
				this.cacheOperationSource.hasCacheOperation(method, targetClass));
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof JCacheOperationSourcePointcut that &&
				ObjectUtils.nullSafeEquals(this.cacheOperationSource, that.cacheOperationSource)));
	}

	@Override
	public int hashCode() {
		return JCacheOperationSourcePointcut.class.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.cacheOperationSource;
	}


	/**
	 * {@link ClassFilter} that delegates to {@link JCacheOperationSource#isCandidateClass}
	 * for filtering classes whose methods are not worth searching to begin with.
	 */
	private final class JCacheOperationSourceClassFilter implements ClassFilter {

		@Override
		public boolean matches(Class<?> clazz) {
			if (CacheManager.class.isAssignableFrom(clazz)) {
				return false;
			}
			return (cacheOperationSource == null || cacheOperationSource.isCandidateClass(clazz));
		}

		@Nullable
		private JCacheOperationSource getCacheOperationSource() {
			return cacheOperationSource;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof JCacheOperationSourceClassFilter that &&
					ObjectUtils.nullSafeEquals(getCacheOperationSource(), that.getCacheOperationSource())));
		}

		@Override
		public int hashCode() {
			return JCacheOperationSourceClassFilter.class.hashCode();
		}

		@Override
		public String toString() {
			return JCacheOperationSourceClassFilter.class.getName() + ": " + getCacheOperationSource();
		}
	}

}
