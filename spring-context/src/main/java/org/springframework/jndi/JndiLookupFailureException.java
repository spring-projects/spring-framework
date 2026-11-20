/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jndi;

import javax.naming.NamingException;

import org.springframework.core.NestedRuntimeException;

/**
 * RuntimeException to be thrown in case of JNDI lookup failures,
 * in particular from code that does not declare JNDI's checked
 * {@link javax.naming.NamingException}: for example, from Spring's
 * {@link JndiObjectTargetSource}.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 */
@SuppressWarnings("serial")
public class JndiLookupFailureException extends NestedRuntimeException {

	/**
	 * Construct a new JndiLookupFailureException,
	 * wrapping the given JNDI NamingException.
	 * @param msg the detail message
	 * @param cause the NamingException root cause
	 */
	public JndiLookupFailureException(String msg, NamingException cause) {
		super(msg, cause);
	}

}
