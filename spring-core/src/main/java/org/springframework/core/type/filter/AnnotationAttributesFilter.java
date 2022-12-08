/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.type.filter;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;


/**
 * A {@link TypeFilter} which matches classes with a given annotation and attributes.
 *
 * @author Shen Feng
 */

public class AnnotationAttributesFilter extends AnnotationTypeFilter {

	private final Map<String, Object> attributes;

	public AnnotationAttributesFilter(Class<? extends Annotation> annotationType) {
		super(annotationType);
		this.attributes = Collections.emptyMap();
	}

	public AnnotationAttributesFilter(Class<? extends Annotation> annotationType, Map<String, Object> attributes) {
		super(annotationType);
		this.attributes = attributes;
	}

	public AnnotationAttributesFilter(Class<? extends Annotation> annotationType, boolean considerMetaAnnotations) {
		super(annotationType, considerMetaAnnotations);
		this.attributes = Collections.emptyMap();
	}

	public AnnotationAttributesFilter(Class<? extends Annotation> annotationType, Map<String, Object> attributes, boolean considerMetaAnnotations) {
		super(annotationType, considerMetaAnnotations);
		this.attributes = attributes;
	}

	public AnnotationAttributesFilter(Class<? extends Annotation> annotationType, boolean considerMetaAnnotations, boolean considerInterfaces) {
		super(annotationType, considerMetaAnnotations, considerInterfaces);
		this.attributes = Collections.emptyMap();
	}

	public AnnotationAttributesFilter(Class<? extends Annotation> annotationType, Map<String, Object> attributes, boolean considerMetaAnnotations, boolean considerInterfaces) {
		super(annotationType, considerMetaAnnotations, considerInterfaces);
		this.attributes = attributes;
	}

	@Override
	protected boolean matchSelf(MetadataReader metadataReader) {
		if (!super.matchSelf(metadataReader)) {
			return false;
		}
		AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();

		Map<String, Object> annotationAttributes = annotationMetadata.getAnnotationAttributes(getAnnotationType().getName());
		return matchAttributes(annotationAttributes);


	}

	private boolean matchAttributes(Map<String, Object> annotationAttributes) {
		if (annotationAttributes == null) {
			return false;
		}
		Set<String> keys = this.attributes.keySet();
		for (String key : keys) {
			Object value = annotationAttributes.get(key);
			//use default value when value is null
			value = value == null ? AnnotationUtils.getDefaultValue(getAnnotationType(), key) : value;
			if (!Objects.equals(value, this.attributes.get(key))) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected Boolean matchSuperClass(String superClassName) {
		return this.match(superClassName);
	}

	@Override
	protected Boolean matchInterface(String interfaceName) {
		return this.match(interfaceName);
	}

	@Nullable
	protected Boolean match(String typeName) {
		if (Object.class.getName().equals(typeName)) {
			return false;
		}
		else if (typeName.startsWith("java")) {
			if (!getAnnotationType().getName().startsWith("java")) {
				// Standard Java types do not have non-standard annotations on them ->
				// skip any load attempt, in particular for Java language interfaces.
				return false;
			}
			try {
				Class<?> clazz = ClassUtils.forName(typeName, getClass().getClassLoader());
				Annotation annotation = this.considerMetaAnnotations ? AnnotationUtils.getAnnotation(clazz, getAnnotationType()) :
						clazz.getAnnotation(getAnnotationType());
				if (annotation == null) {
					return false;
				}
				Map<String, Object> annotationAttributes = AnnotationUtils.getAnnotationAttributes(annotation);
				return matchAttributes(annotationAttributes);
			}
			catch (Throwable ex) {
				// Class not regularly loadable - can't determine a match that way.
			}

		}
		return null;
	}

}
