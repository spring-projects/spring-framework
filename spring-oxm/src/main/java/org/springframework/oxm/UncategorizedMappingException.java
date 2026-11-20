/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.oxm;

/**
 * Exception that indicates that the cause cannot be distinguished further.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
@SuppressWarnings("serial")
public class UncategorizedMappingException extends XmlMappingException {

	/**
	 * Construct an {@code UncategorizedMappingException} with the specified detail message
	 * and nested exception.
	 * @param msg the detail message
	 * @param cause the nested exception
	 */
	public UncategorizedMappingException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
