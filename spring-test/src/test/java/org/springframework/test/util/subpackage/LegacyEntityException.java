/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.util.subpackage;

/**
 * Exception thrown by a {@link LegacyEntity}.
 *
 * @author Sam Brannen
 * @since 4.3.1
 */
@SuppressWarnings("serial")
public class LegacyEntityException extends RuntimeException {

	public LegacyEntityException(String message) {
		super(message);
	}

}
