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

package org.springframework.cache.annotation;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import org.springframework.cache.interceptor.CacheEvictOperation;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CachePutOperation;
import org.springframework.cache.interceptor.CacheableOperation;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Strategy implementation for parsing Spring's {@link Caching}, {@link Cacheable},
 * {@link CacheEvict} and {@link CachePut} annotations.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 3.1
 */
@SuppressWarnings("serial")
public class SpringCacheAnnotationParser implements CacheAnnotationParser, Serializable {

	@Override
	public Collection<CacheOperation> parseCacheAnnotations(Class<?> type) {
		DefaultCacheConfig defaultConfig = getDefaultCacheConfig(type);
		return parseCacheAnnotations(defaultConfig, type);
	}

	@Override
	public Collection<CacheOperation> parseCacheAnnotations(Method method) {
		DefaultCacheConfig defaultConfig = getDefaultCacheConfig(method.getDeclaringClass());
		return parseCacheAnnotations(defaultConfig, method);
	}

	protected Collection<CacheOperation> parseCacheAnnotations(DefaultCacheConfig cachingConfig,
															   AnnotatedElement ae) {
		Collection<CacheOperation> ops = null;

		Collection<Cacheable> cacheables = getAnnotations(ae, Cacheable.class);
		if (cacheables != null) {
			ops = lazyInit(ops);
			for (Cacheable cacheable : cacheables) {
				ops.add(parseCacheableAnnotation(ae, cachingConfig, cacheable));
			}
		}
		Collection<CacheEvict> evicts = getAnnotations(ae, CacheEvict.class);
		if (evicts != null) {
			ops = lazyInit(ops);
			for (CacheEvict e : evicts) {
				ops.add(parseEvictAnnotation(ae, cachingConfig, e));
			}
		}
		Collection<CachePut> updates = getAnnotations(ae, CachePut.class);
		if (updates != null) {
			ops = lazyInit(ops);
			for (CachePut p : updates) {
				ops.add(parseUpdateAnnotation(ae, cachingConfig, p));
			}
		}
		Collection<Caching> caching = getAnnotations(ae, Caching.class);
		if (caching != null) {
			ops = lazyInit(ops);
			for (Caching c : caching) {
				ops.addAll(parseCachingAnnotation(ae, cachingConfig, c));
			}
		}
		return ops;
	}

	private <T extends Annotation> Collection<CacheOperation> lazyInit(Collection<CacheOperation> ops) {
		return (ops != null ? ops : new ArrayList<CacheOperation>(1));
	}

	CacheableOperation parseCacheableAnnotation(AnnotatedElement ae,
												DefaultCacheConfig defaultConfig, Cacheable caching) {
		CacheableOperation cuo = new CacheableOperation();
		cuo.setCacheNames(caching.value());
		cuo.setCondition(caching.condition());
		cuo.setUnless(caching.unless());
		cuo.setKey(caching.key());
		cuo.setKeyGenerator(caching.keyGenerator());
		cuo.setCacheManager(caching.cacheManager());
		cuo.setCacheResolver(caching.cacheResolver());
		cuo.setName(ae.toString());

		defaultConfig.applyDefault(cuo);

		validateCacheOperation(ae, cuo);
		return cuo;
	}

	CacheEvictOperation parseEvictAnnotation(AnnotatedElement ae,
											 DefaultCacheConfig defaultConfig, CacheEvict caching) {
		CacheEvictOperation ceo = new CacheEvictOperation();
		ceo.setCacheNames(caching.value());
		ceo.setCondition(caching.condition());
		ceo.setKey(caching.key());
		ceo.setKeyGenerator(caching.keyGenerator());
		ceo.setCacheManager(caching.cacheManager());
		ceo.setCacheResolver(caching.cacheResolver());
		ceo.setCacheWide(caching.allEntries());
		ceo.setBeforeInvocation(caching.beforeInvocation());
		ceo.setName(ae.toString());

		defaultConfig.applyDefault(ceo);

		validateCacheOperation(ae, ceo);
		return ceo;
	}

	CacheOperation parseUpdateAnnotation(AnnotatedElement ae,
										 DefaultCacheConfig defaultConfig, CachePut caching) {
		CachePutOperation cuo = new CachePutOperation();
		cuo.setCacheNames(caching.value());
		cuo.setCondition(caching.condition());
		cuo.setUnless(caching.unless());
		cuo.setKey(caching.key());
		cuo.setKeyGenerator(caching.keyGenerator());
		cuo.setCacheManager(caching.cacheManager());
		cuo.setCacheResolver(caching.cacheResolver());
		cuo.setName(ae.toString());

		defaultConfig.applyDefault(cuo);

		validateCacheOperation(ae, cuo);
		return cuo;
	}

	Collection<CacheOperation> parseCachingAnnotation(AnnotatedElement ae,
													  DefaultCacheConfig defaultConfig, Caching caching) {
		Collection<CacheOperation> ops = null;

		Cacheable[] cacheables = caching.cacheable();
		if (!ObjectUtils.isEmpty(cacheables)) {
			ops = lazyInit(ops);
			for (Cacheable cacheable : cacheables) {
				ops.add(parseCacheableAnnotation(ae, defaultConfig, cacheable));
			}
		}
		CacheEvict[] evicts = caching.evict();
		if (!ObjectUtils.isEmpty(evicts)) {
			ops = lazyInit(ops);
			for (CacheEvict evict : evicts) {
				ops.add(parseEvictAnnotation(ae, defaultConfig, evict));
			}
		}
		CachePut[] updates = caching.put();
		if (!ObjectUtils.isEmpty(updates)) {
			ops = lazyInit(ops);
			for (CachePut update : updates) {
				ops.add(parseUpdateAnnotation(ae, defaultConfig, update));
			}
		}

		return ops;
	}

	/**
	 * Provides the {@link DefaultCacheConfig} instance for the specified {@link Class}.
	 *
	 * @param target the class-level to handle
	 * @return the default config (never {@code null})
	 */
	DefaultCacheConfig getDefaultCacheConfig(Class<?> target) {
		final CacheConfig annotation = AnnotationUtils.getAnnotation(target, CacheConfig.class);
		if (annotation != null) {
			return new DefaultCacheConfig(annotation.cacheNames(), annotation.keyGenerator(),
					annotation.cacheManager(), annotation.cacheResolver());
		}
		return new DefaultCacheConfig();
	}

	private <T extends Annotation> Collection<T> getAnnotations(AnnotatedElement ae, Class<T> annotationType) {
		Collection<T> anns = new ArrayList<T>(2);

		// look at raw annotation
		T ann = ae.getAnnotation(annotationType);
		if (ann != null) {
			anns.add(ann);
		}

		// scan meta-annotations
		for (Annotation metaAnn : ae.getAnnotations()) {
			ann = metaAnn.annotationType().getAnnotation(annotationType);
			if (ann != null) {
				anns.add(ann);
			}
		}

		return (anns.isEmpty() ? null : anns);
	}

	/**
	 * Validates the specified {@link CacheOperation}.
	 * <p>Throws an {@link IllegalStateException} if the state of the operation is
	 * invalid. As there might be multiple sources for default values, this ensure
	 * that the operation is in a proper state before being returned.
	 *
	 * @param ae the annotated element of the cache operation
	 * @param operation the {@link CacheOperation} to validate
	 */
	private void validateCacheOperation(AnnotatedElement ae, CacheOperation operation) {
		if (StringUtils.hasText(operation.getKey()) && StringUtils.hasText(operation.getKeyGenerator())) {
			throw new IllegalStateException("Invalid cache annotation configuration on '"
					+ ae.toString() + "'. Both 'key' and 'keyGenerator' attributes have been set. " +
					"These attributes are mutually exclusive: either set the SpEL expression used to" +
					"compute the key at runtime or set the name of the KeyGenerator bean to use.");
		}
		if (StringUtils.hasText(operation.getCacheManager()) && StringUtils.hasText(operation.getCacheResolver())) {
			throw new IllegalStateException("Invalid cache annotation configuration on '"
					+ ae.toString() + "'. Both 'cacheManager' and 'cacheResolver' attributes have been set. " +
					"These attributes are mutually exclusive: the cache manager is used to configure a" +
					"default cache resolver if none is set. If a cache resolver is set, the cache manager" +
					"won't be used.");
		}
		if (operation.getCacheNames().isEmpty()) {
			throw new IllegalStateException("No cache names could be detected on '"
					+ ae.toString() + "'. Make sure to set the value parameter on the annotation or " +
					"declare a @CacheConfig at the class-level with the default cache name(s) to use.");
		}
	}

	@Override
	public boolean equals(Object other) {
		return (this == other || other instanceof SpringCacheAnnotationParser);
	}

	@Override
	public int hashCode() {
		return SpringCacheAnnotationParser.class.hashCode();
	}


	/**
	 * Provides default settings for a given set of cache operations.
	 */
	static class DefaultCacheConfig {
		private final String[] cacheNames;

		private final String keyGenerator;

		private final String cacheManager;

		private final String cacheResolver;

		private DefaultCacheConfig(String[] cacheNames, String keyGenerator,
				String cacheManager, String cacheResolver) {
			this.cacheNames = cacheNames;
			this.keyGenerator = keyGenerator;
			this.cacheManager = cacheManager;
			this.cacheResolver = cacheResolver;
		}

		public DefaultCacheConfig() {
			this(null, null, null, null);
		}

		/**
		 * Apply the defaults to the specified {@link CacheOperation}.
		 *
		 * @param operation the operation to update
		 */
		public void applyDefault(CacheOperation operation) {
			if (operation.getCacheNames().isEmpty() && cacheNames != null) {
				operation.setCacheNames(cacheNames);
			}
			if (!StringUtils.hasText(operation.getKey()) && !StringUtils.hasText(operation.getKeyGenerator())
					&& StringUtils.hasText(keyGenerator)) {
				operation.setKeyGenerator(keyGenerator);
			}

			if (isSet(operation.getCacheManager()) || isSet(operation.getCacheResolver())) {
				// One of these is set so we should not inherit anything
			}
			else if (isSet(cacheResolver)) {
				operation.setCacheResolver(cacheResolver);
			}
			else if (isSet(cacheManager)) {
				operation.setCacheManager(cacheManager);
			}
		}

		private boolean isSet(String s) {
			return StringUtils.hasText(s);
		}

	}

}
