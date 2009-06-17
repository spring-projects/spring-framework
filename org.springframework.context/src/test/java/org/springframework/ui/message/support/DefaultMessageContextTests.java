package org.springframework.ui.message.support;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.ui.message.Message;
import org.springframework.ui.message.MessageBuilder;
import org.springframework.ui.message.MessageResolver;
import org.springframework.ui.message.MockMessageSource;
import org.springframework.ui.message.Severity;

public class DefaultMessageContextTests {
	
	private DefaultMessageContext context;

	@Before
	public void setUp() {
		MockMessageSource messageSource = new MockMessageSource();
		messageSource.addMessage("invalidFormat", Locale.US, "#{label} must be in format #{format}");
		messageSource.addMessage("mathForm.decimalField", Locale.US, "Decimal Field");
		context = new DefaultMessageContext(messageSource);
		LocaleContextHolder.setLocale(Locale.US);
	}
	
	@After
	public void tearDown() {
		LocaleContextHolder.setLocale(null);
	}

	@Test
	public void addMessage() {
		MessageBuilder builder = new MessageBuilder();
		MessageResolver message = builder.severity(Severity.ERROR).code("invalidFormat").resolvableArg("label",
				"mathForm.decimalField").arg("format", "#,###.##").defaultText("Field must be in format #,###.##").build();
		context.add(message, "mathForm.decimalField");
		Map<String, List<Message>> messages = context.getMessages();
		assertEquals(1, messages.size());
		assertEquals("Decimal Field must be in format #,###.##", messages.get("mathForm.decimalField").get(0).getText());
		assertEquals("Decimal Field must be in format #,###.##", context.getMessages("mathForm.decimalField").get(0).getText());
	}
}
