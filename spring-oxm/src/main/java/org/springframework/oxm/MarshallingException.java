/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.oxm;

/**
 * Base class for exception thrown when a marshalling or unmarshalling error occurs.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @see MarshallingFailureException
 * @see UnmarshallingFailureException
 */
@SuppressWarnings("serial")
public abstract class MarshallingException extends XmlMappingException {

	/**
	 * Construct a {@code MarshallingException} with the specified detail message.
	 * @param msg the detail message
	 */
	protected MarshallingException(String msg) {
		super(msg);
	}

	/**
	 * Construct a {@code MarshallingException} with the specified detail message
	 * and nested exception.
	 * @param msg the detail message
	 * @param cause the nested exception
	 */
	protected MarshallingException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
