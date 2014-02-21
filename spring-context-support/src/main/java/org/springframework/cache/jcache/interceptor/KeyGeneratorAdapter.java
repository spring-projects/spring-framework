package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheKeyInvocationContext;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.jcache.model.BaseKeyCacheOperation;
import org.springframework.cache.jcache.model.JCacheOperation;
import org.springframework.util.Assert;

/**
 * Spring's {@link KeyGenerator} implementation that delegates to a standard
 * JSR-107 {@link javax.cache.annotation.CacheKeyGenerator}.
 * <p>Used internally to invoke user-based JSR-107 cache key generators.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public class KeyGeneratorAdapter implements KeyGenerator {

	private final JCacheOperationSource cacheOperationSource;

	private final CacheKeyGenerator target;

	public KeyGeneratorAdapter(JCacheOperationSource cacheOperationSource, CacheKeyGenerator target) {
		Assert.notNull(cacheOperationSource, "cacheOperationSource must be set.");
		Assert.notNull(target, "cache key generator must be set.");
		this.cacheOperationSource = cacheOperationSource;
		this.target = target;
	}

	/**
	 * Return the underlying {@link CacheKeyGenerator} that this instance is using.
	 */
	protected CacheKeyGenerator getTarget() {
		return target;
	}

	@Override
	public Object generate(Object target, Method method, Object... params) {
		JCacheOperation<?> operation = cacheOperationSource.getCacheOperation(method, target.getClass());
		if (!(BaseKeyCacheOperation.class.isInstance(operation))) {
			throw new IllegalStateException("Invalid operation, should be a key-based operation " + operation);
		}
		CacheKeyInvocationContext<?> invocationContext = createCacheKeyInvocationContext(target, operation, params);

		return this.target.generateCacheKey(invocationContext);
	}

	@SuppressWarnings("unchecked")
	private CacheKeyInvocationContext<?> createCacheKeyInvocationContext(Object target,
			JCacheOperation<?> operation, Object[] params) {
		BaseKeyCacheOperation<Annotation> keyCacheOperation = (BaseKeyCacheOperation<Annotation>) operation;
		return new DefaultCacheKeyInvocationContext<Annotation>(keyCacheOperation, target, params);
	}

}
