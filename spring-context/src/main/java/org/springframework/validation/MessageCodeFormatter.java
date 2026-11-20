/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.validation;

import org.jspecify.annotations.Nullable;

/**
 * A strategy interface for formatting message codes.
 *
 * @author Chris Beams
 * @since 3.2
 * @see DefaultMessageCodesResolver
 * @see DefaultMessageCodesResolver.Format
 */
@FunctionalInterface
public interface MessageCodeFormatter {

	/**
	 * Build and return a message code consisting of the given fields,
	 * usually delimited by {@link DefaultMessageCodesResolver#CODE_SEPARATOR}.
	 * @param errorCode for example: "typeMismatch"
	 * @param objectName for example: "user"
	 * @param field for example, "age"
	 * @return concatenated message code, for example: "typeMismatch.user.age"
	 * @see DefaultMessageCodesResolver.Format
	 */
	String format(String errorCode, @Nullable String objectName, @Nullable String field);

}
