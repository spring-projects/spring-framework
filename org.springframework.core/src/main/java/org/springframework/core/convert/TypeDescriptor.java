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

import org.springframework.core.MethodParameter;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Context about a type to convert from or to.
 *
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

	private final TypeDescriptor elementTypeDescriptor;

	private final TypeDescriptor mapKeyTypeDescriptor;

	private final TypeDescriptor mapValueTypeDescriptor;

	private final Annotation[] annotations;

	/**
	 * Create a new type descriptor from a {@link MethodParameter}.
	 * Use this constructor when a source or target conversion point is a constructor parameter, method parameter, or method return value. 
	 * @param methodParameter the method parameter
	 */
	public TypeDescriptor(MethodParameter methodParameter) {
		this(new ParameterDescriptor(methodParameter));
	}

	/**
	 * Create a new type descriptor from a {@link Field}.
	 * Use this constructor when source or target conversion point is a field.
	 * @param field the field
	 */
	public TypeDescriptor(Field field) {
		this(new FieldDescriptor(field));
	}

	/**
	 * Create a new type descriptor from a {@link Property}.
	 * Use this constructor when a source or target conversion point is a property on a Java class.
	 * @param property the property
	 */
	public TypeDescriptor(Property property) {
		this(new BeanPropertyDescriptor(property));
	}

	/**
	 * Create a new type descriptor from the given type.
	 * Use this to instruct the conversion system to convert an object to a specific target type, when no type location such as a method parameter or field is available to provide additional conversion context.
	 * Generally prefer use of {@link #forObject(Object)} for constructing type descriptors from source objects, as it handles the null object case.
	 * @param type the class
	 * @return the type descriptor
	 */
	public static TypeDescriptor valueOf(Class<?> type) {
		TypeDescriptor desc = typeDescriptorCache.get(type);
		return (desc != null ? desc : new TypeDescriptor(type));
	}

	/**
	 * Create a new type descriptor from a java.util.Collection type.
	 * Useful for converting to typed Collections.
	 * For example, a List&lt;String&gt; could be converted to a List&lt;EmailAddress&gt; by converting to a targetType built with this method.
	 * The method call to construct such a TypeDescriptor would look something like: collection(List.class, TypeDescriptor.valueOf(EmailAddress.class));
	 * @param collectionType the collection type, which must implement {@link Collection}.
	 * @param elementTypeDescriptor a descriptor for the collection's element type, used to convert collection elements
	 * @return the collection type descriptor
	 */
	public static TypeDescriptor collection(Class<?> collectionType, TypeDescriptor elementTypeDescriptor) {
		if (!Collection.class.isAssignableFrom(collectionType)) {
			throw new IllegalArgumentException("collectionType must be a java.util.Collection");
		}
		return new TypeDescriptor(collectionType, elementTypeDescriptor);
	}

	/**
	 * Create a new type descriptor from a java.util.Map type.
	 * Useful for Converting to typed Maps.
	 * For example, a Map&lt;String, String&gt; could be converted to a Map&lt;Id, EmailAddress&gt; by converting to a targetType built with this method:
	 * The method call to construct such a TypeDescriptor would look something like: map(Map.class, TypeDescriptor.valueOf(Id.class), TypeDescriptor.valueOf(EmailAddress.class));
	 * @param mapType the map type, which must implement {@link Map}.
	 * @param keyTypeDescriptor a descriptor for the map's key type, used to convert map keys
	 * @param valueTypeDescriptor the map's value type, used to convert map values
	 * @return the map type descriptor
	 */
	public static TypeDescriptor map(Class<?> mapType, TypeDescriptor keyTypeDescriptor, TypeDescriptor valueTypeDescriptor) {
		if (!Map.class.isAssignableFrom(mapType)) {
			throw new IllegalArgumentException("mapType must be a java.util.Map");
		}
		return new TypeDescriptor(mapType, keyTypeDescriptor, valueTypeDescriptor);
	}

	/**
	 * Creates a type descriptor for a nested type declared within the method parameter.
	 * For example, if the methodParameter is a List&lt;String&gt; and the nestingLevel is 1, the nested type descriptor will be String.class.
	 * If the methodParameter is a List<List<String>> and the nestingLevel is 2, the nested type descriptor will also be a String.class.
	 * If the methodParameter is a Map<Integer, String> and the nesting level is 1, the nested type descriptor will be String, derived from the map value.
	 * If the methodParameter is a List<Map<Integer, String>> and the nesting level is 2, the nested type descriptor will be String, derived from the map value.
	 * Returns null if a nested type cannot be obtained because it was not declared.
	 * For example, if the method parameter is a List&lt;?&gt;, the nested type descriptor returned will be null.
	 * @param methodParameter the method parameter with a nestingLevel of 1
	 * @param nestingLevel the nesting level of the collection/array element or map key/value declaration within the method parameter.
	 * @return the nested type descriptor at the specified nesting level, or null if it could not be obtained.
	 * @throws IllegalArgumentException if the nesting level of the input {@link MethodParameter} argument is not 1.
	 * @throws IllegalArgumentException if the types up to the specified nesting level are not of collection, array, or map types.
	 */
	public static TypeDescriptor nested(MethodParameter methodParameter, int nestingLevel) {
		if (methodParameter.getNestingLevel() != 1) {
			throw new IllegalArgumentException("methodParameter nesting level must be 1: use the nestingLevel parameter to specify the desired nestingLevel for nested type traversal");
		}
		return nested(new ParameterDescriptor(methodParameter), nestingLevel);
	}

	/**
	 * Creates a type descriptor for a nested type declared within the field.
	 * <p>For example, if the field is a <code>List&lt;String&gt;</code> and the nestingLevel is 1, the nested type descriptor will be <code>String.class</code>.
	 * If the field is a <code>List&lt;List&lt;String&gt;&gt;</code> and the nestingLevel is 2, the nested type descriptor will also be a <code>String.class</code>.
	 * If the field is a <code>Map&lt;Integer, String&gt;</code> and the nestingLevel is 1, the nested type descriptor will be String, derived from the map value.
	 * If the field is a <code>List&lt;Map&lt;Integer, String&gt;&gt;</code> and the nestingLevel is 2, the nested type descriptor will be String, derived from the map value.
	 * Returns <code>null</code> if a nested type cannot be obtained because it was not declared.
	 * For example, if the field is a <code>List&lt;?&gt;</code>, the nested type descriptor returned will be <code>null</code>.
	 * @param field the field
	 * @param nestingLevel the nesting level of the collection/array element or map key/value declaration within the field.
	 * @return the nested type descriptor at the specified nestingLevel, or null if it could not be obtained
	 * @throws IllegalArgumentException if the types up to the specified nesting level are not of collection, array, or map types.
	 */
	public static TypeDescriptor nested(Field field, int nestingLevel) {
		return nested(new FieldDescriptor(field), nestingLevel);
	}

	/**
	 * Creates a type descriptor for a nested type declared within the property.
	 * <p>For example, if the property is a <code>List&lt;String&gt;</code> and the nestingLevel is 1, the nested type descriptor will be <code>String.class</code>.
	 * If the property is a <code>List&lt;List&lt;String&gt;&gt;</code> and the nestingLevel is 2, the nested type descriptor will also be a <code>String.class</code>.
	 * If the property is a <code>Map&lt;Integer, String&gt;</code> and the nestingLevel is 1, the nested type descriptor will be String, derived from the map value.
	 * If the property is a <code>List&lt;Map&lt;Integer, String&gt;&gt;</code> and the nestingLevel is 2, the nested type descriptor will be String, derived from the map value.
	 * Returns <code>null</code> if a nested type cannot be obtained because it was not declared.
	 * For example, if the property is a <code>List&lt;?&gt;</code>, the nested type descriptor returned will be <code>null</code>.
	 * @param property the property
	 * @param nestingLevel the nesting level of the collection/array element or map key/value declaration within the property.
	 * @return the nested type descriptor at the specified nestingLevel, or <code>null</code> if it could not be obtained
	 * @throws IllegalArgumentException if the types up to the specified nesting level are not of collection, array, or map types.
	 */
	public static TypeDescriptor nested(Property property, int nestingLevel) {
		return nested(new BeanPropertyDescriptor(property), nestingLevel);
	}

	/**
	 * Create a new type descriptor for an object.
	 * Use this factory method to introspect a source object before asking the conversion system to convert it to some another type.
	 * If the provided object is null, returns null, else calls {@link #valueOf(Class)} to build a TypeDescriptor from the object's class.
	 * @param object the source object
	 * @return the type descriptor
	 */
	public static TypeDescriptor forObject(Object source) {
		return (source != null ? valueOf(source.getClass()) : null);
	}

	/**
	 * The type of the backing class, method parameter, field, or property described by this TypeDescriptor.
	 * Returns primitive types as-is.
	 * See {@link #getObjectType()} for a variation of this operation that resolves primitive types to their corresponding Object types if necessary.
	 * @return the type, or <code>null</code> if this is {@link TypeDescriptor#NULL}
	 * @see #getObjectType()
	 */
	public Class<?> getType() {
		return this.type;
	}

	/**
	 * Variation of {@link #getType()} that accounts for a primitive type by returning its object wrapper type.
	 * This is useful for conversion service implementations that wish to normalize to object-based types and not work with primitive types directly.
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
		return new TypeDescriptor(value.getClass(), this.elementTypeDescriptor,
				this.mapKeyTypeDescriptor, this.mapValueTypeDescriptor, this.annotations);
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
	 * Returns true if an object of this type descriptor can be assigned to the location described by the given type descriptor.
	 * For example, valueOf(String.class).isAssignableTo(valueOf(CharSequence.class)) returns true because a String value can be assigned to a CharSequence variable. 
	 * On the other hand, valueOf(Number.class).isAssignableTo(valueOf(Integer.class)) returns false because, while all Integers are Numbers, not all Numbers are Integers.
	 * <p>
	 * For arrays, collections, and maps, element and key/value types are checked if declared.
	 * For example, a List&lt;String&gt; field value is assignable to a Collection&lt;CharSequence&gt; field, but List&lt;Number&gt; is not assignable to List&lt;Integer&gt;.
	 * @return true if this type is assignable to the type represented by the provided type descriptor.
	 * @see #getObjectType()
	 */
	public boolean isAssignableTo(TypeDescriptor typeDescriptor) {
		boolean typesAssignable = typeDescriptor.getObjectType().isAssignableFrom(getObjectType());
		if (!typesAssignable) {
			return false;
		}
		if (isArray() && typeDescriptor.isArray()) {
			return getElementTypeDescriptor().isAssignableTo(typeDescriptor.getElementTypeDescriptor());
		}
		else if (isCollection() && typeDescriptor.isCollection()) {
			return isNestedAssignable(getElementTypeDescriptor(), typeDescriptor.getElementTypeDescriptor());
		}
		else if (isMap() && typeDescriptor.isMap()) {
			return isNestedAssignable(getMapKeyTypeDescriptor(), typeDescriptor.getMapKeyTypeDescriptor()) &&
				isNestedAssignable(getMapValueTypeDescriptor(), typeDescriptor.getMapValueTypeDescriptor());
		}
		else {
			return true;
		}
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
	public TypeDescriptor getElementTypeDescriptor() {
		assertCollectionOrArray();
		return this.elementTypeDescriptor;
	}

	/**
	 * If this type is a {@link Collection} or an Array, creates a element TypeDescriptor from the provided collection or array element.
	 * Narrows the {@link #getElementTypeDescriptor() elementType} property to the class of the provided collection or array element.
	 * For example, if this describes a java.util.List&lt;java.lang.Number&lt; and the element argument is a java.lang.Integer, the returned TypeDescriptor will be java.lang.Integer.
	 * If this describes a java.util.List&lt;?&gt; and the element argument is a java.lang.Integer, the returned TypeDescriptor will be java.lang.Integer as well.
	 * Annotation and nested type context will be preserved in the narrowed TypeDescriptor that is returned. 
	 * @param element the collection or array element
	 * @return a element type descriptor, narrowed to the type of the provided element
	 * @throws IllegalStateException if this type is not a java.util.Collection or Array type
	 * @see #narrow(Object)
	 */
	public TypeDescriptor elementTypeDescriptor(Object element) {
		return narrow(element, getElementTypeDescriptor());
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
	public TypeDescriptor getMapKeyTypeDescriptor() {
		assertMap();
		return this.mapKeyTypeDescriptor;
	}

	/**
	 * If this type is a {@link Map}, creates a mapKey {@link TypeDescriptor} from the provided map key.
	 * Narrows the {@link #getMapKeyTypeDescriptor() mapKeyType} property to the class of the provided map key.
	 * For example, if this describes a java.util.Map&lt;java.lang.Number, java.lang.String&lt; and the key argument is a java.lang.Integer, the returned TypeDescriptor will be java.lang.Integer.
	 * If this describes a java.util.Map&lt;?, ?&gt; and the key argument is a java.lang.Integer, the returned TypeDescriptor will be java.lang.Integer as well.
	 * Annotation and nested type context will be preserved in the narrowed TypeDescriptor that is returned. 
	 * @param mapKey the map key
	 * @return the map key type descriptor
	 * @throws IllegalStateException if this type is not a java.util.Map.
	 * @see #narrow(Object)
	 */
	public TypeDescriptor getMapKeyTypeDescriptor(Object mapKey) {
		return narrow(mapKey, getMapKeyTypeDescriptor());
	}

	/**
	 * If this type is a {@link Map} and its value type is parameterized, returns the map's value type.
	 * If the Map's value type is not parameterized, returns null indicating the value type is not declared.
	 * @return the Map value type, or <code>null</code> if this type is a Map but its value type is not parameterized.
	 * @throws IllegalStateException if this type is not a java.util.Map.
	 */
	public TypeDescriptor getMapValueTypeDescriptor() {
		assertMap();
		return this.mapValueTypeDescriptor;
	}

	/**
	 * If this type is a {@link Map}, creates a mapValue {@link TypeDescriptor} from the provided map value.
	 * Narrows the {@link #getMapValueTypeDescriptor() mapValueType} property to the class of the provided map value.
	 * For example, if this describes a java.util.Map&lt;java.lang.String, java.lang.Number&lt; and the value argument is a java.lang.Integer, the returned TypeDescriptor will be java.lang.Integer.
	 * If this describes a java.util.Map&lt;?, ?&gt; and the value argument is a java.lang.Integer, the returned TypeDescriptor will be java.lang.Integer as well.
	 * Annotation and nested type context will be preserved in the narrowed TypeDescriptor that is returned. 
	 * @param mapValue the map value
	 * @return the map value type descriptor
	 * @throws IllegalStateException if this type is not a java.util.Map. 
	 */
	public TypeDescriptor getMapValueTypeDescriptor(Object mapValue) {
		return narrow(mapValue, getMapValueTypeDescriptor());		
	}


	// deprecations in Spring 3.1
	
	/**
	 * Returns the value of {@link TypeDescriptor#getType() getType()} for the {@link #getElementTypeDescriptor() elementTypeDescriptor}.
	 * @deprecated in Spring 3.1 in favor of {@link #getElementTypeDescriptor()}.
	 * @throws IllegalStateException if this type is not a java.util.Collection or Array type
	 */
	@Deprecated
	public Class<?> getElementType() {
		return getElementTypeDescriptor().getType();
	}

	/**
	 * Returns the value of {@link TypeDescriptor#getType() getType()} for the {@link #getMapKeyTypeDescriptor() getMapKeyTypeDescriptor}.
	 * @deprecated in Spring 3.1 in favor of {@link #getMapKeyTypeDescriptor()}.
	 * @throws IllegalStateException if this type is not a java.util.Map.
	 */
	@Deprecated
	public Class<?> getMapKeyType() {
		return getMapKeyTypeDescriptor().getType();
	}

	/**
	 * Returns the value of {@link TypeDescriptor#getType() getType()} for the {@link #getMapValueTypeDescriptor() getMapValueTypeDescriptor}.
	 * @deprecated in Spring 3.1 in favor of {@link #getMapValueTypeDescriptor()}.
	 * @throws IllegalStateException if this type is not a java.util.Map.
	 */
	@Deprecated
	public Class<?> getMapValueType() {
		return getMapValueTypeDescriptor().getType();
	}

	// package private helpers

	TypeDescriptor(AbstractDescriptor descriptor) {
		this.type = descriptor.getType();
		this.elementTypeDescriptor = descriptor.getElementTypeDescriptor();
		this.mapKeyTypeDescriptor = descriptor.getMapKeyTypeDescriptor();
		this.mapValueTypeDescriptor = descriptor.getMapValueTypeDescriptor();
		this.annotations = descriptor.getAnnotations();
	}

	static Annotation[] nullSafeAnnotations(Annotation[] annotations) {
		return annotations != null ? annotations : EMPTY_ANNOTATION_ARRAY;
	}


	// internal constructors

	private TypeDescriptor(Class<?> type) {
		this(new ClassDescriptor(type));
	}

	private TypeDescriptor(Class<?> collectionType, TypeDescriptor elementTypeDescriptor) {
		this(collectionType, elementTypeDescriptor, null, null, EMPTY_ANNOTATION_ARRAY);
	}

	private TypeDescriptor(Class<?> mapType, TypeDescriptor keyTypeDescriptor, TypeDescriptor valueTypeDescriptor) {
		this(mapType, null, keyTypeDescriptor, valueTypeDescriptor, EMPTY_ANNOTATION_ARRAY);
	}

	private TypeDescriptor(Class<?> type, TypeDescriptor elementTypeDescriptor, TypeDescriptor mapKeyTypeDescriptor,
			TypeDescriptor mapValueTypeDescriptor, Annotation[] annotations) {
		this.type = type;
		this.elementTypeDescriptor = elementTypeDescriptor;
		this.mapKeyTypeDescriptor = mapKeyTypeDescriptor;
		this.mapValueTypeDescriptor = mapValueTypeDescriptor;
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

	private TypeDescriptor narrow(Object value, TypeDescriptor typeDescriptor) {
		if (typeDescriptor != null) {
			return typeDescriptor.narrow(value);
		}
		else {
			return (value != null ? new TypeDescriptor(value.getClass(), null, null, null, this.annotations) : null);
		}		
	}

	private boolean isNestedAssignable(TypeDescriptor nestedTypeDescriptor, TypeDescriptor otherNestedTypeDescriptor) {
		if (nestedTypeDescriptor == null || otherNestedTypeDescriptor == null) {
			return true;
		}
		return nestedTypeDescriptor.isAssignableTo(otherNestedTypeDescriptor);
	}

	private String wildcard(TypeDescriptor typeDescriptor) {
		return typeDescriptor != null ? typeDescriptor.toString() : "?";
	}


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
			return ObjectUtils.nullSafeEquals(getElementTypeDescriptor(), other.getElementTypeDescriptor());
		}
		else if (isMap()) {
			return ObjectUtils.nullSafeEquals(getMapKeyTypeDescriptor(), other.getMapKeyTypeDescriptor()) && ObjectUtils.nullSafeEquals(getMapValueTypeDescriptor(), other.getMapValueTypeDescriptor());
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
			builder.append("<").append(wildcard(getMapKeyTypeDescriptor()));
			builder.append(", ").append(wildcard(getMapValueTypeDescriptor())).append(">");
		}
		else if (isCollection()) {
			builder.append("<").append(wildcard(getElementTypeDescriptor())).append(">");
		}
		return builder.toString();
	}

}
