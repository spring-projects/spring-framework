/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.converter;

import java.util.Locale;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Stephane Nicoll
 */
public class GenericMessageConverterTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final ConversionService conversionService = new DefaultConversionService();
	private final GenericMessageConverter converter = new GenericMessageConverter(conversionService);

	@Test
	public void fromMessageWithConversion() {
		Message<String> content = MessageBuilder.withPayload("33").build();
		assertEquals(33, converter.fromMessage(content, Integer.class));
	}

	@Test
	public void fromMessageNoConverter() {
		Message<Integer> content = MessageBuilder.withPayload(1234).build();
		assertNull("No converter from integer to locale", converter.fromMessage(content, Locale.class));
	}

	@Test
	public void fromMessageWithFailedConversion() {
		Message<String> content = MessageBuilder.withPayload("test not a number").build();
		thrown.expect(MessageConversionException.class);
		thrown.expectCause(isA(ConversionException.class));
		converter.fromMessage(content, Integer.class);
	}
}
