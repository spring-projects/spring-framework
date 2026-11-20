/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.json;

/**
 * Modes that can be used to compare JSON.
 *
 * @author Phillip Webb
 * @since 6.2
 */
public enum JsonCompareMode {

	/**
	 * Strict checking.
	 */
	STRICT,

	/**
	 * Lenient checking.
	 */
	LENIENT

}
