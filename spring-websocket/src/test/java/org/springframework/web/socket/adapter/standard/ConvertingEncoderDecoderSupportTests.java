/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.socket.adapter.standard;

import java.nio.ByteBuffer;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.socket.ContextLoaderTestUtils;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Test for {@link org.springframework.web.socket.adapter.standard.ConvertingEncoderDecoderSupport}.
 *
 * @author Phillip Webb
 */
public class ConvertingEncoderDecoderSupportTests {

	private static final String CONVERTED_TEXT = "_test";

	private static final ByteBuffer CONVERTED_BYTES = ByteBuffer.wrap("~test".getBytes());


	@Rule
	public ExpectedException thown = ExpectedException.none();

	private WebApplicationContext applicationContext;

	private MyType myType = new MyType("test");


	@Before
	public void setup() {
		setup(Config.class);
	}

	@After
	public void teardown() {
		ContextLoaderTestUtils.setCurrentWebApplicationContext(null);
	}

	private void setup(Class<?> configurationClass) {
		AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
		applicationContext.register(configurationClass);
		applicationContext.refresh();
		this.applicationContext = applicationContext;
		ContextLoaderTestUtils.setCurrentWebApplicationContext(this.applicationContext);
	}

	@Test
	public void encodeToText() throws Exception {
		assertThat(new MyTextEncoder().encode(myType), equalTo(CONVERTED_TEXT));
	}

	@Test
	public void encodeToTextCannotConvert() throws Exception {
		setup(NoConvertersConfig.class);
		thown.expect(EncodeException.class);
		thown.expectCause(isA(ConverterNotFoundException.class));
		new MyTextEncoder().encode(myType);
	}

	@Test
	public void encodeToBinary() throws Exception {
		assertThat(new MyBinaryEncoder().encode(myType).array(),
				equalTo(CONVERTED_BYTES.array()));
	}

	@Test
	public void encodeToBinaryCannotConvert() throws Exception {
		setup(NoConvertersConfig.class);
		thown.expect(EncodeException.class);
		thown.expectCause(isA(ConverterNotFoundException.class));
		new MyBinaryEncoder().encode(myType);
	}

	@Test
	public void decodeFromText() throws Exception {
		Decoder.Text<MyType> decoder = new MyTextDecoder();
		assertThat(decoder.willDecode(CONVERTED_TEXT), is(true));
		assertThat(decoder.decode(CONVERTED_TEXT), equalTo(myType));
	}

	@Test
	public void decodeFromTextCannotConvert() throws Exception {
		setup(NoConvertersConfig.class);
		Decoder.Text<MyType> decoder = new MyTextDecoder();
		assertThat(decoder.willDecode(CONVERTED_TEXT), is(false));
		thown.expect(DecodeException.class);
		thown.expectCause(isA(ConverterNotFoundException.class));
		decoder.decode(CONVERTED_TEXT);
	}

	@Test
	public void decodeFromBinary() throws Exception {
		Decoder.Binary<MyType> decoder = new MyBinaryDecoder();
		assertThat(decoder.willDecode(CONVERTED_BYTES), is(true));
		assertThat(decoder.decode(CONVERTED_BYTES), equalTo(myType));
	}

	@Test
	public void decodeFromBinaryCannotConvert() throws Exception {
		setup(NoConvertersConfig.class);
		Decoder.Binary<MyType> decoder = new MyBinaryDecoder();
		assertThat(decoder.willDecode(CONVERTED_BYTES), is(false));
		thown.expect(DecodeException.class);
		thown.expectCause(isA(ConverterNotFoundException.class));
		decoder.decode(CONVERTED_BYTES);
	}

	@Test
	public void encodeAndDecodeText() throws Exception {
		MyTextEncoderDecoder encoderDecoder = new MyTextEncoderDecoder();
		String encoded = encoderDecoder.encode(myType);
		assertThat(encoderDecoder.decode(encoded), equalTo(myType));
	}

	@Test
	public void encodeAndDecodeBytes() throws Exception {
		MyBinaryEncoderDecoder encoderDecoder = new MyBinaryEncoderDecoder();
		ByteBuffer encoded = encoderDecoder.encode(myType);
		assertThat(encoderDecoder.decode(encoded), equalTo(myType));
	}

	@Test
	public void autowiresIntoEncoder() throws Exception {
		WithAutowire withAutowire = new WithAutowire();
		withAutowire.init(null);
		assertThat(withAutowire.config, equalTo(applicationContext.getBean(Config.class)));
	}

	@Test
	public void cannotFindApplicationContext() throws Exception {
		ContextLoaderTestUtils.setCurrentWebApplicationContext(null);
		WithAutowire encoder = new WithAutowire();
		encoder.init(null);
		thown.expect(IllegalStateException.class);
		thown.expectMessage("Unable to locate the Spring ApplicationContext");
		encoder.encode(myType);
	}

	@Test
	public void cannotFindConversionService() throws Exception {
		setup(NoConfig.class);
		MyBinaryEncoder encoder = new MyBinaryEncoder();
		encoder.init(null);
		thown.expect(IllegalStateException.class);
		thown.expectMessage("Unable to find ConversionService");
		encoder.encode(myType);
	}

	@Configuration
	public static class Config {

		@Bean
		public ConversionService webSocketConversionService() {
			GenericConversionService conversionService = new DefaultConversionService();
			conversionService.addConverter(new MyTypeToStringConverter());
			conversionService.addConverter(new MyTypeToBytesConverter());
			conversionService.addConverter(new StringToMyTypeConverter());
			conversionService.addConverter(new BytesToMyTypeConverter());
			return conversionService;
		}

	}

	@Configuration
	public static class NoConvertersConfig {

		@Bean
		public ConversionService webSocketConversionService() {
			return new GenericConversionService();
		}

	}


	@Configuration
	public static class NoConfig {
	}


	public static class MyType {

		private String value;

		public MyType(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof MyType) {
				return ((MyType)obj).value.equals(value);
			}
			return false;
		}
	}


	private static class MyTypeToStringConverter implements Converter<MyType, String> {
		@Override
		public String convert(MyType source) {
			return "_" + source.toString();
		}
	}


	private static class MyTypeToBytesConverter implements Converter<MyType, byte[]> {
		@Override
		public byte[] convert(MyType source) {
			return ("~" + source.toString()).getBytes();
		}
	}


	private static class StringToMyTypeConverter implements Converter<String, MyType> {
		@Override
		public MyType convert(String source) {
			return new MyType(source.substring(1));
		}
	}


	private static class BytesToMyTypeConverter implements Converter<byte[], MyType> {
		@Override
		public MyType convert(byte[] source) {
			return new MyType(new String(source).substring(1));
		}
	}


	public static class MyTextEncoder extends
			ConvertingEncoderDecoderSupport.TextEncoder<MyType> {
	}


	public static class MyBinaryEncoder extends
			ConvertingEncoderDecoderSupport.BinaryEncoder<MyType> {
	}


	public static class MyTextDecoder extends
			ConvertingEncoderDecoderSupport.TextDecoder<MyType> {
	}


	public static class MyBinaryDecoder extends
			ConvertingEncoderDecoderSupport.BinaryDecoder<MyType> {
	}


	public static class MyTextEncoderDecoder extends
			ConvertingEncoderDecoderSupport<MyType, String> implements Encoder.Text<MyType>,
			Decoder.Text<MyType> {
	}


	public static class MyBinaryEncoderDecoder extends
			ConvertingEncoderDecoderSupport<MyType, ByteBuffer> implements Encoder.Binary<MyType>,
			Decoder.Binary<MyType> {
	}


	public static class WithAutowire extends ConvertingEncoderDecoderSupport.TextDecoder<MyType> {

		@Autowired
		private Config config;

	}

}
