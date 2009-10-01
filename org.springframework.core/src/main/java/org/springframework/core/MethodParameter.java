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

package org.springframework.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Helper class that encapsulates the specification of a method parameter, i.e.
 * a Method or Constructor plus a parameter index and a nested type index for
 * a declared generic type. Useful as a specification object to pass along.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Andy Clement
 * @since 2.0
 * @see GenericCollectionTypeResolver
 */
public class MethodParameter {

	private Method method;

	private Constructor constructor;

	private final int parameterIndex;

	private Class<?> parameterType;

	private Type genericParameterType;

	private Annotation[] parameterAnnotations;

	private ParameterNameDiscoverer parameterNameDiscoverer;

	private String parameterName;

	private int nestingLevel = 1;

	/** Map from Integer level to Integer type index */
	private Map<Integer,Integer> typeIndexesPerLevel;

	Map<TypeVariable, Type> typeVariableMap;


	/**
	 * Create a new MethodParameter for the given method, with nesting level 1.
	 * @param method the Method to specify a parameter for
	 * @param parameterIndex the index of the parameter
	 */
	public MethodParameter(Method method, int parameterIndex) {
		this(method, parameterIndex, 1);
	}

	/**
	 * Create a new MethodParameter for the given method.
	 * @param method the Method to specify a parameter for
	 * @param parameterIndex the index of the parameter
	 * (-1 for the method return type; 0 for the first method parameter,
	 * 1 for the second method parameter, etc)
	 * @param nestingLevel the nesting level of the target type
	 * (typically 1; e.g. in case of a List of Lists, 1 would indicate the
	 * nested List, whereas 2 would indicate the element of the nested List)
	 */
	public MethodParameter(Method method, int parameterIndex, int nestingLevel) {
		Assert.notNull(method, "Method must not be null");
		this.method = method;
		this.parameterIndex = parameterIndex;
		this.nestingLevel = nestingLevel;
	}

	/**
	 * Create a new MethodParameter for the given constructor, with nesting level 1.
	 * @param constructor the Constructor to specify a parameter for
	 * @param parameterIndex the index of the parameter
	 */
	public MethodParameter(Constructor constructor, int parameterIndex) {
		this(constructor, parameterIndex, 1);
	}

	/**
	 * Create a new MethodParameter for the given constructor.
	 * @param constructor the Constructor to specify a parameter for
	 * @param parameterIndex the index of the parameter
	 * @param nestingLevel the nesting level of the target type
	 * (typically 1; e.g. in case of a List of Lists, 1 would indicate the
	 * nested List, whereas 2 would indicate the element of the nested List)
	 */
	public MethodParameter(Constructor constructor, int parameterIndex, int nestingLevel) {
		Assert.notNull(constructor, "Constructor must not be null");
		this.constructor = constructor;
		this.parameterIndex = parameterIndex;
		this.nestingLevel = nestingLevel;
	}

	/**
	 * Copy constructor, resulting in an independent MethodParameter object
	 * based on the same metadata and cache state that the original object was in.
	 * @param original the original MethodParameter object to copy from
	 */
	public MethodParameter(MethodParameter original) {
		Assert.notNull(original, "Original must not be null");
		this.method = original.method;
		this.constructor = original.constructor;
		this.parameterIndex = original.parameterIndex;
		this.parameterType = original.parameterType;
		this.parameterAnnotations = original.parameterAnnotations;
		this.typeVariableMap = original.typeVariableMap;
	}


	/**
	 * Return the wrapped Method, if any.
	 * <p>Note: Either Method or Constructor is available.
	 * @return the Method, or <code>null</code> if none
	 */
	public Method getMethod() {
		return this.method;
	}

	/**
	 * Return the wrapped Constructor, if any.
	 * <p>Note: Either Method or Constructor is available.
	 * @return the Constructor, or <code>null</code> if none
	 */
	public Constructor getConstructor() {
		return this.constructor;
	}

	/**
	 * Return the class that declares the underlying Method or Constructor.
	 */
	public Class getDeclaringClass() {
		return (this.method != null ? this.method.getDeclaringClass() : this.constructor.getDeclaringClass());
	}

	/**
	 * Return the index of the method/constructor parameter.
	 * @return the parameter index (never negative)
	 */
	public int getParameterIndex() {
		return this.parameterIndex;
	}

	/**
	 * Set a resolved (generic) parameter type.
	 */
	void setParameterType(Class<?> parameterType) {
		this.parameterType = parameterType;
	}

	/**
	 * Return the type of the method/constructor parameter.
	 * @return the parameter type (never <code>null</code>)
	 */
	public Class<?> getParameterType() {
		if (this.parameterType == null) {
			if (this.parameterIndex < 0) {
				this.parameterType = (this.method != null ? this.method.getReturnType() : null);
			}
			else {
				this.parameterType = (this.method != null ?
					this.method.getParameterTypes()[this.parameterIndex] :
					this.constructor.getParameterTypes()[this.parameterIndex]);
			}
		}
		return this.parameterType;
	}

	/**
	 * Return the generic type of the method/constructor parameter.
	 * @return the parameter type (never <code>null</code>)
	 */
	public Type getGenericParameterType() {
		if (this.genericParameterType == null) {
			if (this.parameterIndex < 0) {
				this.genericParameterType = (this.method != null ? this.method.getGenericReturnType() : null);
			}
			else {
				this.genericParameterType = (this.method != null ?
					this.method.getGenericParameterTypes()[this.parameterIndex] :
					this.constructor.getGenericParameterTypes()[this.parameterIndex]);
			}
		}
		return this.genericParameterType;
	}

	/**
	 * Return the annotations associated with the target method/constructor itself.
	 */
	public Annotation[] getMethodAnnotations() {
		return (this.method != null ? this.method.getAnnotations() : this.constructor.getAnnotations());
	}

	/**
	 * Return the method/constructor annotation of the given type, if available.
	 * @param annotationType the annotation type to look for
	 * @return the annotation object, or <code>null</code> if not found
	 */
	@SuppressWarnings("unchecked")
	public <T extends Annotation> T getMethodAnnotation(Class<T> annotationType) {
		return (this.method != null ? this.method.getAnnotation(annotationType) :
				(T) this.constructor.getAnnotation(annotationType));
	}

	/**
	 * Return the annotations associated with the specific method/constructor parameter.
	 */
	public Annotation[] getParameterAnnotations() {
		if (this.parameterAnnotations == null) {
			Annotation[][] annotationArray = (this.method != null ?
					this.method.getParameterAnnotations() : this.constructor.getParameterAnnotations());
			this.parameterAnnotations = annotationArray[this.parameterIndex];
		}
		return this.parameterAnnotations;
	}

	/**
	 * Return the parameter annotation of the given type, if available.
	 * @param annotationType the annotation type to look for
	 * @return the annotation object, or <code>null</code> if not found
	 */
	@SuppressWarnings("unchecked")
	public <T extends Annotation> T getParameterAnnotation(Class<T> annotationType) {
		Annotation[] anns = getParameterAnnotations();
		for (Annotation ann : anns) {
			if (annotationType.isInstance(ann)) {
				return (T) ann;
			}
		}
		return null;
	}

	/**
	 * Initialize parameter name discovery for this method parameter.
	 * <p>This method does not actually try to retrieve the parameter name at
	 * this point; it just allows discovery to happen when the application calls
	 * {@link #getParameterName()} (if ever).
	 */
	public void initParameterNameDiscovery(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * Return the name of the method/constructor parameter.
	 * @return the parameter name (may be <code>null</code> if no
	 * parameter name metadata is contained in the class file or no
	 * {@link #initParameterNameDiscovery ParameterNameDiscoverer}
	 * has been set to begin with)
	 */
	public String getParameterName() {
		if (this.parameterNameDiscoverer != null) {
			String[] parameterNames = (this.method != null ?
					this.parameterNameDiscoverer.getParameterNames(this.method) :
					this.parameterNameDiscoverer.getParameterNames(this.constructor));
			if (parameterNames != null) {
				this.parameterName = parameterNames[this.parameterIndex];
			}
			this.parameterNameDiscoverer = null;
		}
		return this.parameterName;
	}

	/**
	 * Increase this parameter's nesting level.
	 * @see #getNestingLevel()
	 */
	public void increaseNestingLevel() {
		this.nestingLevel++;
	}

	/**
	 * Decrease this parameter's nesting level.
	 * @see #getNestingLevel()
	 */
	public void decreaseNestingLevel() {
		getTypeIndexesPerLevel().remove(this.nestingLevel);
		this.nestingLevel--;
	}

	/**
	 * Return the nesting level of the target type
	 * (typically 1; e.g. in case of a List of Lists, 1 would indicate the
	 * nested List, whereas 2 would indicate the element of the nested List).
	 */
	public int getNestingLevel() {
		return this.nestingLevel;
	}

	/**
	 * Set the type index for the current nesting level.
	 * @param typeIndex the corresponding type index
	 * (or <code>null</code> for the default type index)
	 * @see #getNestingLevel()
	 */
	public void setTypeIndexForCurrentLevel(int typeIndex) {
		getTypeIndexesPerLevel().put(this.nestingLevel, typeIndex);
	}

	/**
	 * Return the type index for the current nesting level.
	 * @return the corresponding type index, or <code>null</code>
	 * if none specified (indicating the default type index)
	 * @see #getNestingLevel()
	 */
	public Integer getTypeIndexForCurrentLevel() {
		return getTypeIndexForLevel(this.nestingLevel);
	}

	/**
	 * Return the type index for the specified nesting level.
	 * @param nestingLevel the nesting level to check
	 * @return the corresponding type index, or <code>null</code>
	 * if none specified (indicating the default type index)
	 */
	public Integer getTypeIndexForLevel(int nestingLevel) {
		return getTypeIndexesPerLevel().get(nestingLevel);
	}

	/**
	 * Obtain the (lazily constructed) type-indexes-per-level Map.
	 */
	private Map<Integer, Integer> getTypeIndexesPerLevel() {
		if (this.typeIndexesPerLevel == null) {
			this.typeIndexesPerLevel = new HashMap<Integer, Integer>(4);
		}
		return this.typeIndexesPerLevel;
	}


	/**
	 * Create a new MethodParameter for the given method or constructor.
	 * <p>This is a convenience constructor for scenarios where a
	 * Method or Constructor reference is treated in a generic fashion.
	 * @param methodOrConstructor the Method or Constructor to specify a parameter for
	 * @param parameterIndex the index of the parameter
	 * @return the corresponding MethodParameter instance
	 */
	public static MethodParameter forMethodOrConstructor(Object methodOrConstructor, int parameterIndex) {
		if (methodOrConstructor instanceof Method) {
			return new MethodParameter((Method) methodOrConstructor, parameterIndex);
		}
		else if (methodOrConstructor instanceof Constructor) {
			return new MethodParameter((Constructor) methodOrConstructor, parameterIndex);
		}
		else {
			throw new IllegalArgumentException(
					"Given object [" + methodOrConstructor + "] is neither a Method nor a Constructor");
		}
	}

}
