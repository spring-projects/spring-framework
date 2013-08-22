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

package org.springframework.beans.factory.config;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.type.ClassTypeInformation;
import org.springframework.beans.type.TypeInformation;
import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.Assert;

/**
 * Descriptor for a specific dependency that is about to be injected.
 * Wraps a constructor parameter, a method parameter or a field,
 * allowing unified access to their metadata.
 *
 * @author Juergen Hoeller
 * @author Oliver Gierke
 * @since 2.5
 */
@SuppressWarnings("serial")
public class DependencyDescriptor implements Serializable {

	private transient MethodParameter methodParameter;

	private transient Field field;

	private Class declaringClass;

	private Class<?> sourceBeanClass;

	private transient TypeInformation<?> owningTypeInformation;

	private String methodName;

	private Class[] parameterTypes;

	private int parameterIndex;

	private String fieldName;

	private final boolean required;

	private final boolean eager;

	private int nestingLevel = 1;

	private transient Annotation[] fieldAnnotations;


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
		this(methodParameter, required, eager, methodParameter.getDeclaringClass());
	}

	/**
	 * Create a new descriptor for a method or constructor parameter as well as the source bean class which is used to
	 * resolve generic type information if necessary.
	 *
	 * @param methodParameter the {@link MethodParameter} to wrap
	 * @param required whether the dependency is required
	 * @param eager whether this dependency is 'eager' in the sense of eagerly resolving potential target beans for type
	 *          matching
	 * @param sourceBeanClass the owning class of the dependency descriptor
	 */
	public DependencyDescriptor(MethodParameter methodParameter, boolean required, boolean eager, Class<?> sourceBeanClass) {

		Assert.notNull(methodParameter, "MethodParameter must not be null");
		this.methodParameter = methodParameter;
		this.declaringClass = methodParameter.getDeclaringClass();
		if (this.methodParameter.getMethod() != null) {
			this.methodName = methodParameter.getMethod().getName();
			this.parameterTypes = methodParameter.getMethod().getParameterTypes();
		}
		else {
			this.parameterTypes = methodParameter.getConstructor().getParameterTypes();
		}
		this.parameterIndex = methodParameter.getParameterIndex();
		this.required = required;
		this.eager = eager;
		this.sourceBeanClass = sourceBeanClass;
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
		this(field, required, eager, field.getDeclaringClass());
	}

	/**
	 * Create a new descriptor for a field.
	 *
	 * @param field the field to wrap
	 * @param required whether the dependency is required
	 * @param eager whether this dependency is 'eager' in the sense of
	 * eagerly resolving potential target beans for type matching
	 * @param sourceBeanClass the owning class of the dependency descriptor
	 */
	public DependencyDescriptor(Field field, boolean required, boolean eager, Class<?> sourceBeanClass) {

		Assert.notNull(field, "Field must not be null");
		this.field = field;
		this.declaringClass = field.getDeclaringClass();
		this.fieldName = field.getName();
		this.required = required;
		this.eager = eager;
		this.sourceBeanClass = sourceBeanClass;
	}

	/**
	 * Copy constructor.
	 * @param original the original descriptor to create a copy from
	 */
	public DependencyDescriptor(DependencyDescriptor original) {
		this.methodParameter = (original.methodParameter != null ? new MethodParameter(original.methodParameter) : null);
		this.field = original.field;
		this.declaringClass = original.declaringClass;
		this.methodName = original.methodName;
		this.parameterTypes = original.parameterTypes;
		this.parameterIndex = original.parameterIndex;
		this.fieldName = original.fieldName;
		this.required = original.required;
		this.eager = original.eager;
		this.nestingLevel = original.nestingLevel;
		this.fieldAnnotations = original.fieldAnnotations;
		this.sourceBeanClass = original.sourceBeanClass;
		this.owningTypeInformation = original.owningTypeInformation;
	}


	/**
	 * Return the wrapped MethodParameter, if any.
	 * <p>Note: Either MethodParameter or Field is available.
	 * @return the MethodParameter, or {@code null} if none
	 */
	public MethodParameter getMethodParameter() {
		return this.methodParameter;
	}

	/**
	 * Return the wrapped Field, if any.
	 * <p>Note: Either MethodParameter or Field is available.
	 * @return the Field, or {@code null} if none
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
	 * Increase this descriptor's nesting level.
	 * @see MethodParameter#increaseNestingLevel()
	 */
	public void increaseNestingLevel() {
		this.nestingLevel++;
		if (this.methodParameter != null) {
			this.methodParameter.increaseNestingLevel();
		}
	}

	/**
	 * Initialize parameter name discovery for the underlying method parameter, if any.
	 * <p>This method does not actually try to retrieve the parameter name at
	 * this point; it just allows discovery to happen when the application calls
	 * {@link #getDependencyName()} (if ever).
	 */
	public void initParameterNameDiscovery(ParameterNameDiscoverer parameterNameDiscoverer) {
		if (this.methodParameter != null) {
			this.methodParameter.initParameterNameDiscovery(parameterNameDiscoverer);
		}
	}

	/**
	 * Determine the name of the wrapped parameter/field.
	 * @return the declared name (never {@code null})
	 */
	public String getDependencyName() {
		return (this.field != null ? this.field.getName() : this.methodParameter.getParameterName());
	}

	/**
	 * Determine the declared (non-generic) type of the wrapped parameter/field.
	 * @return the declared type (never {@code null})
	 */
	public Class<?> getDependencyType() {
		if (this.field != null) {
			if (this.nestingLevel > 1) {
				Type type = this.field.getGenericType();
				if (type instanceof ParameterizedType) {
					Type arg = ((ParameterizedType) type).getActualTypeArguments()[0];
					if (arg instanceof Class) {
						return (Class) arg;
					}
					else if (arg instanceof ParameterizedType) {
						arg = ((ParameterizedType) arg).getRawType();
						if (arg instanceof Class) {
							return (Class) arg;
						}
					}
				}
				return Object.class;
			}
			else {
				return this.field.getType();
			}
		}
		else {
			return this.methodParameter.getNestedParameterType();
		}
	}

	/**
	 * {@link TypeInformation} based variant of {@link #getDependencyType()} to ensure we keep generics information
	 * around.
	 *
	 * @return
	 */
	public TypeInformation<?> getDependencyTypeInformation() {

		TypeInformation<?> info = null;

		if (this.owningTypeInformation == null) {
			this.owningTypeInformation = ClassTypeInformation.from(this.sourceBeanClass);
		}

		if (field == null) {

			Method method = this.methodParameter.getMethod();
			List<TypeInformation<?>> paramTypeInformations = method == null ?
					this.owningTypeInformation.getParameterTypes(this.methodParameter.getConstructor()) :
					this.owningTypeInformation.getParameterTypes(this.methodParameter.getMethod());

			info = paramTypeInformations.get(this.methodParameter.getParameterIndex());
		} else {
			info = this.owningTypeInformation.getProperty(fieldName);
		}

		return this.nestingLevel > 1 ? info.getComponentType() : info;
	}

	/**
	 * Determine the generic element type of the wrapped Collection parameter/field, if any.
	 * @return the generic type, or {@code null} if none
	 */
	public Class<?> getCollectionType() {
		return (this.field != null ?
				GenericCollectionTypeResolver.getCollectionFieldType(this.field, this.nestingLevel) :
				GenericCollectionTypeResolver.getCollectionParameterType(this.methodParameter));
	}

	/**
	 * Determine the generic key type of the wrapped Map parameter/field, if any.
	 * @return the generic type, or {@code null} if none
	 */
	public Class<?> getMapKeyType() {
		return (this.field != null ?
				GenericCollectionTypeResolver.getMapKeyFieldType(this.field, this.nestingLevel) :
				GenericCollectionTypeResolver.getMapKeyParameterType(this.methodParameter));
	}

	/**
	 * Determine the generic value type of the wrapped Map parameter/field, if any.
	 * @return the generic type, or {@code null} if none
	 */
	public Class<?> getMapValueType() {
		return (this.field != null ?
				GenericCollectionTypeResolver.getMapValueFieldType(this.field, this.nestingLevel) :
				GenericCollectionTypeResolver.getMapValueParameterType(this.methodParameter));
	}

	/**
	 * Obtain the annotations associated with the wrapped parameter/field, if any.
	 */
	public Annotation[] getAnnotations() {
		if (this.field != null) {
			if (this.fieldAnnotations == null) {
				this.fieldAnnotations = this.field.getAnnotations();
			}
			return this.fieldAnnotations;
		}
		else {
			return this.methodParameter.getParameterAnnotations();
		}
	}
	
	/**
	 * Returns whether the dependecy shall be autowired. This will rule out dependency
	 * descriptors for {@link Object} as well as {@link Collection}s and {@link Map}s with
	 * {@link Object} as component or value type.
	 */
	public boolean shouldBeAutowired() {
		
		Class<?> dependencyType = getDependencyType();
		
		if (Collection.class.isAssignableFrom(dependencyType)) {
			return isNotNullAndNotObject(getCollectionType());
		}
		
		if (Map.class.isAssignableFrom(dependencyType) && dependencyType.isInterface()) {
			dependencyType = getMapValueType();
			return isNotNullAndNotObject(getMapValueType());
		}
		
		return isNotNullAndNotObject(dependencyType);
	}
	
	private static boolean isNotNullAndNotObject(Class<?> type) {
		return type != null && !Object.class.equals(type);
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization; just initialize state after deserialization.
		ois.defaultReadObject();

		// Restore reflective handles (which are unfortunately not serializable)
		try {
			if (this.fieldName != null) {
				this.field = this.declaringClass.getDeclaredField(this.fieldName);
			}
			else {
				if (this.methodName != null) {
					this.methodParameter = new MethodParameter(
							this.declaringClass.getDeclaredMethod(this.methodName, this.parameterTypes), this.parameterIndex);
				}
				else {
					this.methodParameter = new MethodParameter(
							this.declaringClass.getDeclaredConstructor(this.parameterTypes), this.parameterIndex);
				}
				for (int i = 1; i < this.nestingLevel; i++) {
					this.methodParameter.increaseNestingLevel();
				}
			}
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Could not find original class structure", ex);
		}
	}

}
