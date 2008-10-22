/*
 * Copyright 2002-2007 the original author or authors.
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
import java.util.HashSet;
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

	public StandardAnnotationMetadata(Class introspectedClass) {
		super(introspectedClass);
	}


	public Set<String> getAnnotationTypes() {
		Set<String> types = new HashSet<String>();
		Annotation[] anns = getIntrospectedClass().getAnnotations();
		for (int i = 0; i < anns.length; i++) {
			types.add(anns[i].annotationType().getName());
		}
		return types;
	}

	public boolean hasAnnotation(String annotationType) {
		Annotation[] anns = getIntrospectedClass().getAnnotations();
		for (int i = 0; i < anns.length; i++) {
			if (anns[i].annotationType().getName().equals(annotationType)) {
				return true;
			}
		}
		return false;
	}

	public Set<String> getMetaAnnotationTypes(String annotationType) {
		Annotation[] anns = getIntrospectedClass().getAnnotations();
		for (int i = 0; i < anns.length; i++) {
			if (anns[i].annotationType().getName().equals(annotationType)) {
				Set<String> types = new HashSet<String>();
				Annotation[] metaAnns = anns[i].annotationType().getAnnotations();
				for (Annotation meta : metaAnns) {
					types.add(meta.annotationType().getName());
				}
				return types;
			}
		}
		return null;
	}

	public boolean hasMetaAnnotation(String annotationType) {
		Annotation[] anns = getIntrospectedClass().getAnnotations();
		for (int i = 0; i < anns.length; i++) {
			Annotation[] metaAnns = anns[i].annotationType().getAnnotations();
			for (Annotation meta : metaAnns) {
				if (meta.annotationType().getName().equals(annotationType)) {
					return true;
				}
			}
		}
		return false;
	}

	public Map<String, Object> getAnnotationAttributes(String annotationType) {
		Annotation[] anns = getIntrospectedClass().getAnnotations();
		for (int i = 0; i < anns.length; i++) {
			Annotation ann = anns[i];
			if (ann.annotationType().getName().equals(annotationType)) {
				return AnnotationUtils.getAnnotationAttributes(ann);
			}
		}
		return null;
	}

}
