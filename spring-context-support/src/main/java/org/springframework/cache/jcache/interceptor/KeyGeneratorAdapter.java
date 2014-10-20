package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheKeyInvocationContext;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Spring's {@link KeyGenerator} implementation that either delegates to a
 * standard JSR-107 {@link javax.cache.annotation.CacheKeyGenerator}, or
 * wrap a standard {@link KeyGenerator} so that only relevant parameters
 * are handled.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
class KeyGeneratorAdapter implements KeyGenerator {

	private final JCacheOperationSource cacheOperationSource;

	private KeyGenerator keyGenerator;

	private CacheKeyGenerator cacheKeyGenerator;

	/**
	 * Create an instance with the given {@link KeyGenerator} so that {@link javax.cache.annotation.CacheKey}
	 * and {@link javax.cache.annotation.CacheValue} are handled according to the spec.
	 */
	public KeyGeneratorAdapter(JCacheOperationSource cacheOperationSource, KeyGenerator target) {
		Assert.notNull(cacheOperationSource, "cacheOperationSource must not be null.");
		Assert.notNull(target, "KeyGenerator must not be null");
		this.cacheOperationSource = cacheOperationSource;
		this.keyGenerator = target;
	}

	/**
	 * Create an instance used to wrap the specified {@link javax.cache.annotation.CacheKeyGenerator}.
	 */
	public KeyGeneratorAdapter(JCacheOperationSource cacheOperationSource, CacheKeyGenerator target) {
		Assert.notNull(cacheOperationSource, "cacheOperationSource must not be null.");
		Assert.notNull(target, "KeyGenerator must not be null");
		this.cacheOperationSource = cacheOperationSource;
		this.cacheKeyGenerator = target;
	}

	/**
	 * Return the target key generator to use in the form of either a {@link KeyGenerator}
	 * or a {@link CacheKeyGenerator}.
	 */
	public Object getTarget() {
		return (this.keyGenerator != null ? this.keyGenerator : this.cacheKeyGenerator);
	}


	@Override
	public Object generate(Object target, Method method, Object... params) {
		JCacheOperation<?> operation = this.cacheOperationSource.getCacheOperation(method, target.getClass());
		if (!(AbstractJCacheKeyOperation.class.isInstance(operation))) {
			throw new IllegalStateException("Invalid operation, should be a key-based operation " + operation);
		}
		CacheKeyInvocationContext<?> invocationContext = createCacheKeyInvocationContext(target, operation, params);

		if (this.cacheKeyGenerator != null) {
			return this.cacheKeyGenerator.generateCacheKey(invocationContext);
		}
		else {
			return doGenerate(this.keyGenerator, invocationContext);
		}
	}

	@SuppressWarnings("unchecked")
	private static Object doGenerate(KeyGenerator keyGenerator, CacheKeyInvocationContext<?> context) {
		List<Object> parameters = new ArrayList<Object>();
		for (CacheInvocationParameter param : context.getKeyParameters()) {
			Object value = param.getValue();
			if (param.getParameterPosition() == context.getAllParameters().length - 1 &&
					context.getMethod().isVarArgs()) {
				parameters.addAll((List<Object>) CollectionUtils.arrayToList(value));
			}
			else {
				parameters.add(value);
			}
		}
		return keyGenerator.generate(context.getTarget(), context.getMethod(),
				parameters.toArray(new Object[parameters.size()]));

	}


	@SuppressWarnings("unchecked")
	private CacheKeyInvocationContext<?> createCacheKeyInvocationContext(Object target,
			JCacheOperation<?> operation, Object[] params) {
		AbstractJCacheKeyOperation<Annotation> keyCacheOperation = (AbstractJCacheKeyOperation<Annotation>) operation;
		return new DefaultCacheKeyInvocationContext<Annotation>(keyCacheOperation, target, params);
	}

}
