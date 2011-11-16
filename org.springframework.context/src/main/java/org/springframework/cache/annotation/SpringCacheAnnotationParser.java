/*
 * Copyright 2002-2011 the original author or authors.
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
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;

import org.springframework.cache.interceptor.CacheEvictOperation;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CachePutOperation;
import org.springframework.cache.interceptor.CacheableOperation;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ObjectUtils;

/**
 * Strategy implementation for parsing Spring's {@link Caching}, {@link Cacheable},
 * {@link CacheEvict} and {@link CachePut} annotations.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.1
 */
@SuppressWarnings("serial")
public class SpringCacheAnnotationParser implements CacheAnnotationParser, Serializable {

	public Collection<CacheOperation> parseCacheAnnotations(AnnotatedElement ae) {
		Collection<CacheOperation> ops = null;

		Cacheable cache = AnnotationUtils.getAnnotation(ae, Cacheable.class);
		if (cache != null) {
			ops = lazyInit(ops);
			ops.add(parseCacheableAnnotation(ae, cache));
		}
		CacheEvict evict = AnnotationUtils.getAnnotation(ae, CacheEvict.class);
		if (evict != null) {
			ops = lazyInit(ops);
			ops.add(parseEvictAnnotation(ae, evict));
		}
		CachePut update = AnnotationUtils.getAnnotation(ae, CachePut.class);
		if (update != null) {
			ops = lazyInit(ops);
			ops.add(parseUpdateAnnotation(ae, update));
		}
		Caching caching = AnnotationUtils.getAnnotation(ae, Caching.class);
		if (caching != null) {
			ops = lazyInit(ops);
			ops.addAll(parseCachingAnnotation(ae, caching));
		}
		return ops;
	}

	private Collection<CacheOperation> lazyInit(Collection<CacheOperation> ops) {
		return (ops != null ? ops : new ArrayList<CacheOperation>(2));
	}

	CacheableOperation parseCacheableAnnotation(AnnotatedElement ae, Cacheable caching) {
		CacheableOperation cuo = new CacheableOperation();
		cuo.setCacheNames(caching.value());
		cuo.setCondition(caching.condition());
		cuo.setKey(caching.key());
		cuo.setName(ae.toString());
		return cuo;
	}

	CacheEvictOperation parseEvictAnnotation(AnnotatedElement ae, CacheEvict caching) {
		CacheEvictOperation ceo = new CacheEvictOperation();
		ceo.setCacheNames(caching.value());
		ceo.setCondition(caching.condition());
		ceo.setKey(caching.key());
		ceo.setCacheWide(caching.allEntries());
		ceo.setName(ae.toString());
		return ceo;
	}

	CacheOperation parseUpdateAnnotation(AnnotatedElement ae, CachePut caching) {
		CachePutOperation cuo = new CachePutOperation();
		cuo.setCacheNames(caching.value());
		cuo.setCondition(caching.condition());
		cuo.setKey(caching.key());
		cuo.setName(ae.toString());
		return cuo;
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
				ops.add(parseUpdateAnnotation(ae, update));
			}
		}

		return ops;
	}
}
