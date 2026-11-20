/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.oxm;

import org.springframework.core.NestedRuntimeException;

/**
 * Root of the hierarchy of Object XML Mapping exceptions.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
@SuppressWarnings("serial")
public abstract class XmlMappingException extends NestedRuntimeException {

	/**
	 * Construct an {@code XmlMappingException} with the specified detail message.
	 * @param msg the detail message
	 */
	public XmlMappingException(String msg) {
		super(msg);
	}

	/**
	 * Construct an {@code XmlMappingException} with the specified detail message
	 * and nested exception.
	 * @param msg the detail message
	 * @param cause the nested exception
	 */
	public XmlMappingException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
