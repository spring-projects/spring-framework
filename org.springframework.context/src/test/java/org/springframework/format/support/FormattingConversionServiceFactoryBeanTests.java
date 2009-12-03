/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.format.support;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Locale;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.ConversionService;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 */
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
		BigDecimal value = conversionService.convert("3000.25", BigDecimal.class);
		assertEquals("3000.25", conversionService.convert(value, String.class));
	}
	
	@Test
	public void testFormatDate() {
		Date value = conversionService.convert("10/29/09 12:00 PM", Date.class);
		assertEquals("10/29/09 12:00 PM", conversionService.convert(value, String.class));
	}

}
