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

package org.springframework.mail.javamail;

import javax.mail.internet.MimeMessage;

/**
 * Callback interface for the preparation of JavaMail MIME messages.
 *
 * <p>The corresponding <code>send</code> methods of {@link JavaMailSender}
 * will take care of the actual creation of a {@link MimeMessage} instance,
 * and of proper exception conversion.
 *
 * <p>It is often convenient to use a {@link MimeMessageHelper} for populating
 * the passed-in MimeMessage, in particular when working with attachments or
 * special character encodings.
 * See {@link MimeMessageHelper MimeMessageHelper's javadoc} for an example.
 *
 * @author Juergen Hoeller
 * @since 07.10.2003
 * @see JavaMailSender#send(MimeMessagePreparator)
 * @see JavaMailSender#send(MimeMessagePreparator[])
 * @see MimeMessageHelper
 */
public interface MimeMessagePreparator {

	/**
	 * Prepare the given new MimeMessage instance.
	 * @param mimeMessage the message to prepare
	 * @throws javax.mail.MessagingException passing any exceptions thrown by MimeMessage
	 * methods through for automatic conversion to the MailException hierarchy
	 * @throws java.io.IOException passing any exceptions thrown by MimeMessage methods
	 * through for automatic conversion to the MailException hierarchy
	 * @throws Exception if mail preparation failed, for example when a
	 * Velocity template cannot be rendered for the mail text
	 */
	void prepare(MimeMessage mimeMessage) throws Exception;

}
