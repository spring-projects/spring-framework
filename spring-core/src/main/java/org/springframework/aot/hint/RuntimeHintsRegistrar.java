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

package org.springframework.aot.hint;

import org.springframework.lang.Nullable;

/**
 * Contract for registering {@link RuntimeHints} in a static fashion.
 * <p>Implementations will contribute hints without any knowledge of the application context
 * and can only use the given {@link ClassLoader} to conditionally contribute hints.
 * <p>{@code RuntimeHintsRegistrar} can be declared as {@code spring/aot.factories} entries;
 * the registrar will be processed as soon as its declaration is found in the classpath.
 * A standard no-arg constructor is required for implementations.
 *
 * @author Brian Clozel
 * @since 6.0
 */
@FunctionalInterface
public interface RuntimeHintsRegistrar {

	/**
	 * Contribute hints to the given {@link RuntimeHints} instance.
	 * @param hints the hints contributed so far for the application
	 * @param classLoader the classloader, or {@code null} if even the system ClassLoader isn't accessible
	 */
	void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader);

}
