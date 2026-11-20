/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.generate;

/**
 * Thrown when a {@link ValueCodeGenerator} could not generate the code for a
 * given value.
 *
 * @author Stephane Nicoll
 * @since 6.1.2
 */
@SuppressWarnings("serial")
public class UnsupportedTypeValueCodeGenerationException extends ValueCodeGenerationException {

	public UnsupportedTypeValueCodeGenerationException(Object value) {
		super("Code generation does not support " + value.getClass().getName(), value, null);
	}

}
