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
import java.lang.reflect.Modifier;
import java.util.Map;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

/**
 * {@link MethodMetadata} implementation that uses standard reflection
 * to introspect a given <code>Method</code>.
 *
 * @author Juergen Hoeller
 * @author Mark Pollack
 * @author Chris Beams
 * @since 3.0
 */
public class StandardMethodMetadata implements MethodMetadata {

	private final Method introspectedMethod;


	/**
	 * Create a new StandardMethodMetadata wrapper for the given Method.
	 * @param introspectedMethod the Method to introspect
	 */
	public StandardMethodMetadata(Method introspectedMethod) {
		Assert.notNull(introspectedMethod, "Method must not be null");
		this.introspectedMethod = introspectedMethod;
	}

	/**
	 * Return the underlying Method.
	 */
	public final Method getIntrospectedMethod() {
		return this.introspectedMethod;
	}

	
	public String getMethodName() {
		return this.introspectedMethod.getName();
	}
	
	public String getDeclaringClassName() {
		return this.introspectedMethod.getDeclaringClass().getName();
	}

	public boolean isStatic() {
		return Modifier.isStatic(this.introspectedMethod.getModifiers());
	}

	public boolean isFinal() {
		return Modifier.isFinal(this.introspectedMethod.getModifiers());
	}

	public boolean isOverridable() {
		return (!isStatic() && !isFinal() && !Modifier.isPrivate(this.introspectedMethod.getModifiers()));
	}

	public boolean isAnnotated(String annotationType) {
		Annotation[] anns = this.introspectedMethod.getAnnotations();
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
		Annotation[] anns = this.introspectedMethod.getAnnotations();
		for (Annotation ann : anns) {
			if (ann.annotationType().getName().equals(annotationType)) {
				return AnnotationUtils.getAnnotationAttributes(ann, true);
			}
			for (Annotation metaAnn : ann.annotationType().getAnnotations()) {
				if (metaAnn.annotationType().getName().equals(annotationType)) {
					return AnnotationUtils.getAnnotationAttributes(metaAnn, true);
				}
			}
		}
		return null;
	}

}
