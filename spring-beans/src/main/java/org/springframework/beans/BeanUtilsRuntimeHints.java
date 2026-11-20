/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.io.ResourceEditor;

/**
 * {@link RuntimeHintsRegistrar} to register hints for popular conventions in
 * {@link BeanUtils#findEditorByConvention(Class)}.
 *
 * @author Sebastien Deleuze
 * @since 6.0.10
 */
class BeanUtilsRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		ReflectionHints reflectionHints = hints.reflection();
		reflectionHints.registerType(ResourceEditor.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		reflectionHints.registerTypeIfPresent(classLoader, "org.springframework.http.MediaTypeEditor",
				MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
	}

}
