/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.core.convert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;

import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;

/**
 * Type metadata about a bindable target value.
 * 
 * @author Keith Donald
 */
public class TypeDescriptor {

	private MethodParameter methodParameter;

	private Field field;

	private Annotation[] cachedFieldAnnotations;

	private Class<?> type;
	
	/**
	 * Creates a new descriptor for the given type.
	 * Use this constructor when a bound value comes from a source such as a Map or collection, where no additional binding metadata is available.
	 * @param type the actual type
	 */
	public TypeDescriptor(Class<?> type) {
		this.type = type;
	}
	
	/**
	 * Create a new descriptor for a method or constructor parameter.
	 * Use this constructor when a bound value originates from a method parameter, such as a setter method argument.
	 * @param methodParameter the MethodParameter to wrap
	 */
	public TypeDescriptor(MethodParameter methodParameter) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		this.methodParameter = methodParameter;
	}

	/**
	 * Create a new descriptor for a field.
	 * Use this constructor when a bound value originates from a field.
	 * @param field the field to wrap
	 */
	public TypeDescriptor(Field field) {
		Assert.notNull(field, "Field must not be null");
		this.field = field;
	}

	/**
	 * Determine the declared (non-generic) type of the wrapped parameter/field.
	 * 
	 * @return the declared type (never <code>null</code>)
	 */
	public Class<?> getType() {
		if (type != null) {
			return type;
		} else if (field != null) {
			return field.getType();
		} else {
			return methodParameter.getParameterType();
		}
	}

	/**
	 * If the actual type is a primitive, returns its wrapper type, else just returns {@link #getType()}.
	 * @return the wrapper type if the underlying type is a primitive, else the actual type as-is
	 */
	public Class<?> getWrapperTypeIfPrimitive() {
		Class<?> type = getType();
		if (type.isPrimitive()) {
			if (type.equals(int.class)) {
				return Integer.class;
			} else if (type.equals(short.class)) {
				return Short.class;
			} else if (type.equals(long.class)) {
				return Long.class;
			} else if (type.equals(float.class)) {
				return Float.class;
			} else if (type.equals(double.class)) {
				return Double.class;
			} else if (type.equals(byte.class)) {
				return Byte.class;
			} else if (type.equals(boolean.class)) {
				return Boolean.class;
			} else if (type.equals(char.class)) {
				return Character.class;
			} else {
				throw new IllegalStateException("Should never happen - primitive type is not a primitive?");
			}
		} else {
			return type;
		}
	}
	
	/**
	 * Returns the name of this type; the fully qualified classname.
	 */
	public String getName() {
		return getType().getName();
	}

	/**
	 * Is this type an array type?
	 */
	public boolean isArray() {
		return getType().isArray();
	}

	/**
	 * Is this type a {@link Collection} type?
	 */
	public boolean isCollection() {
		return Collection.class.isAssignableFrom(getType());
	}

	/**
	 * If this type is an array type or {@link Collection} type, returns the underlying element type.
	 * Returns null if the type is neither an array or collection.
	 */
	public Class<?> getElementType() {
		if (isArray()) {
			return getArrayComponentType();
		} else if (isCollection()) {
			return getCollectionElementType();
		} else {
			return null;
		}
	}
	
	/**
	 * Determine the generic key type of the wrapped Map parameter/field, if any.
	 * 
	 * @return the generic type, or <code>null</code> if none
	 */
	public Class<?> getMapKeyType() {
		return (field != null ? GenericCollectionTypeResolver.getMapKeyFieldType(field) : GenericCollectionTypeResolver
				.getMapKeyParameterType(methodParameter));
	}

	/**
	 * Determine the generic value type of the wrapped Map parameter/field, if any.
	 * 
	 * @return the generic type, or <code>null</code> if none
	 */
	public Class<?> getMapValueType() {
		return (field != null ? GenericCollectionTypeResolver.getMapValueFieldType(field)
				: GenericCollectionTypeResolver.getMapValueParameterType(methodParameter));
	}

	/**
	 * Obtain the annotations associated with the wrapped parameter/field, if any.
	 */
	public Annotation[] getAnnotations() {
		if (field != null) {
			if (cachedFieldAnnotations == null) {
				cachedFieldAnnotations = field.getAnnotations();
			}
			return cachedFieldAnnotations;
		} else {
			return methodParameter.getParameterAnnotations();
		}
	}

	/**
	 * Return the wrapped MethodParameter, if any.
	 * <p>
	 * Note: Either MethodParameter or Field is available.
	 * 
	 * @return the MethodParameter, or <code>null</code> if none
	 */
	public MethodParameter getMethodParameter() {
		return methodParameter;
	}

	/**
	 * Return the wrapped Field, if any.
	 * <p>
	 * Note: Either MethodParameter or Field is available.
	 * 
	 * @return the Field, or <code>null</code> if none
	 */
	public Field getField() {
		return field;
	}

	/**
	 * Returns true if this type is an abstract class.
	 */
	public boolean isAbstractClass() {
		return !getType().isInterface() && Modifier.isAbstract(getType().getModifiers());
	}

	/**
	 * Is the obj an instance of this type?
	 */
	public boolean isInstance(Object obj) {
		return getType().isInstance(obj);
	}

	/**
	 * Returns true if an object this type can be assigned to a rereference of given targetType.
	 * @param targetType the target type
	 * @return true if this type is assignable to the target
	 */
	public boolean isAssignableTo(TypeDescriptor targetType) {
		return targetType.getType().isAssignableFrom(getType());
	}
	

	/**
	 * Creates a new type descriptor for the given class.
	 * @param type the class
	 * @return the type descriptor
	 */
	public static TypeDescriptor valueOf(Class<? extends Object> type) {
		return new TypeDescriptor(type);
	}

	// internal helpers
	
	private Class<?> getArrayComponentType() {
		return getType().getComponentType();
	}
	
	private Class<?> getCollectionElementType() {
		if (type != null) {
			return GenericCollectionTypeResolver.getCollectionType((Class<? extends Collection>) type);
		} else if (field != null) {
			return GenericCollectionTypeResolver.getCollectionFieldType(field);
		} else {
			return  GenericCollectionTypeResolver.getCollectionParameterType(methodParameter);
		}
	}

}