package org.springframework.ui.message.support;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.ui.message.Message;
import org.springframework.ui.message.MessageBuilder;
import org.springframework.ui.message.MessageResolver;
import org.springframework.ui.message.Severity;

public class DefaultMessageContextTests {
	
	private DefaultMessageContext context;

	@Before
	public void setUp() {
		StaticMessageSource messageSource = new StaticMessageSource();
		messageSource.addMessage("invalidFormat", Locale.US, "{0} must be in format {1}");
		messageSource.addMessage("mathForm.decimalField", Locale.US, "Decimal Field");
		context = new DefaultMessageContext(messageSource);
	}

	@Test
	public void addMessage() {
		MessageBuilder builder = new MessageBuilder();
		MessageResolver message = builder.severity(Severity.ERROR).code("invalidFormat").resolvableArg(
				"mathForm.decimalField").arg("#,###.##").defaultText("Field must be in format #,###.##").build();
		context.addMessage("mathForm.decimalField", message);
		Map<String, List<Message>> messages = context.getMessages();
		assertEquals(1, messages.size());
		assertEquals("Decimal Field must be in format #,###.##", messages.get("mathForm.decimalField").get(0).getText());
		assertEquals("Decimal Field must be in format #,###.##", context.getMessages("mathForm.decimalField").get(0).getText());
	}
}
