/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Utility class that contains various methods useful for the implementation of
 * autowire-capable bean factories.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Sam Brannen
 * @since 1.1.2
 * @see AbstractAutowireCapableBeanFactory
 */
public abstract class AutowireUtils {

	private static final Comparator<Executable> EXECUTABLE_COMPARATOR = (e1, e2) -> {
		int result = Boolean.compare(Modifier.isPublic(e2.getModifiers()), Modifier.isPublic(e1.getModifiers()));
		return result != 0 ? result : Integer.compare(e2.getParameterCount(), e1.getParameterCount());
	};

	private static final AnnotatedElement EMPTY_ANNOTATED_ELEMENT = new AnnotatedElement() {
		@Override
		@Nullable
		public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
			return null;
		}
		@Override
		public Annotation[] getAnnotations() {
			return new Annotation[0];
		}
		@Override
		public Annotation[] getDeclaredAnnotations() {
			return new Annotation[0];
		}
	};


	/**
	 * Sort the given constructors, preferring public constructors and "greedy" ones with
	 * a maximum number of arguments. The result will contain public constructors first,
	 * with decreasing number of arguments, then non-public constructors, again with
	 * decreasing number of arguments.
	 * @param constructors the constructor array to sort
	 */
	static void sortConstructors(Constructor<?>[] constructors) {
		Arrays.sort(constructors, EXECUTABLE_COMPARATOR);
	}

	/**
	 * Sort the given factory methods, preferring public methods and "greedy" ones
	 * with a maximum of arguments. The result will contain public methods first,
	 * with decreasing number of arguments, then non-public methods, again with
	 * decreasing number of arguments.
	 * @param factoryMethods the factory method array to sort
	 */
	static void sortFactoryMethods(Method[] factoryMethods) {
		Arrays.sort(factoryMethods, EXECUTABLE_COMPARATOR);
	}

	/**
	 * Determine whether the given bean property is excluded from dependency checks.
	 * <p>This implementation excludes properties defined by CGLIB.
	 * @param pd the PropertyDescriptor of the bean property
	 * @return whether the bean property is excluded
	 */
	static boolean isExcludedFromDependencyCheck(PropertyDescriptor pd) {
		Method wm = pd.getWriteMethod();
		if (wm == null) {
			return false;
		}
		if (!wm.getDeclaringClass().getName().contains("$$")) {
			// Not a CGLIB method so it's OK.
			return false;
		}
		// It was declared by CGLIB, but we might still want to autowire it
		// if it was actually declared by the superclass.
		Class<?> superclass = wm.getDeclaringClass().getSuperclass();
		return !ClassUtils.hasMethod(superclass, wm.getName(), wm.getParameterTypes());
	}

	/**
	 * Return whether the setter method of the given bean property is defined
	 * in any of the given interfaces.
	 * @param pd the PropertyDescriptor of the bean property
	 * @param interfaces the Set of interfaces (Class objects)
	 * @return whether the setter method is defined by an interface
	 */
	static boolean isSetterDefinedInInterface(PropertyDescriptor pd, Set<Class<?>> interfaces) {
		Method setter = pd.getWriteMethod();
		if (setter != null) {
			Class<?> targetClass = setter.getDeclaringClass();
			for (Class<?> ifc : interfaces) {
				if (ifc.isAssignableFrom(targetClass) &&
						ClassUtils.hasMethod(ifc, setter.getName(), setter.getParameterTypes())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Resolve the given autowiring value against the given required type,
	 * e.g. an {@link ObjectFactory} value to its actual object result.
	 * @param autowiringValue the value to resolve
	 * @param requiredType the type to assign the result to
	 * @return the resolved value
	 */
	static Object resolveAutowiringValue(Object autowiringValue, Class<?> requiredType) {
		if (autowiringValue instanceof ObjectFactory && !requiredType.isInstance(autowiringValue)) {
			ObjectFactory<?> factory = (ObjectFactory<?>) autowiringValue;
			if (autowiringValue instanceof Serializable && requiredType.isInterface()) {
				autowiringValue = Proxy.newProxyInstance(requiredType.getClassLoader(),
						new Class<?>[] {requiredType}, new ObjectFactoryDelegatingInvocationHandler(factory));
			}
			else {
				return factory.getObject();
			}
		}
		return autowiringValue;
	}

	/**
	 * Determine the target type for the generic return type of the given
	 * <em>generic factory method</em>, where formal type variables are declared
	 * on the given method itself.
	 * <p>For example, given a factory method with the following signature, if
	 * {@code resolveReturnTypeForFactoryMethod()} is invoked with the reflected
	 * method for {@code createProxy()} and an {@code Object[]} array containing
	 * {@code MyService.class}, {@code resolveReturnTypeForFactoryMethod()} will
	 * infer that the target return type is {@code MyService}.
	 * <pre class="code">{@code public static <T> T createProxy(Class<T> clazz)}</pre>
	 * <h4>Possible Return Values</h4>
	 * <ul>
	 * <li>the target return type, if it can be inferred</li>
	 * <li>the {@linkplain Method#getReturnType() standard return type}, if
	 * the given {@code method} does not declare any {@linkplain
	 * Method#getTypeParameters() formal type variables}</li>
	 * <li>the {@linkplain Method#getReturnType() standard return type}, if the
	 * target return type cannot be inferred (e.g., due to type erasure)</li>
	 * <li>{@code null}, if the length of the given arguments array is shorter
	 * than the length of the {@linkplain
	 * Method#getGenericParameterTypes() formal argument list} for the given
	 * method</li>
	 * </ul>
	 * @param method the method to introspect (never {@code null})
	 * @param args the arguments that will be supplied to the method when it is
	 * invoked (never {@code null})
	 * @param classLoader the ClassLoader to resolve class names against,
	 * if necessary (never {@code null})
	 * @return the resolved target return type or the standard method return type
	 * @since 3.2.5
	 */
	static Class<?> resolveReturnTypeForFactoryMethod(
			Method method, Object[] args, @Nullable ClassLoader classLoader) {

		Assert.notNull(method, "Method must not be null");
		Assert.notNull(args, "Argument array must not be null");

		TypeVariable<Method>[] declaredTypeVariables = method.getTypeParameters();
		Type genericReturnType = method.getGenericReturnType();
		Type[] methodParameterTypes = method.getGenericParameterTypes();
		Assert.isTrue(args.length == methodParameterTypes.length, "Argument array does not match parameter count");

		// Ensure that the type variable (e.g., T) is declared directly on the method
		// itself (e.g., via <T>), not on the enclosing class or interface.
		boolean locallyDeclaredTypeVariableMatchesReturnType = false;
		for (TypeVariable<Method> currentTypeVariable : declaredTypeVariables) {
			if (currentTypeVariable.equals(genericReturnType)) {
				locallyDeclaredTypeVariableMatchesReturnType = true;
				break;
			}
		}

		if (locallyDeclaredTypeVariableMatchesReturnType) {
			for (int i = 0; i < methodParameterTypes.length; i++) {
				Type methodParameterType = methodParameterTypes[i];
				Object arg = args[i];
				if (methodParameterType.equals(genericReturnType)) {
					if (arg instanceof TypedStringValue) {
						TypedStringValue typedValue = ((TypedStringValue) arg);
						if (typedValue.hasTargetType()) {
							return typedValue.getTargetType();
						}
						try {
							Class<?> resolvedType = typedValue.resolveTargetType(classLoader);
							if (resolvedType != null) {
								return resolvedType;
							}
						}
						catch (ClassNotFoundException ex) {
							throw new IllegalStateException("Failed to resolve value type [" +
									typedValue.getTargetTypeName() + "] for factory method argument", ex);
						}
					}
					else if (arg != null && !(arg instanceof BeanMetadataElement)) {
						// Only consider argument type if it is a simple value...
						return arg.getClass();
					}
					return method.getReturnType();
				}
				else if (methodParameterType instanceof ParameterizedType) {
					ParameterizedType parameterizedType = (ParameterizedType) methodParameterType;
					Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
					for (Type typeArg : actualTypeArguments) {
						if (typeArg.equals(genericReturnType)) {
							if (arg instanceof Class) {
								return (Class<?>) arg;
							}
							else {
								String className = null;
								if (arg instanceof String) {
									className = (String) arg;
								}
								else if (arg instanceof TypedStringValue) {
									TypedStringValue typedValue = ((TypedStringValue) arg);
									String targetTypeName = typedValue.getTargetTypeName();
									if (targetTypeName == null || Class.class.getName().equals(targetTypeName)) {
										className = typedValue.getValue();
									}
								}
								if (className != null) {
									try {
										return ClassUtils.forName(className, classLoader);
									}
									catch (ClassNotFoundException ex) {
										throw new IllegalStateException("Could not resolve class name [" + arg +
												"] for factory method argument", ex);
									}
								}
								// Consider adding logic to determine the class of the typeArg, if possible.
								// For now, just fall back...
								return method.getReturnType();
							}
						}
					}
				}
			}
		}

		// Fall back...
		return method.getReturnType();
	}

	/**
	 * Determine if the supplied {@link Parameter} can <em>potentially</em> be
	 * autowired from an {@link AutowireCapableBeanFactory}.
	 * <p>Returns {@code true} if the supplied parameter is annotated or
	 * meta-annotated with {@link Autowired @Autowired},
	 * {@link Qualifier @Qualifier}, or {@link Value @Value}.
	 * <p>Note that {@link #resolveDependency} may still be able to resolve the
	 * dependency for the supplied parameter even if this method returns {@code false}.
	 * @param parameter the parameter whose dependency should be autowired
	 * (must not be {@code null})
	 * @param parameterIndex the index of the parameter in the constructor or method
	 * that declares the parameter
	 * @since 5.2
	 * @see #resolveDependency
	 */
	public static boolean isAutowirable(Parameter parameter, int parameterIndex) {
		Assert.notNull(parameter, "Parameter must not be null");
		AnnotatedElement annotatedParameter = getEffectiveAnnotatedParameter(parameter, parameterIndex);
		return (AnnotatedElementUtils.hasAnnotation(annotatedParameter, Autowired.class) ||
				AnnotatedElementUtils.hasAnnotation(annotatedParameter, Qualifier.class) ||
				AnnotatedElementUtils.hasAnnotation(annotatedParameter, Value.class));
	}

	/**
	 * Resolve the dependency for the supplied {@link Parameter} from the
	 * supplied {@link AutowireCapableBeanFactory}.
	 * <p>Provides comprehensive autowiring support for individual method parameters
	 * on par with Spring's dependency injection facilities for autowired fields and
	 * methods, including support for {@link Autowired @Autowired},
	 * {@link Qualifier @Qualifier}, and {@link Value @Value} with support for property
	 * placeholders and SpEL expressions in {@code @Value} declarations.
	 * <p>The dependency is required unless the parameter is annotated or meta-annotated
	 * with {@link Autowired @Autowired} with the {@link Autowired#required required}
	 * flag set to {@code false}.
	 * <p>If an explicit <em>qualifier</em> is not declared, the name of the parameter
	 * will be used as the qualifier for resolving ambiguities.
	 * @param parameter the parameter whose dependency should be resolved (must not be
	 * {@code null})
	 * @param parameterIndex the index of the parameter in the constructor or method
	 * that declares the parameter
	 * @param containingClass the concrete class that contains the parameter; this may
	 * differ from the class that declares the parameter in that it may be a subclass
	 * thereof, potentially substituting type variables (must not be {@code null})
	 * @param beanFactory the {@code AutowireCapableBeanFactory} from which to resolve
	 * the dependency (must not be {@code null})
	 * @return the resolved object, or {@code null} if none found
	 * @throws BeansException if dependency resolution failed
	 * @since 5.2
	 * @see #isAutowirable
	 * @see Autowired#required
	 * @see SynthesizingMethodParameter#forExecutable(Executable, int)
	 * @see AutowireCapableBeanFactory#resolveDependency(DependencyDescriptor, String)
	 */
	@Nullable
	public static Object resolveDependency(
			Parameter parameter, int parameterIndex, Class<?> containingClass, AutowireCapableBeanFactory beanFactory)
			throws BeansException {

		Assert.notNull(parameter, "Parameter must not be null");
		Assert.notNull(containingClass, "Containing class must not be null");
		Assert.notNull(beanFactory, "AutowireCapableBeanFactory must not be null");

		AnnotatedElement annotatedParameter = getEffectiveAnnotatedParameter(parameter, parameterIndex);
		Autowired autowired = AnnotatedElementUtils.findMergedAnnotation(annotatedParameter, Autowired.class);
		boolean required = (autowired == null || autowired.required());

		MethodParameter methodParameter = SynthesizingMethodParameter.forExecutable(
				parameter.getDeclaringExecutable(), parameterIndex);
		DependencyDescriptor descriptor = new DependencyDescriptor(methodParameter, required);
		descriptor.setContainingClass(containingClass);
		return beanFactory.resolveDependency(descriptor, null);
	}

	/**
	 * Due to a bug in {@code javac} on JDK versions prior to JDK 9, looking up
	 * annotations directly on a {@link Parameter} will fail for inner class
	 * constructors.
	 * <h4>Bug in javac in JDK &lt; 9</h4>
	 * <p>The parameter annotations array in the compiled byte code excludes an entry
	 * for the implicit <em>enclosing instance</em> parameter for an inner class
	 * constructor.
	 * <h4>Workaround</h4>
	 * <p>This method provides a workaround for this off-by-one error by allowing the
	 * caller to access annotations on the preceding {@link Parameter} object (i.e.,
	 * {@code index - 1}). If the supplied {@code index} is zero, this method returns
	 * an empty {@code AnnotatedElement}.
	 * <h4>WARNING</h4>
	 * <p>The {@code AnnotatedElement} returned by this method should never be cast and
	 * treated as a {@code Parameter} since the metadata (e.g., {@link Parameter#getName()},
	 * {@link Parameter#getType()}, etc.) will not match those for the declared parameter
	 * at the given index in an inner class constructor.
	 * @return the supplied {@code parameter} or the <em>effective</em> {@code Parameter}
	 * if the aforementioned bug is in effect
	 */
	private static AnnotatedElement getEffectiveAnnotatedParameter(Parameter parameter, int index) {
		Executable executable = parameter.getDeclaringExecutable();
		if (executable instanceof Constructor && ClassUtils.isInnerClass(executable.getDeclaringClass()) &&
				executable.getParameterAnnotations().length == executable.getParameterCount() - 1) {
			// Bug in javac in JDK <9: annotation array excludes enclosing instance parameter
			// for inner classes, so access it with the actual parameter index lowered by 1
			return (index == 0 ? EMPTY_ANNOTATED_ELEMENT : executable.getParameters()[index - 1]);
		}
		return parameter;
	}


	/**
	 * Reflective {@link InvocationHandler} for lazy access to the current target object.
	 */
	@SuppressWarnings("serial")
	private static class ObjectFactoryDelegatingInvocationHandler implements InvocationHandler, Serializable {

		private final ObjectFactory<?> objectFactory;

		public ObjectFactoryDelegatingInvocationHandler(ObjectFactory<?> objectFactory) {
			this.objectFactory = objectFactory;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			String methodName = method.getName();
			if (methodName.equals("equals")) {
				// Only consider equal when proxies are identical.
				return (proxy == args[0]);
			}
			else if (methodName.equals("hashCode")) {
				// Use hashCode of proxy.
				return System.identityHashCode(proxy);
			}
			else if (methodName.equals("toString")) {
				return this.objectFactory.toString();
			}
			try {
				return method.invoke(this.objectFactory.getObject(), args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

}
