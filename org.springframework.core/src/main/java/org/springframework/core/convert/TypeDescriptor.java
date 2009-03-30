package org.springframework.core.convert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;

import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;

/**
 * Metadata about a retrieved value or value type.
 * 
 * @author Keith Donald
 */
public class TypeDescriptor {

	private MethodParameter methodParameter;

	private Field field;

	private Annotation[] cachedFieldAnnotations;

	private Class<?> type;
	
	public TypeDescriptor(Class<?> type) {
		this.type = type;
	}
	
	/**
	 * Create a new descriptor for a method or constructor parameter.
	 * 
	 * @param methodParameter the MethodParameter to wrap
	 */
	public TypeDescriptor(MethodParameter methodParameter) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		this.methodParameter = methodParameter;
	}

	/**
	 * Create a new descriptor for a field. Considers the dependency as 'eager'.
	 * 
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
	
	public String getName() {
		return getType().getName();
	}
	
	public boolean isArray() {
		return getType().isArray();
	}
	
	public Class<?> getElementType() {
		return isArray() ? getArrayComponentType() : getCollectionElementType();
	}
	
	public Class<?> getArrayComponentType() {
		return getType().getComponentType();
	}

	public boolean isInstance(Object source) {
		return getType().isInstance(source);
	}

	
	/**
	 * Determine the generic element type of the wrapped Collection parameter/field, if any.
	 * 
	 * @return the generic type, or <code>null</code> if none
	 */
	public Class<?> getCollectionElementType() {
		if (type != null) {
			return GenericCollectionTypeResolver.getCollectionType((Class<? extends Collection>) type);
		} else if (field != null) {
			return GenericCollectionTypeResolver.getCollectionFieldType(field);
		} else {
			return  GenericCollectionTypeResolver.getCollectionParameterType(methodParameter);
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

	public boolean isCollection() {
		return Collection.class.isAssignableFrom(getType());
	}

	public boolean isAbstractClass() {
		return !getType().isInterface() && Modifier.isAbstract(getType().getModifiers());
	}

}
