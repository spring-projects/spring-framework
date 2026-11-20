/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.interceptor;

import org.springframework.core.NestedRuntimeException;

/**
 * An example {@link RuntimeException} for use in testing rollback rules.
 *
 * @author Chris Beams
 */
@SuppressWarnings("serial")
class MyRuntimeException extends NestedRuntimeException {

	public MyRuntimeException() {
		super("");
	}

	public MyRuntimeException(String msg) {
		super(msg);
	}

}
