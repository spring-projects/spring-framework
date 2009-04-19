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
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

/**
 * @author Mark Pollack
 * @since 3.0
 */
public class StandardMethodMetadata implements MethodMetadata {

	private final Method introspectedMethod;
	
	public StandardMethodMetadata(Method method) {
		Assert.notNull(method, "Method must not be null");
		introspectedMethod = method;
	}
	
	public final Method getIntrospectedMethod() {
		return this.introspectedMethod;
	}
	

	public Map<String, Object> getAnnotationAttributes(String annotationType) {
		Annotation[] anns = getIntrospectedMethod().getAnnotations();
		for (Annotation ann : anns) {
			if (ann.annotationType().getName().equals(annotationType)) {
				return AnnotationUtils.getAnnotationAttributes(ann);
			}
		}
		return null;
	}

	public Set<String> getAnnotationTypes() {
		Set<String> types = new HashSet<String>();
		Annotation[] anns = getIntrospectedMethod().getAnnotations();
		for (Annotation ann : anns) {
			types.add(ann.annotationType().getName());
		}
		return types;
	}

	public String getMethodName() {
		return introspectedMethod.getName();
	}

	public int getModifiers() {
		return introspectedMethod.getModifiers();
	}

	public boolean hasAnnotation(String annotationType) {
		Annotation[] anns = getIntrospectedMethod().getAnnotations();
		for (Annotation ann : anns) {
			if (ann.annotationType().getName().equals(annotationType)) {
				return true;
			}
		}
		return false;
	}

	public boolean isStatic() {
		return Modifier.isStatic(getIntrospectedMethod().getModifiers());
	}

	public Set<String> getMetaAnnotationTypes(String annotationType) {
		Annotation[] anns = getIntrospectedMethod().getAnnotations();
		for (Annotation ann : anns) {
			if (ann.annotationType().getName().equals(annotationType)) {
				Set<String> types = new HashSet<String>();
				Annotation[] metaAnns = ann.annotationType().getAnnotations();
				for (Annotation meta : metaAnns) {
					types.add(meta.annotationType().getName());
				}
				return types;
			}
		}
		return null;
	}

	public boolean hasMetaAnnotation(String metaAnnotationType) {
		//TODO can refactor into shared (utility) method with StandardAnnotationMetadata
		Annotation[] anns = getIntrospectedMethod().getAnnotations();
		for (Annotation ann : anns) {
			Annotation[] metaAnns = ann.annotationType().getAnnotations();
			for (Annotation meta : metaAnns) {
				if (meta.annotationType().getName().equals(metaAnnotationType)) {
					return true;
				}
			}
		}
		return false;
	}

	public Set<String> getAnnotationTypesWithMetaAnnotation(String qualifierClassName) {
		// TODO Auto-generated method stub
		return null;
	}
	
	

}
