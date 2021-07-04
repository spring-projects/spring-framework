/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.mail.javamail;

import java.util.Date;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.mail.MailMessage;
import org.springframework.mail.MailParseException;

/**
 * Implementation of the MailMessage interface for a JavaMail MIME message,
 * to let message population code interact with a simple message or a MIME
 * message through a common interface.
 *
 * <p>Uses a MimeMessageHelper underneath. Can either be created with a
 * MimeMessageHelper instance or with a JavaMail MimeMessage instance.
 *
 * @author Juergen Hoeller
 * @since 1.1.5
 * @see MimeMessageHelper
 * @see javax.mail.internet.MimeMessage
 */
public class MimeMailMessage implements MailMessage {

	private final MimeMessageHelper helper;


	/**
	 * Create a new MimeMailMessage based on the given MimeMessageHelper.
	 * @param mimeMessageHelper the MimeMessageHelper
	 */
	public MimeMailMessage(MimeMessageHelper mimeMessageHelper) {
		this.helper = mimeMessageHelper;
	}

	/**
	 * Create a new MimeMailMessage based on the given JavaMail MimeMessage.
	 * @param mimeMessage the JavaMail MimeMessage
	 */
	public MimeMailMessage(MimeMessage mimeMessage) {
		this.helper = new MimeMessageHelper(mimeMessage);
	}

	/**
	 * Return the MimeMessageHelper that this MimeMailMessage is based on.
	 */
	public final MimeMessageHelper getMimeMessageHelper() {
		return this.helper;
	}

	/**
	 * Return the JavaMail MimeMessage that this MimeMailMessage is based on.
	 */
	public final MimeMessage getMimeMessage() {
		return this.helper.getMimeMessage();
	}


	@Override
	public void setFrom(String from) throws MailParseException {
		try {
			this.helper.setFrom(from);
		}
		catch (MessagingException ex) {
			throw new MailParseException(ex);
		}
	}

	@Override
	public void setReplyTo(String replyTo) throws MailParseException {
		try {
			this.helper.setReplyTo(replyTo);
		}
		catch (MessagingException ex) {
			throw new MailParseException(ex);
		}
	}

	@Override
	public void setTo(String to) throws MailParseException {
		try {
			this.helper.setTo(to);
		}
		catch (MessagingException ex) {
			throw new MailParseException(ex);
		}
	}

	@Override
	public void setTo(String... to) throws MailParseException {
		try {
			this.helper.setTo(to);
		}
		catch (MessagingException ex) {
			throw new MailParseException(ex);
		}
	}

	@Override
	public void setCc(String cc) throws MailParseException {
		try {
			this.helper.setCc(cc);
		}
		catch (MessagingException ex) {
			throw new MailParseException(ex);
		}
	}

	@Override
	public void setCc(String... cc) throws MailParseException {
		try {
			this.helper.setCc(cc);
		}
		catch (MessagingException ex) {
			throw new MailParseException(ex);
		}
	}

	@Override
	public void setBcc(String bcc) throws MailParseException {
		try {
			this.helper.setBcc(bcc);
		}
		catch (MessagingException ex) {
			throw new MailParseException(ex);
		}
	}

	@Override
	public void setBcc(String... bcc) throws MailParseException {
		try {
			this.helper.setBcc(bcc);
		}
		catch (MessagingException ex) {
			throw new MailParseException(ex);
		}
	}

	@Override
	public void setSentDate(Date sentDate) throws MailParseException {
		try {
			this.helper.setSentDate(sentDate);
		}
		catch (MessagingException ex) {
			throw new MailParseException(ex);
		}
	}

	@Override
	public void setSubject(String subject) throws MailParseException {
		try {
			this.helper.setSubject(subject);
		}
		catch (MessagingException ex) {
			throw new MailParseException(ex);
		}
	}

	@Override
	public void setText(String text) throws MailParseException {
		try {
			this.helper.setText(text);
		}
		catch (MessagingException ex) {
			throw new MailParseException(ex);
		}
	}

}
