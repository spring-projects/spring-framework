/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core.namedparam;

import org.jspecify.annotations.Nullable;

/**
 * A simple empty implementation of the {@link SqlParameterSource} interface.
 *
 * @author Juergen Hoeller
 * @since 3.2.2
 */
public class EmptySqlParameterSource implements SqlParameterSource {

	/**
	 * A shared instance of {@link EmptySqlParameterSource}.
	 */
	public static final EmptySqlParameterSource INSTANCE = new EmptySqlParameterSource();


	@Override
	public boolean hasValue(String paramName) {
		return false;
	}

	@Override
	public @Nullable Object getValue(String paramName) throws IllegalArgumentException {
		throw new IllegalArgumentException("This SqlParameterSource is empty");
	}

}
