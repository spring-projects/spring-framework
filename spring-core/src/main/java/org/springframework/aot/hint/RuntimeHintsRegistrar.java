/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint;

import org.jspecify.annotations.Nullable;

/**
 * Contract for registering {@link RuntimeHints} based on the {@link ClassLoader}
 * of the deployment unit. Implementations should, if possible, use the specified
 * {@link ClassLoader} to determine if hints have to be contributed.
 *
 * <p>Implementations of this interface can be registered dynamically by using
 * {@link org.springframework.context.annotation.ImportRuntimeHints @ImportRuntimeHints}
 * or statically in {@code META-INF/spring/aot.factories} by using the fully-qualified
 * class name of this interface as the key. A standard no-arg constructor is required
 * for implementations.
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
	 * @param classLoader the ClassLoader to use, or {@code null} for the default
	 */
	void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader);

}
