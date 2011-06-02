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
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

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


	private final Class<?> type;

	private final TypeDescriptor elementType;

	private final TypeDescriptor mapKeyType;

	private final TypeDescriptor mapValueType;

	private final Annotation[] annotations;

	/**
	 * Create a new type descriptor from a method or constructor parameter.
	 * Use this constructor when a target conversion point is a method parameter.
	 * @param methodParameter the method parameter
	 */
	public TypeDescriptor(MethodParameter methodParameter) {
		this(new ParameterDescriptor(methodParameter));
	}

	/**
	 * Create a new type descriptor for a field.
	 * Use this constructor when a target conversion point is a field.
	 * @param field the field
	 */
	public TypeDescriptor(Field field) {
		this(new FieldDescriptor(field));
	}

	/**
	 * Create a new type descriptor for a property.
	 * Use this constructor when a target conversion point is a property.
	 * @param property the property
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
		if (type == null) {
			return NULL;
		}
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
		return new TypeDescriptor(mapType, keyType, valueType);
	}
	
	/**
	 * Create a new type descriptor for an object.
	 * Use this factory method to introspect a source object's type before asking the conversion system to convert it to some another type.
	 * Builds in population of nested type descriptors for collection and map objects through object introspection.
	 * If the object is null, returns {@link TypeDescriptor#NULL}.
	 * If the object is not a collection or map, simply calls {@link #valueOf(Class)}.
	 * If the object is a collection or map, this factory method will derive the element type(s) by introspecting the collection or map.
	 * @param object the source object
	 * @return the type descriptor
	 * @see ConversionService#convert(Object, Class)
	 * @see CollectionUtils#findCommonElementType(Collection)
	 */
	public static TypeDescriptor forObject(Object object) {
		if (object == null) {
			return NULL;
		}
		if (object instanceof Collection<?>) {
			return new TypeDescriptor(object.getClass(), findCommonElement((Collection<?>) object));
		}
		else if (object instanceof Map<?, ?>) {
			Map<?, ?> map = (Map<?, ?>) object;
			return new TypeDescriptor(map.getClass(), findCommonElement(map.keySet()), findCommonElement(map.values()));
		}
		else {
			return valueOf(object.getClass());
		}
	}

	/**
	 * Creates a type descriptor for a nested type declared by the method parameter.
	 * For example, if the methodParameter is a List&lt;String&gt; and the nestingLevel is 1, the nested type descriptor will be String.class.
	 * If the methodParameter is a List<List<String>> and the nestingLevel is 2, the nested type descriptor will also be a String.class.
	 * If the methodParameter is a Map<Integer, String> and the nesting level is 1, the nested type descriptor will be String, derived from the map value.
	 * If the methodParameter is a List<Map<Integer, String>> and the nesting level is 2, the nested type descriptor will be String, derived from the map value.
	 * @param methodParameter the method parameter
	 * @return the nested type descriptor
	 * @throws IllegalArgumentException if the method parameter is not of a collection, array, or map type.
	 */
	public static TypeDescriptor nested(MethodParameter methodParameter, int nestingLevel) {
		return nested(new ParameterDescriptor(methodParameter), nestingLevel);
	}

	/**
	 * Creates a type descriptor for a nested type declared by the field.
	 * For example, if the field is a List&lt;String&gt; and the nestingLevel is 1, the nested type descriptor will be String.class.
	 * If the field is a List<List<String>> and the nestingLevel is 2, the nested type descriptor will also be a String.class. 
	 * If the field is a Map<Integer, String> and the nestingLevel is 1, the nested type descriptor will be String, derived from the map value. 
	 * If the field is a List<Map<Integer, String>> and the nestingLevel is 2, the nested type descriptor will be String, derived from the map value.
	 * @param field the field
	 * @param nestingLevel the nesting level
	 * @return the nested type descriptor
	 * @throws IllegalArgumentException if the field is not of a collection, array, or map type.
	 */
	public static TypeDescriptor nested(Field field, int nestingLevel) {
		return nested(new FieldDescriptor(field), nestingLevel);
	}

	/**
	 * Creates a type descriptor for a nested type declared by the property.
	 * For example, if the property is a List&lt;String&gt; and the nestingLevel is 1, the nested type descriptor will be String.class.
	 * If the property is a List<List<String>> and the nestingLevel is 2, the nested type descriptor will also be a String.class. 
	 * If the field is a Map<Integer, String> and the nestingLevel is 1, the nested type descriptor will be String, derived from the map value. 
	 * If the property is a List<Map<Integer, String>> and the nestingLevel is 2, the nested type descriptor will be String, derived from the map value.
	 * @param property the property
	 * @param nestingLevel the nesting level
	 * @return the nested type descriptor
	 * @throws IllegalArgumentException if the property is not of a collection, array, or map type.
	 */
	public static TypeDescriptor nested(Class<?> beanClass, PropertyDescriptor property, int nestingLevel) {
		return nested(new BeanPropertyDescriptor(beanClass, property), nestingLevel);
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
	 * Obtain the annotations associated with the wrapped parameter/field, if any.
	 */
	public Annotation[] getAnnotations() {
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

	// indexable type descriptor operations
	
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
	 * Returns <code>null</code> if this type is neither an array or collection.
	 * Returns Object.class if the element type is for a collection and was not explicitly declared.
	 * @return the map element type, or <code>null</code> if not a collection or array.
	 */
	public Class<?> getElementType() {
		return getElementTypeDescriptor().getType();
	}

	/**
	 * Return the element type as a type descriptor.
	 */
	public TypeDescriptor getElementTypeDescriptor() {
		return this.elementType;
	}

	// map type descriptor operations
	
	/**
	 * Is this type a {@link Map} type?
	 */
	public boolean isMap() {
		return Map.class.isAssignableFrom(getType());
	}

	/**
	 * Determine the generic key type of the wrapped Map parameter/field, if any.
	 * Returns <code>null</code> if this type is not map.
	 * Returns Object.class if the map's key type was not explicitly declared.
	 * @return the map key type, or <code>null</code> if not a map.
	 */
	public Class<?> getMapKeyType() {
		return getMapKeyTypeDescriptor().getType();
	}

	/**
	 * Returns map key type as a type descriptor.
	 */
	public TypeDescriptor getMapKeyTypeDescriptor() {
		return this.mapKeyType;
	}

	/**
	 * Determine the generic value type of the wrapped Map parameter/field, if any.
	 * Returns <code>null</code> if this type is not map.
	 * Returns Object.class if the map's value type was not explicitly declared.
	 * @return the map value type, or <code>null</code> if not a map.
	 */
	public Class<?> getMapValueType() {
		return getMapValueTypeDescriptor().getType();
	}

	/**
	 * Returns map value type as a type descriptor.
	 */
	public TypeDescriptor getMapValueTypeDescriptor() {
		return this.mapValueType;
	}
	
	// extending Object
	
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof TypeDescriptor) || obj == TypeDescriptor.NULL) {
			return false;
		}
		TypeDescriptor other = (TypeDescriptor) obj;
		boolean annotatedTypeEquals = getType().equals(other.getType()) && ObjectUtils.nullSafeEquals(getAnnotations(), other.getAnnotations());
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

	// internal constructors

	private TypeDescriptor(Class<?> type) {
		this(new ClassDescriptor(type));
	}

	private TypeDescriptor(AbstractDescriptor descriptor) {
		this.type = descriptor.getType();
		this.elementType = descriptor.getElementType();
		this.mapKeyType = descriptor.getMapKeyType();
		this.mapValueType = descriptor.getMapValueType();
		this.annotations = descriptor.getAnnotations();
	}

	private TypeDescriptor() {
		this(null, TypeDescriptor.NULL, TypeDescriptor.NULL, TypeDescriptor.NULL);
	}

	private TypeDescriptor(Class<?> collectionType, TypeDescriptor elementType) {
		this(collectionType, elementType, TypeDescriptor.NULL, TypeDescriptor.NULL);
	}

	private TypeDescriptor(Class<?> mapType, TypeDescriptor keyType, TypeDescriptor valueType) {
		this(mapType, TypeDescriptor.NULL, keyType, valueType);
	}

	private TypeDescriptor(Class<?> collectionType, CommonElement commonElement) {
		this(collectionType, fromCommonElement(commonElement), TypeDescriptor.NULL, TypeDescriptor.NULL);
	}

	private TypeDescriptor(Class<?> mapType, CommonElement commonKey, CommonElement commonValue) {
		this(mapType, TypeDescriptor.NULL, fromCommonElement(commonKey), fromCommonElement(commonValue));
	}
	
	private TypeDescriptor(Class<?> type, TypeDescriptor elementType, TypeDescriptor mapKeyType, TypeDescriptor mapValueType) {
		this.type = type;
		this.elementType = elementType;
		this.mapKeyType = mapKeyType;
		this.mapValueType = mapValueType;
		this.annotations = EMPTY_ANNOTATION_ARRAY;
	}

	private static Annotation[] nullSafeAnnotations(Annotation[] annotations) {
		return annotations != null ? annotations : EMPTY_ANNOTATION_ARRAY;
	}
	
	// forObject-related internal helpers

	private static CommonElement findCommonElement(Collection<?> values) {
		Class<?> commonType = null;
		Object candidate = null;
		for (Object value : values) {
			if (value != null) {
				if (candidate == null) {
					commonType = value.getClass();
					candidate = value;
				} else {
					commonType = commonType(commonType, value.getClass());
					if (commonType == Object.class) {
						return null;
					}
				}
			}
		}
		return new CommonElement(commonType, candidate);
	}

	private static Class<?> commonType(Class<?> commonType, Class<?> valueClass) {
		Set<Class<?>> interfaces = new LinkedHashSet<Class<?>>();
		LinkedList<Class<?>> classQueue = new LinkedList<Class<?>>();
		classQueue.addFirst(commonType);
		while (!classQueue.isEmpty()) {
			Class<?> currentClass = classQueue.removeLast();
			if (currentClass.isAssignableFrom(valueClass)) {
				return currentClass;
			}
			Class<?> superClass = currentClass.getSuperclass();
			if (superClass != null && superClass != Object.class) {
				classQueue.addFirst(currentClass.getSuperclass());
			}
			for (Class<?> interfaceType : currentClass.getInterfaces()) {
				addInterfaceHierarchy(interfaceType, interfaces);
			}
		}
		for (Class<?> interfaceType : interfaces) {
			if (interfaceType.isAssignableFrom(valueClass)) {
				return interfaceType;
			}			
		}
		return Object.class;
	}

	private static void addInterfaceHierarchy(Class<?> interfaceType, Set<Class<?>> interfaces) {
		interfaces.add(interfaceType);
		for (Class<?> inheritedInterface : interfaceType.getInterfaces()) {
			addInterfaceHierarchy(inheritedInterface, interfaces);
		}
	}

	private static TypeDescriptor fromCommonElement(CommonElement commonElement) {
		if (commonElement == null) {
			return TypeDescriptor.valueOf(Object.class);
		}
		if (commonElement.getValue() instanceof Collection<?>) {
			Collection<?> collection = (Collection<?>) commonElement.getValue();
			if (collection.size() == 0) {
				return TypeDescriptor.valueOf(Object.class);
			}
			return new TypeDescriptor(commonElement.getType(), findCommonElement(collection));			
		}
		else if (commonElement.getValue() instanceof Map<?, ?>) {
			Map<?, ?> map = (Map<?, ?>) commonElement.getValue();
			if (map.size() == 0) {
				return TypeDescriptor.valueOf(Object.class);				
			}
			return new TypeDescriptor(commonElement.getType(), findCommonElement(map.keySet()), findCommonElement(map.values()));
		}
		else {
			return TypeDescriptor.valueOf(commonElement.getType());
		}
	}

	private static TypeDescriptor nested(AbstractDescriptor descriptor, int nestingLevel) {
		for (int i = 0; i < nestingLevel; i++) {
			descriptor = descriptor.nested();
		}
		return new TypeDescriptor(descriptor);		
	}
	
	// inner classes

	private abstract static class AbstractDescriptor {

		private final Class<?> type;

		public AbstractDescriptor(Class<?> type) {
			this.type = type;
		}
		
		public Class<?> getType() {
			return type;		
		}

		public TypeDescriptor getElementType() {
			if (isCollection()) {
				Class<?> elementType = wildcard(getCollectionElementClass());
				return new TypeDescriptor(nested(elementType, 0));
			} else if (isArray()) {
				Class<?> elementType = getType().getComponentType();
				return new TypeDescriptor(nested(elementType, 0));				
			} else {
				return TypeDescriptor.NULL;
			}
		}
		
		public TypeDescriptor getMapKeyType() {
			if (isMap()) {
				Class<?> keyType = wildcard(getMapKeyClass());
				return new TypeDescriptor(nested(keyType, 0));
			} else {
				return TypeDescriptor.NULL;
			}
		}
		
		public TypeDescriptor getMapValueType() {
			if (isMap()) {
				Class<?> valueType = wildcard(getMapValueClass());
				return new TypeDescriptor(nested(valueType, 1));
			} else {
				return TypeDescriptor.NULL;
			}
		}

		public abstract Annotation[] getAnnotations();

		public AbstractDescriptor nested() {
			if (isCollection()) {
				return nested(wildcard(getCollectionElementClass()), 0);
			} else if (isArray()) {
				return nested(getType().getComponentType(), 0);
			} else if (isMap()) {
				return nested(wildcard(getMapValueClass()), 1);
			} else {
				throw new IllegalStateException("Not a collection, array, or map: cannot resolve nested value types");
			}
		}
		
		// subclassing hooks
		
		protected abstract Class<?> getCollectionElementClass();
		
		protected abstract Class<?> getMapKeyClass();
		
		protected abstract Class<?> getMapValueClass();
		
		protected abstract AbstractDescriptor nested(Class<?> type, int typeIndex);
		
		// internal helpers
		
		private boolean isCollection() {
			return Collection.class.isAssignableFrom(getType());
		}
		
		private boolean isArray() {
			return getType().isArray();
		}
		
		private boolean isMap() {
			return Map.class.isAssignableFrom(getType());
		}
		
		private Class<?> wildcard(Class<?> type) {
			return type != null ? type : Object.class;
		}
		
	}
	
	private static class FieldDescriptor extends AbstractDescriptor {

		private final Field field;

		private final int nestingLevel;

		public FieldDescriptor(Field field) {
			this(field.getType(), field, 1, 0);
		}

		@Override
		public Annotation[] getAnnotations() {
			return nullSafeAnnotations(field.getAnnotations());
		}
		
		@Override
		protected Class<?> getCollectionElementClass() {
			return GenericCollectionTypeResolver.getCollectionFieldType(this.field, this.nestingLevel);
		}

		@Override
		protected Class<?> getMapKeyClass() {
			return GenericCollectionTypeResolver.getMapKeyFieldType(this.field, this.nestingLevel);
		}

		@Override
		protected Class<?> getMapValueClass() {
			return GenericCollectionTypeResolver.getMapValueFieldType(this.field, this.nestingLevel);
		}

		@Override
		protected AbstractDescriptor nested(Class<?> type, int typeIndex) {
			return new FieldDescriptor(type, this.field, this.nestingLevel + 1, typeIndex);
		}

		// internal
		
		private FieldDescriptor(Class<?> type, Field field, int nestingLevel, int typeIndex) {
			super(type);
			this.field = field;
			this.nestingLevel = nestingLevel;
		}

	}
	
	private static class ParameterDescriptor extends AbstractDescriptor {

		private final MethodParameter methodParameter;

		public ParameterDescriptor(MethodParameter methodParameter) {
			super(methodParameter.getParameterType());
			if (methodParameter.getNestingLevel() != 1) {
				throw new IllegalArgumentException("The MethodParameter argument must have its nestingLevel set to 1");
			}			
			this.methodParameter = methodParameter;
		}

		@Override
		public Annotation[] getAnnotations() {
			if (methodParameter.getParameterIndex() == -1) {				
				return nullSafeAnnotations(methodParameter.getMethodAnnotations());
			}
			else {
				return nullSafeAnnotations(methodParameter.getParameterAnnotations());
			}
		}
		
		@Override
		protected Class<?> getCollectionElementClass() {
			return GenericCollectionTypeResolver.getCollectionParameterType(methodParameter);
		}

		@Override
		protected Class<?> getMapKeyClass() {
			return GenericCollectionTypeResolver.getMapKeyParameterType(methodParameter);
		}

		@Override
		protected Class<?> getMapValueClass() {
			return GenericCollectionTypeResolver.getMapValueParameterType(methodParameter);
		}

		@Override
		protected AbstractDescriptor nested(Class<?> type, int typeIndex) {
			MethodParameter methodParameter = new MethodParameter(this.methodParameter);
			methodParameter.increaseNestingLevel();
			methodParameter.setTypeIndexForCurrentLevel(typeIndex);
			return new ParameterDescriptor(type, methodParameter);
		}

		// internal
		
		private ParameterDescriptor(Class<?> type, MethodParameter methodParameter) {
			super(type);
			this.methodParameter = methodParameter;
		}

	}

	private static class BeanPropertyDescriptor extends AbstractDescriptor {

		private final Class<?> beanClass;
		
		private final PropertyDescriptor property;

		private final MethodParameter methodParameter;
		
		private final Annotation[] annotations;
		
		public BeanPropertyDescriptor(Class<?> beanClass, PropertyDescriptor property) {
			super(property.getPropertyType());
			this.beanClass = beanClass;
			this.property = property;
			this.methodParameter = resolveMethodParameter();
			this.annotations = resolveAnnotations();
		}

		@Override
		public Annotation[] getAnnotations() {
			return annotations;
		}
		
		@Override
		protected Class<?> getCollectionElementClass() {
			return GenericCollectionTypeResolver.getCollectionParameterType(methodParameter);
		}

		@Override
		protected Class<?> getMapKeyClass() {
			return GenericCollectionTypeResolver.getMapKeyParameterType(methodParameter);
		}

		@Override
		protected Class<?> getMapValueClass() {
			return GenericCollectionTypeResolver.getMapValueParameterType(methodParameter);
		}

		@Override
		protected AbstractDescriptor nested(Class<?> type, int typeIndex) {
			MethodParameter methodParameter = new MethodParameter(this.methodParameter);
			methodParameter.increaseNestingLevel();
			methodParameter.setTypeIndexForCurrentLevel(typeIndex);			
			return new BeanPropertyDescriptor(type, beanClass, property, methodParameter, annotations);
		}
		
		// internal

		private MethodParameter resolveMethodParameter() {
			if (property.getReadMethod() != null) {
				MethodParameter parameter = new MethodParameter(property.getReadMethod(), -1);
				GenericTypeResolver.resolveParameterType(parameter, beanClass);
				return parameter;
			} else if (property.getWriteMethod() != null) {
				MethodParameter parameter = new MethodParameter(property.getWriteMethod(), 0);
				GenericTypeResolver.resolveParameterType(parameter, beanClass);
				return parameter;				
			} else {
				throw new IllegalArgumentException("Property is neither readable or writeable");
			}
		}
		
		private Annotation[] resolveAnnotations() {
			Map<Class<?>, Annotation> annMap = new LinkedHashMap<Class<?>, Annotation>();
			Method readMethod = this.property.getReadMethod();
			if (readMethod != null) {
				for (Annotation ann : readMethod.getAnnotations()) {
					annMap.put(ann.annotationType(), ann);
				}
			}
			Method writeMethod = this.property.getWriteMethod();
			if (writeMethod != null) {
				for (Annotation ann : writeMethod.getAnnotations()) {
					annMap.put(ann.annotationType(), ann);
				}
			}			
			Field field = getField();
			if (field != null) {
				for (Annotation ann : field.getAnnotations()) {
					annMap.put(ann.annotationType(), ann);
				}
			}
			return annMap.values().toArray(new Annotation[annMap.size()]);			
		}
		
		private Field getField() {
			String name = this.property.getName();
			if (!StringUtils.hasLength(name)) {
				return null;
			}
			Class<?> declaringClass = declaringClass();
			Field field = ReflectionUtils.findField(declaringClass, name);
			if (field == null) {
				// Same lenient fallback checking as in CachedIntrospectionResults...
				field = ReflectionUtils.findField(declaringClass, name.substring(0, 1).toLowerCase() + name.substring(1));
				if (field == null) {
					field = ReflectionUtils.findField(declaringClass, name.substring(0, 1).toUpperCase() + name.substring(1));
				}
			}
			return field;
		}
		
		private Class<?> declaringClass() {
			if (this.property.getReadMethod() != null) {
				return this.property.getReadMethod().getDeclaringClass();
			} else {
				return this.property.getWriteMethod().getDeclaringClass();
			}
		}
		
		private BeanPropertyDescriptor(Class<?> type, Class<?> beanClass, java.beans.PropertyDescriptor propertyDescriptor, MethodParameter methodParameter, Annotation[] annotations) {
			super(type);
			this.beanClass = beanClass;
			this.property = propertyDescriptor;
			this.methodParameter = methodParameter;
			this.annotations = annotations;
		}
		
	}
	
	private static class ClassDescriptor extends AbstractDescriptor {

		private ClassDescriptor(Class<?> type) {
			super(type);
		}

		@Override
		public Annotation[] getAnnotations() {
			return EMPTY_ANNOTATION_ARRAY;
		}

		@Override
		protected Class<?> getCollectionElementClass() {
			return Object.class;
		}

		@Override
		protected Class<?> getMapKeyClass() {
			return Object.class;
		}

		@Override
		protected Class<?> getMapValueClass() {
			return Object.class;
		}

		@Override
		protected AbstractDescriptor nested(Class<?> type, int typeIndex) {
			return new ClassDescriptor(type);
		}
		
	}
	
	private static class CommonElement {
		
		private Class<?> type;
		
		private Object value;

		public CommonElement(Class<?> type, Object value) {
			this.type = type;
			this.value = value;
		}

		public Class<?> getType() {
			return type;
		}

		public Object getValue() {
			return value;
		}
		
	}

}