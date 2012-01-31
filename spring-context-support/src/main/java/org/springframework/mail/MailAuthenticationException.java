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

/**
 * Exception thrown on failed authentication.
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 */
public class MailAuthenticationException extends MailException {

	/**
	 * Constructor for MailAuthenticationException.
	 * @param msg message
	 */
	public MailAuthenticationException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for MailAuthenticationException.
	 * @param msg the detail message
	 * @param cause the root cause from the mail API in use
	 */
	public MailAuthenticationException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * Constructor for MailAuthenticationException.
	 * @param cause the root cause from the mail API in use
	 */
	public MailAuthenticationException(Throwable cause) {
		super("Authentication failed", cause);
	}

}
