/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.support;

import org.jspecify.annotations.Nullable;

/**
 * Strategy for providing named properties &mdash; for example, for looking up
 * key-value pairs in a generic fashion.
 *
 * <p>Primarily intended for use within the framework.
 *
 * @author Sam Brannen
 * @since 5.3
 */
@FunctionalInterface
public interface PropertyProvider {

	/**
	 * Get the value of the named property.
	 * @param name the name of the property to retrieve
	 * @return the value of the property or {@code null} if not found
	 */
	@Nullable String get(String name);

}
