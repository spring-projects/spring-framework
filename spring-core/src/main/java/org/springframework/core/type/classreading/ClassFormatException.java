/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.type.classreading;

import java.io.IOException;

import org.springframework.core.io.Resource;

/**
 * Exception that indicates an incompatible class format encountered
 * in a class file during metadata reading.
 *
 * @author Juergen Hoeller
 * @since 6.1.2
 * @see MetadataReaderFactory#getMetadataReader(Resource)
 * @see ClassFormatError
 */
@SuppressWarnings("serial")
public class ClassFormatException extends IOException {

	/**
	 * Construct a new {@code ClassFormatException} with the
	 * supplied message.
	 * @param message the detail message
	 */
	public ClassFormatException(String message) {
		super(message);
	}

	/**
	 * Construct a new {@code ClassFormatException} with the
	 * supplied message and cause.
	 * @param message the detail message
	 * @param cause the root cause
	 */
	public ClassFormatException(String message, Throwable cause) {
		super(message, cause);
	}

}
