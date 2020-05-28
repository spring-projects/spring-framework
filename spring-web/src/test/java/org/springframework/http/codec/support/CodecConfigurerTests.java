/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.http.codec.support;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.google.protobuf.ExtensionRegistry;
import org.junit.Test;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.ByteArrayDecoder;
import org.springframework.core.codec.ByteArrayEncoder;
import org.springframework.core.codec.ByteBufferDecoder;
import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.DataBufferDecoder;
import org.springframework.core.codec.DataBufferEncoder;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.ResourceDecoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.MediaType;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.FormHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.codec.ServerSentEventHttpMessageReader;
import org.springframework.http.codec.ServerSentEventHttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.json.Jackson2SmileDecoder;
import org.springframework.http.codec.json.Jackson2SmileEncoder;
import org.springframework.http.codec.protobuf.ProtobufDecoder;
import org.springframework.http.codec.protobuf.ProtobufEncoder;
import org.springframework.http.codec.protobuf.ProtobufHttpMessageWriter;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.util.MimeTypeUtils;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BaseDefaultCodecs}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class CodecConfigurerTests {

	private final CodecConfigurer configurer = new TestCodecConfigurer();

	private final AtomicInteger index = new AtomicInteger(0);


	@Test
	public void defaultReaders() {
		List<HttpMessageReader<?>> readers = this.configurer.getReaders();
		assertEquals(11, readers.size());
		assertEquals(ByteArrayDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(ByteBufferDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(DataBufferDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(ResourceDecoder.class, getNextDecoder(readers).getClass());
		assertStringDecoder(getNextDecoder(readers), true);
		assertEquals(ProtobufDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(FormHttpMessageReader.class, readers.get(this.index.getAndIncrement()).getClass());
		assertEquals(Jackson2JsonDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(Jackson2SmileDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(Jaxb2XmlDecoder.class, getNextDecoder(readers).getClass());
		assertStringDecoder(getNextDecoder(readers), false);
	}

	@Test
	public void defaultWriters() {
		List<HttpMessageWriter<?>> writers = this.configurer.getWriters();
		assertEquals(10, writers.size());
		assertEquals(ByteArrayEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(ByteBufferEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(DataBufferEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(ResourceHttpMessageWriter.class, writers.get(index.getAndIncrement()).getClass());
		assertStringEncoder(getNextEncoder(writers), true);
		assertEquals(ProtobufHttpMessageWriter.class, writers.get(index.getAndIncrement()).getClass());
		assertEquals(Jackson2JsonEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(Jackson2SmileEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(Jaxb2XmlEncoder.class, getNextEncoder(writers).getClass());
		assertStringEncoder(getNextEncoder(writers), false);
	}

	@Test
	public void defaultAndCustomReaders() {
		Decoder<?> customDecoder1 = mock(Decoder.class);
		Decoder<?> customDecoder2 = mock(Decoder.class);

		when(customDecoder1.canDecode(ResolvableType.forClass(Object.class), null)).thenReturn(false);
		when(customDecoder2.canDecode(ResolvableType.forClass(Object.class), null)).thenReturn(true);

		HttpMessageReader<?> customReader1 = mock(HttpMessageReader.class);
		HttpMessageReader<?> customReader2 = mock(HttpMessageReader.class);

		when(customReader1.canRead(ResolvableType.forClass(Object.class), null)).thenReturn(false);
		when(customReader2.canRead(ResolvableType.forClass(Object.class), null)).thenReturn(true);

		this.configurer.customCodecs().register(customDecoder1);
		this.configurer.customCodecs().register(customDecoder2);

		this.configurer.customCodecs().register(customReader1);
		this.configurer.customCodecs().register(customReader2);

		List<HttpMessageReader<?>> readers = this.configurer.getReaders();

		assertEquals(15, readers.size());
		assertEquals(ByteArrayDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(ByteBufferDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(DataBufferDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(ResourceDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(StringDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(ProtobufDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(FormHttpMessageReader.class, readers.get(this.index.getAndIncrement()).getClass());
		assertSame(customDecoder1, getNextDecoder(readers));
		assertSame(customReader1, readers.get(this.index.getAndIncrement()));
		assertEquals(Jackson2JsonDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(Jackson2SmileDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(Jaxb2XmlDecoder.class, getNextDecoder(readers).getClass());
		assertSame(customDecoder2, getNextDecoder(readers));
		assertSame(customReader2, readers.get(this.index.getAndIncrement()));
		assertEquals(StringDecoder.class, getNextDecoder(readers).getClass());
	}

	@Test
	public void defaultAndCustomWriters() {
		Encoder<?> customEncoder1 = mock(Encoder.class);
		Encoder<?> customEncoder2 = mock(Encoder.class);

		when(customEncoder1.canEncode(ResolvableType.forClass(Object.class), null)).thenReturn(false);
		when(customEncoder2.canEncode(ResolvableType.forClass(Object.class), null)).thenReturn(true);

		HttpMessageWriter<?> customWriter1 = mock(HttpMessageWriter.class);
		HttpMessageWriter<?> customWriter2 = mock(HttpMessageWriter.class);

		when(customWriter1.canWrite(ResolvableType.forClass(Object.class), null)).thenReturn(false);
		when(customWriter2.canWrite(ResolvableType.forClass(Object.class), null)).thenReturn(true);

		this.configurer.customCodecs().register(customEncoder1);
		this.configurer.customCodecs().register(customEncoder2);

		this.configurer.customCodecs().register(customWriter1);
		this.configurer.customCodecs().register(customWriter2);

		List<HttpMessageWriter<?>> writers = this.configurer.getWriters();

		assertEquals(14, writers.size());
		assertEquals(ByteArrayEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(ByteBufferEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(DataBufferEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(ResourceHttpMessageWriter.class, writers.get(index.getAndIncrement()).getClass());
		assertEquals(CharSequenceEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(ProtobufHttpMessageWriter.class, writers.get(index.getAndIncrement()).getClass());
		assertSame(customEncoder1, getNextEncoder(writers));
		assertSame(customWriter1, writers.get(this.index.getAndIncrement()));
		assertEquals(Jackson2JsonEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(Jackson2SmileEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(Jaxb2XmlEncoder.class, getNextEncoder(writers).getClass());
		assertSame(customEncoder2, getNextEncoder(writers));
		assertSame(customWriter2, writers.get(this.index.getAndIncrement()));
		assertEquals(CharSequenceEncoder.class, getNextEncoder(writers).getClass());
	}

	@Test
	public void defaultsOffCustomReaders() {
		Decoder<?> customDecoder1 = mock(Decoder.class);
		Decoder<?> customDecoder2 = mock(Decoder.class);

		when(customDecoder1.canDecode(ResolvableType.forClass(Object.class), null)).thenReturn(false);
		when(customDecoder2.canDecode(ResolvableType.forClass(Object.class), null)).thenReturn(true);

		HttpMessageReader<?> customReader1 = mock(HttpMessageReader.class);
		HttpMessageReader<?> customReader2 = mock(HttpMessageReader.class);

		when(customReader1.canRead(ResolvableType.forClass(Object.class), null)).thenReturn(false);
		when(customReader2.canRead(ResolvableType.forClass(Object.class), null)).thenReturn(true);

		this.configurer.customCodecs().register(customDecoder1);
		this.configurer.customCodecs().register(customDecoder2);

		this.configurer.customCodecs().register(customReader1);
		this.configurer.customCodecs().register(customReader2);

		this.configurer.registerDefaults(false);

		List<HttpMessageReader<?>> readers = this.configurer.getReaders();

		assertEquals(4, readers.size());
		assertSame(customDecoder1, getNextDecoder(readers));
		assertSame(customReader1, readers.get(this.index.getAndIncrement()));
		assertSame(customDecoder2, getNextDecoder(readers));
		assertSame(customReader2, readers.get(this.index.getAndIncrement()));
	}

	@Test
	public void defaultsOffWithCustomWriters() {
		Encoder<?> customEncoder1 = mock(Encoder.class);
		Encoder<?> customEncoder2 = mock(Encoder.class);

		when(customEncoder1.canEncode(ResolvableType.forClass(Object.class), null)).thenReturn(false);
		when(customEncoder2.canEncode(ResolvableType.forClass(Object.class), null)).thenReturn(true);

		HttpMessageWriter<?> customWriter1 = mock(HttpMessageWriter.class);
		HttpMessageWriter<?> customWriter2 = mock(HttpMessageWriter.class);

		when(customWriter1.canWrite(ResolvableType.forClass(Object.class), null)).thenReturn(false);
		when(customWriter2.canWrite(ResolvableType.forClass(Object.class), null)).thenReturn(true);

		this.configurer.customCodecs().register(customEncoder1);
		this.configurer.customCodecs().register(customEncoder2);

		this.configurer.customCodecs().register(customWriter1);
		this.configurer.customCodecs().register(customWriter2);

		this.configurer.registerDefaults(false);

		List<HttpMessageWriter<?>> writers = this.configurer.getWriters();

		assertEquals(4, writers.size());
		assertSame(customEncoder1, getNextEncoder(writers));
		assertSame(customWriter1, writers.get(this.index.getAndIncrement()));
		assertSame(customEncoder2, getNextEncoder(writers));
		assertSame(customWriter2, writers.get(this.index.getAndIncrement()));
	}

	@Test
	public void encoderDecoderOverrides() {
		Jackson2JsonDecoder jacksonDecoder = new Jackson2JsonDecoder();
		Jackson2JsonEncoder jacksonEncoder = new Jackson2JsonEncoder();
		ProtobufDecoder protobufDecoder = new ProtobufDecoder(ExtensionRegistry.newInstance());
		ProtobufEncoder protobufEncoder = new ProtobufEncoder();
		Jaxb2XmlEncoder jaxb2Encoder = new Jaxb2XmlEncoder();
		Jaxb2XmlDecoder jaxb2Decoder = new Jaxb2XmlDecoder();

		this.configurer.defaultCodecs().jackson2JsonDecoder(jacksonDecoder);
		this.configurer.defaultCodecs().jackson2JsonEncoder(jacksonEncoder);
		this.configurer.defaultCodecs().protobufDecoder(protobufDecoder);
		this.configurer.defaultCodecs().protobufEncoder(protobufEncoder);
		this.configurer.defaultCodecs().jaxb2Decoder(jaxb2Decoder);
		this.configurer.defaultCodecs().jaxb2Encoder(jaxb2Encoder);

		assertDecoderInstance(jacksonDecoder);
		assertDecoderInstance(protobufDecoder);
		assertDecoderInstance(jaxb2Decoder);
		assertEncoderInstance(jacksonEncoder);
		assertEncoderInstance(protobufEncoder);
		assertEncoderInstance(jaxb2Encoder);
	}

	@Test
	public void cloneEmptyCustomCodecs() {
		this.configurer.registerDefaults(false);
		assertEquals(0, this.configurer.getReaders().size());
		assertEquals(0, this.configurer.getWriters().size());

		CodecConfigurer clone = this.configurer.clone();
		clone.customCodecs().register(new Jackson2JsonEncoder());
		clone.customCodecs().register(new Jackson2JsonDecoder());
		clone.customCodecs().register(new ServerSentEventHttpMessageReader());
		clone.customCodecs().register(new ServerSentEventHttpMessageWriter());

		assertEquals(0, this.configurer.getReaders().size());
		assertEquals(0, this.configurer.getWriters().size());
		assertEquals(2, clone.getReaders().size());
		assertEquals(2, clone.getWriters().size());
	}

	@Test
	public void cloneCustomCodecs() {
		this.configurer.registerDefaults(false);
		assertEquals(0, this.configurer.getReaders().size());
		assertEquals(0, this.configurer.getWriters().size());

		this.configurer.customCodecs().register(new Jackson2JsonEncoder());
		this.configurer.customCodecs().register(new Jackson2JsonDecoder());
		this.configurer.customCodecs().register(new ServerSentEventHttpMessageReader());
		this.configurer.customCodecs().register(new ServerSentEventHttpMessageWriter());
		assertEquals(2, this.configurer.getReaders().size());
		assertEquals(2, this.configurer.getWriters().size());

		CodecConfigurer clone = this.configurer.clone();
		assertEquals(2, this.configurer.getReaders().size());
		assertEquals(2, this.configurer.getWriters().size());
		assertEquals(2, clone.getReaders().size());
		assertEquals(2, clone.getWriters().size());
	}

	@Test
	public void cloneDefaultCodecs() {
		CodecConfigurer clone = this.configurer.clone();

		Jackson2JsonDecoder jacksonDecoder = new Jackson2JsonDecoder();
		Jackson2JsonEncoder jacksonEncoder = new Jackson2JsonEncoder();
		Jaxb2XmlDecoder jaxb2Decoder = new Jaxb2XmlDecoder();
		Jaxb2XmlEncoder jaxb2Encoder = new Jaxb2XmlEncoder();
		ProtobufDecoder protoDecoder = new ProtobufDecoder();
		ProtobufEncoder protoEncoder = new ProtobufEncoder();

		clone.defaultCodecs().jackson2JsonDecoder(jacksonDecoder);
		clone.defaultCodecs().jackson2JsonEncoder(jacksonEncoder);
		clone.defaultCodecs().jaxb2Decoder(jaxb2Decoder);
		clone.defaultCodecs().jaxb2Encoder(jaxb2Encoder);
		clone.defaultCodecs().protobufDecoder(protoDecoder);
		clone.defaultCodecs().protobufEncoder(protoEncoder);

		// Clone has the customized the customizations

		List<Decoder<?>> decoders = clone.getReaders().stream()
				.filter(reader -> reader instanceof DecoderHttpMessageReader)
				.map(reader -> ((DecoderHttpMessageReader<?>) reader).getDecoder())
				.collect(Collectors.toList());

		List<Encoder<?>> encoders = clone.getWriters().stream()
				.filter(writer -> writer instanceof EncoderHttpMessageWriter)
				.map(reader -> ((EncoderHttpMessageWriter<?>) reader).getEncoder())
				.collect(Collectors.toList());

		assertTrue(decoders.containsAll(Arrays.asList(jacksonDecoder, jaxb2Decoder, protoDecoder)));
		assertTrue(encoders.containsAll(Arrays.asList(jacksonEncoder, jaxb2Encoder, protoEncoder)));

		// Original does not have the customizations

		decoders = this.configurer.getReaders().stream()
				.filter(reader -> reader instanceof DecoderHttpMessageReader)
				.map(reader -> ((DecoderHttpMessageReader<?>) reader).getDecoder())
				.collect(Collectors.toList());

		encoders = this.configurer.getWriters().stream()
				.filter(writer -> writer instanceof EncoderHttpMessageWriter)
				.map(reader -> ((EncoderHttpMessageWriter<?>) reader).getEncoder())
				.collect(Collectors.toList());

		assertFalse(decoders.containsAll(Arrays.asList(jacksonDecoder, jaxb2Decoder, protoDecoder)));
		assertFalse(encoders.containsAll(Arrays.asList(jacksonEncoder, jaxb2Encoder, protoEncoder)));
	}

	@SuppressWarnings("deprecation")
	@Test
	public void withDefaultCodecConfig() {
		AtomicBoolean callbackCalled = new AtomicBoolean(false);
		this.configurer.defaultCodecs().enableLoggingRequestDetails(true);
		this.configurer.customCodecs().withDefaultCodecConfig(config -> {
			assertTrue(config.isEnableLoggingRequestDetails());
			callbackCalled.compareAndSet(false, true);
		});
		this.configurer.getReaders();
		assertTrue(callbackCalled.get());
	}

	private Decoder<?> getNextDecoder(List<HttpMessageReader<?>> readers) {
		HttpMessageReader<?> reader = readers.get(this.index.getAndIncrement());
		assertEquals(DecoderHttpMessageReader.class, reader.getClass());
		return ((DecoderHttpMessageReader<?>) reader).getDecoder();
	}

	private Encoder<?> getNextEncoder(List<HttpMessageWriter<?>> writers) {
		HttpMessageWriter<?> writer = writers.get(this.index.getAndIncrement());
		assertEquals(EncoderHttpMessageWriter.class, writer.getClass());
		return ((EncoderHttpMessageWriter<?>) writer).getEncoder();
	}

	private void assertStringDecoder(Decoder<?> decoder, boolean textOnly) {
		assertEquals(StringDecoder.class, decoder.getClass());
		assertTrue(decoder.canDecode(ResolvableType.forClass(String.class), MimeTypeUtils.TEXT_PLAIN));
		assertEquals(!textOnly, decoder.canDecode(ResolvableType.forClass(String.class), MediaType.TEXT_EVENT_STREAM));
	}

	private void assertStringEncoder(Encoder<?> encoder, boolean textOnly) {
		assertEquals(CharSequenceEncoder.class, encoder.getClass());
		assertTrue(encoder.canEncode(ResolvableType.forClass(String.class), MimeTypeUtils.TEXT_PLAIN));
		assertEquals(!textOnly, encoder.canEncode(ResolvableType.forClass(String.class), MediaType.TEXT_EVENT_STREAM));
	}

	private void assertDecoderInstance(Decoder<?> decoder) {
		assertSame(decoder, this.configurer.getReaders().stream()
				.filter(writer -> writer instanceof DecoderHttpMessageReader)
				.map(writer -> ((DecoderHttpMessageReader<?>) writer).getDecoder())
				.filter(e -> decoder.getClass().equals(e.getClass()))
				.findFirst()
				.filter(e -> e == decoder).orElse(null));
	}

	private void assertEncoderInstance(Encoder<?> encoder) {
		assertSame(encoder, this.configurer.getWriters().stream()
				.filter(writer -> writer instanceof EncoderHttpMessageWriter)
				.map(writer -> ((EncoderHttpMessageWriter<?>) writer).getEncoder())
				.filter(e -> encoder.getClass().equals(e.getClass()))
				.findFirst()
				.filter(e -> e == encoder).orElse(null));
	}


	private static class TestCodecConfigurer extends BaseCodecConfigurer {

		TestCodecConfigurer() {
			super(new BaseDefaultCodecs());
		}

		TestCodecConfigurer(TestCodecConfigurer other) {
			super(other);
		}

		@Override
		protected BaseDefaultCodecs cloneDefaultCodecs() {
			return new BaseDefaultCodecs((BaseDefaultCodecs) defaultCodecs());
		}

		@Override
		public CodecConfigurer clone() {
			return new TestCodecConfigurer(this);
		}
	}

}
