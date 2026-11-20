/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.jcache.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.cache.CacheManager;
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

	private @Nullable JCacheOperationSource cacheOperationSource;


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

		private @Nullable JCacheOperationSource getCacheOperationSource() {
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
