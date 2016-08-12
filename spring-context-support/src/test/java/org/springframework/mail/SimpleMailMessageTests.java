/*
 * Copyright 2002-2007 the original author or authors.
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

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Chris Beams
 * @since 10.09.2003
 */
public class SimpleMailMessageTests {

	@Test
	public void testSimpleMessageCopyCtor() {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom("me@mail.org");
		message.setTo("you@mail.org");

		SimpleMailMessage messageCopy = new SimpleMailMessage(message);
		assertEquals("me@mail.org", messageCopy.getFrom());
		assertEquals("you@mail.org", messageCopy.getTo()[0]);

		message.setReplyTo("reply@mail.org");
		message.setCc(new String[]{"he@mail.org", "she@mail.org"});
		message.setBcc(new String[]{"us@mail.org", "them@mail.org"});
		Date sentDate = new Date();
		message.setSentDate(sentDate);
		message.setSubject("my subject");
		message.setText("my text");

		assertEquals("me@mail.org", message.getFrom());
		assertEquals("reply@mail.org", message.getReplyTo());
		assertEquals("you@mail.org", message.getTo()[0]);
		List<String> ccs = Arrays.asList(message.getCc());
		assertTrue(ccs.contains("he@mail.org"));
		assertTrue(ccs.contains("she@mail.org"));
		List<String> bccs = Arrays.asList(message.getBcc());
		assertTrue(bccs.contains("us@mail.org"));
		assertTrue(bccs.contains("them@mail.org"));
		assertEquals(sentDate, message.getSentDate());
		assertEquals("my subject", message.getSubject());
		assertEquals("my text", message.getText());

		messageCopy = new SimpleMailMessage(message);
		assertEquals("me@mail.org", messageCopy.getFrom());
		assertEquals("reply@mail.org", messageCopy.getReplyTo());
		assertEquals("you@mail.org", messageCopy.getTo()[0]);
		ccs = Arrays.asList(messageCopy.getCc());
		assertTrue(ccs.contains("he@mail.org"));
		assertTrue(ccs.contains("she@mail.org"));
		bccs = Arrays.asList(message.getBcc());
		assertTrue(bccs.contains("us@mail.org"));
		assertTrue(bccs.contains("them@mail.org"));
		assertEquals(sentDate, messageCopy.getSentDate());
		assertEquals("my subject", messageCopy.getSubject());
		assertEquals("my text", messageCopy.getText());
	}

	@Test
	public void testDeepCopyOfStringArrayTypedFieldsOnCopyCtor() throws Exception {

		SimpleMailMessage original = new SimpleMailMessage();
		original.setTo(new String[]{"fiona@mail.org", "apple@mail.org"});
		original.setCc(new String[]{"he@mail.org", "she@mail.org"});
		original.setBcc(new String[]{"us@mail.org", "them@mail.org"});

		SimpleMailMessage copy = new SimpleMailMessage(original);

		original.getTo()[0] = "mmm@mmm.org";
		original.getCc()[0] = "mmm@mmm.org";
		original.getBcc()[0] = "mmm@mmm.org";

		assertEquals("fiona@mail.org", copy.getTo()[0]);
		assertEquals("he@mail.org", copy.getCc()[0]);
		assertEquals("us@mail.org", copy.getBcc()[0]);
	}

	/**
	 * Tests that two equal SimpleMailMessages have equal hash codes.
	 */
	@Test
	public final void testHashCode() {
		SimpleMailMessage message1 = new SimpleMailMessage();
		message1.setFrom("from@somewhere");
		message1.setReplyTo("replyTo@somewhere");
		message1.setTo("to@somewhere");
		message1.setCc("cc@somewhere");
		message1.setBcc("bcc@somewhere");
		message1.setSentDate(new Date());
		message1.setSubject("subject");
		message1.setText("text");

		// Copy the message
		SimpleMailMessage message2 = new SimpleMailMessage(message1);

		assertEquals(message1, message2);
		assertEquals(message1.hashCode(), message2.hashCode());
	}

	public final void testEqualsObject() {
		SimpleMailMessage message1;
		SimpleMailMessage message2;

		// Same object is equal
		message1 = new SimpleMailMessage();
		message2 = message1;
		assertTrue(message1.equals(message2));

		// Null object is not equal
		message1 = new SimpleMailMessage();
		message2 = null;
		assertTrue(!(message1.equals(message2)));

		// Different class is not equal
		assertTrue(!(message1.equals(new Object())));

		// Equal values are equal
		message1 = new SimpleMailMessage();
		message2 = new SimpleMailMessage();
		assertTrue(message1.equals(message2));

		message1 = new SimpleMailMessage();
		message1.setFrom("from@somewhere");
		message1.setReplyTo("replyTo@somewhere");
		message1.setTo("to@somewhere");
		message1.setCc("cc@somewhere");
		message1.setBcc("bcc@somewhere");
		message1.setSentDate(new Date());
		message1.setSubject("subject");
		message1.setText("text");
		message2 = new SimpleMailMessage(message1);
		assertTrue(message1.equals(message2));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCopyCtorChokesOnNullOriginalMessage() throws Exception {
		new SimpleMailMessage(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCopyToChokesOnNullTargetMessage() throws Exception {
		new SimpleMailMessage().copyTo(null);
	}

}
