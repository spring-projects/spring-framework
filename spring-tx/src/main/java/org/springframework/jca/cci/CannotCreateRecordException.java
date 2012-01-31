/*
 * Copyright 2002-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jca.cci;

import javax.resource.ResourceException;

import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Exception thrown when the creating of a CCI Record failed
 * for connector-internal reasons.
 *
 * @author Juergen Hoeller
 * @since 1.2
 */
public class CannotCreateRecordException extends DataAccessResourceFailureException {

	/**
	 * Constructor for CannotCreateRecordException.
	 * @param msg message
	 * @param ex ResourceException root cause
	 */
	public CannotCreateRecordException(String msg, ResourceException ex) {
		super(msg, ex);
	}

}
