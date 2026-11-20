/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.mail;

import java.util.Date;

/**
 * This is a common interface for mail messages, allowing a user to set key
 * values required in assembling a mail message, without needing to know if
 * the underlying message is a simple text message or a more sophisticated
 * MIME message.
 *
 * <p>Implemented by both SimpleMailMessage and MimeMessageHelper,
 * to let message population code interact with a simple message or a
 * MIME message through a common interface.
 *
 * @author Juergen Hoeller
 * @since 1.1.5
 * @see SimpleMailMessage
 * @see org.springframework.mail.javamail.MimeMessageHelper
 */
public interface MailMessage {

	void setFrom(String from) throws MailParseException;

	void setReplyTo(String replyTo) throws MailParseException;

	void setTo(String to) throws MailParseException;

	void setTo(String... to) throws MailParseException;

	void setCc(String cc) throws MailParseException;

	void setCc(String... cc) throws MailParseException;

	void setBcc(String bcc) throws MailParseException;

	void setBcc(String... bcc) throws MailParseException;

	void setSentDate(Date sentDate) throws MailParseException;

	void setSubject(String subject) throws MailParseException;

	void setText(String text) throws MailParseException;

}
