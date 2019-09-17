/*
 * Copyright 2002-2019 the original author or authors.
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

import java.io.InputStream;

import javax.mail.internet.MimeMessage;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;


/**
 * A decorator pattern facility for a {@link JavaMailSender}, where the sending
 * operation can be customized (e.g. implement a whitelist, feature-toggle, etc)
 * by overriding {@link JavaMailSenderDecorator#send(MimeMessage...)}.
 * Any deriving class <em>may</em> also override the methods to
 * {@link #createMimeMessage() create messages}, if applicable, but this is not
 * a requirement.
 *
 * <h2>Implementation note</h2>
 * When overriding {@link #send(MimeMessage...)}, the decorated
 * {@code JavaMailSender} can be accessed as {@code super.}{@link #decoratedMailSender}.
 *
 * @author Rune Flobakk
 */
public abstract class JavaMailSenderDecorator implements JavaMailSender {

	/**
	 * The decorated {@link JavaMailSender} which may (or not) be delegated to
	 * by {@link #send(MimeMessage...)}.
	 */
	protected final JavaMailSender decoratedMailSender;

	public JavaMailSenderDecorator(JavaMailSender decoratedMailSender) {
		this.decoratedMailSender = decoratedMailSender;
	}

	@Override
	public MimeMessage createMimeMessage() {
		return this.decoratedMailSender.createMimeMessage();
	}

	@Override
	public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
		return this.decoratedMailSender.createMimeMessage(contentStream);
	}

	@Override
	public final void send(SimpleMailMessage simpleMessage) throws MailException {
		JavaMailSender.super.send(simpleMessage);
	}

	@Override
	public final void send(SimpleMailMessage... simpleMessages) throws MailException {
		JavaMailSender.super.send(simpleMessages);
	}

	@Override
	public final void send(MimeMessage mimeMessage) throws MailException {
		JavaMailSender.super.send(mimeMessage);
	}

	@Override
	public final void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
		JavaMailSender.super.send(mimeMessagePreparator);
	}

	@Override
	public final void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
		JavaMailSender.super.send(mimeMessagePreparators);
	}

}
