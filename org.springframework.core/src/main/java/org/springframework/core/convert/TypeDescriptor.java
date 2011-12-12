/*
 * Copyright 2002-2011 the original author or authors.
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Context about a type to convert to.
 *
 * @author Keith Donald
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class TypeDescriptor {

	/** Constant defining a TypeDescriptor for a <code>null</code> value */
	public static final TypeDescriptor NULL = new TypeDescriptor();

	/** Constant defining a TypeDescriptor for 'unknown type' */
	private static final TypeDescriptor UNKNOWN = new TypeDescriptor(Object.class);

	private static final Map<Class<?>, TypeDescriptor> typeDescriptorCache = new HashMap<Class<?>, TypeDescriptor>();

	private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

	static {
		typeDescriptorCache.put(boolean.class, new TypeDescriptor(boolean.class));
		typeDescriptorCache.put(Boolean.class, new TypeDescriptor(Boolean.class));
		typeDescriptorCache.put(byte.class, new TypeDescriptor(byte.class));
		typeDescriptorCache.put(Byte.class, new TypeDescriptor(Byte.class));
		typeDescriptorCache.put(char.class, new TypeDescriptor(char.class));
		typeDescriptorCache.put(Character.class, new TypeDescriptor(Character.class));
		typeDescriptorCache.put(short.class, new TypeDescriptor(short.class));
		typeDescriptorCache.put(Short.class, new TypeDescriptor(Short.class));
		typeDescriptorCache.put(int.class, new TypeDescriptor(int.class));
		typeDescriptorCache.put(Integer.class, new TypeDescriptor(Integer.class));
		typeDescriptorCache.put(long.class, new TypeDescriptor(long.class));
		typeDescriptorCache.put(Long.class, new TypeDescriptor(Long.class));
		typeDescriptorCache.put(float.class, new TypeDescriptor(float.class));
		typeDescriptorCache.put(Float.class, new TypeDescriptor(Float.class));
		typeDescriptorCache.put(double.class, new TypeDescriptor(double.class));
		typeDescriptorCache.put(Double.class, new TypeDescriptor(Double.class));
		typeDescriptorCache.put(String.class, new TypeDescriptor(String.class));
	}


	private Class<?> type;

	private MethodParameter methodParameter;

	private Field field;

	private int fieldNestingLevel = 1;

	private Object value;

	private volatile TypeDescriptor elementType;

	private volatile TypeDescriptor mapKeyType;

	private volatile TypeDescriptor mapValueType;

	private volatile Annotation[] annotations;


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

	/**
	 * Create a new type descriptor from a method or constructor parameter.
	 * <p>Use this constructor when a target conversion point originates from a method parameter,
	 * such as a setter method argument.
	 * @param methodParameter the MethodParameter to wrap
	 * @param type the specific type to expose (may be an array/collection element)
	 */
	public TypeDescriptor(MethodParameter methodParameter, Class<?> type) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		this.methodParameter = methodParameter;
		this.type = type;
	}

	/**
	 * Create a new type descriptor for a field.
	 * Use this constructor when a target conversion point originates from a field.
	 * @param field the field to wrap
	 * @param type the specific type to expose (may be an array/collection element)
	 */
	public TypeDescriptor(Field field, Class<?> type) {
		Assert.notNull(field, "Field must not be null");
		this.field = field;
		this.type = type;
	}

	/**
	 * Create a new type descriptor for a field.
	 * Use this constructor when a target conversion point originates from a field.
	 * @param field the field to wrap
	 * @param type the specific type to expose (may be an array/collection element)
	 */
	private TypeDescriptor(Field field, int nestingLevel, Class<?> type) {
		Assert.notNull(field, "Field must not be null");
		this.field = field;
		this.fieldNestingLevel = nestingLevel;
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
	 * @param value the value to determine the actual type from
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
	 * @return the declared type, or <code>null</code> if this is {@link TypeDescriptor#NULL}
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
		return Collection.class.isAssignableFrom(getType());
	}

	/**
	 * If this type is an array type or {@link Collection} type, returns the underlying element type.
	 * Returns <code>null</code> if the type is neither an array or collection.
	 */
	public Class<?> getElementType() {
		return getElementTypeDescriptor().getType();
	}

	/**
	 * Return the element type as a type descriptor.
	 */
	public TypeDescriptor getElementTypeDescriptor() {
		if (this.elementType == null) {
			this.elementType = forElementType(resolveElementType());
		}
		return this.elementType;
	}

	/**
	 * Return the element type as a type descriptor. If the element type is null
	 * (cannot be determined), the type descriptor is derived from the element argument.
	 * @param element the element
	 * @return the element type descriptor
	 */
	public TypeDescriptor getElementTypeDescriptor(Object element) {
		TypeDescriptor elementType = getElementTypeDescriptor();
		return (!TypeDescriptor.UNKNOWN.equals(elementType) ? elementType : forObject(element));
	}

	/**
	 * Is this type a {@link Map} type?
	 */
	public boolean isMap() {
		return Map.class.isAssignableFrom(getType());
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
	public Class<?> getMapKeyType() {
		return getMapKeyTypeDescriptor().getType();
	}

	/**
	 * Returns map key type as a type descriptor.
	 */
	public TypeDescriptor getMapKeyTypeDescriptor() {
		if (this.mapKeyType == null) {
			this.mapKeyType = forElementType(resolveMapKeyType());
		}
		return this.mapKeyType;
	}

	/**
	 * Return the map key type as a type descriptor. If the key type is <code>null</code>
	 * (cannot be determined), the type descriptor is derived from the key argument.
	 * @param key the key
	 * @return the map key type descriptor
	 */
	public TypeDescriptor getMapKeyTypeDescriptor(Object key) {
		TypeDescriptor keyType = getMapKeyTypeDescriptor();
		return (!TypeDescriptor.UNKNOWN.equals(keyType) ? keyType : TypeDescriptor.forObject(key));
	}

	/**
	 * Determine the generic value type of the wrapped Map parameter/field, if any.
	 * @return the generic type, or <code>null</code> if none
	 */
	public Class<?> getMapValueType() {
		return getMapValueTypeDescriptor().getType();
	}

	/**
	 * Returns map value type as a type descriptor.
	 */
	public TypeDescriptor getMapValueTypeDescriptor() {
		if (this.mapValueType == null) {
			this.mapValueType = forElementType(resolveMapValueType());
		}
		return this.mapValueType;
	}

	/**
	 * Return the map value type as a type descriptor. If the value type is null
	 * (cannot be determined), the type descriptor is derived from the value argument.
	 * @param value the value
	 * @return the map value type descriptor
	 */
	public TypeDescriptor getMapValueTypeDescriptor(Object value) {
		TypeDescriptor valueType = getMapValueTypeDescriptor();
		return (!TypeDescriptor.UNKNOWN.equals(valueType) ? valueType : TypeDescriptor.forObject(value));
	}

	/**
	 * Obtain the annotations associated with the wrapped parameter/field, if any.
	 */
	public Annotation[] getAnnotations() {
		if (this.annotations == null) {
			this.annotations = resolveAnnotations();
		}
		return this.annotations;
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
	 * Returns true if an object of this type can be assigned to a reference of given targetType.
	 * @param targetType the target type
	 * @return true if this type is assignable to the target
	 */
	public boolean isAssignableTo(TypeDescriptor targetType) {
		if (this == TypeDescriptor.NULL || targetType == TypeDescriptor.NULL) {
			return true;
		}
		if (isCollection() && targetType.isCollection() || isArray() && targetType.isArray()) {
			return targetType.getType().isAssignableFrom(getType()) &&
					getElementTypeDescriptor().isAssignableTo(targetType.getElementTypeDescriptor());
		}
		else if (isMap() && targetType.isMap()) {
			return targetType.getType().isAssignableFrom(getType()) &&
					getMapKeyTypeDescriptor().isAssignableTo(targetType.getMapKeyTypeDescriptor()) &&
					getMapValueTypeDescriptor().isAssignableTo(targetType.getMapValueTypeDescriptor());
		}
		else {
			return targetType.getObjectType().isAssignableFrom(getObjectType());
		}
	}

	/**
	 * Create a copy of this type descriptor, preserving the context information
	 * but exposing the specified element type (e.g. an array/collection/map element).
	 * @param elementType the desired type to expose
	 * @return the type descriptor
	 */
	public TypeDescriptor forElementType(Class<?> elementType) {
		if (elementType == null) {
			return TypeDescriptor.UNKNOWN;
		}
		else if (this.methodParameter != null) {
			MethodParameter nested = new MethodParameter(this.methodParameter);
			nested.increaseNestingLevel();
			return new TypeDescriptor(nested, elementType);
		}
		else if (this.field != null) {
			return new TypeDescriptor(this.field, this.fieldNestingLevel + 1, elementType);
		}
		else {
			return TypeDescriptor.valueOf(elementType);
		}
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof TypeDescriptor) || obj == TypeDescriptor.NULL) {
			return false;
		}
		TypeDescriptor other = (TypeDescriptor) obj;
		boolean annotatedTypeEquals =
				getType().equals(other.getType()) && ObjectUtils.nullSafeEquals(getAnnotations(), other.getAnnotations());
		if (isCollection()) {
			return annotatedTypeEquals && ObjectUtils.nullSafeEquals(getElementType(), other.getElementType());
		}
		else if (isMap()) {
			return annotatedTypeEquals && ObjectUtils.nullSafeEquals(getMapKeyType(), other.getMapKeyType()) &&
					ObjectUtils.nullSafeEquals(getMapValueType(), other.getMapValueType());
		}
		else {
			return annotatedTypeEquals;
		}
	}

	public int hashCode() {
		return (this == TypeDescriptor.NULL ? 0 : getType().hashCode());
	}

	/**
	 * A textual representation of the type descriptor (eg. Map<String,Foo>) for use in messages.
	 */
	public String asString() {
		return toString();
	}

	public String toString() {
		if (this == TypeDescriptor.NULL) {
			return "null";
		}
		else {
			StringBuilder builder = new StringBuilder();
			Annotation[] anns = getAnnotations();
			for (Annotation ann : anns) {
				builder.append("@").append(ann.annotationType().getName()).append(' ');
			}
			builder.append(ClassUtils.getQualifiedName(getType()));
			if (isMap()) {
				builder.append("<").append(getMapKeyTypeDescriptor());
				builder.append(", ").append(getMapValueTypeDescriptor()).append(">");
			}
			else if (isCollection()) {
				builder.append("<").append(getElementTypeDescriptor()).append(">");
			}
			return builder.toString();
		}
	}


	// internal helpers

	private Class<?> resolveElementType() {
		if (isArray()) {
			return getType().getComponentType();
		}
		else if (isCollection()) {
			return resolveCollectionElementType();
		}
		else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private Class<?> resolveCollectionElementType() {
		if (this.field != null) {
			return GenericCollectionTypeResolver.getCollectionFieldType(this.field, this.fieldNestingLevel);
		}
		else if (this.methodParameter != null) {
			return GenericCollectionTypeResolver.getCollectionParameterType(this.methodParameter);
		}
		else if (this.value instanceof Collection) {
			Class<?> elementType = CollectionUtils.findCommonElementType((Collection) this.value);
			if (elementType != null) {
				return elementType;
			}
		}
        else if (this.type != null) {
			return GenericCollectionTypeResolver.getCollectionType((Class<? extends Collection>) this.type);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private Class<?> resolveMapKeyType() {
		if (this.field != null) {
			return GenericCollectionTypeResolver.getMapKeyFieldType(this.field);
		}
		else if (this.methodParameter != null) {
			return GenericCollectionTypeResolver.getMapKeyParameterType(this.methodParameter);
		}
		else if (this.value instanceof Map<?, ?>) {
			Class<?> keyType = CollectionUtils.findCommonElementType(((Map<?, ?>) this.value).keySet());
			if (keyType != null) {
				return keyType;
			}
		}
		else if (this.type != null && isMap()) {
			return GenericCollectionTypeResolver.getMapKeyType((Class<? extends Map>) this.type);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private Class<?> resolveMapValueType() {
		if (this.field != null) {
			return GenericCollectionTypeResolver.getMapValueFieldType(this.field);
		}
		else if (this.methodParameter != null) {
			return GenericCollectionTypeResolver.getMapValueParameterType(this.methodParameter);
		}
		else if (this.value instanceof Map<?, ?>) {
			Class<?> valueType = CollectionUtils.findCommonElementType(((Map<?, ?>) this.value).values());
			if (valueType != null) {
				return valueType;
			}
		}
		else if (this.type != null && isMap()) {
			return GenericCollectionTypeResolver.getMapValueType((Class<? extends Map>) this.type);
		}
		return null;
	}

	private Annotation[] resolveAnnotations() {
		if (this.field != null) {
			return this.field.getAnnotations();
		}
		else if (this.methodParameter != null) {
			if (this.methodParameter.getParameterIndex() < 0) {
				return this.methodParameter.getMethodAnnotations();
			}
			else {
				return this.methodParameter.getParameterAnnotations();
			}
		}
		else {
			return EMPTY_ANNOTATION_ARRAY;
		}
	}


	// static factory methods

	/**
	 * Create a new type descriptor for the class of the given object.
	 * @param object the object
	 * @return the type descriptor
	 */
	public static TypeDescriptor forObject(Object object) {
		if (object == null) {
			return NULL;
		}
		else if (object instanceof Collection<?> || object instanceof Map<?, ?>) {
			return new TypeDescriptor(object);
		}
		else {
			return valueOf(object.getClass());
		}
	}

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

}
