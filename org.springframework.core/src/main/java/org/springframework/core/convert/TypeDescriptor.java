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

package org.springframework.core.convert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Context about a type to convert to.
 *
 * @author Keith Donald
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0 
 */
public class TypeDescriptor {

	/** Constant defining an 'unknown' TypeDescriptor */
	public static final TypeDescriptor NULL = new TypeDescriptor();

	private static final Map<Class<?>, TypeDescriptor> typeDescriptorCache = new HashMap<Class<?>, TypeDescriptor>();

	static {
		typeDescriptorCache.put(boolean.class, new TypeDescriptor(boolean.class));
		typeDescriptorCache.put(Boolean.class, new TypeDescriptor(Boolean.class));

		typeDescriptorCache.put(byte.class, new TypeDescriptor(byte.class));
		typeDescriptorCache.put(Byte.class, new TypeDescriptor(Byte.class));
		
		typeDescriptorCache.put(char.class, new TypeDescriptor(char.class));
		typeDescriptorCache.put(Character.class, new TypeDescriptor(Character.class));
		
		typeDescriptorCache.put(double.class, new TypeDescriptor(double.class));
		typeDescriptorCache.put(Double.class, new TypeDescriptor(Double.class));

		typeDescriptorCache.put(float.class, new TypeDescriptor(float.class));
		typeDescriptorCache.put(Float.class, new TypeDescriptor(Float.class));

		typeDescriptorCache.put(int.class, new TypeDescriptor(int.class));
		typeDescriptorCache.put(Integer.class, new TypeDescriptor(Integer.class));
		
		typeDescriptorCache.put(long.class, new TypeDescriptor(long.class));
		typeDescriptorCache.put(Long.class, new TypeDescriptor(Long.class));
		
		typeDescriptorCache.put(short.class, new TypeDescriptor(short.class));
		typeDescriptorCache.put(Short.class, new TypeDescriptor(Short.class));

		typeDescriptorCache.put(String.class, new TypeDescriptor(String.class));
	}
	

	private Object value;

	private Class<?> type;

	private MethodParameter methodParameter;

	private Field field;

	private Annotation[] cachedFieldAnnotations;


	/**
	 * Create a new type descriptor from a method or constructor parameter.
	 * <p>Use this constructor when a target conversion point originates from a method parameter,
	 * such as a setter method argument.
	 * @param methodParameter the MethodParameter to wrap
	 */
	public TypeDescriptor(MethodParameter methodParameter) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		this.methodParameter = methodParameter;
	}

	/**
	 * Create a new type descriptor for a field.
	 * Use this constructor when a target conversion point originates from a field.
	 * @param field the field to wrap
	 */
	public TypeDescriptor(Field field) {
		Assert.notNull(field, "Field must not be null");
		this.field = field;
	}

	// protected constructors for subclasses
	
	/**
	 * Create a new type descriptor from a method or constructor parameter.
	 * <p>Use this constructor when a target conversion point originates from a method parameter,
	 * such as a setter method argument.
	 * @param methodParameter the MethodParameter to wrap
	 * @param type the specific type to expose (may be an array/collection element)
	 */
	protected TypeDescriptor(MethodParameter methodParameter, Class<?> type) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		this.methodParameter = methodParameter;
		this.type = type;
	}

	/**
	 * Internal constructor for a NULL descriptor.
	 */
	private TypeDescriptor() {
	}

	/**
	 * Create a new descriptor for the type of the given value.
	 * <p>Use this constructor when a conversion point comes from a source such as a Map or
	 * Collection, where no additional context is available but elements can be introspected.
	 * @param type the actual type to wrap
	 */
	private TypeDescriptor(Object value) {
		Assert.notNull(value, "Value must not be null");
		this.value = value;
		this.type = value.getClass();
	}

	/**
	 * Create a new descriptor for the given type.
	 * <p>Use this constructor when a conversion point comes from a plain source type,
	 * where no additional context is available.
	 * @param type the actual type to wrap
	 */
	private TypeDescriptor(Class<?> type) {
		Assert.notNull(type, "Type must not be null");
		this.type = type;
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
	 * Determine the declared (non-generic) type of the wrapped parameter/field.
	 * @return the declared type, or null if this is {@link TypeDescriptor#NULL}.
	 */
	public Class<?> getType() {
		if (this.type != null) {
			return this.type;
		}
		else if (this.field != null) {
			return this.field.getType();
		}
		else if (this.methodParameter != null) {
			return this.methodParameter.getParameterType();
		}
		else {
			return null;
		}
	}

	/**
	 * Determine the declared type of the wrapped parameter/field.
	 * Returns the Object wrapper type if the underlying type is a primitive.
	 */
	public Class<?> getObjectType() {
		Class<?> type = getType();
		return (type != null ? ClassUtils.resolvePrimitiveIfNecessary(type) : type);
	}

	/**
	 * Returns the name of this type: the fully qualified class name.
	 */
	public String getName() {
		Class<?> type = getType();
		return (type != null ? ClassUtils.getQualifiedName(type) : null);
	}

	/**
	 * Is this type a primitive type?
	 */
	public boolean isPrimitive() {
		Class<?> type = getType();
		return (type != null && type.isPrimitive());
	}

	/**
	 * Is this type an array type?
	 */
	public boolean isArray() {
		Class<?> type = getType();
		return (type != null && type.isArray());
	}

	/**
	 * Is this type a {@link Collection} type?
	 */
	public boolean isCollection() {
		return isTypeAssignableTo(Collection.class);
	}

	/**
	 * If this type is an array type or {@link Collection} type, returns the underlying element type.
	 * Returns null if the type is neither an array or collection.
	 */
	public Class<?> getElementType() {
		return getElementTypeDescriptor().getType();
	}

	/**
	 * Return the element type as a type descriptor.
	 */
	public TypeDescriptor getElementTypeDescriptor() {
		if (isArray()) {
			return TypeDescriptor.valueOf(getArrayComponentType());
		}
		else if (isCollection()) {
			return TypeDescriptor.valueOf(getCollectionElementType());
		}
		else {
			return TypeDescriptor.NULL;
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
		return (isMap() && getMapKeyType() != null && getMapValueType() != null);
	}

	/**
	 * Determine the generic key type of the wrapped Map parameter/field, if any.
	 * @return the generic type, or <code>null</code> if none
	 */
	@SuppressWarnings("unchecked")
	public Class<?> getMapKeyType() {
		if (this.field != null) {
			return GenericCollectionTypeResolver.getMapKeyFieldType(field);
		}
		else if (this.methodParameter != null) {
			return GenericCollectionTypeResolver.getMapKeyParameterType(this.methodParameter);
		}
		else if (this.value instanceof Map) {
			Map map = (Map) this.value;
			if (!map.isEmpty()) {
				Object key = map.keySet().iterator().next();
				if (key != null) {
					return key.getClass();
				}
			}
		}
		if (this.type != null) {
			return GenericCollectionTypeResolver.getMapKeyType((Class<? extends Map>) this.type);
		}
		else {
			return null;
		}
	}

	/**
	 * Determine the generic value type of the wrapped Map parameter/field, if any.
	 * @return the generic type, or <code>null</code> if none
	 */
	@SuppressWarnings("unchecked")
	public Class<?> getMapValueType() {
		if (this.field != null) {
			return GenericCollectionTypeResolver.getMapValueFieldType(this.field);
		}
		else if (this.methodParameter != null) {
			return GenericCollectionTypeResolver.getMapValueParameterType(this.methodParameter);
		}
		else if (this.value instanceof Map) {
			Map map = (Map) this.value;
			if (!map.isEmpty()) {
				Object val = map.values().iterator().next();
				if (val != null) {
					return val.getClass();
				}
			}
		}
		if (this.type != null) {
			return GenericCollectionTypeResolver.getMapValueType((Class<? extends Map>) this.type);
		}
		else {
			return null;
		}
	}

	/**
	 * Returns map key type as a type descriptor.
	 */
	public TypeDescriptor getMapKeyTypeDescriptor() {
		return TypeDescriptor.valueOf(getMapKeyType());
	}

	/**
	 * Returns map value type as a type descriptor.
	 */
	public TypeDescriptor getMapValueTypeDescriptor() {
		return TypeDescriptor.valueOf(getMapValueType());
	}

	/**
	 * Obtain the annotations associated with the wrapped parameter/field, if any.
	 */
	public Annotation[] getAnnotations() {
		if (this.field != null) {
			if (this.cachedFieldAnnotations == null) {
				this.cachedFieldAnnotations = this.field.getAnnotations();
			}
			return this.cachedFieldAnnotations;
		}
		else if (this.methodParameter != null) {
			return this.methodParameter.getParameterAnnotations();
		}
		else {
			return new Annotation[0];
		}
	}

	/**
	 * Obtain the annotation associated with the wrapped parameter/field, if any.
	 */
	public Annotation getAnnotation(Class<? extends Annotation> annotationType) {
		for (Annotation annotation : getAnnotations()) {
			if (annotation.annotationType().equals(annotationType)) {
				return annotation;
			}
		}
		return null;		
	}

	/**
	 * Returns true if an object this type can be assigned to a reference of given targetType.
	 * @param targetType the target type
	 * @return true if this type is assignable to the target
	 */
	public boolean isAssignableTo(TypeDescriptor targetType) {
		return isTypeAssignableTo(targetType.getType());
	}

	/**
	 * A textual representation of the type descriptor (eg. Map<String,Foo>) for use in messages
	 */
	public String asString() {
		StringBuffer stringValue = new StringBuffer();
		if (isArray()) {
			// TODO should properly handle multi dimensional arrays
			stringValue.append(getArrayComponentType().getName()).append("[]");
		}
		else {
			Class<?> clazz = getType();
			if (clazz == null) {
				return "null";
			}
			stringValue.append(clazz.getName());
			if (isCollection()) {
				Class<?> collectionType = getCollectionElementType();
				if (collectionType != null) {
					stringValue.append("<").append(collectionType.getName()).append(">");
				}
			}
			else if (isMap()) {
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

	public String toString() {
		if (this == TypeDescriptor.NULL) {
			return "TypeDescriptor.NULL";
		}
		else {
			StringBuilder builder = new StringBuilder();
			builder.append("TypeDescriptor ");
			Annotation[] anns = getAnnotations();
			for (Annotation ann : anns) {
				builder.append("@").append(ann.annotationType().getName()).append(' ');
			}
			builder.append(ClassUtils.getQualifiedName(getType()));
			return builder.toString();
		}
	}


	// internal helpers
	
	private Class<?> getArrayComponentType() {
		return getType().getComponentType();
	}

	@SuppressWarnings("unchecked")
	private Class<?> getCollectionElementType() {
		if (this.field != null) {
			return GenericCollectionTypeResolver.getCollectionFieldType(this.field);
		}
		else if (this.methodParameter != null) {
			return GenericCollectionTypeResolver.getCollectionParameterType(this.methodParameter);
		}
		else if (this.value instanceof Collection) {
			Collection coll = (Collection) this.value;
			if (!coll.isEmpty()) {
				Object elem = coll.iterator().next();
				if (elem != null) {
					return elem.getClass();
				}
			}
		}
		if (this.type != null) {
			return GenericCollectionTypeResolver.getCollectionType((Class<? extends Collection>) this.type);
		}
		else {
			return null;
		}
	}

	private boolean isTypeAssignableTo(Class<?> clazz) {
		Class<?> type = getType();
		return (type != null && ClassUtils.isAssignable(clazz, type));
	}


	// static factory methods

	/**
	 * Create a new type descriptor for the given class.
	 * @param type the class
	 * @return the type descriptor
	 */
	public static TypeDescriptor valueOf(Class<?> type) {
		if (type == null) {
			return TypeDescriptor.NULL;
		}
		TypeDescriptor desc = typeDescriptorCache.get(type);
		return (desc != null ? desc : new TypeDescriptor(type));
	}

	/**
	 * Create a new type descriptor for the class of the given object.
	 * @param object the object
	 * @return the type descriptor
	 */
	public static TypeDescriptor forObject(Object object) {
		if (object == null) {
			return NULL;
		}
		else if (object instanceof Collection || object instanceof Map) {
			return new TypeDescriptor(object);
		}
		else {
			return valueOf(object.getClass());
		}
	}

}
