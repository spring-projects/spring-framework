/*
 * Copyright 2002-2008 the original author or authors.
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

import java.io.Serializable;
import java.util.Date;

import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.Assert;

/**
 * Models a simple mail message, including data such as the from, to, cc, subject, and text fields.
 *
 * <p>Consider <code>JavaMailSender</code> and JavaMail <code>MimeMessages</code> for creating
 * more sophisticated messages, for example messages with attachments, special
 * character encodings, or personal names that accompany mail addresses.
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @since 10.09.2003
 * @see MailSender
 * @see org.springframework.mail.javamail.JavaMailSender
 * @see org.springframework.mail.javamail.MimeMessagePreparator
 * @see org.springframework.mail.javamail.MimeMessageHelper
 * @see org.springframework.mail.javamail.MimeMailMessage
 */
public class SimpleMailMessage implements MailMessage, Serializable {

	private String from;

	private String replyTo;

	private String[] to;

	private String[] cc;

	private String[] bcc;

	private Date sentDate;

	private String subject;

	private String text;


	/**
	 * Create a new <code>SimpleMailMessage</code>.
	 */
	public SimpleMailMessage() {
	}

	/**
	 * Copy constructor for creating a new <code>SimpleMailMessage</code> from the state
	 * of an existing <code>SimpleMailMessage</code> instance.
	 * @throws IllegalArgumentException if the supplied message is <code>null</code>
	 */
	public SimpleMailMessage(SimpleMailMessage original) {
		Assert.notNull(original, "The 'original' message argument cannot be null");
		this.from = original.getFrom();
		this.replyTo = original.getReplyTo();
		if (original.getTo() != null) {
			this.to = copy(original.getTo());
		}
		if (original.getCc() != null) {
			this.cc = copy(original.getCc());
		}
		if (original.getBcc() != null) {
			this.bcc = copy(original.getBcc());
		}
		this.sentDate = original.getSentDate();
		this.subject = original.getSubject();
		this.text = original.getText();
	}


	public void setFrom(String from) {
		this.from = from;
	}

	public String getFrom() {
		return this.from;
	}

	public void setReplyTo(String replyTo) {
		this.replyTo = replyTo;
	}

	public String getReplyTo() {
		return replyTo;
	}

	public void setTo(String to) {
		this.to = new String[] {to};
	}

	public void setTo(String[] to) {
		this.to = to;
	}

	public String[] getTo() {
		return this.to;
	}

	public void setCc(String cc) {
		this.cc = new String[] {cc};
	}

	public void setCc(String[] cc) {
		this.cc = cc;
	}

	public String[] getCc() {
		return cc;
	}

	public void setBcc(String bcc) {
		this.bcc = new String[] {bcc};
	}

	public void setBcc(String[] bcc) {
		this.bcc = bcc;
	}

	public String[] getBcc() {
		return bcc;
	}

	public void setSentDate(Date sentDate) {
		this.sentDate = sentDate;
	}

	public Date getSentDate() {
		return sentDate;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getSubject() {
		return this.subject;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getText() {
		return this.text;
	}


	/**
	 * Copy the contents of this message to the given target message.
	 * @param target the <code>MailMessage</code> to copy to
	 * @throws IllegalArgumentException if the supplied <code>target</code> is <code>null</code>
	 */
	public void copyTo(MailMessage target) {
		Assert.notNull(target, "The 'target' message argument cannot be null");
		if (getFrom() != null) {
			target.setFrom(getFrom());
		}
		if (getReplyTo() != null) {
			target.setReplyTo(getReplyTo());
		}
		if (getTo() != null) {
			target.setTo(getTo());
		}
		if (getCc() != null) {
			target.setCc(getCc());
		}
		if (getBcc() != null) {
			target.setBcc(getBcc());
		}
		if (getSentDate() != null) {
			target.setSentDate(getSentDate());
		}
		if (getSubject() != null) {
			target.setSubject(getSubject());
		}
		if (getText() != null) {
			target.setText(getText());
		}
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("SimpleMailMessage: ");
		sb.append("from=").append(this.from).append("; ");
		sb.append("replyTo=").append(this.replyTo).append("; ");
		sb.append("to=").append(StringUtils.arrayToCommaDelimitedString(this.to)).append("; ");
		sb.append("cc=").append(StringUtils.arrayToCommaDelimitedString(this.cc)).append("; ");
		sb.append("bcc=").append(StringUtils.arrayToCommaDelimitedString(this.bcc)).append("; ");
		sb.append("sentDate=").append(this.sentDate).append("; ");
		sb.append("subject=").append(this.subject).append("; ");
		sb.append("text=").append(this.text);
		return sb.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof SimpleMailMessage)) {
			return false;
		}
		SimpleMailMessage otherMessage = (SimpleMailMessage) other;
		return (ObjectUtils.nullSafeEquals(this.from, otherMessage.from) &&
				ObjectUtils.nullSafeEquals(this.replyTo, otherMessage.replyTo) &&
				java.util.Arrays.equals(this.to, otherMessage.to) &&
				java.util.Arrays.equals(this.cc, otherMessage.cc) &&
				java.util.Arrays.equals(this.bcc, otherMessage.bcc) &&
				ObjectUtils.nullSafeEquals(this.sentDate, otherMessage.sentDate) &&
				ObjectUtils.nullSafeEquals(this.subject, otherMessage.subject) &&
				ObjectUtils.nullSafeEquals(this.text, otherMessage.text));
	}

	@Override
	public int hashCode() {
		int hashCode = (this.from == null ? 0 : this.from.hashCode());
		hashCode = 29 * hashCode + (this.replyTo == null ? 0 : this.replyTo.hashCode());
		for (int i = 0; this.to != null && i < this.to.length; i++) {
			hashCode = 29 * hashCode + (this.to == null ? 0 : this.to[i].hashCode());
		}
		for (int i = 0; this.cc != null && i < this.cc.length; i++) {
			hashCode = 29 * hashCode + (this.cc == null ? 0 : this.cc[i].hashCode());
		}
		for (int i = 0; this.bcc != null && i < this.bcc.length; i++) {
			hashCode = 29 * hashCode + (this.bcc == null ? 0 : this.bcc[i].hashCode());
		}
		hashCode = 29 * hashCode + (this.sentDate == null ? 0 : this.sentDate.hashCode());
		hashCode = 29 * hashCode + (this.subject == null ? 0 : this.subject.hashCode());
		hashCode = 29 * hashCode + (this.text == null ? 0 : this.text.hashCode());
		return hashCode;
	}


	private static String[] copy(String[] state) {
		String[] copy = new String[state.length];
		System.arraycopy(state, 0, copy, 0, state.length);
		return copy;
	}

}
