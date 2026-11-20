/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.io.support;

import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Descriptor for a {@link org.springframework.core.env.PropertySource PropertySource}.
 *
 * @author Stephane Nicoll
 * @since 6.0
 * @param locations the locations to consider
 * @param ignoreResourceNotFound whether a failure to find a property resource
 * should be ignored
 * @param name the name of the property source, or {@code null} to infer one
 * @param propertySourceFactory the type of {@link PropertySourceFactory} to use,
 * or {@code null} to use the default
 * @param encoding the encoding, or {@code null} to use the default encoding
 * @see org.springframework.core.env.PropertySource
 * @see org.springframework.context.annotation.PropertySource
 */
public record PropertySourceDescriptor(List<String> locations, boolean ignoreResourceNotFound,
		@Nullable String name, @Nullable Class<? extends PropertySourceFactory> propertySourceFactory,
		@Nullable String encoding) {

	/**
	 * Create a descriptor with the specified locations.
	 * @param locations the locations to consider
	 */
	public PropertySourceDescriptor(String... locations) {
		this(Arrays.asList(locations), false, null, null, null);
	}

}
