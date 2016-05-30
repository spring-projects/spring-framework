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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1.1
 */
class RecursiveAnnotationAttributesVisitor extends AbstractRecursiveAnnotationVisitor {

	private final String annotationType;


	public RecursiveAnnotationAttributesVisitor(
			String annotationType, AnnotationAttributes attributes, ClassLoader classLoader) {

		super(classLoader, attributes);
		this.annotationType = annotationType;
	}


	@Override
	public final void visitEnd() {
		try {
			Class<?> annotationClass = this.classLoader.loadClass(this.annotationType);
			doVisitEnd(annotationClass);
		}
		catch (ClassNotFoundException ex) {
			logger.debug("Failed to class-load type while reading annotation metadata. " +
					"This is a non-fatal error, but certain annotation metadata may be unavailable.", ex);
		}
	}

	protected void doVisitEnd(Class<?> annotationClass) {
		registerDefaultValues(annotationClass);
	}

	private void registerDefaultValues(Class<?> annotationClass) {
		// Only do defaults scanning for public annotations; we'd run into
		// IllegalAccessExceptions otherwise, and we don't want to mess with
		// accessibility in a SecurityManager environment.
		if (Modifier.isPublic(annotationClass.getModifiers())) {
			// Check declared default values of attributes in the annotation type.
			Method[] annotationAttributes = annotationClass.getMethods();
			for (Method annotationAttribute : annotationAttributes) {
				String attributeName = annotationAttribute.getName();
				Object defaultValue = annotationAttribute.getDefaultValue();
				if (defaultValue != null && !this.attributes.containsKey(attributeName)) {
					if (defaultValue instanceof Annotation) {
						defaultValue = AnnotationAttributes.fromMap(AnnotationUtils.getAnnotationAttributes(
								(Annotation) defaultValue, false, true));
					}
					else if (defaultValue instanceof Annotation[]) {
						Annotation[] realAnnotations = (Annotation[]) defaultValue;
						AnnotationAttributes[] mappedAnnotations = new AnnotationAttributes[realAnnotations.length];
						for (int i = 0; i < realAnnotations.length; i++) {
							mappedAnnotations[i] = AnnotationAttributes.fromMap(
									AnnotationUtils.getAnnotationAttributes(realAnnotations[i], false, true));
						}
						defaultValue = mappedAnnotations;
					}
					this.attributes.put(attributeName, defaultValue);
				}
			}
		}
	}

}
