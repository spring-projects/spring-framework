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

package org.springframework.core.type.classreading;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * ASM visitor which looks for the annotations defined on a class or method, including
 * tracking meta-annotations.
 *
 * <p>As of Spring 3.1.1, this visitor is fully recursive, taking into account any nested
 * annotations or nested annotation arrays. These annotations are in turn read into
 * {@link AnnotationAttributes} map structures.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.0
 */
final class AnnotationAttributesReadingVisitor extends RecursiveAnnotationAttributesVisitor {

	private final String annotationType;

	private final MultiValueMap<String, AnnotationAttributes> attributesMap;

	private final Map<String, Set<String>> metaAnnotationMap;


	public AnnotationAttributesReadingVisitor(String annotationType,
			MultiValueMap<String, AnnotationAttributes> attributesMap, Map<String, Set<String>> metaAnnotationMap,
			ClassLoader classLoader) {

		super(annotationType, new AnnotationAttributes(), classLoader);
		this.annotationType = annotationType;
		this.attributesMap = attributesMap;
		this.metaAnnotationMap = metaAnnotationMap;
	}


	@Override
	public void doVisitEnd(Class<?> annotationClass) {
		super.doVisitEnd(annotationClass);
		List<AnnotationAttributes> attributes = this.attributesMap.get(this.annotationType);
		if (attributes == null) {
			this.attributesMap.add(this.annotationType, this.attributes);
		}
		else {
			attributes.add(0, this.attributes);
		}
		Set<Annotation> visited = new LinkedHashSet<>();
		Annotation[] metaAnnotations = AnnotationUtils.getAnnotations(annotationClass);
		if (!ObjectUtils.isEmpty(metaAnnotations)) {
			for (Annotation metaAnnotation : metaAnnotations) {
				if (!AnnotationUtils.isInJavaLangAnnotationPackage(metaAnnotation)) {
					recursivelyCollectMetaAnnotations(visited, metaAnnotation);
				}
			}
		}
		if (this.metaAnnotationMap != null) {
			Set<String> metaAnnotationTypeNames = new LinkedHashSet<>(visited.size());
			for (Annotation ann : visited) {
				metaAnnotationTypeNames.add(ann.annotationType().getName());
			}
			this.metaAnnotationMap.put(annotationClass.getName(), metaAnnotationTypeNames);
		}
	}

	private void recursivelyCollectMetaAnnotations(Set<Annotation> visited, Annotation annotation) {
		if (!AnnotationUtils.isInJavaLangAnnotationPackage(annotation) && visited.add(annotation)) {
			try {
				// Only do attribute scanning for public annotations; we'd run into
				// IllegalAccessExceptions otherwise, and we don't want to mess with
				// accessibility in a SecurityManager environment.
				if (Modifier.isPublic(annotation.annotationType().getModifiers())) {
					String annotationName = annotation.annotationType().getName();
					this.attributesMap.add(annotationName, AnnotationUtils.getAnnotationAttributes(annotation, false, true));
				}
				for (Annotation metaMetaAnnotation : annotation.annotationType().getAnnotations()) {
					recursivelyCollectMetaAnnotations(visited, metaMetaAnnotation);
				}
			}
			catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to introspect meta-annotations on [" + annotation + "]: " + ex);
				}
			}
		}
	}

}
