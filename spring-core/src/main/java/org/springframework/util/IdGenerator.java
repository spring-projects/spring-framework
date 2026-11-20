/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util;

import java.util.UUID;

/**
 * Contract for generating universally unique identifiers ({@link UUID UUIDs}).
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@FunctionalInterface
public interface IdGenerator {

	/**
	 * Generate a new identifier.
	 * @return the generated identifier
	 */
	UUID generateId();

}
