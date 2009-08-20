package org.springframework.context.message;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;
import org.springframework.context.message.MessageResolver;
import org.springframework.context.message.MessageResolverBuilder;
import org.springframework.context.message.ResolvableArgument;

public class MessageResolverBuilderTests {
	
	private MessageResolverBuilder builder = new MessageResolverBuilder();

	@Test
	public void buildMessage() {
		MessageResolver resolver = builder.code("invalidFormat").arg("label", new ResolvableArgument("mathForm.decimalField"))
				.arg("format", "#,###.##").defaultMessage("Field must be in format #,###.##").build();
		MockMessageSource messageSource = new MockMessageSource();
		messageSource.addMessage("invalidFormat", Locale.US, "#{label} must be in format #{format}");
		messageSource.addMessage("mathForm.decimalField", Locale.US, "Decimal Field");
		String message = resolver.resolveMessage(messageSource, Locale.US);
		assertEquals("Decimal Field must be in format #,###.##", message);
	}
}
