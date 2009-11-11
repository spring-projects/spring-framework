package org.springframework.format.support;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.ConversionService;

public class FormattingConversionServiceFactoryBeanTests {

	private ConversionService conversionService;

	@Before
	public void setUp() {
		FormattingConversionServiceFactoryBean factory = new FormattingConversionServiceFactoryBean();
		factory.afterPropertiesSet();
		this.conversionService = factory.getObject();
		LocaleContextHolder.setLocale(Locale.US);
	}

	@After
	public void tearDown() {
		LocaleContextHolder.setLocale(null);
	}

	@Test
	public void testFormatNumber() {
		BigDecimal value = conversionService.convert("3,000.25", BigDecimal.class);
		assertEquals("3,000.25", conversionService.convert(value, String.class));
	}
	
	@Test
	public void testFormatDate() {
		Date value = conversionService.convert("10/29/09 12:00 PM", Date.class);
		assertEquals("10/29/09 12:00 PM", conversionService.convert(value, String.class));		
	}
}
