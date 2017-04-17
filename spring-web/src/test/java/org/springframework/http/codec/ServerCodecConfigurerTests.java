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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import reactor.core.publisher.Flux;

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
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.util.MimeTypeUtils;

import static org.junit.Assert.*;
import static org.springframework.core.ResolvableType.forClass;

/**
 * Unit tests for {@link ServerCodecConfigurer}.
 * @author Rossen Stoyanchev
 */
public class ServerCodecConfigurerTests {

	private final ServerCodecConfigurer configurer = new DefaultServerCodecConfigurer();

	private final AtomicInteger index = new AtomicInteger(0);


	@Test
	public void defaultReaders() throws Exception {
		List<HttpMessageReader<?>> readers = this.configurer.getReaders();
		assertEquals(9, readers.size());
		assertEquals(ByteArrayDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(ByteBufferDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(DataBufferDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(ResourceDecoder.class, getNextDecoder(readers).getClass());
		assertStringDecoder(getNextDecoder(readers), true);
		assertEquals(FormHttpMessageReader.class, readers.get(this.index.getAndIncrement()).getClass());
		assertEquals(Jaxb2XmlDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(Jackson2JsonDecoder.class, getNextDecoder(readers).getClass());
		assertStringDecoder(getNextDecoder(readers), false);
	}

	@Test
	public void defaultWriters() throws Exception {
		List<HttpMessageWriter<?>> writers = this.configurer.getWriters();
		assertEquals(9, writers.size());
		assertEquals(ByteArrayEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(ByteBufferEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(DataBufferEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(ResourceHttpMessageWriter.class, writers.get(index.getAndIncrement()).getClass());
		assertStringEncoder(getNextEncoder(writers), true);
		assertEquals(Jaxb2XmlEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(Jackson2JsonEncoder.class, getNextEncoder(writers).getClass());
		assertSseWriter(writers);
		assertStringEncoder(getNextEncoder(writers), false);
	}

	@Test
	public void jackson2EncoderOverride() throws Exception {

		Jackson2JsonEncoder encoder = new Jackson2JsonEncoder();
		this.configurer.defaultCodecs().jackson2Encoder(encoder);

		assertSame(encoder, this.configurer.getWriters().stream()
				.filter(writer -> ServerSentEventHttpMessageWriter.class.equals(writer.getClass()))
				.map(writer -> (ServerSentEventHttpMessageWriter) writer)
				.findFirst()
				.map(ServerSentEventHttpMessageWriter::getEncoder)
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

	@SuppressWarnings("unchecked")
	private void assertStringDecoder(Decoder<?> decoder, boolean textOnly) {
		assertEquals(StringDecoder.class, decoder.getClass());
		assertTrue(decoder.canDecode(forClass(String.class), MimeTypeUtils.TEXT_PLAIN));
		assertEquals(!textOnly, decoder.canDecode(forClass(String.class), MediaType.TEXT_EVENT_STREAM));

		Flux<String> flux = (Flux<String>) decoder.decode(
				Flux.just(new DefaultDataBufferFactory().wrap("line1\nline2".getBytes(StandardCharsets.UTF_8))),
				ResolvableType.forClass(String.class), MimeTypeUtils.TEXT_PLAIN, Collections.emptyMap());

		assertEquals(Arrays.asList("line1\n", "line2"), flux.collectList().block(Duration.ZERO));
	}

	private void assertStringEncoder(Encoder<?> encoder, boolean textOnly) {
		assertEquals(CharSequenceEncoder.class, encoder.getClass());
		assertTrue(encoder.canEncode(forClass(String.class), MimeTypeUtils.TEXT_PLAIN));
		assertEquals(!textOnly, encoder.canEncode(forClass(String.class), MediaType.TEXT_EVENT_STREAM));
	}

	private void assertSseWriter(List<HttpMessageWriter<?>> writers) {
		HttpMessageWriter<?> writer = writers.get(this.index.getAndIncrement());
		assertEquals(ServerSentEventHttpMessageWriter.class, writer.getClass());
		Encoder<?> encoder = ((ServerSentEventHttpMessageWriter) writer).getEncoder();
		assertNotNull(encoder);
		assertEquals(Jackson2JsonEncoder.class, encoder.getClass());
	}

}
