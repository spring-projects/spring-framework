package org.springframework.ui.message;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Ignore;
import org.junit.Test;

public class MessageBuilderTests {
	
	private MessageBuilder builder = new MessageBuilder();

	@Test
	@Ignore
	public void buildMessage() {
		MessageResolver resolver = builder.severity(Severity.ERROR).code("invalidFormat").resolvableArg("label", "mathForm.decimalField")
				.arg("format", "#,###.##").defaultText("Field must be in format #,###.##").build();
		MockMessageSource messageSource = new MockMessageSource();
		messageSource.addMessage("invalidFormat", Locale.US, "#{label} must be in format #{format}");
		messageSource.addMessage("mathForm.decimalField", Locale.US, "Decimal Field");
		Message message = resolver.resolveMessage(messageSource, Locale.US);
		assertEquals(Severity.ERROR, message.getSeverity());
		assertEquals("Decimal Field must be in format #,###.##", message.getText());
	}
}
