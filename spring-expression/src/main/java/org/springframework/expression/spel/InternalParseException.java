/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel;

import org.jspecify.annotations.Nullable;

/**
 * Wraps a real parse exception. This exception flows to the top parse method and then
 * the wrapped exception is thrown as the real problem.
 *
 * @author Andy Clement
 * @since 3.0
 */
@SuppressWarnings("serial")
public class InternalParseException extends RuntimeException {

	public InternalParseException(@Nullable SpelParseException cause) {
		super(cause);
	}

	@Override
	public @Nullable SpelParseException getCause() {
		return (SpelParseException) super.getCause();
	}

}
