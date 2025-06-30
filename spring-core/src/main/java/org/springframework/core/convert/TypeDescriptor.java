/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.convert;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementAdapter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Contract;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Contextual descriptor about a type to convert from or to.
 *
 * <p>Capable of representing arrays and generic collection types.
 *
 * @author Keith Donald
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @since 3.0
 * @see ConversionService#canConvert(TypeDescriptor, TypeDescriptor)
 * @see ConversionService#convert(Object, TypeDescriptor, TypeDescriptor)
 */
@SuppressWarnings("serial")
public class TypeDescriptor implements Serializable {

	private static final Map<Class<?>, TypeDescriptor> commonTypesCache = new HashMap<>(32);

	private static final Class<?>[] CACHED_COMMON_TYPES = {
			boolean.class, Boolean.class, byte.class, Byte.class, char.class, Character.class,
			double.class, Double.class, float.class, Float.class, int.class, Integer.class,
			long.class, Long.class, short.class, Short.class, String.class, Object.class};

	static {
		for (Class<?> preCachedClass : CACHED_COMMON_TYPES) {
			commonTypesCache.put(preCachedClass, valueOf(preCachedClass));
		}
	}


	private final Class<?> type;

	private final ResolvableType resolvableType;

	private final AnnotatedElementSupplier annotatedElementSupplier;

	private volatile @Nullable AnnotatedElementAdapter annotatedElement;


	/**
	 * Create a new type descriptor from a {@link MethodParameter}.
	 * <p>Use this constructor when a source or target conversion point is a
	 * constructor parameter, method parameter, or method return value.
	 * @param methodParameter the method parameter
	 */
	public TypeDescriptor(MethodParameter methodParameter) {
		this.resolvableType = ResolvableType.forMethodParameter(methodParameter);
		this.type = this.resolvableType.resolve(methodParameter.getNestedParameterType());
		this.annotatedElementSupplier = () -> AnnotatedElementAdapter.from(methodParameter.getParameterIndex() == -1 ?
				methodParameter.getMethodAnnotations() : methodParameter.getParameterAnnotations());
	}

	/**
	 * Create a new type descriptor from a {@link Field}.
	 * <p>Use this constructor when a source or target conversion point is a field.
	 * @param field the field
	 */
	public TypeDescriptor(Field field) {
		this.resolvableType = ResolvableType.forField(field);
		this.type = this.resolvableType.resolve(field.getType());
		this.annotatedElementSupplier = () -> AnnotatedElementAdapter.from(field.getAnnotations());
	}

	/**
	 * Create a new type descriptor from a {@link Property}.
	 * <p>Use this constructor when a source or target conversion point is a
	 * property on a Java class.
	 * @param property the property
	 */
	public TypeDescriptor(Property property) {
		Assert.notNull(property, "Property must not be null");
		this.resolvableType = ResolvableType.forMethodParameter(property.getMethodParameter());
		this.type = this.resolvableType.resolve(property.getType());
		this.annotatedElementSupplier = () -> AnnotatedElementAdapter.from(property.getAnnotations());
	}

	/**
	 * Create a new type descriptor from a {@link ResolvableType}.
	 * <p>This constructor is used internally and may also be used by subclasses
	 * that support non-Java languages with extended type systems. It is public
	 * as of 5.1.4 whereas it was protected before.
	 * @param resolvableType the resolvable type
	 * @param type the backing type (or {@code null} if it should get resolved)
	 * @param annotations the type annotations
	 * @since 4.0
	 */
	public TypeDescriptor(ResolvableType resolvableType, @Nullable Class<?> type, Annotation @Nullable [] annotations) {
		this.resolvableType = resolvableType;
		this.type = (type != null ? type : resolvableType.toClass());
		this.annotatedElementSupplier = () -> AnnotatedElementAdapter.from(annotations);
	}


	/**
	 * Variation of {@link #getType()} that accounts for a primitive type by
	 * returning its object wrapper type.
	 * <p>This is useful for conversion service implementations that wish to
	 * normalize to object-based types and not work with primitive types directly.
	 */
	public Class<?> getObjectType() {
		return ClassUtils.resolvePrimitiveIfNecessary(getType());
	}

	/**
	 * The type of the backing class, method parameter, field, or property
	 * described by this TypeDescriptor.
	 * <p>Returns primitive types as-is. See {@link #getObjectType()} for a
	 * variation of this operation that resolves primitive types to their
	 * corresponding Object types if necessary.
	 * @see #getObjectType()
	 */
	public Class<?> getType() {
		return this.type;
	}

	/**
	 * Return the underlying {@link ResolvableType}.
	 * @since 4.0
	 */
	public ResolvableType getResolvableType() {
		return this.resolvableType;
	}

	/**
	 * Return the underlying source of the descriptor. Will return a {@link Field},
	 * {@link MethodParameter} or {@link Type} depending on how the {@link TypeDescriptor}
	 * was constructed. This method is primarily to provide access to additional
	 * type information or meta-data that alternative JVM languages may provide.
	 * @since 4.0
	 */
	public Object getSource() {
		return this.resolvableType.getSource();
	}


	/**
	 * Create a type descriptor for a nested type declared within this descriptor.
	 * @param nestingLevel the nesting level of the collection/array element or
	 * map key/value declaration within the property
	 * @return the nested type descriptor at the specified nesting level, or
	 * {@code null} if it could not be obtained
	 * @since 6.1
	 */
	public @Nullable TypeDescriptor nested(int nestingLevel) {
		ResolvableType nested = this.resolvableType;
		for (int i = 0; i < nestingLevel; i++) {
			if (Object.class == nested.getType()) {
				// Could be a collection type but we don't know about its element type,
				// so let's just assume there is an element type of type Object...
			}
			else {
				nested = nested.getNested(2);
			}
		}
		if (nested == ResolvableType.NONE) {
			return null;
		}
		return getRelatedIfResolvable(nested);
	}

	/**
	 * Narrows this {@link TypeDescriptor} by setting its type to the class of the
	 * provided value.
	 * <p>If the value is {@code null}, no narrowing is performed and this TypeDescriptor
	 * is returned unchanged.
	 * <p>Designed to be called by binding frameworks when they read property, field,
	 * or method return values. Allows such frameworks to narrow a TypeDescriptor built
	 * from a declared property, field, or method return value type. For example, a field
	 * declared as {@code java.lang.Object} would be narrowed to {@code java.util.HashMap}
	 * if it was set to a {@code java.util.HashMap} value. The narrowed TypeDescriptor
	 * can then be used to convert the HashMap to some other type. Annotation and nested
	 * type context is preserved by the narrowed copy.
	 * @param value the value to use for narrowing this type descriptor
	 * @return this TypeDescriptor narrowed (returns a copy with its type updated to the
	 * class of the provided value)
	 */
	public TypeDescriptor narrow(@Nullable Object value) {
		if (value == null) {
			return this;
		}
		ResolvableType narrowed = ResolvableType.forType(value.getClass(), getResolvableType());
		return new TypeDescriptor(narrowed, value.getClass(), getAnnotations());
	}

	/**
	 * Cast this {@link TypeDescriptor} to a superclass or implemented interface
	 * preserving annotations and nested type context.
	 * @param superType the supertype to cast to (can be {@code null})
	 * @return a new TypeDescriptor for the up-cast type
	 * @throws IllegalArgumentException if this type is not assignable to the super-type
	 * @since 3.2
	 */
	public @Nullable TypeDescriptor upcast(@Nullable Class<?> superType) {
		if (superType == null) {
			return null;
		}
		Assert.isAssignable(superType, getType());
		return new TypeDescriptor(getResolvableType().as(superType), superType, getAnnotations());
	}

	/**
	 * Return the name of this type: the fully qualified class name.
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

	private AnnotatedElementAdapter getAnnotatedElement() {
		AnnotatedElementAdapter annotatedElement = this.annotatedElement;
		if (annotatedElement == null) {
			annotatedElement = this.annotatedElementSupplier.get();
			this.annotatedElement = annotatedElement;
		}
		return annotatedElement;
	}

	/**
	 * Return the annotations associated with this type descriptor, if any.
	 * @return the annotations, or an empty array if none
	 */
	public Annotation[] getAnnotations() {
		return getAnnotatedElement().getAnnotations();
	}

	/**
	 * Determine if this type descriptor has the specified annotation.
	 * <p>As of Spring Framework 4.2, this method supports arbitrary levels
	 * of meta-annotations.
	 * @param annotationType the annotation type
	 * @return {@code true} if the annotation is present
	 */
	public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
		AnnotatedElementAdapter annotatedElement = getAnnotatedElement();
		if (annotatedElement.isEmpty()) {
			// Shortcut: AnnotatedElementUtils would have to expect AnnotatedElement.getAnnotations()
			// to return a copy of the array, whereas we can do it more efficiently here.
			return false;
		}
		return AnnotatedElementUtils.isAnnotated(annotatedElement, annotationType);
	}

	/**
	 * Obtain the annotation of the specified {@code annotationType} that is on this type descriptor.
	 * <p>As of Spring Framework 4.2, this method supports arbitrary levels of meta-annotations.
	 * @param annotationType the annotation type
	 * @return the annotation, or {@code null} if no such annotation exists on this type descriptor
	 */
	public <T extends Annotation> @Nullable T getAnnotation(Class<T> annotationType) {
		AnnotatedElementAdapter annotatedElement = getAnnotatedElement();
		if (annotatedElement.isEmpty()) {
			// Shortcut: AnnotatedElementUtils would have to expect AnnotatedElement.getAnnotations()
			// to return a copy of the array, whereas we can do it more efficiently here.
			return null;
		}
		return AnnotatedElementUtils.getMergedAnnotation(annotatedElement, annotationType);
	}

	/**
	 * Returns true if an object of this type descriptor can be assigned to the location
	 * described by the given type descriptor.
	 * <p>For example, {@code valueOf(String.class).isAssignableTo(valueOf(CharSequence.class))}
	 * returns {@code true} because a String value can be assigned to a CharSequence variable.
	 * On the other hand, {@code valueOf(Number.class).isAssignableTo(valueOf(Integer.class))}
	 * returns {@code false} because, while all Integers are Numbers, not all Numbers are Integers.
	 * <p>For arrays, collections, and maps, element and key/value types are checked if declared.
	 * For example, a {@code List<String>} field value is assignable to a {@code Collection<CharSequence>}
	 * field, but {@code List<Number>} is not assignable to {@code List<Integer>}.
	 * @return {@code true} if this type is assignable to the type represented by the provided
	 * type descriptor
	 * @see #getObjectType()
	 */
	public boolean isAssignableTo(TypeDescriptor typeDescriptor) {
		boolean typesAssignable = typeDescriptor.getObjectType().isAssignableFrom(getObjectType());
		if (!typesAssignable) {
			return false;
		}
		if (isArray() && typeDescriptor.isArray()) {
			return isNestedAssignable(getElementTypeDescriptor(), typeDescriptor.getElementTypeDescriptor());
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

	private boolean isNestedAssignable(@Nullable TypeDescriptor nestedTypeDescriptor,
			@Nullable TypeDescriptor otherNestedTypeDescriptor) {

		return (nestedTypeDescriptor == null || otherNestedTypeDescriptor == null ||
				nestedTypeDescriptor.isAssignableTo(otherNestedTypeDescriptor));
	}

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
	 * If this type is a {@code Stream}, returns the stream's component type.
	 * If this type is a {@link Collection} and it is parameterized, returns the Collection's element type.
	 * If the Collection is not parameterized, returns {@code null} indicating the element type is not declared.
	 * @return the array component type or Collection element type, or {@code null} if this type is not
	 * an array type or a {@code java.util.Collection} or if its element type is not parameterized
	 * @see #elementTypeDescriptor(Object)
	 */
	public @Nullable TypeDescriptor getElementTypeDescriptor() {
		if (getResolvableType().isArray()) {
			return new TypeDescriptor(getResolvableType().getComponentType(), null, getAnnotations());
		}
		if (Stream.class.isAssignableFrom(getType())) {
			return getRelatedIfResolvable(getResolvableType().as(Stream.class).getGeneric(0));
		}
		return getRelatedIfResolvable(getResolvableType().asCollection().getGeneric(0));
	}

	/**
	 * If this type is a {@link Collection} or an array, creates an element TypeDescriptor
	 * from the provided collection or array element.
	 * <p>Narrows the {@link #getElementTypeDescriptor() elementType} property to the class
	 * of the provided collection or array element. For example, if this describes a
	 * {@code java.util.List<java.lang.Number>} and the element argument is a
	 * {@code java.lang.Integer}, the returned TypeDescriptor will be {@code java.lang.Integer}.
	 * If this describes a {@code java.util.List<?>} and the element argument is a
	 * {@code java.lang.Integer}, the returned TypeDescriptor will be {@code java.lang.Integer}
	 * as well.
	 * <p>Annotation and nested type context will be preserved in the narrowed
	 * TypeDescriptor that is returned.
	 * @param element the collection or array element
	 * @return an element type descriptor, narrowed to the type of the provided element
	 * @see #getElementTypeDescriptor()
	 * @see #narrow(Object)
	 */
	public @Nullable TypeDescriptor elementTypeDescriptor(Object element) {
		return narrow(element, getElementTypeDescriptor());
	}

	/**
	 * Is this type a {@link Map} type?
	 */
	public boolean isMap() {
		return Map.class.isAssignableFrom(getType());
	}

	/**
	 * If this type is a {@link Map} and its key type is parameterized,
	 * returns the map's key type. If the Map's key type is not parameterized,
	 * returns {@code null} indicating the key type is not declared.
	 * @return the Map key type, or {@code null} if this type is a Map
	 * but its key type is not parameterized
	 * @throws IllegalStateException if this type is not a {@code java.util.Map}
	 */
	public @Nullable TypeDescriptor getMapKeyTypeDescriptor() {
		Assert.state(isMap(), "Not a [java.util.Map]");
		return getRelatedIfResolvable(getResolvableType().asMap().getGeneric(0));
	}

	/**
	 * If this type is a {@link Map}, creates a mapKey {@link TypeDescriptor}
	 * from the provided map key.
	 * <p>Narrows the {@link #getMapKeyTypeDescriptor() mapKeyType} property
	 * to the class of the provided map key. For example, if this describes a
	 * {@code java.util.Map<java.lang.Number, java.lang.String>} and the key
	 * argument is a {@code java.lang.Integer}, the returned TypeDescriptor will be
	 * {@code java.lang.Integer}. If this describes a {@code java.util.Map<?, ?>}
	 * and the key argument is a {@code java.lang.Integer}, the returned
	 * TypeDescriptor will be {@code java.lang.Integer} as well.
	 * <p>Annotation and nested type context will be preserved in the narrowed
	 * TypeDescriptor that is returned.
	 * @param mapKey the map key
	 * @return the map key type descriptor
	 * @throws IllegalStateException if this type is not a {@code java.util.Map}
	 * @see #narrow(Object)
	 */
	public @Nullable TypeDescriptor getMapKeyTypeDescriptor(Object mapKey) {
		return narrow(mapKey, getMapKeyTypeDescriptor());
	}

	/**
	 * If this type is a {@link Map} and its value type is parameterized,
	 * returns the map's value type.
	 * <p>If the Map's value type is not parameterized, returns {@code null}
	 * indicating the value type is not declared.
	 * @return the Map value type, or {@code null} if this type is a Map
	 * but its value type is not parameterized
	 * @throws IllegalStateException if this type is not a {@code java.util.Map}
	 */
	public @Nullable TypeDescriptor getMapValueTypeDescriptor() {
		Assert.state(isMap(), "Not a [java.util.Map]");
		return getRelatedIfResolvable(getResolvableType().asMap().getGeneric(1));
	}

	/**
	 * If this type is a {@link Map}, creates a mapValue {@link TypeDescriptor}
	 * from the provided map value.
	 * <p>Narrows the {@link #getMapValueTypeDescriptor() mapValueType} property
	 * to the class of the provided map value. For example, if this describes a
	 * {@code java.util.Map<java.lang.String, java.lang.Number>} and the value
	 * argument is a {@code java.lang.Integer}, the returned TypeDescriptor will be
	 * {@code java.lang.Integer}. If this describes a {@code java.util.Map<?, ?>}
	 * and the value argument is a {@code java.lang.Integer}, the returned
	 * TypeDescriptor will be {@code java.lang.Integer} as well.
	 * <p>Annotation and nested type context will be preserved in the narrowed
	 * TypeDescriptor that is returned.
	 * @param mapValue the map value
	 * @return the map value type descriptor
	 * @throws IllegalStateException if this type is not a {@code java.util.Map}
	 * @see #narrow(Object)
	 */
	public @Nullable TypeDescriptor getMapValueTypeDescriptor(@Nullable Object mapValue) {
		return narrow(mapValue, getMapValueTypeDescriptor());
	}

	private @Nullable TypeDescriptor getRelatedIfResolvable(ResolvableType type) {
		if (type.resolve() == null) {
			return null;
		}
		return new TypeDescriptor(type, null, getAnnotations());
	}

	private @Nullable TypeDescriptor narrow(@Nullable Object value, @Nullable TypeDescriptor typeDescriptor) {
		if (typeDescriptor != null) {
			return typeDescriptor.narrow(value);
		}
		if (value != null) {
			return narrow(value);
		}
		return null;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TypeDescriptor otherDesc)) {
			return false;
		}
		if (getType() != otherDesc.getType()) {
			return false;
		}
		if (!annotationsMatch(otherDesc)) {
			return false;
		}
		return Arrays.equals(getResolvableType().getGenerics(), otherDesc.getResolvableType().getGenerics());
	}

	private boolean annotationsMatch(TypeDescriptor otherDesc) {
		Annotation[] anns = getAnnotations();
		Annotation[] otherAnns = otherDesc.getAnnotations();
		if (anns == otherAnns) {
			return true;
		}
		if (anns.length != otherAnns.length) {
			return false;
		}
		if (anns.length > 0) {
			for (int i = 0; i < anns.length; i++) {
				if (!annotationEquals(anns[i], otherAnns[i])) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean annotationEquals(Annotation ann, Annotation otherAnn) {
		// Annotation.equals is reflective and pretty slow, so let's check identity and proxy type first.
		return (ann == otherAnn || (ann.getClass() == otherAnn.getClass() && ann.equals(otherAnn)));
	}

	@Override
	public int hashCode() {
		return getType().hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Annotation ann : getAnnotations()) {
			builder.append('@').append(getName(ann.annotationType())).append(' ');
		}
		builder.append(getResolvableType());
		return builder.toString();
	}


	/**
	 * Create a new type descriptor for an object.
	 * <p>Use this factory method to introspect a source object before asking the
	 * conversion system to convert it to some other type.
	 * <p>If the provided object is {@code null}, returns {@code null}, else calls
	 * {@link #valueOf(Class)} to build a TypeDescriptor from the object's class.
	 * @param source the source object
	 * @return the type descriptor
	 */
	@Contract("!null -> !null; null -> null")
	public static @Nullable TypeDescriptor forObject(@Nullable Object source) {
		return (source != null ? valueOf(source.getClass()) : null);
	}

	/**
	 * Create a new type descriptor from the given type.
	 * <p>Use this to instruct the conversion system to convert an object to a
	 * specific target type, when no type location such as a method parameter or
	 * field is available to provide additional conversion context.
	 * <p>Generally prefer use of {@link #forObject(Object)} for constructing type
	 * descriptors from source objects, as it handles the {@code null} object case.
	 * @param type the class (may be {@code null} to indicate {@code Object.class})
	 * @return the corresponding type descriptor
	 */
	public static TypeDescriptor valueOf(@Nullable Class<?> type) {
		if (type == null) {
			type = Object.class;
		}
		TypeDescriptor desc = commonTypesCache.get(type);
		return (desc != null ? desc : new TypeDescriptor(ResolvableType.forClass(type), null, null));
	}

	/**
	 * Create a new type descriptor from a {@link java.util.Collection} type.
	 * <p>Useful for converting to typed Collections.
	 * <p>For example, a {@code List<String>} could be converted to a
	 * {@code List<EmailAddress>} by converting to a targetType built with this method.
	 * The method call to construct such a {@code TypeDescriptor} would look something
	 * like: {@code collection(List.class, TypeDescriptor.valueOf(EmailAddress.class));}
	 * @param collectionType the collection type, which must implement {@link Collection}.
	 * @param elementTypeDescriptor a descriptor for the collection's element type,
	 * used to convert collection elements
	 * @return the collection type descriptor
	 */
	public static TypeDescriptor collection(Class<?> collectionType, @Nullable TypeDescriptor elementTypeDescriptor) {
		Assert.notNull(collectionType, "Collection type must not be null");
		if (!Collection.class.isAssignableFrom(collectionType)) {
			throw new IllegalArgumentException("Collection type must be a [java.util.Collection]");
		}
		ResolvableType element = (elementTypeDescriptor != null ? elementTypeDescriptor.resolvableType : null);
		return new TypeDescriptor(ResolvableType.forClassWithGenerics(collectionType, element), null, null);
	}

	/**
	 * Create a new type descriptor from a {@link java.util.Map} type.
	 * <p>Useful for converting to typed Maps.
	 * <p>For example, a {@code Map<String, String>} could be converted to a {@code Map<Id, EmailAddress>}
	 * by converting to a targetType built with this method:
	 * The method call to construct such a TypeDescriptor would look something like:
	 * <pre class="code">
	 * map(Map.class, TypeDescriptor.valueOf(Id.class), TypeDescriptor.valueOf(EmailAddress.class));
	 * </pre>
	 * @param mapType the map type, which must implement {@link Map}
	 * @param keyTypeDescriptor a descriptor for the map's key type, used to convert map keys
	 * @param valueTypeDescriptor the map's value type, used to convert map values
	 * @return the map type descriptor
	 */
	public static TypeDescriptor map(Class<?> mapType, @Nullable TypeDescriptor keyTypeDescriptor,
			@Nullable TypeDescriptor valueTypeDescriptor) {

		Assert.notNull(mapType, "Map type must not be null");
		if (!Map.class.isAssignableFrom(mapType)) {
			throw new IllegalArgumentException("Map type must be a [java.util.Map]");
		}
		ResolvableType key = (keyTypeDescriptor != null ? keyTypeDescriptor.resolvableType : null);
		ResolvableType value = (valueTypeDescriptor != null ? valueTypeDescriptor.resolvableType : null);
		return new TypeDescriptor(ResolvableType.forClassWithGenerics(mapType, key, value), null, null);
	}

	/**
	 * Create a new type descriptor as an array of the specified type.
	 * <p>For example to create a {@code Map<String,String>[]} use:
	 * <pre class="code">
	 * TypeDescriptor.array(TypeDescriptor.map(Map.class, TypeDescriptor.value(String.class), TypeDescriptor.value(String.class)));
	 * </pre>
	 * @param elementTypeDescriptor the {@link TypeDescriptor} of the array element or {@code null}
	 * @return an array {@link TypeDescriptor} or {@code null} if {@code elementTypeDescriptor} is {@code null}
	 * @since 3.2.1
	 */
	@Contract("!null -> !null; null -> null")
	public static @Nullable TypeDescriptor array(@Nullable TypeDescriptor elementTypeDescriptor) {
		if (elementTypeDescriptor == null) {
			return null;
		}
		return new TypeDescriptor(ResolvableType.forArrayComponent(elementTypeDescriptor.resolvableType),
				null, elementTypeDescriptor.getAnnotations());
	}

	/**
	 * Create a type descriptor for a nested type declared within the method parameter.
	 * <p>For example, if the methodParameter is a {@code List<String>} and the
	 * nesting level is 1, the nested type descriptor will be String.class.
	 * <p>If the methodParameter is a {@code List<List<String>>} and the nesting
	 * level is 2, the nested type descriptor will also be a String.class.
	 * <p>If the methodParameter is a {@code Map<Integer, String>} and the nesting
	 * level is 1, the nested type descriptor will be String, derived from the map value.
	 * <p>If the methodParameter is a {@code List<Map<Integer, String>>} and the
	 * nesting level is 2, the nested type descriptor will be String, derived from the map value.
	 * <p>Returns {@code null} if a nested type cannot be obtained because it was not declared.
	 * For example, if the method parameter is a {@code List<?>}, the nested type
	 * descriptor returned will be {@code null}.
	 * @param methodParameter the method parameter with a nestingLevel of 1
	 * @param nestingLevel the nesting level of the collection/array element or
	 * map key/value declaration within the method parameter
	 * @return the nested type descriptor at the specified nesting level,
	 * or {@code null} if it could not be obtained
	 * @throws IllegalArgumentException if the nesting level of the input
	 * {@link MethodParameter} argument is not 1, or if the types up to the
	 * specified nesting level are not of collection, array, or map types
	 */
	public static @Nullable TypeDescriptor nested(MethodParameter methodParameter, int nestingLevel) {
		if (methodParameter.getNestingLevel() != 1) {
			throw new IllegalArgumentException("MethodParameter nesting level must be 1: " +
					"use the nestingLevel parameter to specify the desired nestingLevel for nested type traversal");
		}
		return new TypeDescriptor(methodParameter).nested(nestingLevel);
	}

	/**
	 * Create a type descriptor for a nested type declared within the field.
	 * <p>For example, if the field is a {@code List<String>} and the nesting
	 * level is 1, the nested type descriptor will be {@code String.class}.
	 * <p>If the field is a {@code List<List<String>>} and the nesting level is
	 * 2, the nested type descriptor will also be a {@code String.class}.
	 * <p>If the field is a {@code Map<Integer, String>} and the nesting level
	 * is 1, the nested type descriptor will be String, derived from the map value.
	 * <p>If the field is a {@code List<Map<Integer, String>>} and the nesting
	 * level is 2, the nested type descriptor will be String, derived from the map value.
	 * <p>Returns {@code null} if a nested type cannot be obtained because it was not
	 * declared. For example, if the field is a {@code List<?>}, the nested type
	 * descriptor returned will be {@code null}.
	 * @param field the field
	 * @param nestingLevel the nesting level of the collection/array element or
	 * map key/value declaration within the field
	 * @return the nested type descriptor at the specified nesting level,
	 * or {@code null} if it could not be obtained
	 * @throws IllegalArgumentException if the types up to the specified nesting
	 * level are not of collection, array, or map types
	 */
	public static @Nullable TypeDescriptor nested(Field field, int nestingLevel) {
		return new TypeDescriptor(field).nested(nestingLevel);
	}

	/**
	 * Create a type descriptor for a nested type declared within the property.
	 * <p>For example, if the property is a {@code List<String>} and the nesting
	 * level is 1, the nested type descriptor will be {@code String.class}.
	 * <p>If the property is a {@code List<List<String>>} and the nesting level
	 * is 2, the nested type descriptor will also be a {@code String.class}.
	 * <p>If the property is a {@code Map<Integer, String>} and the nesting level
	 * is 1, the nested type descriptor will be String, derived from the map value.
	 * <p>If the property is a {@code List<Map<Integer, String>>} and the nesting
	 * level is 2, the nested type descriptor will be String, derived from the map value.
	 * <p>Returns {@code null} if a nested type cannot be obtained because it was not
	 * declared. For example, if the property is a {@code List<?>}, the nested type
	 * descriptor returned will be {@code null}.
	 * @param property the property
	 * @param nestingLevel the nesting level of the collection/array element or
	 * map key/value declaration within the property
	 * @return the nested type descriptor at the specified nesting level, or
	 * {@code null} if it could not be obtained
	 * @throws IllegalArgumentException if the types up to the specified nesting
	 * level are not of collection, array, or map types
	 */
	public static @Nullable TypeDescriptor nested(Property property, int nestingLevel) {
		return new TypeDescriptor(property).nested(nestingLevel);
	}

	private static String getName(Class<?> clazz) {
		String canonicalName = clazz.getCanonicalName();
		return (canonicalName != null ? canonicalName : clazz.getName());
	}


	private interface AnnotatedElementSupplier extends Supplier<AnnotatedElementAdapter>, Serializable {
	}

}
