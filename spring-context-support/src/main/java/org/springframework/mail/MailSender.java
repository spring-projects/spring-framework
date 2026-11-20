/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	 * @throws MailParseException in case of failure when parsing the message
	 * @throws MailAuthenticationException in case of authentication failure
	 * @throws MailSendException in case of failure when sending the message
	 */
	default void send(SimpleMailMessage simpleMessage) throws MailException {
		send(new SimpleMailMessage[] {simpleMessage});
	}

	/**
	 * Send the given array of simple mail messages in batch.
	 * @param simpleMessages the messages to send
	 * @throws MailParseException in case of failure when parsing a message
	 * @throws MailAuthenticationException in case of authentication failure
	 * @throws MailSendException in case of failure when sending a message
	 */
	void send(SimpleMailMessage... simpleMessages) throws MailException;

}
