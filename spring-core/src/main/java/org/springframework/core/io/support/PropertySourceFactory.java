/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.io.support;

import java.io.IOException;

import org.jspecify.annotations.Nullable;

import org.springframework.core.env.PropertySource;

/**
 * Strategy interface for creating resource-based {@link PropertySource} wrappers.
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @see DefaultPropertySourceFactory
 * @see ResourcePropertySource
 */
public interface PropertySourceFactory {

	/**
	 * Create a {@link PropertySource} that wraps the given resource.
	 * <p>Implementations will typically create {@link ResourcePropertySource}
	 * instances, with {@link PropertySourceProcessor} automatically adapting
	 * property source names via {@link ResourcePropertySource#withResourceName()}
	 * if necessary, for example, when combining multiple sources for the same name
	 * into a {@link org.springframework.core.env.CompositePropertySource}.
	 * Custom implementations with custom {@link PropertySource} types need
	 * to make sure to expose distinct enough names, possibly deriving from
	 * {@link ResourcePropertySource} where possible.
	 * @param name the name of the property source
	 * (can be {@code null} in which case the factory implementation
	 * will have to generate a name based on the given resource)
	 * @param resource the resource (potentially encoded) to wrap
	 * @return the new {@link PropertySource} (never {@code null})
	 * @throws IOException if resource resolution failed
	 */
	PropertySource<?> createPropertySource(@Nullable String name, EncodedResource resource) throws IOException;

}
