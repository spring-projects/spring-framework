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
import java.util.Map;

import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;

// TODO doesn't support more than depth of one (eg. Map<String,List<Foo>> or List<String>[])
/**
 * Type metadata about a bindable target value.
 * 
 * @author Keith Donald
 * @author Andy Clement
 */
public class TypeDescriptor {

	/**
	 * Constant value typeDescriptor for the type of a null value
	 */
	public final static TypeDescriptor NULL_TYPE_DESCRIPTOR = new TypeDescriptor((Class<?>) null);

	private MethodParameter methodParameter;

	private Field field;

	private Annotation[] cachedFieldAnnotations;

	private Class<?> type;

	/**
	 * Creates a new descriptor for the given type. Use this constructor when a bound value comes from a source such as
	 * a Map or collection, where no additional binding metadata is available.
	 * @param type the actual type
	 */
	public TypeDescriptor(Class<?> type) {
		this.type = type;
	}

	/**
	 * Create a new descriptor for a method or constructor parameter. Use this constructor when a bound value originates
	 * from a method parameter, such as a setter method argument.
	 * @param methodParameter the MethodParameter to wrap
	 */
	public TypeDescriptor(MethodParameter methodParameter) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		this.methodParameter = methodParameter;
	}

	/**
	 * Create a new descriptor for a field. Use this constructor when a bound value originates from a field.
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
			return wrapperType(type);
		} else if (field != null) {
			return wrapperType(field.getType());
		} else if (methodParameter != null) {
			return wrapperType(methodParameter.getParameterType());
		} else {
			return null;
		}
	}

	/**
	 * Returns the name of this type; the fully qualified classname.
	 */
	public String getName() {
		Class<?> type = getType();
		if (type != null) {
			return getType().getName();
		} else {
			return null;
		}
	}

	/**
	 * Is this type an array type?
	 */
	public boolean isArray() {
		Class<?> type = getType();
		if (type != null) {
			return type.isArray();
		} else {
			return false;
		}
	}

	/**
	 * Is this type a {@link Collection} type?
	 */
	public boolean isCollection() {
		return isTypeAssignableTo(Collection.class);
	}

	/**
	 * If this type is an array type or {@link Collection} type, returns the underlying element type. Returns null if
	 * the type is neither an array or collection.
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
	 * Is this type a {@link Map} type?
	 */
	public boolean isMap() {
		return isTypeAssignableTo(Map.class);
	}
	
	/**
	 * Is this descriptor for a map where the key type and value type are known? 
	 */
	public boolean isMapEntryTypeKnown() {
		return isMap() && getMapKeyType() != null && getMapValueType() != null;
	}

	/**
	 * Determine the generic key type of the wrapped Map parameter/field, if any.
	 * 
	 * @return the generic type, or <code>null</code> if none
	 */
	public Class<?> getMapKeyType() {
		if (field != null) {
			return GenericCollectionTypeResolver.getMapKeyFieldType(field);
		} else if (methodParameter != null) {
			return GenericCollectionTypeResolver.getMapKeyParameterType(methodParameter);
		} else {
			return null;
		}
	}

	/**
	 * Determine the generic value type of the wrapped Map parameter/field, if any.
	 * 
	 * @return the generic type, or <code>null</code> if none
	 */
	public Class<?> getMapValueType() {
		if (field != null) {
			return GenericCollectionTypeResolver.getMapValueFieldType(field);
		} else if (methodParameter != null) {
			return GenericCollectionTypeResolver.getMapValueParameterType(methodParameter);
		} else {
			return null;
		}
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
		} else if (methodParameter != null) {
			return methodParameter.getParameterAnnotations();
		} else {
			return null;
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
		Class<?> type = getType();
		if (type != null) {
			return !getType().isInterface() && Modifier.isAbstract(getType().getModifiers());			
		} else {
			return false;
		}
	}

	/**
	 * Is the obj an instance of this type?
	 */
	public boolean isInstance(Object obj) {
		Class<?> type = getType();
		if (type != null) {
			return getType().isInstance(obj);
		} else {
			return false;
		}
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
		// TODO needs a cache for common type descriptors
		return new TypeDescriptor(type);
	}

	/**
	 * Creates a new type descriptor for the class of the given object.
	 * @param object the object
	 * @return the type descriptor
	 */
	public static TypeDescriptor forObject(Object object) {
		if (object == null) {
			return NULL_TYPE_DESCRIPTOR;
		} else {
			return valueOf(object.getClass());
		}
	}

	/**
	 * @return a textual representation of the type descriptor (eg. Map<String,Foo>) for use in messages
	 */
	public String asString() {
		StringBuffer stringValue = new StringBuffer();
		if (isArray()) {
			// TODO should properly handle multi dimensional arrays
			stringValue.append(getArrayComponentType().getName()).append("[]");
		} else {
			Class<?> clazz = getType();
			if (clazz==null) {
				return "null";
			}
			stringValue.append(clazz.getName());
			if (isCollection()) {
				Class<?> collectionType = getCollectionElementType();
				if (collectionType != null) {
					stringValue.append("<").append(collectionType.getName()).append(">");
				}
			} else if (isMap()) {
				Class<?> keyType = getMapKeyType();
				Class<?> valType = getMapValueType();
				if (keyType != null && valType != null) {
					stringValue.append("<").append(keyType.getName()).append(",");
					stringValue.append(valType).append(">");
				}
			}
		}
		return stringValue.toString();
	}

	// internal helpers
	
	private Class<?> wrapperType(Class<?> type) {
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
	
	private Class<?> getArrayComponentType() {
		return getType().getComponentType();
	}

	@SuppressWarnings("unchecked")
	private Class<?> getCollectionElementType() {
		if (type != null) {
			return GenericCollectionTypeResolver.getCollectionType((Class<? extends Collection>) type);
		} else if (field != null) {
			return GenericCollectionTypeResolver.getCollectionFieldType(field);
		} else {
			return GenericCollectionTypeResolver.getCollectionParameterType(methodParameter);
		}
	}
	
	private boolean isTypeAssignableTo(Class<?> clazz) {
		Class<?> type = getType();
		if (type != null) {
			return clazz.isAssignableFrom(type);
		} else {
			return false;
		}
	}

}