/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.converter;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 *
 * @author Stephane Nicoll
 */
class GenericMessageConverterTests {

	private final ConversionService conversionService = new DefaultConversionService();
	private final GenericMessageConverter converter = new GenericMessageConverter(conversionService);

	@Test
	void fromMessageWithConversion() {
		Message<String> content = MessageBuilder.withPayload("33").build();
		assertThat(converter.fromMessage(content, Integer.class)).isEqualTo(33);
	}

	@Test
	void fromMessageNoConverter() {
		Message<Integer> content = MessageBuilder.withPayload(1234).build();
		assertThat(converter.fromMessage(content, Locale.class)).as("No converter from integer to locale").isNull();
	}

	@Test
	void fromMessageWithFailedConversion() {
		Message<String> content = MessageBuilder.withPayload("test not a number").build();
		assertThatExceptionOfType(MessageConversionException.class).isThrownBy(() ->
				converter.fromMessage(content, Integer.class))
			.withCauseInstanceOf(ConversionException.class);
	}
}
