/*
 * Copyright 2002-2009 the original author or authors.
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
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Type;
import org.springframework.asm.commons.EmptyVisitor;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

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

	private final Set<MethodMetadata> methodMetadataSet = new LinkedHashSet<MethodMetadata>();

	private final ClassLoader classLoader;


	public AnnotationMetadataReadingVisitor(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodMetadataReadingVisitor md = new MethodMetadataReadingVisitor(classLoader, name, access);
		methodMetadataSet.add(md);
		return md;
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
		final String className = Type.getType(desc).getClassName();
		final Map<String, Object> attributes = new LinkedHashMap<String, Object>();
		return new AnnotationVisitor() {
			public void visit(String name, Object value) {
				// Explicitly defined annotation attribute value.
				Object valueToUse = value;
				if (value instanceof Type) {
					try {
						valueToUse = classLoader.loadClass(((Type) value).getClassName());
					}
					catch (ClassNotFoundException ex) {
						// Class not found - can't resolve class reference in annotation attribute.
					}
				}
				attributes.put(name, valueToUse);
			}
			public void visitEnum(String name, String desc, String value) {
				Object valueToUse = value;
				try {
					Class<?> enumType = classLoader.loadClass(Type.getType(desc).getClassName());
					Field enumConstant = ReflectionUtils.findField(enumType, value);
					if (enumConstant != null) {
						valueToUse = enumConstant.get(null);
					}
				}
				catch (Exception ex) {
					// Class not found - can't resolve class reference in annotation attribute.
				}
				attributes.put(name, valueToUse);
			}
			public AnnotationVisitor visitAnnotation(String name, String desc) {
				return new EmptyVisitor();
			}
			public AnnotationVisitor visitArray(final String attrName) {
				return new AnnotationVisitor() {
					public void visit(String name, Object value) {
						Object newValue = value;
						Object existingValue = attributes.get(attrName);
						if (existingValue != null) {
							newValue = ObjectUtils.addObjectToArray((Object[]) existingValue, newValue);
						}
						else {
							Object[] newArray = (Object[]) Array.newInstance(newValue.getClass(), 1);
							newArray[0] = newValue;
							newValue = newArray;
						}
						attributes.put(attrName, newValue);
					}
					public void visitEnum(String name, String desc, String value) {
					}
					public AnnotationVisitor visitAnnotation(String name, String desc) {
						return new EmptyVisitor();
					}
					public AnnotationVisitor visitArray(String name) {
						return new EmptyVisitor();
					}
					public void visitEnd() {
					}
				};
			}
			public void visitEnd() {
				try {
					Class<?> annotationClass = classLoader.loadClass(className);
					// Check declared default values of attributes in the annotation type.
					Method[] annotationAttributes = annotationClass.getMethods();
					for (Method annotationAttribute : annotationAttributes) {
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


	public Set<MethodMetadata> getAnnotatedMethods(String annotationType) {
		Set<MethodMetadata> annotatedMethods = new LinkedHashSet<MethodMetadata>();
		for (MethodMetadata method : methodMetadataSet) {
			if (method.hasAnnotation(annotationType))
			{
				annotatedMethods.add(method);
			}
		}
		return annotatedMethods;
	}

}
