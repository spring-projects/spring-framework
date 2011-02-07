/*
 * Copyright 2010 the original author or authors.
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

import org.springframework.cache.interceptor.CacheDefinition;
import org.springframework.cache.interceptor.CacheInvalidateDefinition;
import org.springframework.cache.interceptor.CacheUpdateDefinition;
import org.springframework.cache.interceptor.DefaultCacheInvalidateDefinition;
import org.springframework.cache.interceptor.DefaultCacheUpdateDefinition;

/**
 * Strategy implementation for parsing Spring's {@link Cacheable} and {@link CacheEvict} annotations.
 * 
 * @author Costin Leau
 */
@SuppressWarnings("serial")
public class SpringCachingAnnotationParser implements CacheAnnotationParser, Serializable {

	public CacheDefinition parseCacheAnnotation(AnnotatedElement ae) {
		Cacheable update = findAnnotation(ae, Cacheable.class);

		if (update != null) {
			return parseCacheableAnnotation(ae, update);
		}

		CacheEvict invalidate = findAnnotation(ae, CacheEvict.class);

		if (invalidate != null) {
			return parseInvalidateAnnotation(ae, invalidate);
		}

		return null;
	}

	private <T extends Annotation> T findAnnotation(AnnotatedElement ae, Class<T> annotationType) {
		T ann = ae.getAnnotation(annotationType);
		if (ann == null) {
			for (Annotation metaAnn : ae.getAnnotations()) {
				ann = metaAnn.annotationType().getAnnotation(annotationType);
				if (ann != null) {
					break;
				}
			}
		}
		return ann;
	}

	CacheUpdateDefinition parseCacheableAnnotation(AnnotatedElement target, Cacheable ann) {
		DefaultCacheUpdateDefinition dcud = new DefaultCacheUpdateDefinition();
		dcud.setCacheNames(ann.value());
		dcud.setCondition(ann.condition());
		dcud.setKey(ann.key());
		dcud.setName(target.toString());

		return dcud;
	}

	CacheInvalidateDefinition parseInvalidateAnnotation(AnnotatedElement target, CacheEvict ann) {
		DefaultCacheInvalidateDefinition dcid = new DefaultCacheInvalidateDefinition();
		dcid.setCacheNames(ann.value());
		dcid.setCondition(ann.condition());
		dcid.setKey(ann.key());
		dcid.setCacheWide(ann.allEntries());
		dcid.setName(target.toString());
		
		return dcid;
	}
}