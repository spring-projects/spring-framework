/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc;

import java.io.IOException;

import org.springframework.dao.DataRetrievalFailureException;

/**
 * Exception to be thrown when a LOB could not be retrieved.
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 */
@SuppressWarnings("serial")
public class LobRetrievalFailureException extends DataRetrievalFailureException {

	/**
	 * Constructor for LobRetrievalFailureException.
	 * @param msg the detail message
	 */
	public LobRetrievalFailureException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for LobRetrievalFailureException.
	 * @param msg the detail message
	 * @param ex the root cause IOException
	 */
	public LobRetrievalFailureException(String msg, IOException ex) {
		super(msg, ex);
	}

}
