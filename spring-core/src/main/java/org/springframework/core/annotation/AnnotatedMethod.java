/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * A convenient wrapper for a {@link Method} handle, providing deep annotation
 * introspection on methods and method parameters, including the exposure of
 * interface-declared parameter annotations from the concrete target method.
 *
 * @author Juergen Hoeller
 * @since 6.1
 * @see #getMethodAnnotation(Class)
 * @see #getMethodParameters()
 * @see AnnotatedElementUtils
 * @see SynthesizingMethodParameter
 */
public class AnnotatedMethod {

	private final Method method;

	private final Method bridgedMethod;

	private final MethodParameter[] parameters;

	@Nullable
	private volatile List<Annotation[][]> inheritedParameterAnnotations;


	/**
	 * Create an instance that wraps the given {@link Method}.
	 * @param method the {@code Method} handle to wrap
	 */
	public AnnotatedMethod(Method method) {
		Assert.notNull(method, "Method is required");
		this.method = method;
		this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
		ReflectionUtils.makeAccessible(this.bridgedMethod);
		this.parameters = initMethodParameters();
	}

	/**
	 * Copy constructor for use in subclasses.
	 */
	protected AnnotatedMethod(AnnotatedMethod annotatedMethod) {
		Assert.notNull(annotatedMethod, "AnnotatedMethod is required");
		this.method = annotatedMethod.method;
		this.bridgedMethod = annotatedMethod.bridgedMethod;
		this.parameters = annotatedMethod.parameters;
		this.inheritedParameterAnnotations = annotatedMethod.inheritedParameterAnnotations;
	}


	/**
	 * Return the annotated method.
	 */
	public final Method getMethod() {
		return this.method;
	}

	/**
	 * If the annotated method is a bridge method, this method returns the bridged
	 * (user-defined) method. Otherwise, it returns the same method as {@link #getMethod()}.
	 */
	protected final Method getBridgedMethod() {
		return this.bridgedMethod;
	}

	/**
	 * Expose the containing class for method parameters.
	 * @see MethodParameter#getContainingClass()
	 */
	protected Class<?> getContainingClass() {
		return this.method.getDeclaringClass();
	}

	/**
	 * Return the method parameters for this {@code AnnotatedMethod}.
	 */
	public final MethodParameter[] getMethodParameters() {
		return this.parameters;
	}

	private MethodParameter[] initMethodParameters() {
		int count = this.bridgedMethod.getParameterCount();
		MethodParameter[] result = new MethodParameter[count];
		for (int i = 0; i < count; i++) {
			result[i] = new AnnotatedMethodParameter(i);
		}
		return result;
	}

	/**
	 * Return a {@link MethodParameter} for the declared return type.
	 */
	public MethodParameter getReturnType() {
		return new AnnotatedMethodParameter(-1);
	}

	/**
	 * Return a {@link MethodParameter} for the actual return value type.
	 */
	public MethodParameter getReturnValueType(@Nullable Object returnValue) {
		return new ReturnValueMethodParameter(returnValue);
	}

	/**
	 * Return {@code true} if the method's return type is void, {@code false} otherwise.
	 */
	public boolean isVoid() {
		return (getReturnType().getParameterType() == void.class);
	}

	/**
	 * Return a single annotation on the underlying method, traversing its super methods
	 * if no annotation can be found on the given method itself.
	 * <p>Supports <em>merged</em> composed annotations with attribute overrides.
	 * @param annotationType the annotation type to look for
	 * @return the annotation, or {@code null} if none found
	 * @see AnnotatedElementUtils#findMergedAnnotation
	 */
	@Nullable
	public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
		return AnnotatedElementUtils.findMergedAnnotation(this.method, annotationType);
	}

	/**
	 * Determine if an annotation of the given type is <em>present</em> or
	 * <em>meta-present</em> on the method.
	 * @param annotationType the annotation type to look for
	 * @see AnnotatedElementUtils#hasAnnotation
	 */
	public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
		return AnnotatedElementUtils.hasAnnotation(this.method, annotationType);
	}

	private List<Annotation[][]> getInheritedParameterAnnotations() {
		List<Annotation[][]> parameterAnnotations = this.inheritedParameterAnnotations;
		if (parameterAnnotations == null) {
			parameterAnnotations = new ArrayList<>();
			Class<?> clazz = this.method.getDeclaringClass();
			while (clazz != null) {
				for (Class<?> ifc : clazz.getInterfaces()) {
					for (Method candidate : ifc.getMethods()) {
						if (isOverrideFor(candidate)) {
							parameterAnnotations.add(candidate.getParameterAnnotations());
						}
					}
				}
				clazz = clazz.getSuperclass();
				if (clazz == Object.class) {
					clazz = null;
				}
				if (clazz != null) {
					for (Method candidate : clazz.getMethods()) {
						if (isOverrideFor(candidate)) {
							parameterAnnotations.add(candidate.getParameterAnnotations());
						}
					}
				}
			}
			this.inheritedParameterAnnotations = parameterAnnotations;
		}
		return parameterAnnotations;
	}

	private boolean isOverrideFor(Method candidate) {
		if (!candidate.getName().equals(this.method.getName()) ||
				candidate.getParameterCount() != this.method.getParameterCount()) {
			return false;
		}
		Class<?>[] paramTypes = this.method.getParameterTypes();
		if (Arrays.equals(candidate.getParameterTypes(), paramTypes)) {
			return true;
		}
		for (int i = 0; i < paramTypes.length; i++) {
			if (paramTypes[i] !=
					ResolvableType.forMethodParameter(candidate, i, this.method.getDeclaringClass()).resolve()) {
				return false;
			}
		}
		return true;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other != null && getClass() == other.getClass() &&
				this.method.equals(((AnnotatedMethod) other).method)));
	}

	@Override
	public int hashCode() {
		return this.method.hashCode();
	}

	@Override
	public String toString() {
		return this.method.toGenericString();
	}


	// Support methods for use in subclass variants

	@Nullable
	protected static Object findProvidedArgument(MethodParameter parameter, @Nullable Object... providedArgs) {
		if (!ObjectUtils.isEmpty(providedArgs)) {
			for (Object providedArg : providedArgs) {
				if (parameter.getParameterType().isInstance(providedArg)) {
					return providedArg;
				}
			}
		}
		return null;
	}

	protected static String formatArgumentError(MethodParameter param, String message) {
		return "Could not resolve parameter [" + param.getParameterIndex() + "] in " +
				param.getExecutable().toGenericString() + (StringUtils.hasText(message) ? ": " + message : "");
	}


	/**
	 * A MethodParameter with AnnotatedMethod-specific behavior.
	 */
	protected class AnnotatedMethodParameter extends SynthesizingMethodParameter {

		@Nullable
		private volatile Annotation[] combinedAnnotations;

		public AnnotatedMethodParameter(int index) {
			super(AnnotatedMethod.this.getBridgedMethod(), index);
		}

		protected AnnotatedMethodParameter(AnnotatedMethodParameter original) {
			super(original);
			this.combinedAnnotations = original.combinedAnnotations;
		}

		@Override
		@NonNull
		public Method getMethod() {
			return AnnotatedMethod.this.getBridgedMethod();
		}

		@Override
		public Class<?> getContainingClass() {
			return AnnotatedMethod.this.getContainingClass();
		}

		@Override
		@Nullable
		public <T extends Annotation> T getMethodAnnotation(Class<T> annotationType) {
			return AnnotatedMethod.this.getMethodAnnotation(annotationType);
		}

		@Override
		public <T extends Annotation> boolean hasMethodAnnotation(Class<T> annotationType) {
			return AnnotatedMethod.this.hasMethodAnnotation(annotationType);
		}

		@Override
		public Annotation[] getParameterAnnotations() {
			Annotation[] anns = this.combinedAnnotations;
			if (anns == null) {
				anns = super.getParameterAnnotations();
				int index = getParameterIndex();
				if (index >= 0) {
					for (Annotation[][] ifcAnns : getInheritedParameterAnnotations()) {
						if (index < ifcAnns.length) {
							Annotation[] paramAnns = ifcAnns[index];
							if (paramAnns.length > 0) {
								List<Annotation> merged = new ArrayList<>(anns.length + paramAnns.length);
								merged.addAll(Arrays.asList(anns));
								for (Annotation paramAnn : paramAnns) {
									boolean existingType = false;
									for (Annotation ann : anns) {
										if (ann.annotationType() == paramAnn.annotationType()) {
											existingType = true;
											break;
										}
									}
									if (!existingType) {
										merged.add(adaptAnnotation(paramAnn));
									}
								}
								anns = merged.toArray(new Annotation[0]);
							}
						}
					}
				}
				this.combinedAnnotations = anns;
			}
			return anns;
		}

		@Override
		public AnnotatedMethodParameter clone() {
			return new AnnotatedMethodParameter(this);
		}
	}


	/**
	 * A MethodParameter for an AnnotatedMethod return type based on an actual return value.
	 */
	private class ReturnValueMethodParameter extends AnnotatedMethodParameter {

		@Nullable
		private final Class<?> returnValueType;

		public ReturnValueMethodParameter(@Nullable Object returnValue) {
			super(-1);
			this.returnValueType = (returnValue != null ? returnValue.getClass() : null);
		}

		protected ReturnValueMethodParameter(ReturnValueMethodParameter original) {
			super(original);
			this.returnValueType = original.returnValueType;
		}

		@Override
		public Class<?> getParameterType() {
			return (this.returnValueType != null ? this.returnValueType : super.getParameterType());
		}

		@Override
		public ReturnValueMethodParameter clone() {
			return new ReturnValueMethodParameter(this);
		}
	}

}
