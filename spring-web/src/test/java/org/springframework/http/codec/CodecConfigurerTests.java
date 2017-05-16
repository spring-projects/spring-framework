/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.http.codec;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.util.MimeTypeUtils;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.core.ResolvableType.forClass;

/**
 * Unit tests for {@link org.springframework.http.codec.DefaultCodecConfigurer.AbstractDefaultCodecsConfigurer}.
 * @author Rossen Stoyanchev
 */
public class CodecConfigurerTests {

	private final CodecConfigurer configurer = new DefaultCodecConfigurer(new TestDefaultCodecConfigurer());

	private final AtomicInteger index = new AtomicInteger(0);


	@Test
	public void defaultReaders() throws Exception {
		List<HttpMessageReader<?>> readers = this.configurer.getReaders();
		assertEquals(8, readers.size());
		assertEquals(ByteArrayDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(ByteBufferDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(DataBufferDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(ResourceDecoder.class, getNextDecoder(readers).getClass());
		assertStringDecoder(getNextDecoder(readers), true);
		assertEquals(Jaxb2XmlDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(Jackson2JsonDecoder.class, getNextDecoder(readers).getClass());
		assertStringDecoder(getNextDecoder(readers), false);
	}

	@Test
	public void defaultWriters() throws Exception {
		List<HttpMessageWriter<?>> writers = this.configurer.getWriters();
		assertEquals(8, writers.size());
		assertEquals(ByteArrayEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(ByteBufferEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(DataBufferEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(ResourceHttpMessageWriter.class, writers.get(index.getAndIncrement()).getClass());
		assertStringEncoder(getNextEncoder(writers), true);
		assertEquals(Jaxb2XmlEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(Jackson2JsonEncoder.class, getNextEncoder(writers).getClass());
		assertStringEncoder(getNextEncoder(writers), false);
	}

	@Test
	public void defaultAndCustomReaders() throws Exception {

		Decoder<?> customDecoder1 = mock(Decoder.class);
		Decoder<?> customDecoder2 = mock(Decoder.class);

		when(customDecoder1.canDecode(ResolvableType.forClass(Object.class), null)).thenReturn(false);
		when(customDecoder2.canDecode(ResolvableType.forClass(Object.class), null)).thenReturn(true);

		HttpMessageReader<?> customReader1 = mock(HttpMessageReader.class);
		HttpMessageReader<?> customReader2 = mock(HttpMessageReader.class);

		when(customReader1.canRead(ResolvableType.forClass(Object.class), null)).thenReturn(false);
		when(customReader2.canRead(ResolvableType.forClass(Object.class), null)).thenReturn(true);

		this.configurer.customCodecs().decoder(customDecoder1);
		this.configurer.customCodecs().decoder(customDecoder2);

		this.configurer.customCodecs().reader(customReader1);
		this.configurer.customCodecs().reader(customReader2);

		List<HttpMessageReader<?>> readers = this.configurer.getReaders();

		assertEquals(12, readers.size());
		assertEquals(ByteArrayDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(ByteBufferDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(DataBufferDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(ResourceDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(StringDecoder.class, getNextDecoder(readers).getClass());
		assertSame(customDecoder1, getNextDecoder(readers));
		assertSame(customReader1, readers.get(this.index.getAndIncrement()));
		assertEquals(Jaxb2XmlDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(Jackson2JsonDecoder.class, getNextDecoder(readers).getClass());
		assertSame(customDecoder2, getNextDecoder(readers));
		assertSame(customReader2, readers.get(this.index.getAndIncrement()));
		assertEquals(StringDecoder.class, getNextDecoder(readers).getClass());
	}

	@Test
	public void defaultAndCustomWriters() throws Exception {

		Encoder<?> customEncoder1 = mock(Encoder.class);
		Encoder<?> customEncoder2 = mock(Encoder.class);

		when(customEncoder1.canEncode(ResolvableType.forClass(Object.class), null)).thenReturn(false);
		when(customEncoder2.canEncode(ResolvableType.forClass(Object.class), null)).thenReturn(true);

		HttpMessageWriter<?> customWriter1 = mock(HttpMessageWriter.class);
		HttpMessageWriter<?> customWriter2 = mock(HttpMessageWriter.class);

		when(customWriter1.canWrite(ResolvableType.forClass(Object.class), null)).thenReturn(false);
		when(customWriter2.canWrite(ResolvableType.forClass(Object.class), null)).thenReturn(true);

		this.configurer.customCodecs().encoder(customEncoder1);
		this.configurer.customCodecs().encoder(customEncoder2);

		this.configurer.customCodecs().writer(customWriter1);
		this.configurer.customCodecs().writer(customWriter2);

		List<HttpMessageWriter<?>> writers = this.configurer.getWriters();

		assertEquals(12, writers.size());
		assertEquals(ByteArrayEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(ByteBufferEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(DataBufferEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(ResourceHttpMessageWriter.class, writers.get(index.getAndIncrement()).getClass());
		assertEquals(CharSequenceEncoder.class, getNextEncoder(writers).getClass());
		assertSame(customEncoder1, getNextEncoder(writers));
		assertSame(customWriter1, writers.get(this.index.getAndIncrement()));
		assertEquals(Jaxb2XmlEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(Jackson2JsonEncoder.class, getNextEncoder(writers).getClass());
		assertSame(customEncoder2, getNextEncoder(writers));
		assertSame(customWriter2, writers.get(this.index.getAndIncrement()));
		assertEquals(CharSequenceEncoder.class, getNextEncoder(writers).getClass());
	}

	@Test
	public void defaultsOffCustomReaders() throws Exception {

		Decoder<?> customDecoder1 = mock(Decoder.class);
		Decoder<?> customDecoder2 = mock(Decoder.class);

		when(customDecoder1.canDecode(ResolvableType.forClass(Object.class), null)).thenReturn(false);
		when(customDecoder2.canDecode(ResolvableType.forClass(Object.class), null)).thenReturn(true);

		HttpMessageReader<?> customReader1 = mock(HttpMessageReader.class);
		HttpMessageReader<?> customReader2 = mock(HttpMessageReader.class);

		when(customReader1.canRead(ResolvableType.forClass(Object.class), null)).thenReturn(false);
		when(customReader2.canRead(ResolvableType.forClass(Object.class), null)).thenReturn(true);

		this.configurer.customCodecs().decoder(customDecoder1);
		this.configurer.customCodecs().decoder(customDecoder2);

		this.configurer.customCodecs().reader(customReader1);
		this.configurer.customCodecs().reader(customReader2);

		this.configurer.registerDefaults(false);

		List<HttpMessageReader<?>> readers = this.configurer.getReaders();

		assertEquals(4, readers.size());
		assertSame(customDecoder1, getNextDecoder(readers));
		assertSame(customReader1, readers.get(this.index.getAndIncrement()));
		assertSame(customDecoder2, getNextDecoder(readers));
		assertSame(customReader2, readers.get(this.index.getAndIncrement()));
	}

	@Test
	public void defaultsOffWithCustomWriters() throws Exception {

		Encoder<?> customEncoder1 = mock(Encoder.class);
		Encoder<?> customEncoder2 = mock(Encoder.class);

		when(customEncoder1.canEncode(ResolvableType.forClass(Object.class), null)).thenReturn(false);
		when(customEncoder2.canEncode(ResolvableType.forClass(Object.class), null)).thenReturn(true);

		HttpMessageWriter<?> customWriter1 = mock(HttpMessageWriter.class);
		HttpMessageWriter<?> customWriter2 = mock(HttpMessageWriter.class);

		when(customWriter1.canWrite(ResolvableType.forClass(Object.class), null)).thenReturn(false);
		when(customWriter2.canWrite(ResolvableType.forClass(Object.class), null)).thenReturn(true);

		this.configurer.customCodecs().encoder(customEncoder1);
		this.configurer.customCodecs().encoder(customEncoder2);

		this.configurer.customCodecs().writer(customWriter1);
		this.configurer.customCodecs().writer(customWriter2);

		this.configurer.registerDefaults(false);

		List<HttpMessageWriter<?>> writers = this.configurer.getWriters();

		assertEquals(4, writers.size());
		assertSame(customEncoder1, getNextEncoder(writers));
		assertSame(customWriter1, writers.get(this.index.getAndIncrement()));
		assertSame(customEncoder2, getNextEncoder(writers));
		assertSame(customWriter2, writers.get(this.index.getAndIncrement()));
	}

	@Test
	public void jackson2DecoderOverride() throws Exception {

		Jackson2JsonDecoder decoder = new Jackson2JsonDecoder();
		this.configurer.defaultCodecs().jackson2Decoder(decoder);

		assertSame(decoder, this.configurer.getReaders().stream()
				.filter(writer -> writer instanceof DecoderHttpMessageReader)
				.map(writer -> ((DecoderHttpMessageReader<?>) writer).getDecoder())
				.filter(e -> Jackson2JsonDecoder.class.equals(e.getClass()))
				.findFirst()
				.filter(e -> e == decoder).orElse(null));
	}

	@Test
	public void jackson2EncoderOverride() throws Exception {

		Jackson2JsonEncoder encoder = new Jackson2JsonEncoder();
		this.configurer.defaultCodecs().jackson2Encoder(encoder);

		assertSame(encoder, this.configurer.getWriters().stream()
				.filter(writer -> writer instanceof EncoderHttpMessageWriter)
				.map(writer -> ((EncoderHttpMessageWriter<?>) writer).getEncoder())
				.filter(e -> Jackson2JsonEncoder.class.equals(e.getClass()))
				.findFirst()
				.filter(e -> e == encoder).orElse(null));
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
		assertTrue(decoder.canDecode(forClass(String.class), MimeTypeUtils.TEXT_PLAIN));
		assertEquals(!textOnly, decoder.canDecode(forClass(String.class), MediaType.TEXT_EVENT_STREAM));
	}

	private void assertStringEncoder(Encoder<?> encoder, boolean textOnly) {
		assertEquals(CharSequenceEncoder.class, encoder.getClass());
		assertTrue(encoder.canEncode(forClass(String.class), MimeTypeUtils.TEXT_PLAIN));
		assertEquals(!textOnly, encoder.canEncode(forClass(String.class), MediaType.TEXT_EVENT_STREAM));
	}




		private static class TestDefaultCodecConfigurer extends DefaultCodecConfigurer.AbstractDefaultCodecsConfigurer {

			@Override
			protected void addStringReaderTextOnlyTo(List<HttpMessageReader<?>> result) {
				addReaderTo(result, () -> new DecoderHttpMessageReader<>(StringDecoder.textPlainOnly(true)));
			}

			@Override
			protected void addStringReaderTo(List<HttpMessageReader<?>> result) {
				addReaderTo(result, () -> new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes(true)));
			}
		}

}
