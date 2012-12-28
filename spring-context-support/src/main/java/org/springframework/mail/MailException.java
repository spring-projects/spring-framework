/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.mail;

import org.springframework.core.NestedRuntimeException;

/**
 * Base class for all mail exceptions.
 *
 * @author Dmitriy Kopylenko
 */
@SuppressWarnings("serial")
public abstract class MailException extends NestedRuntimeException {

	/**
	 * Constructor for MailException.
	 * @param msg the detail message
	 */
	public MailException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for MailException.
	 * @param msg the detail message
	 * @param cause the root cause from the mail API in use
	 */
	public MailException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
