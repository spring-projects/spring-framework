/*
 * Copyright 2002-2008 the original author or authors.
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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;

import org.springframework.core.type.AnnotationMetadata;

/**
 * ASM class visitor which looks for the class name and implemented types as
 * well as for the annotations defined on the class, exposing them through
 * the {@link org.springframework.core.type.AnnotationMetadata} interface.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.5
 */
class AnnotationMetadataReadingVisitor extends ClassMetadataReadingVisitor implements AnnotationMetadata {

	private final Map<String, Map<String, Object>> attributesMap = new LinkedHashMap<String, Map<String, Object>>();

	private final Map<String, Set<String>> metaAnnotationMap = new LinkedHashMap<String, Set<String>>();


	private final ClassLoader classLoader;


	public AnnotationMetadataReadingVisitor(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	public AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
		final String className = Type.getType(desc).getClassName();
		final Map<String, Object> attributes = new LinkedHashMap<String, Object>();
		return new EmptyVisitor() {
			public void visit(String name, Object value) {
				// Explicitly defined annotation attribute value.
				attributes.put(name, value);
			}
			public void visitEnd() {
				try {
					Class annotationClass = classLoader.loadClass(className);
					// Check declared default values of attributes in the annotation type.
					Method[] annotationAttributes = annotationClass.getMethods();
					for (int i = 0; i < annotationAttributes.length; i++) {
						Method annotationAttribute = annotationAttributes[i];
						String attributeName = annotationAttribute.getName();
						Object defaultValue = annotationAttribute.getDefaultValue();
						if (defaultValue != null && !attributes.containsKey(attributeName)) {
							attributes.put(attributeName, defaultValue);
						}
					}
					// Register annotations that the annotation type is annotated with.
					Annotation[] metaAnnotations = annotationClass.getAnnotations();
					Set<String> metaAnnotationTypeNames = new HashSet<String>();
					for (Annotation metaAnnotation : metaAnnotations) {
						metaAnnotationTypeNames.add(metaAnnotation.annotationType().getName());
					}
					metaAnnotationMap.put(className, metaAnnotationTypeNames);
				}
				catch (ClassNotFoundException ex) {
					// Class not found - can't determine meta-annotations.
				}
				attributesMap.put(className, attributes);
			}
		};
	}


	public Set<String> getAnnotationTypes() {
		return this.attributesMap.keySet();
	}

	public boolean hasAnnotation(String annotationType) {
		return this.attributesMap.containsKey(annotationType);
	}

	public Set<String> getMetaAnnotationTypes(String annotationType) {
		return this.metaAnnotationMap.get(annotationType);
	}

	public boolean hasMetaAnnotation(String metaAnnotationType) {
		Collection<Set<String>> allMetaTypes = this.metaAnnotationMap.values();
		for (Set<String> metaTypes : allMetaTypes) {
			if (metaTypes.contains(metaAnnotationType)) {
				return true;
			}
		}
		return false;
	}

	public Map<String, Object> getAnnotationAttributes(String annotationType) {
		return this.attributesMap.get(annotationType);
	}

}
