/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.codec;

import org.jspecify.annotations.Nullable;

import org.springframework.core.NestedRuntimeException;

/**
 * General error that indicates a problem while encoding and decoding to and
 * from an Object stream.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@SuppressWarnings("serial")
public class CodecException extends NestedRuntimeException {

	/**
	 * Create a new CodecException.
	 * @param msg the detail message
	 */
	public CodecException(@Nullable String msg) {
		super(msg);
	}

	/**
	 * Create a new CodecException.
	 * @param msg the detail message
	 * @param cause root cause for the exception, if any
	 */
	public CodecException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
