/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.client;

/**
 * Contract to format the API version for a request.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 * @see DefaultApiVersionInserter.Builder#withVersionFormatter(ApiVersionFormatter)
 */
@FunctionalInterface
public interface ApiVersionFormatter {

	/**
	 * Format the given version Object into a String value.
	 * @param version the version to format
	 * @return the final String version to use
	 */
	String formatVersion(Object version);

}
