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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * General utility methods for working with annotations, handling meta-annotations,
 * bridge methods (which the compiler generates for generic declarations) as well
 * as super methods (for optional <em>annotation inheritance</em>).
 *
 * <p>Note that most of the features of this class are not provided by the
 * JDK's introspection facilities themselves.
 *
 * <p>As a general rule for runtime-retained annotations (e.g. for transaction
 * control, authorization, or service exposure), always use the lookup methods
 * on this class (e.g., {@link #findAnnotation(Method, Class)},
 * {@link #getAnnotation(Method, Class)}, and {@link #getAnnotations(Method)})
 * instead of the plain annotation lookup methods in the JDK. You can still
 * explicitly choose between a <em>get</em> lookup on the given class level only
 * ({@link #getAnnotation(Method, Class)}) and a <em>find</em> lookup in the entire
 * inheritance hierarchy of the given method ({@link #findAnnotation(Method, Class)}).
 *
 * <h3>Terminology</h3>
 * The terms <em>directly present</em> and <em>present</em> have the same
 * meanings as defined in the class-level Javadoc for {@link AnnotatedElement}.
 *
 * <p>An annotation is <em>meta-present</em> on an element if the annotation
 * is declared as a meta-annotation on some other annotation which is
 * <em>present</em> on the element.
 *
 * <h3>Meta-annotation Support</h3>
 * <p>Most {@code find*()} methods and some {@code get*()} methods in this
 * class provide support for finding annotations used as meta-annotations.
 * Consult the Javadoc for each method in this class for details. For support
 * for meta-annotations with <em>attribute overrides</em> in
 * <em>composed annotations</em>, use {@link AnnotatedElementUtils} instead.
 *
 * <h3>Search Scope</h3>
 * <p>The search algorithms used by methods in this class stop searching for
 * an annotation once the first annotation of the specified type has been
 * found. As a consequence, additional annotations of the specified type will
 * be silently ignored.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Mark Fisher
 * @author Chris Beams
 * @author Phillip Webb
 * @since 2.0
 * @see AliasFor
 * @see AnnotationAttributes
 * @see AnnotatedElementUtils
 * @see BridgeMethodResolver
 * @see java.lang.reflect.AnnotatedElement#getAnnotations()
 * @see java.lang.reflect.AnnotatedElement#getAnnotation(Class)
 * @see java.lang.reflect.AnnotatedElement#getDeclaredAnnotations()
 */
public abstract class AnnotationUtils {

	/**
	 * The attribute name for annotations with a single element.
	 */
	public static final String VALUE = "value";


	/**
	 * An object that can be stored in {@link AnnotationAttributes} as a
	 * placeholder for an attribute's declared default value.
	 */
	private static final Object DEFAULT_VALUE_PLACEHOLDER = new String("<SPRING DEFAULT VALUE PLACEHOLDER>");

	private static final Map<AnnotationCacheKey, Annotation> findAnnotationCache =
			new ConcurrentReferenceHashMap<AnnotationCacheKey, Annotation>(256);

	private static final Map<Class<?>, Boolean> annotatedInterfaceCache =
			new ConcurrentReferenceHashMap<Class<?>, Boolean>(256);

	private static final Map<Class<? extends Annotation>, Boolean> synthesizableCache =
			new ConcurrentReferenceHashMap<Class<? extends Annotation>, Boolean>(256);

	private static final Map<Class<? extends Annotation>, Map<String, String>> attributeAliasesCache =
			new ConcurrentReferenceHashMap<Class<? extends Annotation>, Map<String, String>>(256);

	private static final Map<Class<? extends Annotation>, List<Method>> attributeMethodsCache =
			new ConcurrentReferenceHashMap<Class<? extends Annotation>, List<Method>>(256);

	private static transient Log logger;


	/**
	 * Get a single {@link Annotation} of {@code annotationType} from the supplied
	 * annotation: either the given annotation itself or a direct meta-annotation
	 * thereof.
	 * <p>Note that this method supports only a single level of meta-annotations.
	 * For support for arbitrary levels of meta-annotations, use one of the
	 * {@code find*()} methods instead.
	 * @param ann the Annotation to check
	 * @param annotationType the annotation type to look for, both locally and as a meta-annotation
	 * @return the first matching annotation, or {@code null} if not found
	 * @since 4.0
	 */
	@SuppressWarnings("unchecked")
	public static <A extends Annotation> A getAnnotation(Annotation ann, Class<A> annotationType) {
		if (annotationType.isInstance(ann)) {
			return synthesizeAnnotation((A) ann);
		}
		Class<? extends Annotation> annotatedElement = ann.annotationType();
		try {
			return synthesizeAnnotation(annotatedElement.getAnnotation(annotationType), annotatedElement);
		}
		catch (Exception ex) {
			// Assuming nested Class values not resolvable within annotation attributes...
			handleIntrospectionFailure(annotatedElement, ex);
			return null;
		}
	}

	/**
	 * Get a single {@link Annotation} of {@code annotationType} from the supplied
	 * {@link AnnotatedElement}, where the annotation is either <em>present</em> or
	 * <em>meta-present</em> on the {@code AnnotatedElement}.
	 * <p>Note that this method supports only a single level of meta-annotations.
	 * For support for arbitrary levels of meta-annotations, use
	 * {@link #findAnnotation(AnnotatedElement, Class)} instead.
	 * @param annotatedElement the {@code AnnotatedElement} from which to get the annotation
	 * @param annotationType the annotation type to look for, both locally and as a meta-annotation
	 * @return the first matching annotation, or {@code null} if not found
	 * @since 3.1
	 */
	public static <A extends Annotation> A getAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType) {
		try {
			A annotation = annotatedElement.getAnnotation(annotationType);
			if (annotation == null) {
				for (Annotation metaAnn : annotatedElement.getAnnotations()) {
					annotation = metaAnn.annotationType().getAnnotation(annotationType);
					if (annotation != null) {
						break;
					}
				}
			}
			return synthesizeAnnotation(annotation, annotatedElement);
		}
		catch (Exception ex) {
			// Assuming nested Class values not resolvable within annotation attributes...
			handleIntrospectionFailure(annotatedElement, ex);
			return null;
		}
	}

	/**
	 * Get a single {@link Annotation} of {@code annotationType} from the
	 * supplied {@link Method}, where the annotation is either <em>present</em>
	 * or <em>meta-present</em> on the method.
	 * <p>Correctly handles bridge {@link Method Methods} generated by the compiler.
	 * <p>Note that this method supports only a single level of meta-annotations.
	 * For support for arbitrary levels of meta-annotations, use
	 * {@link #findAnnotation(Method, Class)} instead.
	 * @param method the method to look for annotations on
	 * @param annotationType the annotation type to look for
	 * @return the first matching annotation, or {@code null} if not found
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod(Method)
	 * @see #getAnnotation(AnnotatedElement, Class)
	 */
	public static <A extends Annotation> A getAnnotation(Method method, Class<A> annotationType) {
		Method resolvedMethod = BridgeMethodResolver.findBridgedMethod(method);
		return getAnnotation((AnnotatedElement) resolvedMethod, annotationType);
	}

	/**
	 * Get all {@link Annotation Annotations} that are <em>present</em> on the
	 * supplied {@link AnnotatedElement}.
	 * <p>Meta-annotations will <em>not</em> be searched.
	 * @param annotatedElement the Method, Constructor or Field to retrieve annotations from
	 * @return the annotations found, an empty array, or {@code null} if not
	 * resolvable (e.g. because nested Class values in annotation attributes
	 * failed to resolve at runtime)
	 * @since 4.0.8
	 */
	public static Annotation[] getAnnotations(AnnotatedElement annotatedElement) {
		try {
			return annotatedElement.getAnnotations();
		}
		catch (Exception ex) {
			// Assuming nested Class values not resolvable within annotation attributes...
			handleIntrospectionFailure(annotatedElement, ex);
		}
		return null;
	}

	/**
	 * Get all {@link Annotation Annotations} that are <em>present</em on the
	 * supplied {@link Method}.
	 * <p>Correctly handles bridge {@link Method Methods} generated by the compiler.
	 * <p>Meta-annotations will <em>not</em> be searched.
	 * @param method the Method to retrieve annotations from
	 * @return the annotations found, an empty array, or {@code null} if not
	 * resolvable (e.g. because nested Class values in annotation attributes
	 * failed to resolve at runtime)
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod(Method)
	 * @see AnnotatedElement#getAnnotations()
	 */
	public static Annotation[] getAnnotations(Method method) {
		try {
			return BridgeMethodResolver.findBridgedMethod(method).getAnnotations();
		}
		catch (Exception ex) {
			// Assuming nested Class values not resolvable within annotation attributes...
			handleIntrospectionFailure(method, ex);
		}
		return null;
	}

	/**
	 * Get the <em>repeatable</em> {@link Annotation}s of {@code annotationType}
	 * from the supplied {@link Method}, where such annotations are either
	 * <em>present</em> or <em>meta-present</em> on the method.
	 * <p>Handles both single annotations and annotations nested within a
	 * <em>containing annotation</em>.
	 * <p>Correctly handles bridge {@link Method Methods} generated by the compiler.
	 * <p>Meta-annotations will be searched if the annotation is not
	 * <em>present</em> on the supplied method.
	 * @param method the method to look for annotations on; never {@code null}
	 * @param containerAnnotationType the type of the container that holds the
	 * annotations; may be {@code null} if a container is not supported
	 * @param annotationType the annotation type to look for
	 * @return the annotations found or an empty set; never {@code null}
	 * @since 4.0
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod(Method)
	 * @see java.lang.annotation.Repeatable
	 */
	public static <A extends Annotation> Set<A> getRepeatableAnnotation(Method method,
			Class<? extends Annotation> containerAnnotationType, Class<A> annotationType) {

		Method resolvedMethod = BridgeMethodResolver.findBridgedMethod(method);
		return getRepeatableAnnotation((AnnotatedElement) resolvedMethod, containerAnnotationType, annotationType);
	}

	/**
	 * Get the <em>repeatable</em> {@link Annotation}s of {@code annotationType}
	 * from the supplied {@link AnnotatedElement}, where such annotations are
	 * either <em>present</em> or <em>meta-present</em> on the element.
	 * <p>Handles both single annotations and annotations nested within a
	 * <em>containing annotation</em>.
	 * <p>Meta-annotations will be searched if the annotation is not
	 * <em>present</em> on the supplied element.
	 * @param annotatedElement the element to look for annotations on; never {@code null}
	 * @param containerAnnotationType the type of the container that holds the
	 * annotations; may be {@code null} if a container is not supported
	 * @param annotationType the annotation type to look for
	 * @return the annotations found or an empty set; never {@code null}
	 * @since 4.0
	 * @see java.lang.annotation.Repeatable
	 */
	public static <A extends Annotation> Set<A> getRepeatableAnnotation(AnnotatedElement annotatedElement,
			Class<? extends Annotation> containerAnnotationType, Class<A> annotationType) {

		try {
			if (annotatedElement.getAnnotations().length > 0) {
				return new AnnotationCollector<A>(containerAnnotationType, annotationType).getResult(annotatedElement);
			}
		}
		catch (Exception ex) {
			// Assuming nested Class values not resolvable within annotation attributes...
			handleIntrospectionFailure(annotatedElement, ex);
		}
		return Collections.emptySet();
	}

	/**
	 * Find a single {@link Annotation} of {@code annotationType} on the
	 * supplied {@link AnnotatedElement}.
	 * <p>Meta-annotations will be searched if the annotation is not
	 * <em>directly present</em> on the supplied element.
	 * <p><strong>Warning</strong>: this method operates generically on
	 * annotated elements. In other words, this method does not execute
	 * specialized search algorithms for classes or methods. If you require
	 * the more specific semantics of {@link #findAnnotation(Class, Class)}
	 * or {@link #findAnnotation(Method, Class)}, invoke one of those methods
	 * instead.
	 * @param annotatedElement the {@code AnnotatedElement} on which to find the annotation
	 * @param annotationType the annotation type to look for, both locally and as a meta-annotation
	 * @return the first matching annotation, or {@code null} if not found
	 * @since 4.2
	 */
	public static <A extends Annotation> A findAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType) {
		// Do NOT store result in the findAnnotationCache since doing so could break
		// findAnnotation(Class, Class) and findAnnotation(Method, Class).
		return synthesizeAnnotation(findAnnotation(annotatedElement, annotationType, new HashSet<Annotation>()),
			annotatedElement);
	}

	/**
	 * Perform the search algorithm for {@link #findAnnotation(AnnotatedElement, Class)}
	 * avoiding endless recursion by tracking which annotations have already
	 * been <em>visited</em>.
	 * @param annotatedElement the {@code AnnotatedElement} on which to find the annotation
	 * @param annotationType the annotation type to look for, both locally and as a meta-annotation
	 * @param visited the set of annotations that have already been visited
	 * @return the first matching annotation, or {@code null} if not found
	 * @since 4.2
	 */
	@SuppressWarnings("unchecked")
	private static <A extends Annotation> A findAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType, Set<Annotation> visited) {
		Assert.notNull(annotatedElement, "AnnotatedElement must not be null");
		try {
			Annotation[] anns = annotatedElement.getDeclaredAnnotations();
			for (Annotation ann : anns) {
				if (ann.annotationType().equals(annotationType)) {
					return (A) ann;
				}
			}
			for (Annotation ann : anns) {
				if (!isInJavaLangAnnotationPackage(ann) && visited.add(ann)) {
					A annotation = findAnnotation((AnnotatedElement) ann.annotationType(), annotationType, visited);
					if (annotation != null) {
						return annotation;
					}
				}
			}
		}
		catch (Exception ex) {
			// Assuming nested Class values not resolvable within annotation attributes...
			handleIntrospectionFailure(annotatedElement, ex);
		}
		return null;
	}

	/**
	 * Find a single {@link Annotation} of {@code annotationType} on the supplied
	 * {@link Method}, traversing its super methods (i.e., from superclasses and
	 * interfaces) if the annotation is not <em>directly present</em> on the given
	 * method itself.
	 * <p>Correctly handles bridge {@link Method Methods} generated by the compiler.
	 * <p>Meta-annotations will be searched if the annotation is not
	 * <em>directly present</em> on the method.
	 * <p>Annotations on methods are not inherited by default, so we need to handle
	 * this explicitly.
	 * @param method the method to look for annotations on
	 * @param annotationType the annotation type to look for
	 * @return the first matching annotation, or {@code null} if not found
	 * @see #getAnnotation(Method, Class)
	 */
	@SuppressWarnings("unchecked")
	public static <A extends Annotation> A findAnnotation(Method method, Class<A> annotationType) {
		AnnotationCacheKey cacheKey = new AnnotationCacheKey(method, annotationType);
		A result = (A) findAnnotationCache.get(cacheKey);

		if (result == null) {
			Method resolvedMethod = BridgeMethodResolver.findBridgedMethod(method);
			result = findAnnotation((AnnotatedElement) resolvedMethod, annotationType);

			if (result == null) {
				result = searchOnInterfaces(method, annotationType, method.getDeclaringClass().getInterfaces());
			}

			Class<?> clazz = method.getDeclaringClass();
			while (result == null) {
				clazz = clazz.getSuperclass();
				if (clazz == null || Object.class == clazz) {
					break;
				}
				try {
					Method equivalentMethod = clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
					Method resolvedEquivalentMethod = BridgeMethodResolver.findBridgedMethod(equivalentMethod);
					result = findAnnotation((AnnotatedElement) resolvedEquivalentMethod, annotationType);
				}
				catch (NoSuchMethodException ex) {
					// No equivalent method found
				}
				if (result == null) {
					result = searchOnInterfaces(method, annotationType, clazz.getInterfaces());
				}
			}

			if (result != null) {
				findAnnotationCache.put(cacheKey, result);
			}
		}

		return synthesizeAnnotation(result, method);
	}

	private static <A extends Annotation> A searchOnInterfaces(Method method, Class<A> annotationType, Class<?>... ifcs) {
		A annotation = null;
		for (Class<?> iface : ifcs) {
			if (isInterfaceWithAnnotatedMethods(iface)) {
				try {
					Method equivalentMethod = iface.getMethod(method.getName(), method.getParameterTypes());
					annotation = getAnnotation(equivalentMethod, annotationType);
				}
				catch (NoSuchMethodException ex) {
					// Skip this interface - it doesn't have the method...
				}
				if (annotation != null) {
					break;
				}
			}
		}
		return annotation;
	}

	static boolean isInterfaceWithAnnotatedMethods(Class<?> iface) {
		Boolean found = annotatedInterfaceCache.get(iface);
		if (found != null) {
			return found.booleanValue();
		}
		found = Boolean.FALSE;
		for (Method ifcMethod : iface.getMethods()) {
			try {
				if (ifcMethod.getAnnotations().length > 0) {
					found = Boolean.TRUE;
					break;
				}
			}
			catch (Exception ex) {
				// Assuming nested Class values not resolvable within annotation attributes...
				handleIntrospectionFailure(ifcMethod, ex);
			}
		}
		annotatedInterfaceCache.put(iface, found);
		return found.booleanValue();
	}

	/**
	 * Find a single {@link Annotation} of {@code annotationType} on the
	 * supplied {@link Class}, traversing its interfaces, annotations, and
	 * superclasses if the annotation is not <em>directly present</em> on
	 * the given class itself.
	 * <p>This method explicitly handles class-level annotations which are not
	 * declared as {@link java.lang.annotation.Inherited inherited} <em>as well
	 * as meta-annotations and annotations on interfaces</em>.
	 * <p>The algorithm operates as follows:
	 * <ol>
	 * <li>Search for the annotation on the given class and return it if found.
	 * <li>Recursively search through all annotations that the given class declares.
	 * <li>Recursively search through all interfaces that the given class declares.
	 * <li>Recursively search through the superclass hierarchy of the given class.
	 * </ol>
	 * <p>Note: in this context, the term <em>recursively</em> means that the search
	 * process continues by returning to step #1 with the current interface,
	 * annotation, or superclass as the class to look for annotations on.
	 * @param clazz the class to look for annotations on
	 * @param annotationType the type of annotation to look for
	 * @return the first matching annotation, or {@code null} if not found
	 */
	@SuppressWarnings("unchecked")
	public static <A extends Annotation> A findAnnotation(Class<?> clazz, Class<A> annotationType) {
		AnnotationCacheKey cacheKey = new AnnotationCacheKey(clazz, annotationType);
		A result = (A) findAnnotationCache.get(cacheKey);
		if (result == null) {
			result = findAnnotation(clazz, annotationType, new HashSet<Annotation>());
			if (result != null) {
				findAnnotationCache.put(cacheKey, result);
			}
		}
		return synthesizeAnnotation(result, clazz);
	}

	/**
	 * Perform the search algorithm for {@link #findAnnotation(Class, Class)},
	 * avoiding endless recursion by tracking which annotations have already
	 * been <em>visited</em>.
	 * @param clazz the class to look for annotations on
	 * @param annotationType the type of annotation to look for
	 * @param visited the set of annotations that have already been visited
	 * @return the first matching annotation, or {@code null} if not found
	 */
	@SuppressWarnings("unchecked")
	private static <A extends Annotation> A findAnnotation(Class<?> clazz, Class<A> annotationType, Set<Annotation> visited) {
		Assert.notNull(clazz, "Class must not be null");

		try {
			Annotation[] anns = clazz.getDeclaredAnnotations();
			for (Annotation ann : anns) {
				if (ann.annotationType().equals(annotationType)) {
					return (A) ann;
				}
			}
			for (Annotation ann : anns) {
				if (!isInJavaLangAnnotationPackage(ann) && visited.add(ann)) {
					A annotation = findAnnotation(ann.annotationType(), annotationType, visited);
					if (annotation != null) {
						return annotation;
					}
				}
			}
		}
		catch (Exception ex) {
			// Assuming nested Class values not resolvable within annotation attributes...
			handleIntrospectionFailure(clazz, ex);
			return null;
		}

		for (Class<?> ifc : clazz.getInterfaces()) {
			A annotation = findAnnotation(ifc, annotationType, visited);
			if (annotation != null) {
				return annotation;
			}
		}

		Class<?> superclass = clazz.getSuperclass();
		if (superclass == null || Object.class == superclass) {
			return null;
		}
		return findAnnotation(superclass, annotationType, visited);
	}

	/**
	 * Find the first {@link Class} in the inheritance hierarchy of the
	 * specified {@code clazz} (including the specified {@code clazz} itself)
	 * on which an annotation of the specified {@code annotationType} is
	 * <em>directly present</em>.
	 * <p>If the supplied {@code clazz} is an interface, only the interface
	 * itself will be checked; the inheritance hierarchy for interfaces will
	 * not be traversed.
	 * <p>Meta-annotations will <em>not</em> be searched.
	 * <p>The standard {@link Class} API does not provide a mechanism for
	 * determining which class in an inheritance hierarchy actually declares
	 * an {@link Annotation}, so we need to handle this explicitly.
	 * @param annotationType the annotation type to look for
	 * @param clazz the class to check for the annotation on (may be {@code null})
	 * @return the first {@link Class} in the inheritance hierarchy that
	 * declares an annotation of the specified {@code annotationType}, or
	 * {@code null} if not found
	 * @see Class#isAnnotationPresent(Class)
	 * @see Class#getDeclaredAnnotations()
	 * @see #findAnnotationDeclaringClassForTypes(List, Class)
	 * @see #isAnnotationDeclaredLocally(Class, Class)
	 */
	public static Class<?> findAnnotationDeclaringClass(Class<? extends Annotation> annotationType, Class<?> clazz) {
		Assert.notNull(annotationType, "Annotation type must not be null");
		if (clazz == null || Object.class == clazz) {
			return null;
		}
		if (isAnnotationDeclaredLocally(annotationType, clazz)) {
			return clazz;
		}
		return findAnnotationDeclaringClass(annotationType, clazz.getSuperclass());
	}

	/**
	 * Find the first {@link Class} in the inheritance hierarchy of the
	 * specified {@code clazz} (including the specified {@code clazz} itself)
	 * on which at least one of the specified {@code annotationTypes} is
	 * <em>directly present</em>.
	 * <p>If the supplied {@code clazz} is an interface, only the interface
	 * itself will be checked; the inheritance hierarchy for interfaces will
	 * not be traversed.
	 * <p>Meta-annotations will <em>not</em> be searched.
	 * <p>The standard {@link Class} API does not provide a mechanism for
	 * determining which class in an inheritance hierarchy actually declares
	 * one of several candidate {@linkplain Annotation annotations}, so we
	 * need to handle this explicitly.
	 * @param annotationTypes the annotation types to look for
	 * @param clazz the class to check for the annotations on, or {@code null}
	 * @return the first {@link Class} in the inheritance hierarchy that
	 * declares an annotation of at least one of the specified
	 * {@code annotationTypes}, or {@code null} if not found
	 * @since 3.2.2
	 * @see Class#isAnnotationPresent(Class)
	 * @see Class#getDeclaredAnnotations()
	 * @see #findAnnotationDeclaringClass(Class, Class)
	 * @see #isAnnotationDeclaredLocally(Class, Class)
	 */
	public static Class<?> findAnnotationDeclaringClassForTypes(List<Class<? extends Annotation>> annotationTypes, Class<?> clazz) {
		Assert.notEmpty(annotationTypes, "The list of annotation types must not be empty");
		if (clazz == null || Object.class == clazz) {
			return null;
		}
		for (Class<? extends Annotation> annotationType : annotationTypes) {
			if (isAnnotationDeclaredLocally(annotationType, clazz)) {
				return clazz;
			}
		}
		return findAnnotationDeclaringClassForTypes(annotationTypes, clazz.getSuperclass());
	}

	/**
	 * Determine whether an annotation of the specified {@code annotationType}
	 * is declared locally (i.e., <em>directly present</em>) on the supplied
	 * {@code clazz}.
	 * <p>The supplied {@link Class} may represent any type.
	 * <p>Meta-annotations will <em>not</em> be searched.
	 * <p>Note: This method does <strong>not</strong> determine if the annotation
	 * is {@linkplain java.lang.annotation.Inherited inherited}. For greater
	 * clarity regarding inherited annotations, consider using
	 * {@link #isAnnotationInherited(Class, Class)} instead.
	 * @param annotationType the annotation type to look for
	 * @param clazz the class to check for the annotation on
	 * @return {@code true} if an annotation of the specified {@code annotationType}
	 * is <em>directly present</em>
	 * @see java.lang.Class#getDeclaredAnnotations()
	 * @see java.lang.Class#getDeclaredAnnotation(Class)
	 * @see #isAnnotationInherited(Class, Class)
	 */
	public static boolean isAnnotationDeclaredLocally(Class<? extends Annotation> annotationType, Class<?> clazz) {
		Assert.notNull(annotationType, "Annotation type must not be null");
		Assert.notNull(clazz, "Class must not be null");
		try {
			for (Annotation ann : clazz.getDeclaredAnnotations()) {
				if (ann.annotationType().equals(annotationType)) {
					return true;
				}
			}
		}
		catch (Exception ex) {
			// Assuming nested Class values not resolvable within annotation attributes...
			handleIntrospectionFailure(clazz, ex);
		}
		return false;
	}

	/**
	 * Determine whether an annotation of the specified {@code annotationType}
	 * is <em>present</em> on the supplied {@code clazz} and is
	 * {@linkplain java.lang.annotation.Inherited inherited} (i.e., not
	 * <em>directly present</em>).
	 * <p>Meta-annotations will <em>not</em> be searched.
	 * <p>If the supplied {@code clazz} is an interface, only the interface
	 * itself will be checked. In accordance with standard meta-annotation
	 * semantics in Java, the inheritance hierarchy for interfaces will not
	 * be traversed. See the {@linkplain java.lang.annotation.Inherited Javadoc}
	 * for the {@code @Inherited} meta-annotation for further details regarding
	 * annotation inheritance.
	 * @param annotationType the annotation type to look for
	 * @param clazz the class to check for the annotation on
	 * @return {@code true} if an annotation of the specified {@code annotationType}
	 * is <em>present</em> and <em>inherited</em>
	 * @see Class#isAnnotationPresent(Class)
	 * @see #isAnnotationDeclaredLocally(Class, Class)
	 */
	public static boolean isAnnotationInherited(Class<? extends Annotation> annotationType, Class<?> clazz) {
		Assert.notNull(annotationType, "Annotation type must not be null");
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isAnnotationPresent(annotationType) && !isAnnotationDeclaredLocally(annotationType, clazz));
	}

	/**
	 * Determine if the supplied {@link Annotation} is defined in the core JDK
	 * {@code java.lang.annotation} package.
	 * @param annotation the annotation to check (never {@code null})
	 * @return {@code true} if the annotation is in the {@code java.lang.annotation} package
	 */
	public static boolean isInJavaLangAnnotationPackage(Annotation annotation) {
		Assert.notNull(annotation, "Annotation must not be null");
		return isInJavaLangAnnotationPackage(annotation.annotationType().getName());
	}

	/**
	 * Determine if the {@link Annotation} with the supplied name is defined
	 * in the core JDK {@code java.lang.annotation} package.
	 * @param annotationType the name of the annotation type to check (never {@code null} or empty)
	 * @return {@code true} if the annotation is in the {@code java.lang.annotation} package
	 * @since 4.2
	 */
	public static boolean isInJavaLangAnnotationPackage(String annotationType) {
		Assert.hasText(annotationType, "annotationType must not be null or empty");
		return annotationType.startsWith("java.lang.annotation");
	}

	/**
	 * Retrieve the given annotation's attributes as a {@link Map}, preserving all
	 * attribute types.
	 * <p>Equivalent to calling {@link #getAnnotationAttributes(Annotation, boolean, boolean)}
	 * with the {@code classValuesAsString} and {@code nestedAnnotationsAsMap} parameters
	 * set to {@code false}.
	 * <p>Note: This method actually returns an {@link AnnotationAttributes} instance.
	 * However, the {@code Map} signature has been preserved for binary compatibility.
	 * @param annotation the annotation to retrieve the attributes for
	 * @return the Map of annotation attributes, with attribute names as keys and
	 * corresponding attribute values as values; never {@code null}
	 * @see #getAnnotationAttributes(AnnotatedElement, Annotation)
	 * @see #getAnnotationAttributes(Annotation, boolean, boolean)
	 * @see #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean)
	 */
	public static Map<String, Object> getAnnotationAttributes(Annotation annotation) {
		return getAnnotationAttributes(null, annotation);
	}

	/**
	 * Retrieve the given annotation's attributes as a {@link Map}.
	 * <p>Equivalent to calling {@link #getAnnotationAttributes(Annotation, boolean, boolean)}
	 * with the {@code nestedAnnotationsAsMap} parameter set to {@code false}.
	 * <p>Note: This method actually returns an {@link AnnotationAttributes} instance.
	 * However, the {@code Map} signature has been preserved for binary compatibility.
	 * @param annotation the annotation to retrieve the attributes for
	 * @param classValuesAsString whether to convert Class references into Strings (for
	 * compatibility with {@link org.springframework.core.type.AnnotationMetadata})
	 * or to preserve them as Class references
	 * @return the Map of annotation attributes, with attribute names as keys and
	 * corresponding attribute values as values; never {@code null}
	 * @see #getAnnotationAttributes(Annotation, boolean, boolean)
	 */
	public static Map<String, Object> getAnnotationAttributes(Annotation annotation, boolean classValuesAsString) {
		return getAnnotationAttributes(annotation, classValuesAsString, false);
	}

	/**
	 * Retrieve the given annotation's attributes as an {@link AnnotationAttributes} map.
	 * <p>This method provides fully recursive annotation reading capabilities on par with
	 * the reflection-based {@link org.springframework.core.type.StandardAnnotationMetadata}.
	 * @param annotation the annotation to retrieve the attributes for
	 * @param classValuesAsString whether to convert Class references into Strings (for
	 * compatibility with {@link org.springframework.core.type.AnnotationMetadata})
	 * or to preserve them as Class references
	 * @param nestedAnnotationsAsMap whether to convert nested annotations into
	 * {@link AnnotationAttributes} maps (for compatibility with
	 * {@link org.springframework.core.type.AnnotationMetadata}) or to preserve them as
	 * {@code Annotation} instances
	 * @return the annotation attributes (a specialized Map) with attribute names as keys
	 * and corresponding attribute values as values; never {@code null}
	 * @since 3.1.1
	 */
	public static AnnotationAttributes getAnnotationAttributes(Annotation annotation, boolean classValuesAsString,
			boolean nestedAnnotationsAsMap) {
		return getAnnotationAttributes(null, annotation, classValuesAsString, nestedAnnotationsAsMap);
	}

	/**
	 * Retrieve the given annotation's attributes as an {@link AnnotationAttributes} map.
	 * <p>Equivalent to calling {@link #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean)}
	 * with the {@code classValuesAsString} and {@code nestedAnnotationsAsMap} parameters
	 * set to {@code false}.
	 * @param annotatedElement the element that is annotated with the supplied annotation;
	 * may be {@code null} if unknown
	 * @param annotation the annotation to retrieve the attributes for
	 * @return the annotation attributes (a specialized Map) with attribute names as keys
	 * and corresponding attribute values as values; never {@code null}
	 * @since 4.2
	 * @see #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean)
	 */
	public static AnnotationAttributes getAnnotationAttributes(AnnotatedElement annotatedElement, Annotation annotation) {
		return getAnnotationAttributes(annotatedElement, annotation, false, false);
	}

	/**
	 * Retrieve the given annotation's attributes as an {@link AnnotationAttributes} map.
	 * <p>This method provides fully recursive annotation reading capabilities on par with
	 * the reflection-based {@link org.springframework.core.type.StandardAnnotationMetadata}.
	 * @param annotatedElement the element that is annotated with the supplied annotation;
	 * may be {@code null} if unknown
	 * @param annotation the annotation to retrieve the attributes for
	 * @param classValuesAsString whether to convert Class references into Strings (for
	 * compatibility with {@link org.springframework.core.type.AnnotationMetadata})
	 * or to preserve them as Class references
	 * @param nestedAnnotationsAsMap whether to convert nested annotations into
	 * {@link AnnotationAttributes} maps (for compatibility with
	 * {@link org.springframework.core.type.AnnotationMetadata}) or to preserve them as
	 * {@code Annotation} instances
	 * @return the annotation attributes (a specialized Map) with attribute names as keys
	 * and corresponding attribute values as values; never {@code null}
	 * @since 4.2
	 */
	public static AnnotationAttributes getAnnotationAttributes(AnnotatedElement annotatedElement,
			Annotation annotation, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		return getAnnotationAttributes(annotatedElement, annotation, classValuesAsString, nestedAnnotationsAsMap, false);
	}

	/**
	 * Retrieve the given annotation's attributes as an {@link AnnotationAttributes} map.
	 *
	 * <p>This method provides fully recursive annotation reading capabilities on par with
	 * the reflection-based {@link org.springframework.core.type.StandardAnnotationMetadata}.
	 *
	 * <p><strong>NOTE</strong>: This variant of {@code getAnnotationAttributes()} is
	 * only intended for use within the framework. Specifically, the {@code mergeMode} flag
	 * can be set to {@code true} in order to support processing of attribute aliases while
	 * merging attributes within an annotation hierarchy. When running in <em>merge mode</em>,
	 * the following special rules apply:
	 * <ol>
	 * <li>The supplied annotation will <em>not</em> be
	 * {@linkplain #synthesizeAnnotation synthesized} before retrieving its attributes;
	 * however, nested annotations and arrays of nested annotations <em>will</em> be
	 * synthesized.</li>
	 * <li>Default values will be replaced with {@link #DEFAULT_VALUE_PLACEHOLDER}.</li>
	 * <li>The resulting, merged annotation attributes should eventually be
	 * {@linkplain #postProcessAnnotationAttributes post-processed} in order to
	 * ensure that placeholders have been replaced by actual default values and
	 * in order to enforce {@code @AliasFor} semantics.</li>
	 * </ol>
	 *
	 * @param annotatedElement the element that is annotated with the supplied annotation;
	 * may be {@code null} if unknown
	 * @param annotation the annotation to retrieve the attributes for
	 * @param classValuesAsString whether to convert Class references into Strings (for
	 * compatibility with {@link org.springframework.core.type.AnnotationMetadata})
	 * or to preserve them as Class references
	 * @param nestedAnnotationsAsMap whether to convert nested annotations into
	 * {@link AnnotationAttributes} maps (for compatibility with
	 * {@link org.springframework.core.type.AnnotationMetadata}) or to preserve them as
	 * {@code Annotation} instances
	 * @param mergeMode whether the annotation attributes should be created
	 * using <em>merge mode</em>
	 * @return the annotation attributes (a specialized Map) with attribute names as keys
	 * and corresponding attribute values as values; never {@code null}
	 * @since 4.2
	 * @see #postProcessAnnotationAttributes
	 */
	static AnnotationAttributes getAnnotationAttributes(AnnotatedElement annotatedElement, Annotation annotation,
			boolean classValuesAsString, boolean nestedAnnotationsAsMap, boolean mergeMode) {

		if (!mergeMode) {
			annotation = synthesizeAnnotation(annotation, annotatedElement);
		}

		Class<? extends Annotation> annotationType = annotation.annotationType();
		AnnotationAttributes attrs = new AnnotationAttributes(annotationType);
		for (Method method : getAttributeMethods(annotationType)) {
			try {
				Object value = method.invoke(annotation);
				Object defaultValue = method.getDefaultValue();
				if (mergeMode && (defaultValue != null)) {
					if (ObjectUtils.nullSafeEquals(value, defaultValue)) {
						value = DEFAULT_VALUE_PLACEHOLDER;
					}
				}
				attrs.put(method.getName(),
					adaptValue(annotatedElement, value, classValuesAsString, nestedAnnotationsAsMap));
			}
			catch (Exception ex) {
				if (ex instanceof InvocationTargetException) {
					Throwable targetException = ((InvocationTargetException) ex).getTargetException();
					rethrowAnnotationConfigurationException(targetException);
				}
				throw new IllegalStateException("Could not obtain annotation attribute value for " + method, ex);
			}
		}
		return attrs;
	}

	/**
	 * Adapt the given value according to the given class and nested annotation settings.
	 * <p>Nested annotations will be
	 * {@linkplain #synthesizeAnnotation(Annotation, AnnotatedElement) synthesized}.
	 * @param annotatedElement the element that is annotated, used for contextual
	 * logging; may be {@code null} if unknown
	 * @param value the annotation attribute value
	 * @param classValuesAsString whether to convert Class references into Strings (for
	 * compatibility with {@link org.springframework.core.type.AnnotationMetadata})
	 * or to preserve them as Class references
	 * @param nestedAnnotationsAsMap whether to convert nested annotations into
	 * {@link AnnotationAttributes} maps (for compatibility with
	 * {@link org.springframework.core.type.AnnotationMetadata}) or to preserve them as
	 * {@code Annotation} instances
	 * @return the adapted value, or the original value if no adaptation is needed
	 */
	static Object adaptValue(AnnotatedElement annotatedElement, Object value, boolean classValuesAsString,
			boolean nestedAnnotationsAsMap) {

		if (classValuesAsString) {
			if (value instanceof Class) {
				return ((Class<?>) value).getName();
			}
			else if (value instanceof Class[]) {
				Class<?>[] clazzArray = (Class<?>[]) value;
				String[] classNames = new String[clazzArray.length];
				for (int i = 0; i < clazzArray.length; i++) {
					classNames[i] = clazzArray[i].getName();
				}
				return classNames;
			}
		}

		if (value instanceof Annotation) {
			Annotation annotation = (Annotation) value;

			if (nestedAnnotationsAsMap) {
				return getAnnotationAttributes(annotatedElement, annotation, classValuesAsString,
					nestedAnnotationsAsMap);
			}
			else {
				return synthesizeAnnotation(annotation, annotatedElement);
			}
		}

		if (value instanceof Annotation[]) {
			Annotation[] annotations = (Annotation[]) value;

			if (nestedAnnotationsAsMap) {
				AnnotationAttributes[] mappedAnnotations = new AnnotationAttributes[annotations.length];
				for (int i = 0; i < annotations.length; i++) {
					mappedAnnotations[i] = getAnnotationAttributes(annotatedElement, annotations[i],
						classValuesAsString, nestedAnnotationsAsMap);
				}
				return mappedAnnotations;
			}
			else {
				return synthesizeAnnotationArray(annotations, annotatedElement);
			}
		}

		// Fallback
		return value;
	}

	/**
	 * Retrieve the <em>value</em> of the {@code value} attribute of a
	 * single-element Annotation, given an annotation instance.
	 * @param annotation the annotation instance from which to retrieve the value
	 * @return the attribute value, or {@code null} if not found
	 * @see #getValue(Annotation, String)
	 */
	public static Object getValue(Annotation annotation) {
		return getValue(annotation, VALUE);
	}

	/**
	 * Retrieve the <em>value</em> of a named attribute, given an annotation instance.
	 * @param annotation the annotation instance from which to retrieve the value
	 * @param attributeName the name of the attribute value to retrieve
	 * @return the attribute value, or {@code null} if not found
	 * @see #getValue(Annotation)
	 */
	public static Object getValue(Annotation annotation, String attributeName) {
		if (annotation == null || !StringUtils.hasText(attributeName)) {
			return null;
		}
		try {
			Method method = annotation.annotationType().getDeclaredMethod(attributeName);
			ReflectionUtils.makeAccessible(method);
			return method.invoke(annotation);
		}
		catch (Exception ex) {
			return null;
		}
	}

	/**
	 * Retrieve the <em>default value</em> of the {@code value} attribute
	 * of a single-element Annotation, given an annotation instance.
	 * @param annotation the annotation instance from which to retrieve the default value
	 * @return the default value, or {@code null} if not found
	 * @see #getDefaultValue(Annotation, String)
	 */
	public static Object getDefaultValue(Annotation annotation) {
		return getDefaultValue(annotation, VALUE);
	}

	/**
	 * Retrieve the <em>default value</em> of a named attribute, given an annotation instance.
	 * @param annotation the annotation instance from which to retrieve the default value
	 * @param attributeName the name of the attribute value to retrieve
	 * @return the default value of the named attribute, or {@code null} if not found
	 * @see #getDefaultValue(Class, String)
	 */
	public static Object getDefaultValue(Annotation annotation, String attributeName) {
		if (annotation == null) {
			return null;
		}
		return getDefaultValue(annotation.annotationType(), attributeName);
	}

	/**
	 * Retrieve the <em>default value</em> of the {@code value} attribute
	 * of a single-element Annotation, given the {@link Class annotation type}.
	 * @param annotationType the <em>annotation type</em> for which the default value should be retrieved
	 * @return the default value, or {@code null} if not found
	 * @see #getDefaultValue(Class, String)
	 */
	public static Object getDefaultValue(Class<? extends Annotation> annotationType) {
		return getDefaultValue(annotationType, VALUE);
	}

	/**
	 * Retrieve the <em>default value</em> of a named attribute, given the
	 * {@link Class annotation type}.
	 * @param annotationType the <em>annotation type</em> for which the default value should be retrieved
	 * @param attributeName the name of the attribute value to retrieve.
	 * @return the default value of the named attribute, or {@code null} if not found
	 * @see #getDefaultValue(Annotation, String)
	 */
	public static Object getDefaultValue(Class<? extends Annotation> annotationType, String attributeName) {
		if (annotationType == null || !StringUtils.hasText(attributeName)) {
			return null;
		}
		try {
			return annotationType.getDeclaredMethod(attributeName).getDefaultValue();
		}
		catch (Exception ex) {
			return null;
		}
	}

	/**
	 * <em>Synthesize</em> an annotation from the supplied {@code annotation}
	 * by wrapping it in a dynamic proxy that transparently enforces
	 * <em>attribute alias</em> semantics for annotation attributes that are
	 * annotated with {@link AliasFor @AliasFor}.
	 *
	 * @param annotation the annotation to synthesize
	 * @return the synthesized annotation, if the supplied annotation is
	 * <em>synthesizable</em>; {@code null} if the supplied annotation is
	 * {@code null}; otherwise, the supplied annotation unmodified
	 * @throws AnnotationConfigurationException if invalid configuration of
	 * {@code @AliasFor} is detected
	 * @since 4.2
	 * @see #synthesizeAnnotation(Annotation, AnnotatedElement)
	 */
	static <A extends Annotation> A synthesizeAnnotation(A annotation) {
		return synthesizeAnnotation(annotation, null);
	}

	/**
	 * <em>Synthesize</em> an annotation from the supplied {@code annotation}
	 * by wrapping it in a dynamic proxy that transparently enforces
	 * <em>attribute alias</em> semantics for annotation attributes that are
	 * annotated with {@link AliasFor @AliasFor}.
	 *
	 * @param annotation the annotation to synthesize
	 * @param annotatedElement the element that is annotated with the supplied
	 * annotation; may be {@code null} if unknown
	 * @return the synthesized annotation if the supplied annotation is
	 * <em>synthesizable</em>; {@code null} if the supplied annotation is
	 * {@code null}; otherwise the supplied annotation unmodified
	 * @throws AnnotationConfigurationException if invalid configuration of
	 * {@code @AliasFor} is detected
	 * @since 4.2
	 * @see #synthesizeAnnotation(Map, Class, AnnotatedElement)
	 */
	@SuppressWarnings("unchecked")
	public static <A extends Annotation> A synthesizeAnnotation(A annotation, AnnotatedElement annotatedElement) {
		if (annotation == null) {
			return null;
		}
		if (annotation instanceof SynthesizedAnnotation) {
			return annotation;
		}

		Class<? extends Annotation> annotationType = annotation.annotationType();
		if (!isSynthesizable(annotationType)) {
			return annotation;
		}

		AnnotationAttributeExtractor attributeExtractor = new DefaultAnnotationAttributeExtractor(annotation,
			annotatedElement);
		InvocationHandler handler = new SynthesizedAnnotationInvocationHandler(attributeExtractor);
		A synthesizedAnnotation = (A) Proxy.newProxyInstance(ClassUtils.getDefaultClassLoader(), new Class<?>[] {
			(Class<A>) annotationType, SynthesizedAnnotation.class }, handler);

		return synthesizedAnnotation;
	}

	/**
	 * <em>Synthesize</em> an annotation from the supplied map of annotation
	 * attributes by wrapping the map in a dynamic proxy that implements an
	 * annotation of the specified {@code annotationType} and transparently
	 * enforces <em>attribute alias</em> semantics for annotation attributes
	 * that are annotated with {@link AliasFor @AliasFor}.
	 * <p>The supplied map must contain a key-value pair for every attribute
	 * defined in the supplied {@code annotationType} that is not aliased or
	 * does not have a default value.
	 * <p>Note that {@link AnnotationAttributes} is a specialized type of
	 * {@link Map} that is an ideal candidate for this method's
	 * {@code attributes} argument.
	 *
	 * @param attributes the map of annotation attributes to synthesize
	 * @param annotationType the type of annotation to synthesize; never {@code null}
	 * @param annotatedElement the element that is annotated with the annotation
	 * corresponding to the supplied attributes; may be {@code null} if unknown
	 * @return the synthesized annotation, or {@code null} if the supplied attributes
	 * map is {@code null}
	 * @throws IllegalArgumentException if a required attribute is missing or if an
	 * attribute is not of the correct type
	 * @throws AnnotationConfigurationException if invalid configuration of
	 * {@code @AliasFor} is detected
	 * @since 4.2
	 * @see #synthesizeAnnotation(Annotation, AnnotatedElement)
	 */
	@SuppressWarnings("unchecked")
	public static <A extends Annotation> A synthesizeAnnotation(Map<String, Object> attributes,
			Class<A> annotationType, AnnotatedElement annotatedElement) {
		Assert.notNull(annotationType, "annotationType must not be null");

		if (attributes == null) {
			return null;
		}

		AnnotationAttributeExtractor attributeExtractor = new MapAnnotationAttributeExtractor(attributes,
			annotationType, annotatedElement);
		InvocationHandler handler = new SynthesizedAnnotationInvocationHandler(attributeExtractor);
		A synthesizedAnnotation = (A) Proxy.newProxyInstance(ClassUtils.getDefaultClassLoader(), new Class<?>[] {
			annotationType, SynthesizedAnnotation.class }, handler);

		return synthesizedAnnotation;
	}

	/**
	 * <em>Synthesize</em> the supplied array of {@code annotations} by
	 * creating a new array of the same size and type and populating it
	 * with {@linkplain #synthesizeAnnotation(Annotation) synthesized}
	 * versions of the annotations from the input array.
	 *
	 * @param annotations the array of annotations to synthesize
	 * @param annotatedElement the element that is annotated with the supplied
	 * array of annotations; may be {@code null} if unknown
	 * @return a new array of synthesized annotations, or {@code null} if
	 * the supplied array is {@code null}
	 * @throws AnnotationConfigurationException if invalid configuration of
	 * {@code @AliasFor} is detected
	 * @since 4.2
	 * @see #synthesizeAnnotation(Annotation, AnnotatedElement)
	 * @see #synthesizeAnnotation(Map, Class, AnnotatedElement)
	 */
	public static Annotation[] synthesizeAnnotationArray(Annotation[] annotations, AnnotatedElement annotatedElement) {
		if (annotations == null) {
			return null;
		}

		Annotation[] synthesized = (Annotation[]) Array.newInstance(annotations.getClass().getComponentType(), annotations.length);
		for (int i = 0; i < annotations.length; i++) {
			synthesized[i] = synthesizeAnnotation(annotations[i], annotatedElement);
		}
		return synthesized;
	}

	/**
	 * Get a map of all attribute alias pairs, declared via {@code @AliasFor}
	 * in the supplied annotation type.
	 *
	 * <p>The map is keyed by attribute name with each value representing
	 * the name of the aliased attribute. For each entry {@code [x, y]} in
	 * the map there will be a corresponding {@code [y, x]} entry in the map.
	 *
	 * <p>An empty return value implies that the annotation does not declare
	 * any attribute aliases.
	 *
	 * @param annotationType the annotation type to find attribute aliases in
	 * @return a map containing attribute alias pairs; never {@code null}
	 * @since 4.2
	 */
	static Map<String, String> getAttributeAliasMap(Class<? extends Annotation> annotationType) {
		if (annotationType == null) {
			return Collections.emptyMap();
		}

		Map<String, String> map = attributeAliasesCache.get(annotationType);
		if (map != null) {
			return map;
		}

		map = new HashMap<String, String>();
		for (Method attribute : getAttributeMethods(annotationType)) {
			String attributeName = attribute.getName();
			String aliasedAttributeName = getAliasedAttributeName(attribute);
			if (aliasedAttributeName != null) {
				map.put(attributeName, aliasedAttributeName);
			}
		}

		attributeAliasesCache.put(annotationType, map);

		return map;
	}

	/**
	 * Determine if annotations of the supplied {@code annotationType} are
	 * <em>synthesizable</em> (i.e., in need of being wrapped in a dynamic
	 * proxy that provides functionality above that of a standard JDK
	 * annotation).
	 *
	 * <p>Specifically, an annotation is <em>synthesizable</em> if it declares
	 * any attributes that are configured as <em>aliased pairs</em> via
	 * {@link AliasFor @AliasFor} or if any nested annotations used by the
	 * annotation declare such <em>aliased pairs</em>.
	 *
	 * @since 4.2
	 * @see SynthesizedAnnotation
	 * @see SynthesizedAnnotationInvocationHandler
	 */
	@SuppressWarnings("unchecked")
	private static boolean isSynthesizable(Class<? extends Annotation> annotationType) {

		Boolean synthesizable = synthesizableCache.get(annotationType);
		if (synthesizable != null) {
			return synthesizable.booleanValue();
		}

		synthesizable = Boolean.FALSE;

		for (Method attribute : getAttributeMethods(annotationType)) {
			if (getAliasedAttributeName(attribute) != null) {
				synthesizable = Boolean.TRUE;
				break;
			}

			Class<?> returnType = attribute.getReturnType();

			if (Annotation[].class.isAssignableFrom(returnType)) {
				Class<? extends Annotation> nestedAnnotationType = (Class<? extends Annotation>) returnType.getComponentType();
				if (isSynthesizable(nestedAnnotationType)) {
					synthesizable = Boolean.TRUE;
					break;
				}
			}
			else if (Annotation.class.isAssignableFrom(returnType)) {
				Class<? extends Annotation> nestedAnnotationType = (Class<? extends Annotation>) returnType;
				if (isSynthesizable(nestedAnnotationType)) {
					synthesizable = Boolean.TRUE;
					break;
				}
			}
		}

		synthesizableCache.put(annotationType, synthesizable);

		return synthesizable.booleanValue();
	}

	/**
	 * Get the name of the aliased attribute configured via
	 * {@link AliasFor @AliasFor} on the supplied annotation {@code attribute}.
	 *
	 * <p>This method does not resolve aliases in other annotations. In
	 * other words, if {@code @AliasFor} is present on the supplied
	 * {@code attribute} but {@linkplain AliasFor#annotation references an
	 * annotation} other than {@link Annotation}, this method will return
	 * {@code null} immediately.
	 *
	 * @param attribute the attribute to find an alias for
	 * @return the name of the aliased attribute, or {@code null} if not found
	 * @throws IllegalArgumentException if the supplied attribute method is
	 * not from an annotation, or if the supplied target type is {@link Annotation}
	 * @throws AnnotationConfigurationException if invalid configuration of
	 * {@code @AliasFor} is detected
	 * @since 4.2
	 * @see #getAliasedAttributeName(Method, Class)
	 */
	static String getAliasedAttributeName(Method attribute) {
		return getAliasedAttributeName(attribute, null);
	}

	/**
	 * Get the name of the aliased attribute configured via
	 * {@link AliasFor @AliasFor} on the supplied annotation {@code attribute}.
	 *
	 * @param attribute the attribute to find an alias for
	 * @param targetAnnotationType the type of annotation in which the
	 * aliased attribute is allowed to be declared; {@code null} implies
	 * <em>within the same annotation</em>
	 * @return the name of the aliased attribute, or {@code null} if not found
	 * @throws IllegalArgumentException if the supplied attribute method is
	 * not from an annotation, or if the supplied target type is {@link Annotation}
	 * @throws AnnotationConfigurationException if invalid configuration of
	 * {@code @AliasFor} is detected
	 * @since 4.2
	 */
	@SuppressWarnings("unchecked")
	static String getAliasedAttributeName(Method attribute, Class<? extends Annotation> targetAnnotationType) {
		Class<?> declaringClass = attribute.getDeclaringClass();
		Assert.isTrue(declaringClass.isAnnotation(), "attribute method must be from an annotation");
		Assert.isTrue(!Annotation.class.equals(targetAnnotationType),
			"targetAnnotationType must not be java.lang.annotation.Annotation");

		AliasFor aliasFor = attribute.getAnnotation(AliasFor.class);

		// Nothing to check
		if (aliasFor == null) {
			return null;
		}

		Class<? extends Annotation> sourceAnnotationType = (Class<? extends Annotation>) declaringClass;
		Class<? extends Annotation> aliasedAnnotationType = aliasFor.annotation();

		boolean searchWithinSameAnnotation = (targetAnnotationType == null);
		boolean sameTargetDeclared = (sourceAnnotationType.equals(aliasedAnnotationType) || Annotation.class.equals(aliasedAnnotationType));

		// Wrong search scope?
		if (searchWithinSameAnnotation && !sameTargetDeclared) {
			return null;
		}

		String attributeName = attribute.getName();
		String aliasedAttributeName = aliasFor.attribute();

		if (!StringUtils.hasText(aliasedAttributeName)) {
			String msg = String.format(
				"@AliasFor declaration on attribute [%s] in annotation [%s] is missing required 'attribute' value.",
				attributeName, sourceAnnotationType.getName());
			throw new AnnotationConfigurationException(msg);
		}

		if (sameTargetDeclared) {
			aliasedAnnotationType = sourceAnnotationType;
		}

		Method aliasedAttribute = null;
		try {
			aliasedAttribute = aliasedAnnotationType.getDeclaredMethod(aliasedAttributeName);
		}
		catch (NoSuchMethodException e) {
			String msg = String.format(
				"Attribute [%s] in annotation [%s] is declared as an @AliasFor nonexistent attribute [%s] in annotation [%s].",
				attributeName, sourceAnnotationType.getName(), aliasedAttributeName, aliasedAnnotationType.getName());
			throw new AnnotationConfigurationException(msg, e);
		}

		if (sameTargetDeclared) {
			AliasFor mirrorAliasFor = aliasedAttribute.getAnnotation(AliasFor.class);
			if (mirrorAliasFor == null) {
				String msg = String.format("Attribute [%s] in annotation [%s] must be declared as an @AliasFor [%s].",
					aliasedAttributeName, sourceAnnotationType.getName(), attributeName);
				throw new AnnotationConfigurationException(msg);
			}

			String mirrorAliasedAttributeName = mirrorAliasFor.attribute();
			if (!attributeName.equals(mirrorAliasedAttributeName)) {
				String msg = String.format(
					"Attribute [%s] in annotation [%s] must be declared as an @AliasFor [%s], not [%s].",
					aliasedAttributeName, sourceAnnotationType.getName(), attributeName, mirrorAliasedAttributeName);
				throw new AnnotationConfigurationException(msg);
			}
		}

		Class<?> returnType = attribute.getReturnType();
		Class<?> aliasedReturnType = aliasedAttribute.getReturnType();
		if (!returnType.equals(aliasedReturnType)) {
			String msg = String.format("Misconfigured aliases: attribute [%s] in annotation [%s] "
					+ "and attribute [%s] in annotation [%s] must declare the same return type.", attributeName,
				sourceAnnotationType.getName(), aliasedAttributeName, aliasedAnnotationType.getName());
			throw new AnnotationConfigurationException(msg);
		}

		if (sameTargetDeclared) {
			Object defaultValue = attribute.getDefaultValue();
			Object aliasedDefaultValue = aliasedAttribute.getDefaultValue();

			if ((defaultValue == null) || (aliasedDefaultValue == null)) {
				String msg = String.format("Misconfigured aliases: attribute [%s] in annotation [%s] "
						+ "and attribute [%s] in annotation [%s] must declare default values.", attributeName,
					sourceAnnotationType.getName(), aliasedAttributeName, aliasedAnnotationType.getName());
				throw new AnnotationConfigurationException(msg);
			}

			if (!ObjectUtils.nullSafeEquals(defaultValue, aliasedDefaultValue)) {
				String msg = String.format("Misconfigured aliases: attribute [%s] in annotation [%s] "
						+ "and attribute [%s] in annotation [%s] must declare the same default value.", attributeName,
					sourceAnnotationType.getName(), aliasedAttributeName, aliasedAnnotationType.getName());
				throw new AnnotationConfigurationException(msg);
			}
		}

		return aliasedAttributeName;
	}

	/**
	 * Get all methods declared in the supplied {@code annotationType} that
	 * match Java's requirements for annotation <em>attributes</em>.
	 *
	 * <p>All methods in the returned list will be
	 * {@linkplain ReflectionUtils#makeAccessible(Method) made accessible}.
	 *
	 * @param annotationType the type in which to search for attribute methods;
	 * never {@code null}
	 * @return all annotation attribute methods in the specified annotation
	 * type; never {@code null}, though potentially <em>empty</em>
	 * @since 4.2
	 */
	static List<Method> getAttributeMethods(Class<? extends Annotation> annotationType) {

		List<Method> methods = attributeMethodsCache.get(annotationType);
		if (methods != null) {
			return methods;
		}

		methods = new ArrayList<Method>();
		for (Method method : annotationType.getDeclaredMethods()) {
			if (isAttributeMethod(method)) {
				ReflectionUtils.makeAccessible(method);
				methods.add(method);
			}
		}

		attributeMethodsCache.put(annotationType, methods);

		return methods;
	}

	/**
	 * Determine if the supplied {@code method} is an annotation attribute method.
	 * @param method the method to check
	 * @return {@code true} if the method is an attribute method
	 */
	static boolean isAttributeMethod(Method method) {
		return ((method != null) && (method.getParameterTypes().length == 0) && (method.getReturnType() != void.class));
	}

	/**
	 * Determine if the supplied method is an "annotationType" method.
	 * @return {@code true} if the method is an "annotationType" method
	 * @see Annotation#annotationType()
	 */
	static boolean isAnnotationTypeMethod(Method method) {
		return ((method != null) && method.getName().equals("annotationType") && (method.getParameterTypes().length == 0));
	}

	/**
	 * Post-process the supplied {@link AnnotationAttributes}.
	 *
	 * <p>Specifically, this method enforces <em>attribute alias</em> semantics
	 * for annotation attributes that are annotated with {@link AliasFor @AliasFor}
	 * and replaces {@linkplain #DEFAULT_VALUE_PLACEHOLDER placeholders} with their
	 * original default values.
	 *
	 * @param element the element that is annotated with an annotation or
	 * annotation hierarchy from which the supplied attributes were created;
	 * may be {@code null} if unknown
	 * @param attributes the annotation attributes to post-process
	 * @param classValuesAsString whether to convert Class references into Strings (for
	 * compatibility with {@link org.springframework.core.type.AnnotationMetadata})
	 * or to preserve them as Class references
	 * @param nestedAnnotationsAsMap whether to convert nested annotations into
	 * {@link AnnotationAttributes} maps (for compatibility with
	 * {@link org.springframework.core.type.AnnotationMetadata}) or to preserve them as
	 * {@code Annotation} instances
	 * @since 4.2
	 * @see #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean, boolean)
	 * @see #DEFAULT_VALUE_PLACEHOLDER
	 * @see #getDefaultValue(Class, String)
	 */
	static void postProcessAnnotationAttributes(AnnotatedElement element, AnnotationAttributes attributes,
			boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		// Abort?
		if (attributes == null) {
			return;
		}

		Class<? extends Annotation> annotationType = attributes.annotationType();

		// Validate @AliasFor configuration
		Map<String, String> aliasMap = getAttributeAliasMap(annotationType);
		Set<String> validated = new HashSet<String>();
		for (String attributeName : aliasMap.keySet()) {
			String aliasedAttributeName = aliasMap.get(attributeName);

			if (validated.add(attributeName) && validated.add(aliasedAttributeName)) {
				Object value = attributes.get(attributeName);
				Object aliasedValue = attributes.get(aliasedAttributeName);

				if (!ObjectUtils.nullSafeEquals(value, aliasedValue) && (value != DEFAULT_VALUE_PLACEHOLDER)
						&& (aliasedValue != DEFAULT_VALUE_PLACEHOLDER)) {
					String elementAsString = (element == null ? "unknown element" : element.toString());
					String msg = String.format(
						"In AnnotationAttributes for annotation [%s] declared on [%s], attribute [%s] and its alias [%s] are "
								+ "declared with values of [%s] and [%s], but only one declaration is permitted.",
						annotationType.getName(), elementAsString, attributeName, aliasedAttributeName,
						ObjectUtils.nullSafeToString(value), ObjectUtils.nullSafeToString(aliasedValue));
					throw new AnnotationConfigurationException(msg);
				}

				// Replace default values with aliased values...
				if (value == DEFAULT_VALUE_PLACEHOLDER) {
					attributes.put(attributeName,
						adaptValue(element, aliasedValue, classValuesAsString, nestedAnnotationsAsMap));
				}
				if (aliasedValue == DEFAULT_VALUE_PLACEHOLDER) {
					attributes.put(aliasedAttributeName,
						adaptValue(element, value, classValuesAsString, nestedAnnotationsAsMap));
				}
			}
		}

		for (String attributeName : attributes.keySet()) {
			Object value = attributes.get(attributeName);
			if (value == DEFAULT_VALUE_PLACEHOLDER) {
				attributes.put(attributeName,
					adaptValue(element, getDefaultValue(annotationType, attributeName), classValuesAsString,
						nestedAnnotationsAsMap));
			}
		}
	}

	/**
	 * <p>If the supplied throwable is an {@link AnnotationConfigurationException},
	 * it will be cast to an {@code AnnotationConfigurationException} and thrown,
	 * allowing it to propagate to the caller.
	 * <p>Otherwise, this method does nothing.
	 * @param t the throwable to inspect
	 * @since 4.2
	 */
	static void rethrowAnnotationConfigurationException(Throwable t) {
		if (t instanceof AnnotationConfigurationException) {
			throw (AnnotationConfigurationException) t;
		}
	}

	/**
	 * Handle the supplied annotation introspection exception.
	 * <p>If the supplied exception is an {@link AnnotationConfigurationException},
	 * it will simply be thrown, allowing it to propagate to the caller, and
	 * nothing will be logged.
	 * <p>Otherwise, this method logs an introspection failure (in particular
	 * {@code TypeNotPresentExceptions}) &mdash; before moving on, pretending
	 * there were no annotations on this specific element.
	 * @param element the element that we tried to introspect annotations on
	 * @param ex the exception that we encountered
	 * @see #rethrowAnnotationConfigurationException
	 */
	static void handleIntrospectionFailure(AnnotatedElement element, Exception ex) {

		rethrowAnnotationConfigurationException(ex);

		Log loggerToUse = logger;
		if (loggerToUse == null) {
			loggerToUse = LogFactory.getLog(AnnotationUtils.class);
			logger = loggerToUse;
		}
		if (element instanceof Class && Annotation.class.isAssignableFrom((Class<?>) element)) {
			// Meta-annotation lookup on an annotation type
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to introspect meta-annotations on [" + element + "]: " + ex);
			}
		}
		else {
			// Direct annotation lookup on regular Class, Method, Field
			if (loggerToUse.isInfoEnabled()) {
				logger.info("Failed to introspect annotations on [" + element + "]: " + ex);
			}
		}
	}


	/**
	 * Cache key for the AnnotatedElement cache.
	 */
	private static class AnnotationCacheKey {

		private final AnnotatedElement element;

		private final Class<? extends Annotation> annotationType;

		public AnnotationCacheKey(AnnotatedElement element, Class<? extends Annotation> annotationType) {
			this.element = element;
			this.annotationType = annotationType;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof AnnotationCacheKey)) {
				return false;
			}
			AnnotationCacheKey otherKey = (AnnotationCacheKey) other;
			return (this.element.equals(otherKey.element) &&
					ObjectUtils.nullSafeEquals(this.annotationType, otherKey.annotationType));
		}

		@Override
		public int hashCode() {
			return (this.element.hashCode() * 29 + this.annotationType.hashCode());
		}
	}


	private static class AnnotationCollector<A extends Annotation> {

		private final Class<? extends Annotation> containerAnnotationType;

		private final Class<A> annotationType;

		private final Set<AnnotatedElement> visited = new HashSet<AnnotatedElement>();

		private final Set<A> result = new LinkedHashSet<A>();

		public AnnotationCollector(Class<? extends Annotation> containerAnnotationType, Class<A> annotationType) {
			this.containerAnnotationType = containerAnnotationType;
			this.annotationType = annotationType;
		}

		public Set<A> getResult(AnnotatedElement element) {
			process(element);
			return Collections.unmodifiableSet(this.result);
		}

		@SuppressWarnings("unchecked")
		private void process(AnnotatedElement element) {
			if (this.visited.add(element)) {
				try {
					for (Annotation ann : element.getAnnotations()) {
						Class<? extends Annotation> currentAnnotationType = ann.annotationType();
						if (ObjectUtils.nullSafeEquals(this.annotationType, currentAnnotationType)) {
							this.result.add(synthesizeAnnotation((A) ann, element));
						}
						else if (ObjectUtils.nullSafeEquals(this.containerAnnotationType, currentAnnotationType)) {
							this.result.addAll(getValue(element, ann));
						}
						else if (!isInJavaLangAnnotationPackage(ann)) {
							process(currentAnnotationType);
						}
					}
				}
				catch (Exception ex) {
					handleIntrospectionFailure(element, ex);
				}
			}
		}

		@SuppressWarnings("unchecked")
		private List<A> getValue(AnnotatedElement element, Annotation annotation) {
			try {
				Method method = annotation.annotationType().getDeclaredMethod("value");
				ReflectionUtils.makeAccessible(method);
				A[] annotations = (A[]) method.invoke(annotation);

				List<A> synthesizedAnnotations = new ArrayList<A>();
				for (A anno : annotations) {
					synthesizedAnnotations.add(synthesizeAnnotation(anno, element));
				}
				return synthesizedAnnotations;
			}
			catch (Exception ex) {
				rethrowAnnotationConfigurationException(ex);
				// Unable to read value from repeating annotation container -> ignore it.
				return Collections.emptyList();
			}
		}
	}

}
