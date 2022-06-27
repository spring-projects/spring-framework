/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aot.hint.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeHint.Builder;
import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.SynthesizedAnnotation;

/**
 * Utility methods for runtime hints support code.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public abstract class RuntimeHintsUtils {

	/**
	 * A {@link TypeHint} customizer suitable for an annotation. Make sure
	 * that its attributes are visible.
	 */
	public static final Consumer<Builder> ANNOTATION_HINT = hint ->
			hint.withMembers(MemberCategory.INVOKE_DECLARED_METHODS);

	/**
	 * Register the necessary hints so that the specified annotation is visible
	 * at runtime.
	 * <p>If an annotation attribute aliases an attribute of another annotation,
	 * the other annotation is registered as well and a JDK proxy hint is defined
	 * so that the synthesized annotation can be resolved.
	 * @param hints the {@link RuntimeHints} instance to use
	 * @param annotationType the annotation type
	 * @see SynthesizedAnnotation
	 */
	public static void registerAnnotation(RuntimeHints hints, Class<?> annotationType) {
		hints.reflection().registerType(annotationType, ANNOTATION_HINT);
		Set<Class<?>> allAnnotations = new LinkedHashSet<>();
		collectAliasedAnnotations(new HashSet<>(), allAnnotations, annotationType);
		allAnnotations.forEach(annotation -> hints.reflection().registerType(annotation, ANNOTATION_HINT));
		if (!allAnnotations.isEmpty()) {
			hints.proxies().registerJdkProxy(annotationType, SynthesizedAnnotation.class);
		}
	}

	private static void collectAliasedAnnotations(Set<Class<?>> seen, Set<Class<?>> types, Class<?> annotationType) {
		if (seen.contains(annotationType) || Reflective.class.equals(annotationType)) {
			return;
		}
		seen.add(annotationType);
		for (Method method : annotationType.getDeclaredMethods()) {
			AliasFor aliasFor = method.getAnnotation(AliasFor.class);
			if (aliasFor != null) {
				Class<?> annotationAttribute = aliasFor.annotation();
				Class<?> targetAnnotation = (annotationAttribute != Annotation.class
						? annotationAttribute : annotationType);
				if (!types.contains(targetAnnotation)) {
					types.add(targetAnnotation);
					if (!targetAnnotation.equals(annotationType)) {
						collectAliasedAnnotations(seen, types, targetAnnotation);
					}
				}
			}
		}
	}

}
