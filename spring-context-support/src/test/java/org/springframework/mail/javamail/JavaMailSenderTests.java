/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import jakarta.activation.FileTypeMap;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.URLName;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;

import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.entry;

/**
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 09.10.2004
 */
class JavaMailSenderTests {

	private final MockJavaMailSender sender = new MockJavaMailSender();


	@Test
	void javaMailSenderWithSimpleMessage() throws Exception {
		sender.setHost("host");
		sender.setPort(30);
		sender.setUsername("username");
		sender.setPassword("password");

		SimpleMailMessage simpleMessage = new SimpleMailMessage();
		simpleMessage.setFrom("me@mail.org");
		simpleMessage.setReplyTo("reply@mail.org");
		simpleMessage.setTo("you@mail.org");
		simpleMessage.setCc("he@mail.org", "she@mail.org");
		simpleMessage.setBcc("us@mail.org", "them@mail.org");
		Date sentDate = new GregorianCalendar(2004, Calendar.FEBRUARY, 1).getTime();
		simpleMessage.setSentDate(sentDate);
		simpleMessage.setSubject("my subject");
		simpleMessage.setText("my text");
		sender.send(simpleMessage);

		assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
		assertThat(sender.transport.getConnectedPort()).isEqualTo(30);
		assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
		assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
		assertThat(sender.transport.isCloseCalled()).isTrue();

		assertThat(sender.transport.getSentMessages()).hasSize(1);
		MimeMessage sentMessage = sender.transport.getSentMessage(0);
		assertThat(addresses(sentMessage.getFrom())).containsExactly("me@mail.org");
		assertThat(addresses(sentMessage.getReplyTo())).containsExactly("reply@mail.org");
		assertThat(addresses(sentMessage.getRecipients(RecipientType.TO))).containsExactly("you@mail.org");
		assertThat(addresses(sentMessage.getRecipients(RecipientType.CC))).containsExactly("he@mail.org", "she@mail.org");
		assertThat(addresses(sentMessage.getRecipients(RecipientType.BCC))).containsExactly("us@mail.org", "them@mail.org");

		assertThat(sentMessage.getSentDate().getTime()).isEqualTo(sentDate.getTime());
		assertThat(sentMessage.getSubject()).isEqualTo("my subject");
		assertThat(sentMessage.getContent()).isEqualTo("my text");
	}

	@Test
	void javaMailSenderWithSimpleMessages() throws Exception {
		sender.setHost("host");
		sender.setUsername("username");
		sender.setPassword("password");

		SimpleMailMessage simpleMessage1 = new SimpleMailMessage();
		simpleMessage1.setTo("he@mail.org");
		SimpleMailMessage simpleMessage2 = new SimpleMailMessage();
		simpleMessage2.setTo("she@mail.org");
		sender.send(simpleMessage1, simpleMessage2);

		assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
		assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
		assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
		assertThat(sender.transport.isCloseCalled()).isTrue();

		assertThat(sender.transport.getSentMessages()).hasSize(2);
		MimeMessage sentMessage1 = sender.transport.getSentMessage(0);
		MimeMessage sentMessage2 = sender.transport.getSentMessage(1);
		assertThat(addresses(sentMessage1.getRecipients(RecipientType.TO))).containsExactly("he@mail.org");
		assertThat(addresses(sentMessage2.getRecipients(RecipientType.TO))).containsExactly("she@mail.org");
	}

	@Test
	void javaMailSenderWithMimeMessage() throws Exception {
		sender.setHost("host");
		sender.setUsername("username");
		sender.setPassword("password");

		MimeMessage mimeMessage = sender.createMimeMessage();
		mimeMessage.setRecipient(RecipientType.TO, new InternetAddress("you@mail.org"));
		sender.send(mimeMessage);

		assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
		assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
		assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
		assertThat(sender.transport.isCloseCalled()).isTrue();
		assertThat(sender.transport.getSentMessages()).containsExactly(mimeMessage);
	}

	@Test
	void javaMailSenderWithMimeMessages() throws Exception {
		sender.setHost("host");
		sender.setUsername("username");
		sender.setPassword("password");

		MimeMessage mimeMessage1 = sender.createMimeMessage();
		mimeMessage1.setRecipient(RecipientType.TO, new InternetAddress("he@mail.org"));
		MimeMessage mimeMessage2 = sender.createMimeMessage();
		mimeMessage2.setRecipient(RecipientType.TO, new InternetAddress("she@mail.org"));
		sender.send(mimeMessage1, mimeMessage2);

		assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
		assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
		assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
		assertThat(sender.transport.isCloseCalled()).isTrue();
		assertThat(sender.transport.getSentMessages()).containsExactly(mimeMessage1, mimeMessage2);
	}

	@Test
	void javaMailSenderWithMimeMessagePreparator() {
		sender.setHost("host");
		sender.setUsername("username");
		sender.setPassword("password");

		final List<Message> messages = new ArrayList<>();

		MimeMessagePreparator preparator = mimeMessage -> {
			mimeMessage.setRecipient(RecipientType.TO, new InternetAddress("you@mail.org"));
			messages.add(mimeMessage);
		};
		sender.send(preparator);

		assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
		assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
		assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
		assertThat(sender.transport.isCloseCalled()).isTrue();
		assertThat(sender.transport.getSentMessages()).containsExactly(messages.get(0));
	}

	@Test
	void javaMailSenderWithMimeMessagePreparators() {
		sender.setHost("host");
		sender.setUsername("username");
		sender.setPassword("password");

		final List<Message> messages = new ArrayList<>();

		MimeMessagePreparator preparator1 = mimeMessage -> {
			mimeMessage.setRecipient(RecipientType.TO, new InternetAddress("he@mail.org"));
			messages.add(mimeMessage);
		};
		MimeMessagePreparator preparator2 = mimeMessage -> {
			mimeMessage.setRecipient(RecipientType.TO, new InternetAddress("she@mail.org"));
			messages.add(mimeMessage);
		};
		sender.send(preparator1, preparator2);

		assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
		assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
		assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
		assertThat(sender.transport.isCloseCalled()).isTrue();
		assertThat(messages).hasSize(2);
		assertThat(sender.transport.getSentMessages()).containsExactlyElementsOf(messages);
	}

	@Test
	void javaMailSenderWithMimeMessageHelper() throws Exception {
		sender.setHost("host");
		sender.setUsername("username");
		sender.setPassword("password");

		MimeMessageHelper message = new MimeMessageHelper(sender.createMimeMessage());
		assertThat(message.getEncoding()).isNull();
		assertThat(message.getFileTypeMap()).isInstanceOf(ConfigurableMimeFileTypeMap.class);

		message.setTo("you@mail.org");
		sender.send(message.getMimeMessage());

		assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
		assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
		assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
		assertThat(sender.transport.isCloseCalled()).isTrue();
		assertThat(sender.transport.getSentMessages()).containsExactly(message.getMimeMessage());
	}

	@Test
	void javaMailSenderWithMimeMessageHelperAndSpecificEncoding() throws Exception {
		sender.setHost("host");
		sender.setUsername("username");
		sender.setPassword("password");

		MimeMessageHelper message = new MimeMessageHelper(sender.createMimeMessage(), "UTF-8");
		assertThat(message.getEncoding()).isEqualTo("UTF-8");
		FileTypeMap fileTypeMap = new ConfigurableMimeFileTypeMap();
		message.setFileTypeMap(fileTypeMap);
		assertThat(message.getFileTypeMap()).isEqualTo(fileTypeMap);

		message.setTo("you@mail.org");
		sender.send(message.getMimeMessage());

		assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
		assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
		assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
		assertThat(sender.transport.isCloseCalled()).isTrue();
		assertThat(sender.transport.getSentMessages()).containsExactly(message.getMimeMessage());
	}

	@Test
	void javaMailSenderWithMimeMessageHelperAndDefaultEncoding() throws Exception {
		sender.setHost("host");
		sender.setUsername("username");
		sender.setPassword("password");
		sender.setDefaultEncoding("UTF-8");

		FileTypeMap fileTypeMap = new ConfigurableMimeFileTypeMap();
		sender.setDefaultFileTypeMap(fileTypeMap);
		MimeMessageHelper message = new MimeMessageHelper(sender.createMimeMessage());
		assertThat(message.getEncoding()).isEqualTo("UTF-8");
		assertThat(message.getFileTypeMap()).isEqualTo(fileTypeMap);

		message.setTo("you@mail.org");
		sender.send(message.getMimeMessage());

		assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
		assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
		assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
		assertThat(sender.transport.isCloseCalled()).isTrue();
		assertThat(sender.transport.getSentMessages()).containsExactly(message.getMimeMessage());
	}

	@Test
	void javaMailSenderWithParseExceptionOnSimpleMessage() {
		SimpleMailMessage simpleMessage = new SimpleMailMessage();
		simpleMessage.setFrom("");

		assertThatExceptionOfType(MailParseException.class)
			.isThrownBy(() -> sender.send(simpleMessage))
			.withCauseInstanceOf(AddressException.class);
	}

	@Test
	void javaMailSenderWithParseExceptionOnMimeMessagePreparator() {
		MimeMessagePreparator preparator = mimeMessage -> mimeMessage.setFrom(new InternetAddress(""));
		assertThatExceptionOfType(MailParseException.class)
			.isThrownBy(() -> sender.send(preparator))
			.withCauseInstanceOf(AddressException.class);
	}

	@Test
	void javaMailSenderWithCustomSession() throws Exception {
		final Session session = Session.getInstance(new Properties());
		MockJavaMailSender sender = new MockJavaMailSender() {
			@Override
			protected Transport getTransport(Session sess) throws NoSuchProviderException {
				assertThat(sess).isEqualTo(session);
				return super.getTransport(sess);
			}
		};
		sender.setSession(session);
		sender.setHost("host");
		sender.setUsername("username");
		sender.setPassword("password");

		MimeMessage mimeMessage = sender.createMimeMessage();
		mimeMessage.setSubject("custom");
		mimeMessage.setRecipient(RecipientType.TO, new InternetAddress("you@mail.org"));
		mimeMessage.setSentDate(new GregorianCalendar(2005, Calendar.APRIL, 1).getTime());
		sender.send(mimeMessage);

		assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
		assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
		assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
		assertThat(sender.transport.isCloseCalled()).isTrue();
		assertThat(sender.transport.getSentMessages()).containsExactly(mimeMessage);
	}

	@Test
	void javaMailProperties() throws Exception {
		Properties props = new Properties();
		props.setProperty("bogusKey", "bogusValue");
		MockJavaMailSender sender = new MockJavaMailSender() {
			@Override
			protected Transport getTransport(Session sess) throws NoSuchProviderException {
				assertThat(sess.getProperty("bogusKey")).isEqualTo("bogusValue");
				return super.getTransport(sess);
			}
		};
		sender.setJavaMailProperties(props);
		sender.setHost("host");
		sender.setUsername("username");
		sender.setPassword("password");

		MimeMessage mimeMessage = sender.createMimeMessage();
		mimeMessage.setRecipient(RecipientType.TO, new InternetAddress("you@mail.org"));
		sender.send(mimeMessage);

		assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
		assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
		assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
		assertThat(sender.transport.isCloseCalled()).isTrue();
		assertThat(sender.transport.getSentMessages()).containsExactly(mimeMessage);
	}

	@Test
	void failedMailServerConnect() {
		sender.setHost(null);
		sender.setUsername("username");
		sender.setPassword("password");
		SimpleMailMessage simpleMessage1 = new SimpleMailMessage();
		assertThatExceptionOfType(MailSendException.class)
			.isThrownBy(() -> sender.send(simpleMessage1))
			.satisfies(ex -> assertThat(ex.getFailedMessages()).containsExactly(entry(simpleMessage1, (Exception) ex.getCause())));
	}

	@Test
	void failedMailServerClose() {
		sender.setHost("");
		sender.setUsername("username");
		sender.setPassword("password");
		SimpleMailMessage simpleMessage1 = new SimpleMailMessage();
		assertThatExceptionOfType(MailSendException.class)
			.isThrownBy(() -> sender.send(simpleMessage1))
			.satisfies(ex -> assertThat(ex.getFailedMessages()).isEmpty());
	}

	@Test
	void failedSimpleMessage() throws Exception {
		sender.setHost("host");
		sender.setUsername("username");
		sender.setPassword("password");

		SimpleMailMessage simpleMessage1 = new SimpleMailMessage();
		simpleMessage1.setTo("he@mail.org");
		simpleMessage1.setSubject("fail");
		SimpleMailMessage simpleMessage2 = new SimpleMailMessage();
		simpleMessage2.setTo("she@mail.org");

		try {
			sender.send(simpleMessage1, simpleMessage2);
		}
		catch (MailSendException ex) {
			assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
			assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
			assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
			assertThat(sender.transport.isCloseCalled()).isTrue();
			assertThat(sender.transport.getSentMessages()).hasSize(1);
			assertThat(sender.transport.getSentMessage(0).getAllRecipients()[0]).isEqualTo(new InternetAddress("she@mail.org"));
			assertThat(ex.getFailedMessages()).containsOnlyKeys(simpleMessage1);
			assertThat(ex.getFailedMessages().get(simpleMessage1))
					.isInstanceOf(MessagingException.class).hasMessage("failed");
		}
	}

	@Test
	void failedMimeMessage() throws Exception {
		sender.setHost("host");
		sender.setUsername("username");
		sender.setPassword("password");

		MimeMessage mimeMessage1 = sender.createMimeMessage();
		mimeMessage1.setRecipient(RecipientType.TO, new InternetAddress("he@mail.org"));
		mimeMessage1.setSubject("fail");
		MimeMessage mimeMessage2 = sender.createMimeMessage();
		mimeMessage2.setRecipient(RecipientType.TO, new InternetAddress("she@mail.org"));

		try {
			sender.send(mimeMessage1, mimeMessage2);
		}
		catch (MailSendException ex) {
			assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
			assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
			assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
			assertThat(sender.transport.isCloseCalled()).isTrue();
			assertThat(sender.transport.getSentMessages()).containsExactly(mimeMessage2);
			assertThat(ex.getFailedMessages()).containsOnlyKeys(mimeMessage1);
			assertThat(ex.getFailedMessages().get(mimeMessage1))
					.isInstanceOf(MessagingException.class).hasMessage("failed");
		}
	}

	@Test
	void testConnection() {
		sender.setHost("host");
		assertThatNoException().isThrownBy(sender::testConnection);
	}

	@Test
	void testConnectionWithFailure() {
		sender.setHost(null);
		assertThatExceptionOfType(MessagingException.class).isThrownBy(sender::testConnection);
	}


	private static Stream<String> addresses(Address[] addresses) {
		return Arrays.stream(addresses).map(InternetAddress.class::cast).map(InternetAddress::getAddress);
	}


	private static class MockJavaMailSender extends JavaMailSenderImpl {

		private MockTransport transport;

		@Override
		protected Transport getTransport(Session session) throws NoSuchProviderException {
			this.transport = new MockTransport(session, null);
			return transport;
		}
	}


	private static class MockTransport extends Transport {

		private String connectedHost = null;
		private int connectedPort = -2;
		private String connectedUsername = null;
		private String connectedPassword = null;
		private boolean closeCalled = false;
		private final List<Message> sentMessages = new ArrayList<>();

		private MockTransport(Session session, URLName urlName) {
			super(session, urlName);
		}

		public String getConnectedHost() {
			return connectedHost;
		}

		public int getConnectedPort() {
			return connectedPort;
		}

		public String getConnectedUsername() {
			return connectedUsername;
		}

		public String getConnectedPassword() {
			return connectedPassword;
		}

		public boolean isCloseCalled() {
			return closeCalled;
		}

		public List<Message> getSentMessages() {
			return sentMessages;
		}

		public MimeMessage getSentMessage(int index) {
			return (MimeMessage) this.sentMessages.get(index);
		}

		@Override
		public void connect(String host, int port, String username, String password) throws MessagingException {
			if (host == null) {
				throw new MessagingException("no host");
			}
			this.connectedHost = host;
			this.connectedPort = port;
			this.connectedUsername = username;
			this.connectedPassword = password;
			setConnected(true);
		}

		@Override
		public synchronized void close() throws MessagingException {
			if (this.connectedHost.isEmpty()) {
				throw new MessagingException("close failure");
			}
			this.closeCalled = true;
		}

		@Override
		public void sendMessage(Message message, Address[] addresses) throws MessagingException {
			if ("fail".equals(message.getSubject())) {
				throw new MessagingException("failed");
			}
			if (addresses == null || (message.getAllRecipients() == null ? addresses.length > 0 :
					!ObjectUtils.nullSafeEquals(addresses, message.getAllRecipients()))) {
				throw new MessagingException("addresses not correct");
			}
			if (message.getSentDate() == null) {
				throw new MessagingException("No sentDate specified");
			}
			if (message.getSubject() != null && message.getSubject().contains("custom")) {
				assertThat(message.getSentDate()).isEqualTo(new GregorianCalendar(2005, Calendar.APRIL, 1).getTime());
			}
			this.sentMessages.add(message);
		}
	}

}
