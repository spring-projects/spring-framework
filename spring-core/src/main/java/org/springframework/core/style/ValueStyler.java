/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.style;

import org.jspecify.annotations.Nullable;

/**
 * Strategy that encapsulates value String styling algorithms
 * according to Spring conventions.
 *
 * @author Keith Donald
 * @since 1.2.2
 */
public interface ValueStyler {

	/**
	 * Style the given value, returning a String representation.
	 * @param value the Object value to style
	 * @return the styled String
	 */
	String style(@Nullable Object value);

}
