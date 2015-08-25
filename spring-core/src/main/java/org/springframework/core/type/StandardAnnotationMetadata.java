/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.core.type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.MultiValueMap;

/**
 * {@link AnnotationMetadata} implementation that uses standard reflection
 * to introspect a given {@link Class}.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 2.5
 */
public class StandardAnnotationMetadata extends StandardClassMetadata implements AnnotationMetadata {

	private final boolean nestedAnnotationsAsMap;


	/**
	 * Create a new {@code StandardAnnotationMetadata} wrapper for the given Class.
	 * @param introspectedClass the Class to introspect
	 * @see #StandardAnnotationMetadata(Class, boolean)
	 */
	public StandardAnnotationMetadata(Class<?> introspectedClass) {
		this(introspectedClass, false);
	}

	/**
	 * Create a new {@link StandardAnnotationMetadata} wrapper for the given Class,
	 * providing the option to return any nested annotations or annotation arrays in the
	 * form of {@link org.springframework.core.annotation.AnnotationAttributes} instead
	 * of actual {@link Annotation} instances.
	 * @param introspectedClass the Class to introspect
	 * @param nestedAnnotationsAsMap return nested annotations and annotation arrays as
	 * {@link org.springframework.core.annotation.AnnotationAttributes} for compatibility
	 * with ASM-based {@link AnnotationMetadata} implementations
	 * @since 3.1.1
	 */
	public StandardAnnotationMetadata(Class<?> introspectedClass, boolean nestedAnnotationsAsMap) {
		super(introspectedClass);
		this.nestedAnnotationsAsMap = nestedAnnotationsAsMap;
	}


	@Override
	public Set<String> getAnnotationTypes() {
		Set<String> types = new LinkedHashSet<String>();
		Annotation[] anns = getIntrospectedClass().getAnnotations();
		for (Annotation ann : anns) {
			types.add(ann.annotationType().getName());
		}
		return types;
	}

	@Override
	public Set<String> getMetaAnnotationTypes(String annotationName) {
		return AnnotatedElementUtils.getMetaAnnotationTypes(getIntrospectedClass(), annotationName);
	}

	@Override
	public boolean hasAnnotation(String annotationName) {
		Annotation[] anns = getIntrospectedClass().getAnnotations();
		for (Annotation ann : anns) {
			if (ann.annotationType().getName().equals(annotationName)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasMetaAnnotation(String annotationName) {
		return AnnotatedElementUtils.hasMetaAnnotationTypes(getIntrospectedClass(), annotationName);
	}

	@Override
	public boolean isAnnotated(String annotationName) {
		return AnnotatedElementUtils.isAnnotated(getIntrospectedClass(), annotationName);
	}

	@Override
	public Map<String, Object> getAnnotationAttributes(String annotationName) {
		return this.getAnnotationAttributes(annotationName, false);
	}

	@Override
	public Map<String, Object> getAnnotationAttributes(String annotationName, boolean classValuesAsString) {
		return AnnotatedElementUtils.getMergedAnnotationAttributes(getIntrospectedClass(),
				annotationName, classValuesAsString, this.nestedAnnotationsAsMap);
	}

	@Override
	public MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName) {
		return getAllAnnotationAttributes(annotationName, false);
	}

	@Override
	public MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName, boolean classValuesAsString) {
		return AnnotatedElementUtils.getAllAnnotationAttributes(getIntrospectedClass(),
				annotationName, classValuesAsString, this.nestedAnnotationsAsMap);
	}

	@Override
	public boolean hasAnnotatedMethods(String annotationName) {
		Method[] methods = getIntrospectedClass().getDeclaredMethods();
		for (Method method : methods) {
			if (!method.isBridge() && AnnotatedElementUtils.isAnnotated(method, annotationName)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Set<MethodMetadata> getAnnotatedMethods(String annotationName) {
		Method[] methods = getIntrospectedClass().getDeclaredMethods();
		Set<MethodMetadata> annotatedMethods = new LinkedHashSet<MethodMetadata>();
		for (Method method : methods) {
			if (!method.isBridge() && AnnotatedElementUtils.isAnnotated(method, annotationName)) {
				annotatedMethods.add(new StandardMethodMetadata(method, this.nestedAnnotationsAsMap));
			}
		}
		return annotatedMethods;
	}

}
