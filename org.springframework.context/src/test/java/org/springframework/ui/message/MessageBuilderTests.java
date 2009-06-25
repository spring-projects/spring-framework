package org.springframework.ui.message;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;

public class MessageBuilderTests {

	@Test
	public void buildMessage() {
		MockMessageSource messageSource = new MockMessageSource();
		messageSource.addMessage("invalidFormat", Locale.US, "#{label} must be in format #{format}");
		messageSource.addMessage("mathForm.decimalField", Locale.US, "Decimal Field");
		MessageBuilder builder = new MessageBuilder(messageSource);
		String message = builder.code("invalidFormat").arg("label", new ResolvableArgument("mathForm.decimalField"))
				.arg("format", "#,###.##").defaultMessage("Field must be in format #,###.##").build();
		assertEquals("Decimal Field must be in format #,###.##", message);
	}
}
