/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.support;

import org.springframework.dao.DataAccessException;

/**
 * @author Thomas Risberg
 */
@SuppressWarnings("serial")
public class CustomErrorCodeException extends DataAccessException {

	public CustomErrorCodeException(String msg) {
		super(msg);
	}

	public CustomErrorCodeException(String msg, Throwable ex) {
		super(msg, ex);
	}

}
