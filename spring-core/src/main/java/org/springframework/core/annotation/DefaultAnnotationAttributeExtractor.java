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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.util.ReflectionUtils;

/**
 * Default implementation of the {@link AnnotationAttributeExtractor} strategy
 * that is backed by an {@link Annotation}.
 *
 * @author Sam Brannen
 * @since 4.2
 * @see Annotation
 * @see AliasFor
 * @see AbstractAliasAwareAnnotationAttributeExtractor
 * @see MapAnnotationAttributeExtractor
 * @see AnnotationUtils#synthesizeAnnotation
 */
class DefaultAnnotationAttributeExtractor extends AbstractAliasAwareAnnotationAttributeExtractor<Annotation> {

	/**
	 * Construct a new {@code DefaultAnnotationAttributeExtractor}.
	 * @param annotation the annotation to synthesize; never {@code null}
	 * @param annotatedElement the element that is annotated with the supplied
	 * annotation; may be {@code null} if unknown
	 */
	DefaultAnnotationAttributeExtractor(Annotation annotation, Object annotatedElement) {
		super(annotation.annotationType(), annotatedElement, annotation);
	}


	@Override
	protected Object getRawAttributeValue(Method attributeMethod) {
		ReflectionUtils.makeAccessible(attributeMethod);
		return ReflectionUtils.invokeMethod(attributeMethod, getSource());
	}

	@Override
	protected Object getRawAttributeValue(String attributeName) {
		Method attributeMethod = ReflectionUtils.findMethod(getAnnotationType(), attributeName);
		return getRawAttributeValue(attributeMethod);
	}

}
