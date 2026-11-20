/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ClassUtils;

/**
 * A common delegate for detecting Kotlin's presence and for identifying Kotlin types. All the methods of this class
 * can be safely used without any preliminary classpath checks.
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 5.0
 */
@SuppressWarnings("unchecked")
public abstract class KotlinDetector {

	private static final @Nullable Class<? extends Annotation> KOTLIN_METADATA;

	private static final @Nullable Class<? extends Annotation> KOTLIN_JVM_INLINE;

	private static final @Nullable Class<? extends Annotation> KOTLIN_SERIALIZABLE;

	private static final @Nullable Class<?> KOTLIN_COROUTINE_CONTINUATION;

	// For ConstantFieldFeature compliance, otherwise could be deduced from kotlinMetadata
	private static final boolean KOTLIN_PRESENT;

	private static final boolean KOTLIN_REFLECT_PRESENT;

	static {
		ClassLoader classLoader = KotlinDetector.class.getClassLoader();
		Class<?> metadata = null;
		Class<?> jvmInline = null;
		Class<?> serializable = null;
		Class<?> coroutineContinuation = null;
		try {
			metadata = ClassUtils.forName("kotlin.Metadata", classLoader);
			try {
				jvmInline = ClassUtils.forName("kotlin.jvm.JvmInline", classLoader);
			}
			catch (ClassNotFoundException ex) {
				// JVM inline support not available
			}
			try {
				serializable = ClassUtils.forName("kotlinx.serialization.Serializable", classLoader);
			}
			catch (ClassNotFoundException ex) {
				// Kotlin Serialization not available
			}
			try {
				coroutineContinuation = ClassUtils.forName("kotlin.coroutines.Continuation", classLoader);
			}
			catch (ClassNotFoundException ex) {
				// Coroutines support not available
			}
		}
		catch (ClassNotFoundException ex) {
			// Kotlin API not available - no Kotlin support
		}
		KOTLIN_METADATA = (Class<? extends Annotation>) metadata;
		KOTLIN_PRESENT = (KOTLIN_METADATA != null);
		KOTLIN_REFLECT_PRESENT = ClassUtils.isPresent("kotlin.reflect.full.KClasses", classLoader);
		KOTLIN_JVM_INLINE = (Class<? extends Annotation>) jvmInline;
		KOTLIN_SERIALIZABLE = (Class<? extends Annotation>) serializable;
		KOTLIN_COROUTINE_CONTINUATION = coroutineContinuation;
	}


	/**
	 * Determine whether Kotlin is present in general.
	 */
	public static boolean isKotlinPresent() {
		return KOTLIN_PRESENT;
	}

	/**
	 * Determine whether Kotlin reflection is present.
	 * @since 5.1
	 */
	public static boolean isKotlinReflectPresent() {
		return KOTLIN_REFLECT_PRESENT;
	}

	/**
	 * Determine whether the given {@code Class} is a Kotlin type
	 * (with Kotlin metadata present on it).
	 *
	 * <p>As of Kotlin 2.0, this method can't be used to detect Kotlin
	 * lambdas unless they are annotated with <code>@JvmSerializableLambda</code>
	 * as invokedynamic has become the default method for lambda generation.
	 */
	public static boolean isKotlinType(Class<?> clazz) {
		return (KOTLIN_PRESENT && clazz.getDeclaredAnnotation(KOTLIN_METADATA) != null);
	}

	/**
	 * Return {@code true} if the method is a suspending function.
	 * @since 5.3
	 */
	public static boolean isSuspendingFunction(Method method) {
		if (KOTLIN_COROUTINE_CONTINUATION == null) {
			return false;
		}
		int parameterCount = method.getParameterCount();
		return (parameterCount > 0 && method.getParameterTypes()[parameterCount - 1] == KOTLIN_COROUTINE_CONTINUATION);
	}

	/**
	 * Determine whether the given {@code Class} is an inline class
	 * (annotated with {@code @JvmInline}).
	 * @since 6.1.5
	 * @see <a href="https://kotlinlang.org/docs/inline-classes.html">Kotlin inline value classes</a>
	 */
	public static boolean isInlineClass(Class<?> clazz) {
		return (KOTLIN_JVM_INLINE != null && clazz.getDeclaredAnnotation(KOTLIN_JVM_INLINE) != null);
	}

	/**
	 * Determine whether the given {@code ResolvableType} is annotated with {@code @kotlinx.serialization.Serializable}
	 * at type or generics level.
	 * @since 7.0
	 */
	public static boolean hasSerializableAnnotation(ResolvableType type) {
		Class<?> resolvedClass = type.resolve();
		if (KOTLIN_SERIALIZABLE == null || resolvedClass == null) {
			return false;
		}
		if (resolvedClass.isAnnotationPresent(KOTLIN_SERIALIZABLE)) {
			return true;
		}
		for (ResolvableType genericType : type.getGenerics()) {
			if (hasSerializableAnnotation(genericType)) {
				return true;
			}
		}
		return false;
	}

}
