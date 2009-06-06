package org.springframework.ui.message;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;
import org.springframework.context.support.StaticMessageSource;

public class MessageBuilderTests {
	private MessageBuilder builder = new MessageBuilder();

	@Test
	public void buildMessage() {
		MessageResolver resolver = builder.severity(Severity.ERROR).code("invalidFormat").resolvableArg("mathForm.decimalField")
				.arg("#,###.##").defaultText("Field must be in format #,###.##").build();
		StaticMessageSource messageSource = new StaticMessageSource();
		messageSource.addMessage("invalidFormat", Locale.US, "{0} must be in format {1}");
		messageSource.addMessage("mathForm.decimalField", Locale.US, "Decimal Field");
		Message message = resolver.resolveMessage(messageSource, Locale.US);
		assertEquals(Severity.ERROR, message.getSeverity());
		assertEquals("Decimal Field must be in format #,###.##", message.getText());
	}
}
