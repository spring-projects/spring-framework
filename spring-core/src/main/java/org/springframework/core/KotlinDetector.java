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

package org.springframework.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * A common delegate for detecting Kotlin's presence and for identifying Kotlin types.
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 5.0
 */
@SuppressWarnings("unchecked")
public abstract class KotlinDetector {

	@Nullable
	private static final Class<? extends Annotation> kotlinMetadata;

	@Nullable
	private static final Class<? extends Annotation> kotlinJvmInline;

	// For ConstantFieldFeature compliance, otherwise could be deduced from kotlinMetadata
	private static final boolean kotlinPresent;

	private static final boolean kotlinReflectPresent;

	static {
		ClassLoader classLoader = KotlinDetector.class.getClassLoader();
		Class<?> metadata = null;
		Class<?> jvmInline = null;
		try {
			metadata = ClassUtils.forName("kotlin.Metadata", classLoader);
			try {
				jvmInline = ClassUtils.forName("kotlin.jvm.JvmInline", classLoader);
			}
			catch (ClassNotFoundException ex) {
				// JVM inline support not available
			}
		}
		catch (ClassNotFoundException ex) {
			// Kotlin API not available - no Kotlin support
		}
		kotlinMetadata = (Class<? extends Annotation>) metadata;
		kotlinPresent = (kotlinMetadata != null);
		kotlinReflectPresent = kotlinPresent && ClassUtils.isPresent("kotlin.reflect.full.KClasses", classLoader);
		kotlinJvmInline = (Class<? extends Annotation>) jvmInline;
	}


	/**
	 * Determine whether Kotlin is present in general.
	 */
	public static boolean isKotlinPresent() {
		return kotlinPresent;
	}

	/**
	 * Determine whether Kotlin reflection is present.
	 * @since 5.1
	 */
	public static boolean isKotlinReflectPresent() {
		return kotlinReflectPresent;
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
		return (kotlinMetadata != null && clazz.getDeclaredAnnotation(kotlinMetadata) != null);
	}

	/**
	 * Return {@code true} if the method is a suspending function.
	 * @since 5.3
	 */
	public static boolean isSuspendingFunction(Method method) {
		if (KotlinDetector.isKotlinType(method.getDeclaringClass())) {
			Class<?>[] types = method.getParameterTypes();
			if (types.length > 0 && "kotlin.coroutines.Continuation".equals(types[types.length - 1].getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine whether the given {@code Class} is an inline class
	 * (annotated with {@code @JvmInline}).
	 * @since 6.1.5
	 * @see <a href="https://kotlinlang.org/docs/inline-classes.html">Kotlin inline value classes</a>
	 */
	public static boolean isInlineClass(Class<?> clazz) {
		return (kotlinJvmInline != null && clazz.getDeclaredAnnotation(kotlinJvmInline) != null);
	}

}
