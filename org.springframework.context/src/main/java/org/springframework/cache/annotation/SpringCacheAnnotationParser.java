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

import org.springframework.cache.interceptor.CacheEvictOperation;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CacheUpdateOperation;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * Strategy implementation for parsing Spring's {@link Cacheable} and {@link CacheEvict} annotations.
 * 
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.1
 */
@SuppressWarnings("serial")
public class SpringCacheAnnotationParser implements CacheAnnotationParser, Serializable {

	public CacheOperation parseCacheAnnotation(AnnotatedElement ae) {
		Cacheable update = AnnotationUtils.getAnnotation(ae, Cacheable.class);
		if (update != null) {
			return parseCacheableAnnotation(ae, update);
		}
		CacheEvict evict = AnnotationUtils.getAnnotation(ae, CacheEvict.class);
		if (evict != null) {
			return parseEvictAnnotation(ae, evict);
		}
		return null;
	}

	CacheUpdateOperation parseCacheableAnnotation(AnnotatedElement ae, Cacheable ann) {
		CacheUpdateOperation cuo = new CacheUpdateOperation();
		cuo.setCacheNames(ann.value());
		cuo.setCondition(ann.condition());
		cuo.setKey(ann.key());
		cuo.setName(ae.toString());
		return cuo;
	}

	CacheEvictOperation parseEvictAnnotation(AnnotatedElement ae, CacheEvict ann) {
		CacheEvictOperation ceo = new CacheEvictOperation();
		ceo.setCacheNames(ann.value());
		ceo.setCondition(ann.condition());
		ceo.setKey(ann.key());
		ceo.setCacheWide(ann.allEntries());
		ceo.setName(ae.toString());
		return ceo;
	}

}
