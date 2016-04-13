/*
 * Copyright 2002-2016 the original author or authors.
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
import java.util.ArrayList;
import java.util.Collection;

import org.springframework.cache.interceptor.CacheEvictOperation;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CachePutOperation;
import org.springframework.cache.interceptor.CacheableOperation;
import org.springframework.util.ObjectUtils;

/**
 * Strategy implementation for parsing Spring's {@link Caching}, {@link Cacheable},
 * {@link CacheEvict} and {@link CachePut} annotations.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @since 3.1
 */
@SuppressWarnings("serial")
public class SpringCacheAnnotationParser implements CacheAnnotationParser, Serializable {

	@Override
	public Collection<CacheOperation> parseCacheAnnotations(AnnotatedElement ae) {
		Collection<CacheOperation> ops = null;

		Collection<Cacheable> cacheables = getAnnotations(ae, Cacheable.class);
		if (cacheables != null) {
			ops = lazyInit(ops);
			for (Cacheable cacheable : cacheables) {
				ops.add(parseCacheableAnnotation(ae, cacheable));
			}
		}
		Collection<CacheEvict> evicts = getAnnotations(ae, CacheEvict.class);
		if (evicts != null) {
			ops = lazyInit(ops);
			for (CacheEvict evict : evicts) {
				ops.add(parseEvictAnnotation(ae, evict));
			}
		}
		Collection<CachePut> puts = getAnnotations(ae, CachePut.class);
		if (puts != null) {
			ops = lazyInit(ops);
			for (CachePut put : puts) {
				ops.add(parsePutAnnotation(ae, put));
			}
		}
		Collection<Caching> cachings = getAnnotations(ae, Caching.class);
		if (cachings != null) {
			ops = lazyInit(ops);
			for (Caching caching : cachings) {
				Collection<CacheOperation> cachingOps = parseCachingAnnotation(ae, caching);
				if (cachingOps != null) {
					ops.addAll(cachingOps);
				}
			}
		}

		return ops;
	}

	private <T extends Annotation> Collection<CacheOperation> lazyInit(Collection<CacheOperation> ops) {
		return (ops != null ? ops : new ArrayList<CacheOperation>(1));
	}

	CacheableOperation parseCacheableAnnotation(AnnotatedElement ae, Cacheable caching) {
		CacheableOperation op = new CacheableOperation();
		op.setCacheNames(caching.value());
		op.setCondition(caching.condition());
		op.setUnless(caching.unless());
		op.setKey(caching.key());
		op.setName(ae.toString());
		return op;
	}

	CacheEvictOperation parseEvictAnnotation(AnnotatedElement ae, CacheEvict caching) {
		CacheEvictOperation op = new CacheEvictOperation();
		op.setCacheNames(caching.value());
		op.setCondition(caching.condition());
		op.setKey(caching.key());
		op.setCacheWide(caching.allEntries());
		op.setBeforeInvocation(caching.beforeInvocation());
		op.setName(ae.toString());
		return op;
	}

	CacheOperation parsePutAnnotation(AnnotatedElement ae, CachePut caching) {
		CachePutOperation op = new CachePutOperation();
		op.setCacheNames(caching.value());
		op.setCondition(caching.condition());
		op.setUnless(caching.unless());
		op.setKey(caching.key());
		op.setName(ae.toString());
		return op;
	}

	Collection<CacheOperation> parseCachingAnnotation(AnnotatedElement ae, Caching caching) {
		Collection<CacheOperation> ops = null;

		Cacheable[] cacheables = caching.cacheable();
		if (!ObjectUtils.isEmpty(cacheables)) {
			ops = lazyInit(ops);
			for (Cacheable cacheable : cacheables) {
				ops.add(parseCacheableAnnotation(ae, cacheable));
			}
		}
		CacheEvict[] evicts = caching.evict();
		if (!ObjectUtils.isEmpty(evicts)) {
			ops = lazyInit(ops);
			for (CacheEvict evict : evicts) {
				ops.add(parseEvictAnnotation(ae, evict));
			}
		}
		CachePut[] updates = caching.put();
		if (!ObjectUtils.isEmpty(updates)) {
			ops = lazyInit(ops);
			for (CachePut update : updates) {
				ops.add(parsePutAnnotation(ae, update));
			}
		}

		return ops;
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

	@Override
	public boolean equals(Object other) {
		return (this == other || other instanceof SpringCacheAnnotationParser);
	}

	@Override
	public int hashCode() {
		return SpringCacheAnnotationParser.class.hashCode();
	}

}
