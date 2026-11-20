/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.accept;

/**
 * Contract to parse a version into an Object representation.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 * @param <V> the parsed object type
 */
@FunctionalInterface
public interface ApiVersionParser<V extends Comparable<V>> {

	/**
	 * Parse the version into an Object.
	 * @param version the value to parse
	 * @return an Object that represents the version
	 */
	V parseVersion(String version);

}
