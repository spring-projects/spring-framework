/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.beans;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.io.ResourceEditor;
import org.springframework.lang.Nullable;

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
