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

package org.springframework.beans.factory.config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.JdkVersion;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Descriptor for a specific dependency that is about to be injected.
 * Wraps a constructor parameter, a method parameter or a field,
 * allowing unified access to their metadata.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
public class DependencyDescriptor {

	private static final Method fieldAnnotationsMethod =
			ClassUtils.getMethodIfAvailable(Field.class, "getAnnotations", new Class[0]);


	private MethodParameter methodParameter;

	private Field field;

	private final boolean required;

	private final boolean eager;

	private Object[] fieldAnnotations;


	/**
	 * Create a new descriptor for a method or constructor parameter.
	 * Considers the dependency as 'eager'.
	 * @param methodParameter the MethodParameter to wrap
	 * @param required whether the dependency is required
	 */
	public DependencyDescriptor(MethodParameter methodParameter, boolean required) {
		this(methodParameter, required, true);
	}

	/**
	 * Create a new descriptor for a method or constructor parameter.
	 * @param methodParameter the MethodParameter to wrap
	 * @param required whether the dependency is required
	 * @param eager whether this dependency is 'eager' in the sense of
	 * eagerly resolving potential target beans for type matching
	 */
	public DependencyDescriptor(MethodParameter methodParameter, boolean required, boolean eager) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		this.methodParameter = methodParameter;
		this.required = required;
		this.eager = eager;
	}

	/**
	 * Create a new descriptor for a field.
	 * Considers the dependency as 'eager'.
	 * @param field the field to wrap
	 * @param required whether the dependency is required
	 */
	public DependencyDescriptor(Field field, boolean required) {
		this(field, required, true);
	}

	/**
	 * Create a new descriptor for a field.
	 * @param field the field to wrap
	 * @param required whether the dependency is required
	 * @param eager whether this dependency is 'eager' in the sense of
	 * eagerly resolving potential target beans for type matching
	 */
	public DependencyDescriptor(Field field, boolean required, boolean eager) {
		Assert.notNull(field, "Field must not be null");
		this.field = field;
		this.required = required;
		this.eager = eager;
	}


	/**
	 * Return the wrapped MethodParameter, if any.
	 * <p>Note: Either MethodParameter or Field is available.
	 * @return the MethodParameter, or <code>null</code> if none
	 */
	public MethodParameter getMethodParameter() {
		return this.methodParameter;
	}

	/**
	 * Return the wrapped Field, if any.
	 * <p>Note: Either MethodParameter or Field is available.
	 * @return the Field, or <code>null</code> if none
	 */
	public Field getField() {
		return this.field;
	}

	/**
	 * Return whether this dependency is required.
	 */
	public boolean isRequired() {
		return this.required;
	}

	/**
	 * Return whether this dependency is 'eager' in the sense of
	 * eagerly resolving potential target beans for type matching.
	 */
	public boolean isEager() {
		return this.eager;
	}


	/**
	 * Determine the declared (non-generic) type of the wrapped parameter/field.
	 * @return the declared type (never <code>null</code>)
	 */
	public Class getDependencyType() {
		return (this.field != null ? this.field.getType() : this.methodParameter.getParameterType());
	}

	/**
	 * Determine the generic element type of the wrapped Collection parameter/field, if any.
	 * @return the generic type, or <code>null</code> if none
	 */
	public Class getCollectionType() {
		if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_15) {
			return null;
		}
		return (this.field != null ?
				GenericCollectionTypeResolver.getCollectionFieldType(this.field) :
				GenericCollectionTypeResolver.getCollectionParameterType(this.methodParameter));
	}

	/**
	 * Determine the generic key type of the wrapped Map parameter/field, if any.
	 * @return the generic type, or <code>null</code> if none
	 */
	public Class getMapKeyType() {
		if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_15) {
			return null;
		}
		return (this.field != null ?
				GenericCollectionTypeResolver.getMapKeyFieldType(this.field) :
				GenericCollectionTypeResolver.getMapKeyParameterType(this.methodParameter));
	}

	/**
	 * Determine the generic value type of the wrapped Map parameter/field, if any.
	 * @return the generic type, or <code>null</code> if none
	 */
	public Class getMapValueType() {
		if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_15) {
			return null;
		}
		return (this.field != null ?
				GenericCollectionTypeResolver.getMapValueFieldType(this.field) :
				GenericCollectionTypeResolver.getMapValueParameterType(this.methodParameter));
	}

	/**
	 * Obtain the annotations associated with the wrapped parameter/field, if any.
	 * @return the parameter/field annotations, or <code>null</code> if there is
	 * no annotation support (on JDK < 1.5). The return value is an Object array
	 * instead of an Annotation array simply for compatibility with older JDKs;
	 * feel free to cast it to <code>Annotation[]</code> on JDK 1.5 or higher.
	 */
	public Object[] getAnnotations() {
		if (this.field != null) {
			if (this.fieldAnnotations != null) {
				return this.fieldAnnotations;
			}
			if (fieldAnnotationsMethod == null) {
				return null;
			}
			this.fieldAnnotations = (Object[]) ReflectionUtils.invokeMethod(fieldAnnotationsMethod, this.field);
			return this.fieldAnnotations;
		}
		else {
			return this.methodParameter.getParameterAnnotations();
		}
	}

}
