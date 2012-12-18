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

package org.springframework.mail;

/**
 * This interface defines a strategy for sending simple mails. Can be
 * implemented for a variety of mailing systems due to the simple requirements.
 * For richer functionality like MIME messages, consider JavaMailSender.
 *
 * <p>Allows for easy testing of clients, as it does not depend on JavaMail's
 * infrastructure classes: no mocking of JavaMail Session or Transport necessary.
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @since 10.09.2003
 * @see org.springframework.mail.javamail.JavaMailSender
 */
public interface MailSender {

	/**
	 * Send the given simple mail message.
	 * @param simpleMessage the message to send
	 * @throws org.springframework.mail.MailParseException
	 * in case of failure when parsing the message
	 * @throws org.springframework.mail.MailAuthenticationException
	 * in case of authentication failure
	 * @throws org.springframework.mail.MailSendException
	 * in case of failure when sending the message
	 */
	void send(SimpleMailMessage simpleMessage) throws MailException;

	/**
	 * Send the given array of simple mail messages in batch.
	 * @param simpleMessages the messages to send
	 * @throws org.springframework.mail.MailParseException
	 * in case of failure when parsing a message
	 * @throws org.springframework.mail.MailAuthenticationException
	 * in case of authentication failure
	 * @throws org.springframework.mail.MailSendException
	 * in case of failure when sending a message
	 */
	void send(SimpleMailMessage[] simpleMessages) throws MailException;

}
