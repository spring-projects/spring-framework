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
 * Contract for registering {@link RuntimeHints} based on the {@link ClassLoader}
 * of the deployment unit. Implementations should, if possible, use the specified
 * {@link ClassLoader} to determine if hints have to be contributed.
 *
 * <p>Implementations of this interface can be registered dynamically by using
 * {@link org.springframework.context.annotation.ImportRuntimeHints @ImportRuntimeHints}
 * or statically in {@code META-INF/spring/aot.factories} by using the FQN of this
 * interface as the key. A standard no-arg constructor is required for implementations.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 6.0
 */
@FunctionalInterface
public interface RuntimeHintsRegistrar {

	/**
	 * Contribute hints to the given {@link RuntimeHints} instance.
	 * @param hints the hints contributed so far for the deployment unit
	 * @param classLoader the classloader, or {@code null} if even the system ClassLoader isn't accessible
	 */
	void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader);

}
