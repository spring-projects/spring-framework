/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.codec;

import org.jspecify.annotations.Nullable;

/**
 * Indicates an issue with encoding the input Object stream with a focus on
 * not being able to encode Objects. As opposed to a more general I/O errors
 * or a {@link CodecException} such as a configuration issue that an
 * {@link Encoder} may also choose to raise.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see Encoder
 */
@SuppressWarnings("serial")
public class EncodingException extends CodecException {

	/**
	 * Create a new EncodingException.
	 * @param msg the detail message
	 */
	public EncodingException(String msg) {
		super(msg);
	}

	/**
	 * Create a new EncodingException.
	 * @param msg the detail message
	 * @param cause root cause for the exception, if any
	 */
	public EncodingException(String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
