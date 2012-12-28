/*
 * Copyright 2002-2012 the original author or authors.
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
 * Exception to be thrown by user code if a mail cannot be prepared properly,
 * for example when a Velocity template cannot be rendered for the mail text.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.ui.velocity.VelocityEngineUtils#mergeTemplateIntoString
 * @see org.springframework.ui.freemarker.FreeMarkerTemplateUtils#processTemplateIntoString
 */
@SuppressWarnings("serial")
public class MailPreparationException extends MailException {

	/**
	 * Constructor for MailPreparationException.
	 * @param msg the detail message
	 */
	public MailPreparationException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for MailPreparationException.
	 * @param msg the detail message
	 * @param cause the root cause from the mail API in use
	 */
	public MailPreparationException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public MailPreparationException(Throwable cause) {
		super("Could not prepare mail", cause);
	}

}
