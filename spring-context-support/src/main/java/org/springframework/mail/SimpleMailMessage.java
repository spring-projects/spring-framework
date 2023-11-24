/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.mail;

import java.io.Serializable;
import java.util.Date;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Models a simple mail message, including data such as the from, to, cc, subject,
 * and text fields.
 *
 * <p>Consider {@code JavaMailSender} and JavaMail {@code MimeMessages} for creating
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
@SuppressWarnings("serial")
public class SimpleMailMessage implements MailMessage, Serializable {

	@Nullable
	private String from;

	@Nullable
	private String replyTo;

	@Nullable
	private String[] to;

	@Nullable
	private String[] cc;

	@Nullable
	private String[] bcc;

	@Nullable
	private Date sentDate;

	@Nullable
	private String subject;

	@Nullable
	private String text;


	/**
	 * Create a new {@code SimpleMailMessage}.
	 */
	public SimpleMailMessage() {
	}

	/**
	 * Copy constructor for creating a new {@code SimpleMailMessage} from the state
	 * of an existing {@code SimpleMailMessage} instance.
	 */
	public SimpleMailMessage(SimpleMailMessage original) {
		Assert.notNull(original, "'original' message argument must not be null");
		this.from = original.getFrom();
		this.replyTo = original.getReplyTo();
		this.to = copyOrNull(original.getTo());
		this.cc = copyOrNull(original.getCc());
		this.bcc = copyOrNull(original.getBcc());
		this.sentDate = original.getSentDate();
		this.subject = original.getSubject();
		this.text = original.getText();
	}


	@Override
	public void setFrom(@Nullable String from) {
		this.from = from;
	}

	@Nullable
	public String getFrom() {
		return this.from;
	}

	@Override
	public void setReplyTo(@Nullable String replyTo) {
		this.replyTo = replyTo;
	}

	@Nullable
	public String getReplyTo() {
		return this.replyTo;
	}

	@Override
	public void setTo(@Nullable String to) {
		this.to = new String[] {to};
	}

	@Override
	public void setTo(String... to) {
		this.to = to;
	}

	@Nullable
	public String[] getTo() {
		return this.to;
	}

	@Override
	public void setCc(@Nullable String cc) {
		this.cc = new String[] {cc};
	}

	@Override
	public void setCc(@Nullable String... cc) {
		this.cc = cc;
	}

	@Nullable
	public String[] getCc() {
		return this.cc;
	}

	@Override
	public void setBcc(@Nullable String bcc) {
		this.bcc = new String[] {bcc};
	}

	@Override
	public void setBcc(@Nullable String... bcc) {
		this.bcc = bcc;
	}

	@Nullable
	public String[] getBcc() {
		return this.bcc;
	}

	@Override
	public void setSentDate(@Nullable Date sentDate) {
		this.sentDate = sentDate;
	}

	@Nullable
	public Date getSentDate() {
		return this.sentDate;
	}

	@Override
	public void setSubject(@Nullable String subject) {
		this.subject = subject;
	}

	@Nullable
	public String getSubject() {
		return this.subject;
	}

	@Override
	public void setText(@Nullable String text) {
		this.text = text;
	}

	@Nullable
	public String getText() {
		return this.text;
	}


	/**
	 * Copy the contents of this message to the given target message.
	 * @param target the {@code MailMessage} to copy to
	 */
	public void copyTo(MailMessage target) {
		Assert.notNull(target, "'target' MailMessage must not be null");
		if (getFrom() != null) {
			target.setFrom(getFrom());
		}
		if (getReplyTo() != null) {
			target.setReplyTo(getReplyTo());
		}
		if (getTo() != null) {
			target.setTo(copy(getTo()));
		}
		if (getCc() != null) {
			target.setCc(copy(getCc()));
		}
		if (getBcc() != null) {
			target.setBcc(copy(getBcc()));
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
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof SimpleMailMessage that &&
				ObjectUtils.nullSafeEquals(this.from, that.from) &&
				ObjectUtils.nullSafeEquals(this.replyTo, that.replyTo) &&
				ObjectUtils.nullSafeEquals(this.to, that.to) &&
				ObjectUtils.nullSafeEquals(this.cc, that.cc) &&
				ObjectUtils.nullSafeEquals(this.bcc, that.bcc) &&
				ObjectUtils.nullSafeEquals(this.sentDate, that.sentDate) &&
				ObjectUtils.nullSafeEquals(this.subject, that.subject) &&
				ObjectUtils.nullSafeEquals(this.text, that.text)));
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHash(this.from, this.replyTo, this.to, this.cc,
				this.bcc, this.sentDate, this.subject);
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


	@Nullable
	private static String[] copyOrNull(@Nullable String[] state) {
		if (state == null) {
			return null;
		}
		return copy(state);
	}

	private static String[] copy(String[] state) {
		return state.clone();
	}

}
