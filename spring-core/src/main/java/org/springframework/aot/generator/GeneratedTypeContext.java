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

package org.springframework.aot.generator;

import org.springframework.aot.hint.RuntimeHints;

/**
 * Context passed to object that can generate code, giving access to a main
 * {@link GeneratedType} as well as to a {@link GeneratedType} in a given
 * package if privileged access is required.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public interface GeneratedTypeContext {

	/**
	 * Return the {@link RuntimeHints} instance to use to contribute hints for
	 * generated types.
	 * @return the runtime hints
	 */
	RuntimeHints runtimeHints();

	/**
	 * Return a {@link GeneratedType} for the specified package. If it does not
	 * exist, it is created.
	 * @param packageName the package name to use
	 * @return a generated type
	 */
	GeneratedType getGeneratedType(String packageName);

	/**
	 * Return the main {@link GeneratedType}.
	 * @return the generated type for the target package
	 */
	GeneratedType getMainGeneratedType();

}
