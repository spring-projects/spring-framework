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
import org.springframework.core.annotation.MergedAnnotation;
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
			hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS);

	/**
	 * Register the necessary hints so that the specified annotation is visible
	 * at runtime.
	 * @param hints the {@link RuntimeHints} instance ot use
	 * @param annotation the annotation
	 * @see SynthesizedAnnotation
	 */
	public static void registerAnnotation(RuntimeHints hints, MergedAnnotation<?> annotation) {
		hints.reflection().registerType(annotation.getType(), ANNOTATION_HINT);
		MergedAnnotation<?> parentSource = annotation.getMetaSource();
		while (parentSource != null) {
			hints.reflection().registerType(parentSource.getType(), ANNOTATION_HINT);
			parentSource = parentSource.getMetaSource();
		}
		if (annotation.synthesize() instanceof SynthesizedAnnotation) {
			hints.proxies().registerJdkProxy(annotation.getType(), SynthesizedAnnotation.class);
		}
	}

}
