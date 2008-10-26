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

	public void setFrom(String from) throws MailParseException;

	public void setReplyTo(String replyTo) throws MailParseException;

	public void setTo(String to) throws MailParseException;

	public void setTo(String[] to) throws MailParseException;

	public void setCc(String cc) throws MailParseException;

	public void setCc(String[] cc) throws MailParseException;

	public void setBcc(String bcc) throws MailParseException;

	public void setBcc(String[] bcc) throws MailParseException;

	public void setSentDate(Date sentDate) throws MailParseException;

	public void setSubject(String subject) throws MailParseException;

	public void setText(String text) throws MailParseException;

}
