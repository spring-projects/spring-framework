/*
 * Copyright 2002-2015 the original author or authors.
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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Helper class that encapsulates the specification of a method parameter, i.e.
 * a {@link Method} or {@link Constructor} plus a parameter index and a nested
 * type index for a declared generic type. Useful as a specification object to
 * pass along.
 *
 * <p>As of 4.2, there is a {@link org.springframework.core.annotation.SynthesizingMethodParameter
 * SynthesizingMethodParameter} subclass available which synthesizes annotations
 * with attribute aliases. That subclass is used for web and message endpoint
 * processing, in particular.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Andy Clement
 * @author Sam Brannen
 * @since 2.0
 * @see GenericCollectionTypeResolver
 * @see org.springframework.core.annotation.SynthesizingMethodParameter
 */
public class MethodParameter {

	private final Method method;

	private final Constructor<?> constructor;

	private final int parameterIndex;

	private int nestingLevel = 1;

	/** Map from Integer level to Integer type index */
	Map<Integer, Integer> typeIndexesPerLevel;

	private volatile Class<?> containingClass;

	private volatile Class<?> parameterType;

	private volatile Type genericParameterType;

	private volatile Annotation[] parameterAnnotations;

	private volatile ParameterNameDiscoverer parameterNameDiscoverer;

	private volatile String parameterName;


	/**
	 * Create a new {@code MethodParameter} for the given method, with nesting level 1.
	 * @param method the Method to specify a parameter for
	 * @param parameterIndex the index of the parameter: -1 for the method
	 * return type; 0 for the first method parameter; 1 for the second method
	 * parameter, etc.
	 */
	public MethodParameter(Method method, int parameterIndex) {
		this(method, parameterIndex, 1);
	}

	/**
	 * Create a new {@code MethodParameter} for the given method.
	 * @param method the Method to specify a parameter for
	 * @param parameterIndex the index of the parameter: -1 for the method
	 * return type; 0 for the first method parameter; 1 for the second method
	 * parameter, etc.
	 * @param nestingLevel the nesting level of the target type
	 * (typically 1; e.g. in case of a List of Lists, 1 would indicate the
	 * nested List, whereas 2 would indicate the element of the nested List)
	 */
	public MethodParameter(Method method, int parameterIndex, int nestingLevel) {
		Assert.notNull(method, "Method must not be null");
		this.method = method;
		this.parameterIndex = parameterIndex;
		this.nestingLevel = nestingLevel;
		this.constructor = null;
	}

	/**
	 * Create a new MethodParameter for the given constructor, with nesting level 1.
	 * @param constructor the Constructor to specify a parameter for
	 * @param parameterIndex the index of the parameter
	 */
	public MethodParameter(Constructor<?> constructor, int parameterIndex) {
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
	public MethodParameter(Constructor<?> constructor, int parameterIndex, int nestingLevel) {
		Assert.notNull(constructor, "Constructor must not be null");
		this.constructor = constructor;
		this.parameterIndex = parameterIndex;
		this.nestingLevel = nestingLevel;
		this.method = null;
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
		this.nestingLevel = original.nestingLevel;
		this.typeIndexesPerLevel = original.typeIndexesPerLevel;
		this.containingClass = original.containingClass;
		this.parameterType = original.parameterType;
		this.genericParameterType = original.genericParameterType;
		this.parameterAnnotations = original.parameterAnnotations;
		this.parameterNameDiscoverer = original.parameterNameDiscoverer;
		this.parameterName = original.parameterName;
	}


	/**
	 * Return the wrapped Method, if any.
	 * <p>Note: Either Method or Constructor is available.
	 * @return the Method, or {@code null} if none
	 */
	public Method getMethod() {
		return this.method;
	}

	/**
	 * Return the wrapped Constructor, if any.
	 * <p>Note: Either Method or Constructor is available.
	 * @return the Constructor, or {@code null} if none
	 */
	public Constructor<?> getConstructor() {
		return this.constructor;
	}

	/**
	 * Returns the wrapped member.
	 * @return the Method or Constructor as Member
	 */
	public Member getMember() {
		// NOTE: no ternary expression to retain JDK <8 compatibility even when using
		// the JDK 8 compiler (potentially selecting java.lang.reflect.Executable
		// as common type, with that new base class not available on older JDKs)
		if (this.method != null) {
			return this.method;
		}
		else {
			return this.constructor;
		}
	}

	/**
	 * Returns the wrapped annotated element.
	 * @return the Method or Constructor as AnnotatedElement
	 */
	public AnnotatedElement getAnnotatedElement() {
		// NOTE: no ternary expression to retain JDK <8 compatibility even when using
		// the JDK 8 compiler (potentially selecting java.lang.reflect.Executable
		// as common type, with that new base class not available on older JDKs)
		if (this.method != null) {
			return this.method;
		}
		else {
			return this.constructor;
		}
	}

	/**
	 * Return the class that declares the underlying Method or Constructor.
	 */
	public Class<?> getDeclaringClass() {
		return getMember().getDeclaringClass();
	}

	/**
	 * Return the index of the method/constructor parameter.
	 * @return the parameter index (-1 in case of the return type)
	 */
	public int getParameterIndex() {
		return this.parameterIndex;
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
	 * (or {@code null} for the default type index)
	 * @see #getNestingLevel()
	 */
	public void setTypeIndexForCurrentLevel(int typeIndex) {
		getTypeIndexesPerLevel().put(this.nestingLevel, typeIndex);
	}

	/**
	 * Return the type index for the current nesting level.
	 * @return the corresponding type index, or {@code null}
	 * if none specified (indicating the default type index)
	 * @see #getNestingLevel()
	 */
	public Integer getTypeIndexForCurrentLevel() {
		return getTypeIndexForLevel(this.nestingLevel);
	}

	/**
	 * Return the type index for the specified nesting level.
	 * @param nestingLevel the nesting level to check
	 * @return the corresponding type index, or {@code null}
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
	 * Set a containing class to resolve the parameter type against.
	 */
	void setContainingClass(Class<?> containingClass) {
		this.containingClass = containingClass;
	}

	public Class<?> getContainingClass() {
		return (this.containingClass != null ? this.containingClass : getDeclaringClass());
	}

	/**
	 * Set a resolved (generic) parameter type.
	 */
	void setParameterType(Class<?> parameterType) {
		this.parameterType = parameterType;
	}

	/**
	 * Return the type of the method/constructor parameter.
	 * @return the parameter type (never {@code null})
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
	 * @return the parameter type (never {@code null})
	 * @since 3.0
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
	 * Return the nested type of the method/constructor parameter.
	 * @return the parameter type (never {@code null})
	 * @see #getNestingLevel()
	 * @since 3.1
	 */
	public Class<?> getNestedParameterType() {
		if (this.nestingLevel > 1) {
			Type type = getGenericParameterType();
			for (int i = 2; i <= this.nestingLevel; i++) {
				if (type instanceof ParameterizedType) {
					Type[] args = ((ParameterizedType) type).getActualTypeArguments();
					Integer index = getTypeIndexForLevel(i);
					type = args[index != null ? index : args.length - 1];
				}
			}
			if (type instanceof Class) {
				return (Class<?>) type;
			}
			else if (type instanceof ParameterizedType) {
				Type arg = ((ParameterizedType) type).getRawType();
				if (arg instanceof Class) {
					return (Class<?>) arg;
				}
			}
			return Object.class;
		}
		else {
			return getParameterType();
		}
	}

	/**
	 * Return the nested generic type of the method/constructor parameter.
	 * @return the parameter type (never {@code null})
	 * @see #getNestingLevel()
	 * @since 4.2
	 */
	public Type getNestedGenericParameterType() {
		if (this.nestingLevel > 1) {
			Type type = getGenericParameterType();
			for (int i = 2; i <= this.nestingLevel; i++) {
				if (type instanceof ParameterizedType) {
					Type[] args = ((ParameterizedType) type).getActualTypeArguments();
					Integer index = getTypeIndexForLevel(i);
					type = args[index != null ? index : args.length - 1];
				}
			}
			return type;
		}
		else {
			return getGenericParameterType();
		}
	}

	/**
	 * Return the annotations associated with the target method/constructor itself.
	 */
	public Annotation[] getMethodAnnotations() {
		return adaptAnnotationArray(getAnnotatedElement().getAnnotations());
	}

	/**
	 * Return the method/constructor annotation of the given type, if available.
	 * @param annotationType the annotation type to look for
	 * @return the annotation object, or {@code null} if not found
	 */
	public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
		return adaptAnnotation(getAnnotatedElement().getAnnotation(annotationType));
	}

	/**
	 * Return the annotations associated with the specific method/constructor parameter.
	 */
	public Annotation[] getParameterAnnotations() {
		if (this.parameterAnnotations == null) {
			Annotation[][] annotationArray = (this.method != null ?
					this.method.getParameterAnnotations() : this.constructor.getParameterAnnotations());
			if (this.parameterIndex >= 0 && this.parameterIndex < annotationArray.length) {
				this.parameterAnnotations = adaptAnnotationArray(annotationArray[this.parameterIndex]);
			}
			else {
				this.parameterAnnotations = new Annotation[0];
			}
		}
		return this.parameterAnnotations;
	}

	/**
	 * Return the parameter annotation of the given type, if available.
	 * @param annotationType the annotation type to look for
	 * @return the annotation object, or {@code null} if not found
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
	 * Return true if the parameter has at least one annotation, false if it has none.
	 */
	public boolean hasParameterAnnotations() {
		return (getParameterAnnotations().length != 0);
	}

	/**
	 * Return true if the parameter has the given annotation type, and false if it doesn't.
	 */
	public <T extends Annotation> boolean hasParameterAnnotation(Class<T> annotationType) {
		return (getParameterAnnotation(annotationType) != null);
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
	 * @return the parameter name (may be {@code null} if no
	 * parameter name metadata is contained in the class file or no
	 * {@link #initParameterNameDiscovery ParameterNameDiscoverer}
	 * has been set to begin with)
	 */
	public String getParameterName() {
		ParameterNameDiscoverer discoverer = this.parameterNameDiscoverer;
		if (discoverer != null) {
			String[] parameterNames = (this.method != null ?
					discoverer.getParameterNames(this.method) : discoverer.getParameterNames(this.constructor));
			if (parameterNames != null) {
				this.parameterName = parameterNames[this.parameterIndex];
			}
			this.parameterNameDiscoverer = null;
		}
		return this.parameterName;
	}


	/**
	 * A template method to post-process a given annotation instance before
	 * returning it to the caller.
	 * <p>The default implementation simply returns the given annotation as-is.
	 * @param annotation the annotation about to be returned
	 * @return the post-processed annotation (or simply the original one)
	 * @since 4.2
	 */
	protected <A extends Annotation> A adaptAnnotation(A annotation) {
		return annotation;
	}

	/**
	 * A template method to post-process a given annotation array before
	 * returning it to the caller.
	 * <p>The default implementation simply returns the given annotation array as-is.
	 * @param annotations the annotation array about to be returned
	 * @return the post-processed annotation array (or simply the original one)
	 * @since 4.2
	 */
	protected Annotation[] adaptAnnotationArray(Annotation[] annotations) {
		return annotations;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MethodParameter)) {
			return false;
		}
		MethodParameter otherParam = (MethodParameter) other;
		return (this.parameterIndex == otherParam.parameterIndex && getMember().equals(otherParam.getMember()));
	}

	@Override
	public int hashCode() {
		return (getMember().hashCode() * 31 + this.parameterIndex);
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
			return new MethodParameter((Constructor<?>) methodOrConstructor, parameterIndex);
		}
		else {
			throw new IllegalArgumentException(
					"Given object [" + methodOrConstructor + "] is neither a Method nor a Constructor");
		}
	}

}
