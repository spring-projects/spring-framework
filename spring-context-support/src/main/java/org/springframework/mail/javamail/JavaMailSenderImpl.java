/*
 * Copyright 2002-2011 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.activation.FileTypeMap;
import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.util.Assert;

/**
 * Production implementation of the {@link JavaMailSender} interface,
 * supporting both JavaMail {@link MimeMessage MimeMessages} and Spring
 * {@link SimpleMailMessage SimpleMailMessages}. Can also be used as a
 * plain {@link org.springframework.mail.MailSender} implementation.
 *
 * <p>Allows for defining all settings locally as bean properties.
 * Alternatively, a pre-configured JavaMail {@link javax.mail.Session} can be
 * specified, possibly pulled from an application server's JNDI environment.
 *
 * <p>Non-default properties in this object will always override the settings
 * in the JavaMail {@code Session}. Note that if overriding all values locally,
 * there is no added value in setting a pre-configured {@code Session}.
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @since 10.09.2003
 * @see javax.mail.internet.MimeMessage
 * @see javax.mail.Session
 * @see #setSession
 * @see #setJavaMailProperties
 * @see #setHost
 * @see #setPort
 * @see #setUsername
 * @see #setPassword
 */
public class JavaMailSenderImpl implements JavaMailSender {

	/** The default protocol: 'smtp' */
	public static final String DEFAULT_PROTOCOL = "smtp";

	/** The default port: -1 */
	public static final int DEFAULT_PORT = -1;

	private static final String HEADER_MESSAGE_ID = "Message-ID";


	private Properties javaMailProperties = new Properties();

	private Session session;

	private String protocol;

	private String host;

	private int port = DEFAULT_PORT;

	private String username;

	private String password;

	private String defaultEncoding;

	private FileTypeMap defaultFileTypeMap;


	/**
	 * Create a new instance of the {@code JavaMailSenderImpl} class.
	 * <p>Initializes the {@link #setDefaultFileTypeMap "defaultFileTypeMap"}
	 * property with a default {@link ConfigurableMimeFileTypeMap}.
	 */
	public JavaMailSenderImpl() {
		ConfigurableMimeFileTypeMap fileTypeMap = new ConfigurableMimeFileTypeMap();
		fileTypeMap.afterPropertiesSet();
		this.defaultFileTypeMap = fileTypeMap;
	}


	/**
	 * Set JavaMail properties for the {@code Session}.
	 * <p>A new {@code Session} will be created with those properties.
	 * Use either this method or {@link #setSession}, but not both.
	 * <p>Non-default properties in this instance will override given
	 * JavaMail properties.
	 */
	public void setJavaMailProperties(Properties javaMailProperties) {
		this.javaMailProperties = javaMailProperties;
		synchronized (this) {
			this.session = null;
		}
	}

	/**
	 * Allow Map access to the JavaMail properties of this sender,
	 * with the option to add or override specific entries.
	 * <p>Useful for specifying entries directly, for example via
	 * "javaMailProperties[mail.smtp.auth]".
	 */
	public Properties getJavaMailProperties() {
		return this.javaMailProperties;
	}

	/**
	 * Set the JavaMail {@code Session}, possibly pulled from JNDI.
	 * <p>Default is a new {@code Session} without defaults, that is
	 * completely configured via this instance's properties.
	 * <p>If using a pre-configured {@code Session}, non-default properties
	 * in this instance will override the settings in the {@code Session}.
	 * @see #setJavaMailProperties
	 */
	public synchronized void setSession(Session session) {
		Assert.notNull(session, "Session must not be null");
		this.session = session;
	}

	/**
	 * Return the JavaMail {@code Session},
	 * lazily initializing it if hasn't been specified explicitly.
	 */
	public synchronized Session getSession() {
		if (this.session == null) {
			this.session = Session.getInstance(this.javaMailProperties);
		}
		return this.session;
	}

	/**
	 * Set the mail protocol. Default is "smtp".
	 */
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	/**
	 * Return the mail protocol.
	 */
	public String getProtocol() {
		return this.protocol;
	}

	/**
	 * Set the mail server host, typically an SMTP host.
	 * <p>Default is the default host of the underlying JavaMail Session.
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Return the mail server host.
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * Set the mail server port.
	 * <p>Default is {@link #DEFAULT_PORT}, letting JavaMail use the default
	 * SMTP port (25).
	*/
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Return the mail server port.
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * Set the username for the account at the mail host, if any.
	 * <p>Note that the underlying JavaMail {@code Session} has to be
	 * configured with the property {@code "mail.smtp.auth"} set to
	 * {@code true}, else the specified username will not be sent to the
	 * mail server by the JavaMail runtime. If you are not explicitly passing
	 * in a {@code Session} to use, simply specify this setting via
	 * {@link #setJavaMailProperties}.
	 * @see #setSession
	 * @see #setPassword
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Return the username for the account at the mail host.
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * Set the password for the account at the mail host, if any.
	 * <p>Note that the underlying JavaMail {@code Session} has to be
	 * configured with the property {@code "mail.smtp.auth"} set to
	 * {@code true}, else the specified password will not be sent to the
	 * mail server by the JavaMail runtime. If you are not explicitly passing
	 * in a {@code Session} to use, simply specify this setting via
	 * {@link #setJavaMailProperties}.
	 * @see #setSession
	 * @see #setUsername
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Return the password for the account at the mail host.
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * Set the default encoding to use for {@link MimeMessage MimeMessages}
	 * created by this instance.
	 * <p>Such an encoding will be auto-detected by {@link MimeMessageHelper}.
	 */
	public void setDefaultEncoding(String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * Return the default encoding for {@link MimeMessage MimeMessages},
	 * or {@code null} if none.
	 */
	public String getDefaultEncoding() {
		return this.defaultEncoding;
	}

	/**
	 * Set the default Java Activation {@link FileTypeMap} to use for
	 * {@link MimeMessage MimeMessages} created by this instance.
	 * <p>A {@code FileTypeMap} specified here will be autodetected by
	 * {@link MimeMessageHelper}, avoiding the need to specify the
	 * {@code FileTypeMap} for each {@code MimeMessageHelper} instance.
	 * <p>For example, you can specify a custom instance of Spring's
	 * {@link ConfigurableMimeFileTypeMap} here. If not explicitly specified,
	 * a default {@code ConfigurableMimeFileTypeMap} will be used, containing
	 * an extended set of MIME type mappings (as defined by the
	 * {@code mime.types} file contained in the Spring jar).
	 * @see MimeMessageHelper#setFileTypeMap
	 */
	public void setDefaultFileTypeMap(FileTypeMap defaultFileTypeMap) {
		this.defaultFileTypeMap = defaultFileTypeMap;
	}

	/**
	 * Return the default Java Activation {@link FileTypeMap} for
	 * {@link MimeMessage MimeMessages}, or {@code null} if none.
	 */
	public FileTypeMap getDefaultFileTypeMap() {
		return this.defaultFileTypeMap;
	}


	//---------------------------------------------------------------------
	// Implementation of MailSender
	//---------------------------------------------------------------------

	public void send(SimpleMailMessage simpleMessage) throws MailException {
		send(new SimpleMailMessage[] { simpleMessage });
	}

	public void send(SimpleMailMessage[] simpleMessages) throws MailException {
		List<MimeMessage> mimeMessages = new ArrayList<MimeMessage>(simpleMessages.length);
		for (SimpleMailMessage simpleMessage : simpleMessages) {
			MimeMailMessage message = new MimeMailMessage(createMimeMessage());
			simpleMessage.copyTo(message);
			mimeMessages.add(message.getMimeMessage());
		}
		doSend(mimeMessages.toArray(new MimeMessage[mimeMessages.size()]), simpleMessages);
	}


	//---------------------------------------------------------------------
	// Implementation of JavaMailSender
	//---------------------------------------------------------------------

	/**
	 * This implementation creates a SmartMimeMessage, holding the specified
	 * default encoding and default FileTypeMap. This special defaults-carrying
	 * message will be autodetected by {@link MimeMessageHelper}, which will use
	 * the carried encoding and FileTypeMap unless explicitly overridden.
	 * @see #setDefaultEncoding
	 * @see #setDefaultFileTypeMap
	 */
	public MimeMessage createMimeMessage() {
		return new SmartMimeMessage(getSession(), getDefaultEncoding(), getDefaultFileTypeMap());
	}

	public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
		try {
			return new MimeMessage(getSession(), contentStream);
		}
		catch (MessagingException ex) {
			throw new MailParseException("Could not parse raw MIME content", ex);
		}
	}

	public void send(MimeMessage mimeMessage) throws MailException {
		send(new MimeMessage[] {mimeMessage});
	}

	public void send(MimeMessage[] mimeMessages) throws MailException {
		doSend(mimeMessages, null);
	}

	public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
		send(new MimeMessagePreparator[] { mimeMessagePreparator });
	}

	public void send(MimeMessagePreparator[] mimeMessagePreparators) throws MailException {
		try {
			List<MimeMessage> mimeMessages = new ArrayList<MimeMessage>(mimeMessagePreparators.length);
			for (MimeMessagePreparator preparator : mimeMessagePreparators) {
				MimeMessage mimeMessage = createMimeMessage();
				preparator.prepare(mimeMessage);
				mimeMessages.add(mimeMessage);
			}
			send(mimeMessages.toArray(new MimeMessage[mimeMessages.size()]));
		}
		catch (MailException ex) {
			throw ex;
		}
		catch (MessagingException ex) {
			throw new MailParseException(ex);
		}
		catch (IOException ex) {
			throw new MailPreparationException(ex);
		}
		catch (Exception ex) {
			throw new MailPreparationException(ex);
		}
	}


	/**
	 * Actually send the given array of MimeMessages via JavaMail.
	 * @param mimeMessages MimeMessage objects to send
	 * @param originalMessages corresponding original message objects
	 * that the MimeMessages have been created from (with same array
	 * length and indices as the "mimeMessages" array), if any
	 * @throws org.springframework.mail.MailAuthenticationException
	 * in case of authentication failure
	 * @throws org.springframework.mail.MailSendException
	 * in case of failure when sending a message
	 */
	protected void doSend(MimeMessage[] mimeMessages, Object[] originalMessages) throws MailException {
		Map<Object, Exception> failedMessages = new LinkedHashMap<Object, Exception>();

		Transport transport;
		try {
			transport = getTransport(getSession());
			transport.connect(getHost(), getPort(), getUsername(), getPassword());
		}
		catch (AuthenticationFailedException ex) {
			throw new MailAuthenticationException(ex);
		}
		catch (MessagingException ex) {
			// Effectively, all messages failed...
			for (int i = 0; i < mimeMessages.length; i++) {
				Object original = (originalMessages != null ? originalMessages[i] : mimeMessages[i]);
				failedMessages.put(original, ex);
			}
			throw new MailSendException("Mail server connection failed", ex, failedMessages);
		}

		try {
			for (int i = 0; i < mimeMessages.length; i++) {
				MimeMessage mimeMessage = mimeMessages[i];
				try {
					if (mimeMessage.getSentDate() == null) {
						mimeMessage.setSentDate(new Date());
					}
					String messageId = mimeMessage.getMessageID();
					mimeMessage.saveChanges();
					if (messageId != null) {
						// Preserve explicitly specified message id...
						mimeMessage.setHeader(HEADER_MESSAGE_ID, messageId);
					}
					transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
				}
				catch (MessagingException ex) {
					Object original = (originalMessages != null ? originalMessages[i] : mimeMessage);
					failedMessages.put(original, ex);
				}
			}
		}
		finally {
			try {
				transport.close();
			}
			catch (MessagingException ex) {
				if (!failedMessages.isEmpty()) {
					throw new MailSendException("Failed to close server connection after message failures", ex,
							failedMessages);
				}
				else {
					throw new MailSendException("Failed to close server connection after message sending", ex);
				}
			}
		}

		if (!failedMessages.isEmpty()) {
			throw new MailSendException(failedMessages);
		}
	}

	/**
	 * Obtain a Transport object from the given JavaMail Session,
	 * using the configured protocol.
	 * <p>Can be overridden in subclasses, e.g. to return a mock Transport object.
	 * @see javax.mail.Session#getTransport(String)
	 * @see #getProtocol()
	 */
	protected Transport getTransport(Session session) throws NoSuchProviderException {
		String protocol	= getProtocol();
		if (protocol == null) {
			protocol = session.getProperty("mail.transport.protocol");
			if (protocol == null) {
				protocol = DEFAULT_PROTOCOL;
			}
		}
		return session.getTransport(protocol);
	}

}
