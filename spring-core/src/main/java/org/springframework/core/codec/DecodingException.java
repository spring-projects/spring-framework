/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.codec;

import org.jspecify.annotations.Nullable;

/**
 * Indicates an issue with decoding the input stream with a focus on content
 * related issues such as a parse failure. As opposed to more general I/O
 * errors, illegal state, or a {@link CodecException} such as a configuration
 * issue that a {@link Decoder} may choose to raise.
 *
 * <p>For example in server web application, a {@code DecodingException} would
 * translate to a response with a 400 (bad input) status while
 * {@code CodecException} would translate to 500 (server error) status.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see Decoder
 */
@SuppressWarnings("serial")
public class DecodingException extends CodecException {

	/**
	 * Create a new DecodingException.
	 * @param msg the detail message
	 */
	public DecodingException(@Nullable String msg) {
		super(msg);
	}

	/**
	 * Create a new DecodingException.
	 * @param msg the detail message
	 * @param cause root cause for the exception, if any
	 */
	public DecodingException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
