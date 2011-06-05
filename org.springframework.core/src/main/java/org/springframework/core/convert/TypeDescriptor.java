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

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Context about a type to convert from or to.
 * @author Keith Donald
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class TypeDescriptor {

	static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

	private static final Map<Class<?>, TypeDescriptor> typeDescriptorCache = new HashMap<Class<?>, TypeDescriptor>();

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

	private final Class<?> type;

	private final TypeDescriptor elementType;

	private final TypeDescriptor mapKeyType;

	private final TypeDescriptor mapValueType;

	private final Annotation[] annotations;

	/**
	 * Create a new type descriptor from a {@link MethodParameter}.
	 * Use this constructor when a conversion point is a constructor parameter, method parameter, or method return value. 
	 * @param methodParameter the method parameter
	 */
	public TypeDescriptor(MethodParameter methodParameter) {
		this(new ParameterDescriptor(methodParameter));
	}

	/**
	 * Create a new type descriptor for a field.
	 * Use this constructor when a conversion point is a field.
	 * @param field the field
	 */
	public TypeDescriptor(Field field) {
		this(new FieldDescriptor(field));
	}

	/**
	 * Create a new type descriptor for a bean property.
	 * Use this constructor when a target conversion point is a property on a Java class.
	 * @param beanClass the class that declares the property
	 * @param property the property descriptor
	 */
	public TypeDescriptor(Class<?> beanClass, PropertyDescriptor property) {
		this(new BeanPropertyDescriptor(beanClass, property));
	}

	/**
	 * Create a new type descriptor for the given class.
	 * Use this to instruct the conversion system to convert to an object to a specific target type, when no type location such as a method parameter or field is available to provide additional conversion context.
	 * Generally prefer use of {@link #forObject(Object)} for constructing source type descriptors for source objects.
	 * @param type the class
	 * @return the type descriptor
	 */
	public static TypeDescriptor valueOf(Class<?> type) {
		TypeDescriptor desc = typeDescriptorCache.get(type);
		return (desc != null ? desc : new TypeDescriptor(type));
	}

	/**
	 * Create a new type descriptor for a java.util.Collection class.
	 * Useful for supporting conversion of source Collection objects to other types.
	 * Serves as an alternative to {@link #forObject(Object)} to be used when you cannot rely on Collection element introspection to resolve the element type.
	 * @param collectionType the collection type, which must implement {@link Collection}.
	 * @param elementType the collection's element type, used to convert collection elements
	 * @return the collection type descriptor
	 */
	public static TypeDescriptor collection(Class<?> collectionType, TypeDescriptor elementType) {
		if (!Collection.class.isAssignableFrom(collectionType)) {
			throw new IllegalArgumentException("collectionType must be a java.util.Collection");
		}
		return new TypeDescriptor(collectionType, elementType);
	}

	/**
	 * Create a new type descriptor for a java.util.Map class.
	 * Useful for supporting the conversion of source Map objects to other types.
	 * Serves as an alternative to {@link #forObject(Object)} to be used when you cannot rely on Map entry introspection to resolve the key and value type.
	 * @param mapType the map type, which must implement {@link Map}.
	 * @param keyType the map's key type, used to convert map keys
	 * @param valueType the map's value type, used to convert map values
	 * @return the map type descriptor
	 */
	public static TypeDescriptor map(Class<?> mapType, TypeDescriptor keyType, TypeDescriptor valueType) {
		if (!Map.class.isAssignableFrom(mapType)) {
			throw new IllegalArgumentException("mapType must be a java.util.Map");
		}
		return new TypeDescriptor(mapType, keyType, valueType);
	}
	
	/**
	 * Creates a type descriptor for a nested type declared within the method parameter.
	 * For example, if the methodParameter is a List&lt;String&gt; and the nestingLevel is 1, the nested type descriptor will be String.class.
	 * If the methodParameter is a List<List<String>> and the nestingLevel is 2, the nested type descriptor will also be a String.class.
	 * If the methodParameter is a Map<Integer, String> and the nesting level is 1, the nested type descriptor will be String, derived from the map value.
	 * If the methodParameter is a List<Map<Integer, String>> and the nesting level is 2, the nested type descriptor will be String, derived from the map value.
	 * @param methodParameter the method parameter with a nestingLevel of 1
	 * @param nestingLevel the nesting level of the collection/array element or map key/value declaration within the method parameter.
	 * @return the nested type descriptor
	 * @throws IllegalArgumentException if the method parameter is not of a collection, array, or map type.
	 */
	public static TypeDescriptor nested(MethodParameter methodParameter, int nestingLevel) {
		return nested(new ParameterDescriptor(methodParameter), nestingLevel);
	}

	/**
	 * Creates a type descriptor for a nested type declared within the field.
	 * For example, if the field is a List&lt;String&gt; and the nestingLevel is 1, the nested type descriptor will be String.class.
	 * If the field is a List<List<String>> and the nestingLevel is 2, the nested type descriptor will also be a String.class. 
	 * If the field is a Map<Integer, String> and the nestingLevel is 1, the nested type descriptor will be String, derived from the map value. 
	 * If the field is a List<Map<Integer, String>> and the nestingLevel is 2, the nested type descriptor will be String, derived from the map value.
	 * @param field the field
	 * @param nestingLevel the nesting level of the collection/array element or map key/value declaration within the field.
	 * @return the nested type descriptor
	 * @throws IllegalArgumentException if the field is not of a collection, array, or map type.
	 */
	public static TypeDescriptor nested(Field field, int nestingLevel) {
		return nested(new FieldDescriptor(field), nestingLevel);
	}

	/**
	 * Creates a type descriptor for a nested type declared within the property.
	 * For example, if the property is a List&lt;String&gt; and the nestingLevel is 1, the nested type descriptor will be String.class.
	 * If the property is a List<List<String>> and the nestingLevel is 2, the nested type descriptor will also be a String.class. 
	 * If the field is a Map<Integer, String> and the nestingLevel is 1, the nested type descriptor will be String, derived from the map value. 
	 * If the property is a List<Map<Integer, String>> and the nestingLevel is 2, the nested type descriptor will be String, derived from the map value.
	 * @param property the property
	 * @param nestingLevel the nesting level of the collection/array element or map key/value declaration within the property.
	 * @return the nested type descriptor
	 * @throws IllegalArgumentException if the property is not of a collection, array, or map type.
	 */
	public static TypeDescriptor nested(Class<?> beanClass, PropertyDescriptor property, int nestingLevel) {
		return nested(new BeanPropertyDescriptor(beanClass, property), nestingLevel);
	}

	/**
	 * Create a new type descriptor for an object.
	 * Use this factory method to introspect a source object before asking the conversion system to convert it to some another type.
	 * If the provided object is null, returns null, else calls {@link #valueOf(Class)} to build a TypeDescriptor from the object's class.
	 * @param object the source object
	 * @return the type descriptor
	 */
	public static TypeDescriptor forObject(Object source) {
		return source != null ? valueOf(source.getClass()) : null;
	}
	
	/**
	 * Determine the declared (non-generic) type of the wrapped parameter/field.
	 * @return the declared type, or <code>null</code> if this is {@link TypeDescriptor#NULL}
	 */
	public Class<?> getType() {
		return type;
	}

	/**
	 * Determine the declared type of the wrapped parameter/field.
	 * Returns the Object wrapper type if the underlying type is a primitive.
	 */
	public Class<?> getObjectType() {
		return ClassUtils.resolvePrimitiveIfNecessary(getType());
	}

	/**
	 * Narrows this {@link TypeDescriptor} by setting its type to the class of the provided value.
	 * If the value is null, no narrowing is performed and this TypeDescriptor is returned unchanged.
	 * Designed to be called by binding frameworks when they read property, field, or method return values.
	 * Allows such frameworks to narrow a TypeDescriptor built from a declared property, field, or method return value type.
	 * For example, a field declared as java.lang.Object would be narrowed to java.util.HashMap if it was set to a java.util.HashMap value.
	 * The narrowed TypeDescriptor can then be used to convert the HashMap to some other type.
	 * Annotation and nested type context is preserved by the narrowed copy.
	 * @param value the value to use for narrowing this type descriptor
	 * @return this TypeDescriptor narrowed (returns a copy with its type updated to the class of the provided value)
	 */
	public TypeDescriptor narrow(Object value) {
		if (value == null) {
			return this;
		}
		return new TypeDescriptor(value.getClass(), elementType, mapKeyType, mapValueType, annotations);
	}
	
	/**
	 * Returns the name of this type: the fully qualified class name.
	 */
	public String getName() {
		return ClassUtils.getQualifiedName(getType());
	}

	/**
	 * Is this type a primitive type?
	 */
	public boolean isPrimitive() {
		return getType().isPrimitive();
	}

	/**
	 * The annotations associated with this type descriptor, if any.
	 * @return the annotations, or an empty array if none.
	 */
	public Annotation[] getAnnotations() {
		return this.annotations;
	}

	/**
	 * Obtain the annotation associated with this type descriptor of the specified type.
	 * @return the annotation, or null if no such annotation exists on this type descriptor.
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
		boolean typesAssignable = targetType.getObjectType().isAssignableFrom(getObjectType());
		if (!typesAssignable) {
			return false;
		}
		if (isArray() && targetType.isArray()) {
			return getElementType().isAssignableTo(targetType.getElementType());
		}
		if (isCollection() && targetType.isCollection()) {
			return collectionElementsAssignable(targetType.getElementType());
		} else if (isMap() && targetType.isMap()) {
			return mapKeysAssignable(targetType.getMapKeyType()) && mapValuesAssignable(targetType.getMapValueType());
		}
		return true;
	}

	// indexable type descriptor operations
	
	/**
	 * Is this type a {@link Collection} type?
	 */
	public boolean isCollection() {
		return Collection.class.isAssignableFrom(getType());
	}

	/**
	 * Is this type an array type?
	 */
	public boolean isArray() {
		return getType().isArray();
	}

	/**
	 * If this type is an array, returns the array's component type.
	 * If this type is a {@link Collection} and it is parameterized, returns the Collection's element type.
	 * If the Collection is not parameterized, returns null indicating the element type is not declared.
	 * @return the array component type or Collection element type, or <code>null</code> if this type is a Collection but its element type is not parameterized.
	 * @throws IllegalStateException if this type is not a java.util.Collection or Array type
	 */
	public TypeDescriptor getElementType() {
		assertCollectionOrArray();
		return this.elementType;
	}

	/**
	 * If this type is a {@link Collection} or an Array, creates a elementType descriptor from the provided collection or array element.
	 * Narrows the {@link #getElementType() elementType} property to the class of the provided collection or array element.
	 * For example, if this describes a java.util.List&lt;java.lang.Number&lt; and the element argument is a java.lang.Integer, the returned TypeDescriptor will be java.lang.Integer.
	 * If this describes a java.util.List&lt;?&gt; and the element argument is a java.lang.Integer, the returned TypeDescriptor will be java.lang.Integer as well.
	 * Annotation and nested type context will be preserved in the narrowed TypeDescriptor that is returned. 
	 * @param element the collection or array element
	 * @return a element type descriptor, narrowed to the type of the provided element
	 * @throws IllegalStateException if this type is not a java.util.Collection or Array type
	 * @see #narrow(Object)
	 */
	public TypeDescriptor elementType(Object element) {
		assertCollectionOrArray();
		if (elementType != null) {
			return elementType.narrow(element);
		} else {
			return element != null ? new TypeDescriptor(element.getClass(), null, null, null, annotations) : null;
		}
	}

	// map type descriptor operations
	
	/**
	 * Is this type a {@link Map} type?
	 */
	public boolean isMap() {
		return Map.class.isAssignableFrom(getType());
	}

	/**
	 * If this type is a {@link Map} and its key type is parameterized, returns the map's key type.
	 * If the Map's key type is not parameterized, returns null indicating the key type is not declared.
	 * @return the Map key type, or <code>null</code> if this type is a Map but its key type is not parameterized.
	 * @throws IllegalStateException if this type is not a java.util.Map.
	 */
	public TypeDescriptor getMapKeyType() {
		assertMap();
		return this.mapKeyType;
	}

	/**
	 * If this type is a {@link Map}, creates a mapKeyType descriptor from the provided map key.
	 * Narrows the {@link #getMapKeyType() mapKeyType} property to the class of the provided map key.
	 * For example, if this describes a java.util.Map&lt;java.lang.Number, java.lang.String&lt; and the key argument is a java.lang.Integer, the returned TypeDescriptor will be java.lang.Integer.
	 * If this describes a java.util.Map&lt;?, ?&gt; and the key argument is a java.lang.Integer, the returned TypeDescriptor will be java.lang.Integer as well.
	 * Annotation and nested type context will be preserved in the narrowed TypeDescriptor that is returned. 
	 * @param mapKey the map key
	 * @return the map key type descriptor
	 * @throws IllegalStateException if this type is not a java.util.Map.
	 * @see #narrow(Object)
	 */
	public TypeDescriptor mapKeyType(Object mapKey) {
		assertMap();
		if (mapKeyType != null) {
			return mapKeyType.narrow(mapKey);
		} else {
			return mapKey != null ? new TypeDescriptor(mapKey.getClass(), null, null, null, annotations) : null;
		}
	}

	/**
	 * If this type is a {@link Map} and its value type is parameterized, returns the map's value type.
	 * If the Map's value type is not parameterized, returns null indicating the value type is not declared.
	 * @return the Map value type, or <code>null</code> if this type is a Map but its value type is not parameterized.
	 * @throws IllegalStateException if this type is not a java.util.Map.
	 */
	public TypeDescriptor getMapValueType() {
		assertMap();
		return this.mapValueType;
	}

	/**
	 * If this type is a {@link Map}, creates a mapValueType descriptor from the provided map value.
	 * Narrows the {@link #getMapValueType() mapValueType} property to the class of the provided map value.
	 * For example, if this describes a java.util.Map&lt;java.lang.String, java.lang.Number&lt; and the value argument is a java.lang.Integer, the returned TypeDescriptor will be java.lang.Integer.
	 * If this describes a java.util.Map&lt;?, ?&gt; and the value argument is a java.lang.Integer, the returned TypeDescriptor will be java.lang.Integer as well.
	 * Annotation and nested type context will be preserved in the narrowed TypeDescriptor that is returned. 
	 * @param mapValue the map value
	 * @return the map value type descriptor
	 * @throws IllegalStateException if this type is not a java.util.Map. 
	 */
	public TypeDescriptor mapValueType(Object mapValue) {
		assertMap();
		if (mapValueType != null) {
			return mapValueType.narrow(mapValue);
		} else {
			return mapValue != null ? new TypeDescriptor(mapValue.getClass(), null, null, null, annotations) : null;
		}
	}

	// extending Object
	
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof TypeDescriptor)) {
			return false;
		}
		TypeDescriptor other = (TypeDescriptor) obj;
		boolean annotatedTypeEquals = ObjectUtils.nullSafeEquals(getType(), other.getType()) && ObjectUtils.nullSafeEquals(getAnnotations(), other.getAnnotations());
		if (!annotatedTypeEquals) {
			return false;
		}
		if (isCollection() || isArray()) {
			return ObjectUtils.nullSafeEquals(getElementType(), other.getElementType());
		}
		else if (isMap()) {
			return ObjectUtils.nullSafeEquals(getMapKeyType(), other.getMapKeyType()) && ObjectUtils.nullSafeEquals(getMapValueType(), other.getMapValueType());
		}
		else {
			return true;
		}
	}

	public int hashCode() {
		return getType().hashCode();
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		Annotation[] anns = getAnnotations();
		for (Annotation ann : anns) {
			builder.append("@").append(ann.annotationType().getName()).append(' ');
		}
		builder.append(ClassUtils.getQualifiedName(getType()));
		if (isMap()) {
			builder.append("<").append(wildcard(getMapKeyType()));
			builder.append(", ").append(wildcard(getMapValueType())).append(">");
		}
		else if (isCollection()) {
			builder.append("<").append(wildcard(getElementType())).append(">");
		}
		return builder.toString();
	}

	// package private

	TypeDescriptor(AbstractDescriptor descriptor) {
		this.type = descriptor.getType();
		this.elementType = descriptor.getElementType();
		this.mapKeyType = descriptor.getMapKeyType();
		this.mapValueType = descriptor.getMapValueType();
		this.annotations = descriptor.getAnnotations();
	}

	static Annotation[] nullSafeAnnotations(Annotation[] annotations) {
		return annotations != null ? annotations : EMPTY_ANNOTATION_ARRAY;
	}
	
	// internal constructors

	private TypeDescriptor(Class<?> type) {
		this(new ClassDescriptor(type));
	}

	private TypeDescriptor(Class<?> collectionType, TypeDescriptor elementType) {
		this(collectionType, elementType, null, null, EMPTY_ANNOTATION_ARRAY);
	}

	private TypeDescriptor(Class<?> mapType, TypeDescriptor keyType, TypeDescriptor valueType) {
		this(mapType, null, keyType, valueType, EMPTY_ANNOTATION_ARRAY);
	}

	private TypeDescriptor(Class<?> type, TypeDescriptor elementType, TypeDescriptor mapKeyType, TypeDescriptor mapValueType, Annotation[] annotations) {
		this.type = type;
		this.elementType = elementType;
		this.mapKeyType = mapKeyType;
		this.mapValueType = mapValueType;
		this.annotations = annotations;
	}

	private static TypeDescriptor nested(AbstractDescriptor descriptor, int nestingLevel) {
		for (int i = 0; i < nestingLevel; i++) {
			descriptor = descriptor.nested();
			if (descriptor == null) {
				return null;
			}
		}
		return new TypeDescriptor(descriptor);		
	}

	// internal helpers

	private boolean mapKeysAssignable(TypeDescriptor targetKeyType) {
		TypeDescriptor keyType = getMapKeyType();
		if (targetKeyType == null) {
			return true;
		}
		if (keyType == null) {
			return false;
		}
		return keyType.isAssignableTo(targetKeyType);		
	}

	private boolean collectionElementsAssignable(TypeDescriptor targetElementType) {
		TypeDescriptor elementType = getElementType();
		if (targetElementType == null) {
			return true;
		}
		if (elementType == null) {
			return false;
		}
		return elementType.isAssignableTo(targetElementType);				
	}
	
	private boolean mapValuesAssignable(TypeDescriptor targetValueType) {
		TypeDescriptor valueType = getMapValueType();
		if (targetValueType == null) {
			return true;
		}
		if (valueType == null) {
			return false;
		}
		return valueType.isAssignableTo(targetValueType);		
	}

	private void assertCollectionOrArray() {
		if (!isCollection() && !isArray()) {
			throw new IllegalStateException("Not a java.util.Collection or Array");
		}		
	}
	
	private void assertMap() {
		if (!isMap()) {
			throw new IllegalStateException("Not a java.util.Map");
		}		
	}
	
	private String wildcard(TypeDescriptor nestedType) {
		return nestedType != null ? nestedType.toString() : "?";
	}

}