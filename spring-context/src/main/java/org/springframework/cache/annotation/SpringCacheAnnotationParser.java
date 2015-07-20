/*
 * Copyright 2002-2015 the original author or authors.
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
 * {@link CacheEvict}, and {@link CachePut} annotations.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sam Brannen
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

	protected Collection<CacheOperation> parseCacheAnnotations(DefaultCacheConfig cachingConfig, AnnotatedElement ae) {
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
			for (CacheEvict evict : evicts) {
				ops.add(parseEvictAnnotation(ae, cachingConfig, evict));
			}
		}
		Collection<CachePut> puts = getAnnotations(ae, CachePut.class);
		if (puts != null) {
			ops = lazyInit(ops);
			for (CachePut put : puts) {
				ops.add(parsePutAnnotation(ae, cachingConfig, put));
			}
		}
		Collection<Caching> cachings = getAnnotations(ae, Caching.class);
		if (cachings != null) {
			ops = lazyInit(ops);
			for (Caching caching : cachings) {
				ops.addAll(parseCachingAnnotation(ae, cachingConfig, caching));
			}
		}

		return ops;
	}

	private <T extends Annotation> Collection<CacheOperation> lazyInit(Collection<CacheOperation> ops) {
		return (ops != null ? ops : new ArrayList<CacheOperation>(1));
	}

	CacheableOperation parseCacheableAnnotation(AnnotatedElement ae, DefaultCacheConfig defaultConfig, Cacheable cacheable) {
		CacheableOperation op = new CacheableOperation();

		op.setCacheNames(cacheable.cacheNames());
		op.setCondition(cacheable.condition());
		op.setUnless(cacheable.unless());
		op.setKey(cacheable.key());
		op.setKeyGenerator(cacheable.keyGenerator());
		op.setCacheManager(cacheable.cacheManager());
		op.setCacheResolver(cacheable.cacheResolver());
		op.setName(ae.toString());

		defaultConfig.applyDefault(op);
		validateCacheOperation(ae, op);

		return op;
	}

	CacheEvictOperation parseEvictAnnotation(AnnotatedElement ae, DefaultCacheConfig defaultConfig, CacheEvict cacheEvict) {
		CacheEvictOperation op = new CacheEvictOperation();

		op.setCacheNames(cacheEvict.cacheNames());
		op.setCondition(cacheEvict.condition());
		op.setKey(cacheEvict.key());
		op.setKeyGenerator(cacheEvict.keyGenerator());
		op.setCacheManager(cacheEvict.cacheManager());
		op.setCacheResolver(cacheEvict.cacheResolver());
		op.setCacheWide(cacheEvict.allEntries());
		op.setBeforeInvocation(cacheEvict.beforeInvocation());
		op.setName(ae.toString());

		defaultConfig.applyDefault(op);
		validateCacheOperation(ae, op);

		return op;
	}

	CacheOperation parsePutAnnotation(AnnotatedElement ae, DefaultCacheConfig defaultConfig, CachePut cachePut) {
		CachePutOperation op = new CachePutOperation();

		op.setCacheNames(cachePut.cacheNames());
		op.setCondition(cachePut.condition());
		op.setUnless(cachePut.unless());
		op.setKey(cachePut.key());
		op.setKeyGenerator(cachePut.keyGenerator());
		op.setCacheManager(cachePut.cacheManager());
		op.setCacheResolver(cachePut.cacheResolver());
		op.setName(ae.toString());

		defaultConfig.applyDefault(op);
		validateCacheOperation(ae, op);

		return op;
	}

	Collection<CacheOperation> parseCachingAnnotation(AnnotatedElement ae, DefaultCacheConfig defaultConfig, Caching caching) {
		Collection<CacheOperation> ops = null;

		Cacheable[] cacheables = caching.cacheable();
		if (!ObjectUtils.isEmpty(cacheables)) {
			ops = lazyInit(ops);
			for (Cacheable cacheable : cacheables) {
				ops.add(parseCacheableAnnotation(ae, defaultConfig, cacheable));
			}
		}
		CacheEvict[] cacheEvicts = caching.evict();
		if (!ObjectUtils.isEmpty(cacheEvicts)) {
			ops = lazyInit(ops);
			for (CacheEvict cacheEvict : cacheEvicts) {
				ops.add(parseEvictAnnotation(ae, defaultConfig, cacheEvict));
			}
		}
		CachePut[] cachePuts = caching.put();
		if (!ObjectUtils.isEmpty(cachePuts)) {
			ops = lazyInit(ops);
			for (CachePut cachePut : cachePuts) {
				ops.add(parsePutAnnotation(ae, defaultConfig, cachePut));
			}
		}

		return ops;
	}

	/**
	 * Provides the {@link DefaultCacheConfig} instance for the specified {@link Class}.
	 * @param target the class-level to handle
	 * @return the default config (never {@code null})
	 */
	DefaultCacheConfig getDefaultCacheConfig(Class<?> target) {
		CacheConfig annotation = AnnotationUtils.getAnnotation(target, CacheConfig.class);
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
			anns.add(AnnotationUtils.synthesizeAnnotation(ann, ae));
		}

		// scan meta-annotations
		for (Annotation metaAnn : ae.getAnnotations()) {
			ann = metaAnn.annotationType().getAnnotation(annotationType);
			if (ann != null) {
				anns.add(AnnotationUtils.synthesizeAnnotation(ann, ae));
			}
		}

		return (anns.isEmpty() ? null : anns);
	}

	/**
	 * Validates the specified {@link CacheOperation}.
	 * <p>Throws an {@link IllegalStateException} if the state of the operation is
	 * invalid. As there might be multiple sources for default values, this ensure
	 * that the operation is in a proper state before being returned.
	 * @param ae the annotated element of the cache operation
	 * @param operation the {@link CacheOperation} to validate
	 */
	private void validateCacheOperation(AnnotatedElement ae, CacheOperation operation) {
		if (StringUtils.hasText(operation.getKey()) && StringUtils.hasText(operation.getKeyGenerator())) {
			throw new IllegalStateException("Invalid cache annotation configuration on '" +
					ae.toString() + "'. Both 'key' and 'keyGenerator' attributes have been set. " +
					"These attributes are mutually exclusive: either set the SpEL expression used to" +
					"compute the key at runtime or set the name of the KeyGenerator bean to use.");
		}
		if (StringUtils.hasText(operation.getCacheManager()) && StringUtils.hasText(operation.getCacheResolver())) {
			throw new IllegalStateException("Invalid cache annotation configuration on '" +
					ae.toString() + "'. Both 'cacheManager' and 'cacheResolver' attributes have been set. " +
					"These attributes are mutually exclusive: the cache manager is used to configure a" +
					"default cache resolver if none is set. If a cache resolver is set, the cache manager" +
					"won't be used.");
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

		public DefaultCacheConfig() {
			this(null, null, null, null);
		}

		private DefaultCacheConfig(String[] cacheNames, String keyGenerator, String cacheManager, String cacheResolver) {
			this.cacheNames = cacheNames;
			this.keyGenerator = keyGenerator;
			this.cacheManager = cacheManager;
			this.cacheResolver = cacheResolver;
		}

		/**
		 * Apply the defaults to the specified {@link CacheOperation}.
		 * @param operation the operation to update
		 */
		public void applyDefault(CacheOperation operation) {
			if (operation.getCacheNames().isEmpty() && this.cacheNames != null) {
				operation.setCacheNames(this.cacheNames);
			}
			if (!StringUtils.hasText(operation.getKey()) && !StringUtils.hasText(operation.getKeyGenerator()) &&
					StringUtils.hasText(this.keyGenerator)) {
				operation.setKeyGenerator(this.keyGenerator);
			}

			if (StringUtils.hasText(operation.getCacheManager()) || StringUtils.hasText(operation.getCacheResolver())) {
				// One of these is set so we should not inherit anything
			}
			else if (StringUtils.hasText(this.cacheResolver)) {
				operation.setCacheResolver(this.cacheResolver);
			}
			else if (StringUtils.hasText(this.cacheManager)) {
				operation.setCacheManager(this.cacheManager);
			}
		}

	}

}
