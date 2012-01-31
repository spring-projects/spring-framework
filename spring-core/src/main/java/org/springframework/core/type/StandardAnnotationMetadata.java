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

package org.springframework.core.type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.annotation.AnnotationUtils;

/**
 * {@link AnnotationMetadata} implementation that uses standard reflection
 * to introspect a given <code>Class</code>.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.5
 */
public class StandardAnnotationMetadata extends StandardClassMetadata implements AnnotationMetadata {

	/**
	 * Create a new StandardAnnotationMetadata wrapper for the given Class.
	 * @param introspectedClass the Class to introspect
	 */
	public StandardAnnotationMetadata(Class introspectedClass) {
		super(introspectedClass);
	}


	public Set<String> getAnnotationTypes() {
		Set<String> types = new LinkedHashSet<String>();
		Annotation[] anns = getIntrospectedClass().getAnnotations();
		for (Annotation ann : anns) {
			types.add(ann.annotationType().getName());
		}
		return types;
	}

	public Set<String> getMetaAnnotationTypes(String annotationType) {
		Annotation[] anns = getIntrospectedClass().getAnnotations();
		for (Annotation ann : anns) {
			if (ann.annotationType().getName().equals(annotationType)) {
				Set<String> types = new LinkedHashSet<String>();
				Annotation[] metaAnns = ann.annotationType().getAnnotations();
				for (Annotation metaAnn : metaAnns) {
					types.add(metaAnn.annotationType().getName());
					for (Annotation metaMetaAnn : metaAnn.annotationType().getAnnotations()) {
						types.add(metaMetaAnn.annotationType().getName());
					}
				}
				return types;
			}
		}
		return null;
	}

	public boolean hasAnnotation(String annotationType) {
		Annotation[] anns = getIntrospectedClass().getAnnotations();
		for (Annotation ann : anns) {
			if (ann.annotationType().getName().equals(annotationType)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasMetaAnnotation(String annotationType) {
		Annotation[] anns = getIntrospectedClass().getAnnotations();
		for (Annotation ann : anns) {
			Annotation[] metaAnns = ann.annotationType().getAnnotations();
			for (Annotation metaAnn : metaAnns) {
				if (metaAnn.annotationType().getName().equals(annotationType)) {
					return true;
				}
				for (Annotation metaMetaAnn : metaAnn.annotationType().getAnnotations()) {
					if (metaMetaAnn.annotationType().getName().equals(annotationType)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean isAnnotated(String annotationType) {
		Annotation[] anns = getIntrospectedClass().getAnnotations();
		for (Annotation ann : anns) {
			if (ann.annotationType().getName().equals(annotationType)) {
				return true;
			}
			for (Annotation metaAnn : ann.annotationType().getAnnotations()) {
				if (metaAnn.annotationType().getName().equals(annotationType)) {
					return true;
				}
			}
		}
		return false;
	}

	public Map<String, Object> getAnnotationAttributes(String annotationType) {
		return getAnnotationAttributes(annotationType, false);
	}

	public Map<String, Object> getAnnotationAttributes(String annotationType, boolean classValuesAsString) {
		Annotation[] anns = getIntrospectedClass().getAnnotations();
		for (Annotation ann : anns) {
			if (ann.annotationType().getName().equals(annotationType)) {
				return AnnotationUtils.getAnnotationAttributes(ann, classValuesAsString);
			}
			for (Annotation metaAnn : ann.annotationType().getAnnotations()) {
				if (metaAnn.annotationType().getName().equals(annotationType)) {
					return AnnotationUtils.getAnnotationAttributes(metaAnn, classValuesAsString);
				}
			}
		}
		return null;
	}

	public boolean hasAnnotatedMethods(String annotationType) {
		Method[] methods = getIntrospectedClass().getDeclaredMethods();
		for (Method method : methods) {
			for (Annotation ann : method.getAnnotations()) {
				if (ann.annotationType().getName().equals(annotationType)) {
					return true;
				}
				else {
					for (Annotation metaAnn : ann.annotationType().getAnnotations()) {
						if (metaAnn.annotationType().getName().equals(annotationType)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public Set<MethodMetadata> getAnnotatedMethods(String annotationType) {
		Method[] methods = getIntrospectedClass().getDeclaredMethods();
		Set<MethodMetadata> annotatedMethods = new LinkedHashSet<MethodMetadata>();
		for (Method method : methods) {
			for (Annotation ann : method.getAnnotations()) {
				if (ann.annotationType().getName().equals(annotationType)) {
					annotatedMethods.add(new StandardMethodMetadata(method));
					break;
				}
				else {
					for (Annotation metaAnn : ann.annotationType().getAnnotations()) {
						if (metaAnn.annotationType().getName().equals(annotationType)) {
							annotatedMethods.add(new StandardMethodMetadata(method));
							break;
						}
					}
				}
			}
		}
		return annotatedMethods;
	}

}
