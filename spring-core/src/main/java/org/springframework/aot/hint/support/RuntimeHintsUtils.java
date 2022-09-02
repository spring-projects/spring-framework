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

import java.util.function.Consumer;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeHint.Builder;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.MergedAnnotation;

/**
 * Utility methods for runtime hints support code.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.0
 */
public abstract class RuntimeHintsUtils {

	/**
	 * A {@link TypeHint} customizer suitable for an annotation. Make sure
	 * that its attributes are visible.
	 * @deprecated as annotation attributes are visible without additional hints
	 */
	@Deprecated
	public static final Consumer<Builder> ANNOTATION_HINT = hint ->
			hint.withMembers(MemberCategory.INVOKE_DECLARED_METHODS);

	/**
	 * Register the necessary hints so that the specified annotation is visible
	 * at runtime.
	 * @param hints the {@link RuntimeHints} instance to use
	 * @param annotationType the annotation type
	 * @deprecated For removal prior to Spring Framework 6.0
	 */
	@Deprecated
	public static void registerAnnotation(RuntimeHints hints, Class<?> annotationType) {
		registerSynthesizedAnnotation(hints, annotationType);
	}

	/**
	 * Register the necessary hints so that the specified annotation can be
	 * synthesized at runtime if necessary. Such hints are usually required
	 * if any of the following apply:
	 * <ul>
	 * <li>Use {@link AliasFor} for local aliases</li>
	 * <li>Has a meta-annotation that uses {@link AliasFor} for attribute overrides</li>
	 * <li>Has nested annotations or arrays of annotations that are synthesizable</li>
	 * </ul>
	 * Consider using {@link #registerAnnotationIfNecessary(RuntimeHints, MergedAnnotation)}
	 * that determines if the hints are required.
	 * @param hints the {@link RuntimeHints} instance to use
	 * @param annotationType the annotation type
	 * @deprecated For removal prior to Spring Framework 6.0
	 */
	@Deprecated
	@SuppressWarnings("deprecation")
	public static void registerSynthesizedAnnotation(RuntimeHints hints, Class<?> annotationType) {
		hints.proxies().registerJdkProxy(annotationType,
				org.springframework.core.annotation.SynthesizedAnnotation.class);
	}

	/**
	 * Determine if the specified annotation can be synthesized at runtime, and
	 * register the necessary hints accordingly.
	 * @param hints the {@link RuntimeHints} instance to use
	 * @param annotation the annotation
	 * @see #registerSynthesizedAnnotation(RuntimeHints, Class)
	 * @deprecated For removal prior to Spring Framework 6.0
	 */
	@Deprecated
	public static void registerAnnotationIfNecessary(RuntimeHints hints, MergedAnnotation<?> annotation) {
		if (annotation.isSynthesizable()) {
			registerSynthesizedAnnotation(hints, annotation.getType());
		}
	}

}
