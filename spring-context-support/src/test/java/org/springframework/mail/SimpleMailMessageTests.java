/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.mail;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Chris Beams
 * @since 10.09.2003
 */
class SimpleMailMessageTests {

	@Test
	void simpleMessageCopyCtor() {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom("me@mail.org");
		message.setTo("you@mail.org");

		SimpleMailMessage messageCopy = new SimpleMailMessage(message);
		assertThat(messageCopy.getFrom()).isEqualTo("me@mail.org");
		assertThat(messageCopy.getTo()[0]).isEqualTo("you@mail.org");

		message.setReplyTo("reply@mail.org");
		message.setCc("he@mail.org", "she@mail.org");
		message.setBcc("us@mail.org", "them@mail.org");
		Date sentDate = new Date();
		message.setSentDate(sentDate);
		message.setSubject("my subject");
		message.setText("my text");

		assertThat(message.getFrom()).isEqualTo("me@mail.org");
		assertThat(message.getReplyTo()).isEqualTo("reply@mail.org");
		assertThat(message.getTo()[0]).isEqualTo("you@mail.org");
		List<String> ccs = Arrays.asList(message.getCc());
		assertThat(ccs).contains("he@mail.org");
		assertThat(ccs).contains("she@mail.org");
		List<String> bccs = Arrays.asList(message.getBcc());
		assertThat(bccs).contains("us@mail.org");
		assertThat(bccs).contains("them@mail.org");
		assertThat(message.getSentDate()).isEqualTo(sentDate);
		assertThat(message.getSubject()).isEqualTo("my subject");
		assertThat(message.getText()).isEqualTo("my text");

		messageCopy = new SimpleMailMessage(message);
		assertThat(messageCopy.getFrom()).isEqualTo("me@mail.org");
		assertThat(messageCopy.getReplyTo()).isEqualTo("reply@mail.org");
		assertThat(messageCopy.getTo()[0]).isEqualTo("you@mail.org");
		ccs = Arrays.asList(messageCopy.getCc());
		assertThat(ccs).contains("he@mail.org");
		assertThat(ccs).contains("she@mail.org");
		bccs = Arrays.asList(message.getBcc());
		assertThat(bccs).contains("us@mail.org");
		assertThat(bccs).contains("them@mail.org");
		assertThat(messageCopy.getSentDate()).isEqualTo(sentDate);
		assertThat(messageCopy.getSubject()).isEqualTo("my subject");
		assertThat(messageCopy.getText()).isEqualTo("my text");
	}

	@Test
	void deepCopyOfStringArrayTypedFieldsOnCopyCtor() {
		SimpleMailMessage original = new SimpleMailMessage();
		original.setTo("fiona@mail.org", "apple@mail.org");
		original.setCc("he@mail.org", "she@mail.org");
		original.setBcc("us@mail.org", "them@mail.org");

		SimpleMailMessage copy = new SimpleMailMessage(original);

		original.getTo()[0] = "mmm@mmm.org";
		original.getCc()[0] = "mmm@mmm.org";
		original.getBcc()[0] = "mmm@mmm.org";

		assertThat(copy.getTo()[0]).isEqualTo("fiona@mail.org");
		assertThat(copy.getCc()[0]).isEqualTo("he@mail.org");
		assertThat(copy.getBcc()[0]).isEqualTo("us@mail.org");
	}

	/**
	 * Tests that two equal SimpleMailMessages have equal hash codes.
	 */
	@Test
	void equalMessagesHaveEqualHashCodes() {
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

		assertThat(message2).isEqualTo(message1);
		assertThat(message2).hasSameHashCodeAs(message1);
	}

	@Test
	void equalsBehavior() {
		SimpleMailMessage message1;
		SimpleMailMessage message2;

		// Same object is equal
		message1 = new SimpleMailMessage();
		message2 = message1;
		assertThat(message1).isEqualTo(message2);

		// Null object is not equal
		message1 = new SimpleMailMessage();
		message2 = null;
		assertThat(message1).isNotEqualTo(message2);

		// Different class is not equal
		assertThat(message1).isNotEqualTo(new Object());

		// Equal values are equal
		message1 = new SimpleMailMessage();
		message2 = new SimpleMailMessage();
		assertThat(message1).isEqualTo(message2);

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
		assertThat(message1).isEqualTo(message2);
	}

	@Test
	void copyCtorChokesOnNullOriginalMessage() {
		assertThatIllegalArgumentException().isThrownBy(() -> new SimpleMailMessage(null));
	}

	@Test
	void copyToChokesOnNullTargetMessage() {
		assertThatIllegalArgumentException().isThrownBy(() -> new SimpleMailMessage().copyTo(null));
	}

}
