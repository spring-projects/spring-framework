/*
 * Copyright 2002-2013 the original author or authors.
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.SpringAsmInfo;
import org.springframework.asm.Type;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.1.1
 */
abstract class AbstractRecursiveAnnotationVisitor extends AnnotationVisitor {

	protected final MetadataReaderLog logger;

	protected final AnnotationAttributes attributes;

	protected final ClassLoader classLoader;


	public AbstractRecursiveAnnotationVisitor(ClassLoader classLoader,
			AnnotationAttributes attributes, MetadataReaderLog logger) {
		super(SpringAsmInfo.ASM_VERSION);
		this.classLoader = classLoader;
		this.attributes = attributes;
		this.logger = (logger == null ? new MetadataReaderLogAdapter() : logger);
	}


	public void visit(String attributeName, Object attributeValue) {
		this.attributes.put(attributeName, attributeValue);
	}

	public AnnotationVisitor visitAnnotation(String attributeName, String asmTypeDescriptor) {
		String annotationType = Type.getType(asmTypeDescriptor).getClassName();
		AnnotationAttributes nestedAttributes = new AnnotationAttributes();
		this.attributes.put(attributeName, nestedAttributes);
		return new RecursiveAnnotationAttributesVisitor(annotationType, nestedAttributes,
				this.classLoader, this.logger);
	}

	public AnnotationVisitor visitArray(String attributeName) {
		return new RecursiveAnnotationArrayVisitor(attributeName, this.attributes,
				this.classLoader, this.logger);
	}

	public void visitEnum(String attributeName, String asmTypeDescriptor, String attributeValue) {
		Object valueToUse = attributeValue;
		try {
			Class<?> enumType = this.classLoader.loadClass(Type.getType(asmTypeDescriptor).getClassName());
			Field enumConstant = ReflectionUtils.findField(enumType, attributeValue);
			if (enumConstant != null) {
				valueToUse = enumConstant.get(null);
			}
		}
		catch (ClassNotFoundException ex) {
			this.logger.log("Failed to classload enum type while reading annotation metadata", ex);
		}
		catch (IllegalAccessException ex) {
			this.logger.log("Could not access enum value while reading annotation metadata", ex);
		}
		this.attributes.put(attributeName, valueToUse);
	}
}


/**
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1.1
 */
final class RecursiveAnnotationArrayVisitor extends AbstractRecursiveAnnotationVisitor {

	private final String attributeName;

	private final List<AnnotationAttributes> allNestedAttributes = new ArrayList<AnnotationAttributes>();


	public RecursiveAnnotationArrayVisitor(String attributeName,
			AnnotationAttributes attributes, ClassLoader classLoader,
			MetadataReaderLog logger) {
		super(classLoader, attributes, logger);
		this.attributeName = attributeName;
	}

	@Override
	public void visit(String attributeName, Object attributeValue) {
		Object newValue = attributeValue;
		Object existingValue = this.attributes.get(this.attributeName);
		if (existingValue != null) {
			newValue = ObjectUtils.addObjectToArray((Object[]) existingValue, newValue);
		}
		else {
			Object[] newArray = (Object[]) Array.newInstance(newValue.getClass(), 1);
			newArray[0] = newValue;
			newValue = newArray;
		}
		this.attributes.put(this.attributeName, newValue);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String attributeName, String asmTypeDescriptor) {
		String annotationType = Type.getType(asmTypeDescriptor).getClassName();
		AnnotationAttributes nestedAttributes = new AnnotationAttributes();
		this.allNestedAttributes.add(nestedAttributes);
		return new RecursiveAnnotationAttributesVisitor(annotationType, nestedAttributes,
				this.classLoader, this.logger);
	}

	public void visitEnd() {
		if (!this.allNestedAttributes.isEmpty()) {
			this.attributes.put(this.attributeName, this.allNestedAttributes.toArray(
					new AnnotationAttributes[this.allNestedAttributes.size()]));
		}
	}
}


/**
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1.1
 */
class RecursiveAnnotationAttributesVisitor extends AbstractRecursiveAnnotationVisitor {

	private final String annotationType;


	public RecursiveAnnotationAttributesVisitor(String annotationType,
			AnnotationAttributes attributes, ClassLoader classLoader,
			MetadataReaderLog logger) {
		super(classLoader, attributes, logger);
		this.annotationType = annotationType;
	}


	public final void visitEnd() {
		try {
			Class<?> annotationClass = this.classLoader.loadClass(this.annotationType);
			this.doVisitEnd(annotationClass);
		}
		catch (ClassNotFoundException ex) {
			this.logger.log("Failed to classload type while reading annotation " +
					"metadata. This is a non-fatal error, but certain annotation " +
					"metadata may be unavailable.", ex);
		}
	}

	protected void doVisitEnd(Class<?> annotationClass) {
		registerDefaultValues(annotationClass);
	}

	private void registerDefaultValues(Class<?> annotationClass) {
		// Check declared default values of attributes in the annotation type.
		Method[] annotationAttributes = annotationClass.getMethods();
		for (Method annotationAttribute : annotationAttributes) {
			String attributeName = annotationAttribute.getName();
			Object defaultValue = annotationAttribute.getDefaultValue();
			if (defaultValue != null && !this.attributes.containsKey(attributeName)) {
				if (defaultValue instanceof Annotation) {
					defaultValue = AnnotationAttributes.fromMap(
							AnnotationUtils.getAnnotationAttributes((Annotation)defaultValue, false, true));
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
 * @since 3.0
 */
final class AnnotationAttributesReadingVisitor extends RecursiveAnnotationAttributesVisitor {

	private final String annotationType;

	private final MultiValueMap<String, AnnotationAttributes> attributesMap;

	private final Map<String, Set<String>> metaAnnotationMap;


	public AnnotationAttributesReadingVisitor(String annotationType,
			MultiValueMap<String, AnnotationAttributes> attributesMap,
			Map<String, Set<String>> metaAnnotationMap, ClassLoader classLoader,
			MetadataReaderLog logger) {
		super(annotationType, new AnnotationAttributes(), classLoader, logger);
		this.annotationType = annotationType;
		this.attributesMap = attributesMap;
		this.metaAnnotationMap = metaAnnotationMap;
	}

	@Override
	public void doVisitEnd(Class<?> annotationClass) {
		super.doVisitEnd(annotationClass);
		List<AnnotationAttributes> attributes = this.attributesMap.get(this.annotationType);
		if(attributes == null) {
			this.attributesMap.add(this.annotationType, this.attributes);
		} else {
			attributes.add(0, this.attributes);
		}
		Set<String> metaAnnotationTypeNames = new LinkedHashSet<String>();
		for (Annotation metaAnnotation : annotationClass.getAnnotations()) {
			recusivelyCollectMetaAnnotations(metaAnnotationTypeNames, metaAnnotation);
		}
		if (this.metaAnnotationMap != null) {
			this.metaAnnotationMap.put(annotationClass.getName(), metaAnnotationTypeNames);
		}
	}



	private void recusivelyCollectMetaAnnotations(Set<String> visited, Annotation annotation) {
		if(visited.add(annotation.annotationType().getName())) {
			this.attributesMap.add(annotation.annotationType().getName(),
					AnnotationUtils.getAnnotationAttributes(annotation, true, true));
			for (Annotation metaMetaAnnotation : annotation.annotationType().getAnnotations()) {
				recusivelyCollectMetaAnnotations(visited, metaMetaAnnotation);
			}
		}
	}
}


class MetadataReaderLogAdapter implements MetadataReaderLog {

	private Log logger = LogFactory.getLog(AnnotationAttributesReadingVisitor.class);

	@Override
	public void log(String message, Throwable t) {
		logger.debug(message, t);
	}
}
