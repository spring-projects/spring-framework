/*
 * Copyright 2002-2021 the original author or authors.
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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
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
import org.springframework.core.codec.NettyByteBufDecoder;
import org.springframework.core.codec.NettyByteBufEncoder;
import org.springframework.core.codec.ResourceDecoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.FormHttpMessageReader;
import org.springframework.http.codec.FormHttpMessageWriter;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageReader;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.codec.ServerSentEventHttpMessageReader;
import org.springframework.http.codec.json.Jackson2CodecSupport;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.json.Jackson2SmileDecoder;
import org.springframework.http.codec.json.Jackson2SmileEncoder;
import org.springframework.http.codec.json.KotlinSerializationJsonDecoder;
import org.springframework.http.codec.json.KotlinSerializationJsonEncoder;
import org.springframework.http.codec.multipart.MultipartHttpMessageWriter;
import org.springframework.http.codec.protobuf.ProtobufDecoder;
import org.springframework.http.codec.protobuf.ProtobufHttpMessageWriter;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.ResolvableType.forClass;

/**
 * Unit tests for {@link ClientCodecConfigurer}.
 *
 * @author Rossen Stoyanchev
 */
public class ClientCodecConfigurerTests {

	private final ClientCodecConfigurer configurer = new DefaultClientCodecConfigurer();

	private final AtomicInteger index = new AtomicInteger();


	@Test
	public void defaultReaders() {
		List<HttpMessageReader<?>> readers = this.configurer.getReaders();
		assertThat(readers.size()).isEqualTo(14);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(ByteArrayDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(ByteBufferDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(DataBufferDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(NettyByteBufDecoder.class);
		assertThat(readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(ResourceHttpMessageReader.class);
		assertStringDecoder(getNextDecoder(readers), true);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(ProtobufDecoder.class);
		// SPR-16804
		assertThat(readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(FormHttpMessageReader.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(KotlinSerializationJsonDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(Jackson2JsonDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(Jackson2SmileDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(Jaxb2XmlDecoder.class);
		assertSseReader(readers);
		assertStringDecoder(getNextDecoder(readers), false);
	}

	@Test
	public void defaultWriters() {
		List<HttpMessageWriter<?>> writers = this.configurer.getWriters();
		assertThat(writers.size()).isEqualTo(13);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(ByteArrayEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(ByteBufferEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(DataBufferEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(NettyByteBufEncoder.class);
		assertThat(writers.get(index.getAndIncrement()).getClass()).isEqualTo(ResourceHttpMessageWriter.class);
		assertStringEncoder(getNextEncoder(writers), true);
		assertThat(writers.get(index.getAndIncrement()).getClass()).isEqualTo(ProtobufHttpMessageWriter.class);
		assertThat(writers.get(this.index.getAndIncrement()).getClass()).isEqualTo(MultipartHttpMessageWriter.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(KotlinSerializationJsonEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(Jackson2JsonEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(Jackson2SmileEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(Jaxb2XmlEncoder.class);
		assertStringEncoder(getNextEncoder(writers), false);
	}

	@Test
	public void jackson2CodecCustomization() {
		Jackson2JsonDecoder decoder = new Jackson2JsonDecoder();
		Jackson2JsonEncoder encoder = new Jackson2JsonEncoder();
		this.configurer.defaultCodecs().jackson2JsonDecoder(decoder);
		this.configurer.defaultCodecs().jackson2JsonEncoder(encoder);

		List<HttpMessageReader<?>> readers = this.configurer.getReaders();
		Jackson2JsonDecoder actualDecoder = findCodec(readers, Jackson2JsonDecoder.class);
		assertThat(actualDecoder).isSameAs(decoder);
		assertThat(findCodec(readers, ServerSentEventHttpMessageReader.class).getDecoder()).isSameAs(decoder);

		List<HttpMessageWriter<?>> writers = this.configurer.getWriters();
		Jackson2JsonEncoder actualEncoder = findCodec(writers, Jackson2JsonEncoder.class);
		assertThat(actualEncoder).isSameAs(encoder);

		MultipartHttpMessageWriter multipartWriter = findCodec(writers, MultipartHttpMessageWriter.class);
		actualEncoder = findCodec(multipartWriter.getPartWriters(), Jackson2JsonEncoder.class);
		assertThat(actualEncoder).isSameAs(encoder);
	}

	@Test
	public void objectMapperCustomization() {
		ObjectMapper objectMapper = new ObjectMapper();
		this.configurer.defaultCodecs().configureDefaultCodec(codec -> {
			if (codec instanceof Jackson2CodecSupport) {
				((Jackson2CodecSupport) codec).setObjectMapper(objectMapper);
			}
		});

		List<HttpMessageReader<?>> readers = this.configurer.getReaders();
		Jackson2JsonDecoder actualDecoder = findCodec(readers, Jackson2JsonDecoder.class);
		assertThat(actualDecoder.getObjectMapper()).isSameAs(objectMapper);

		List<HttpMessageWriter<?>> writers = this.configurer.getWriters();
		Jackson2JsonEncoder actualEncoder = findCodec(writers, Jackson2JsonEncoder.class);
		assertThat(actualEncoder.getObjectMapper()).isSameAs(objectMapper);

		MultipartHttpMessageWriter multipartWriter = findCodec(writers, MultipartHttpMessageWriter.class);
		actualEncoder = findCodec(multipartWriter.getPartWriters(), Jackson2JsonEncoder.class);
		assertThat(actualEncoder.getObjectMapper()).isSameAs(objectMapper);
	}

	@Test
	public void maxInMemorySize() {
		int size = 99;
		this.configurer.defaultCodecs().maxInMemorySize(size);
		List<HttpMessageReader<?>> readers = this.configurer.getReaders();
		assertThat(readers.size()).isEqualTo(14);
		assertThat(((ByteArrayDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((ByteBufferDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((DataBufferDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((NettyByteBufDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((ResourceDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((StringDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((ProtobufDecoder) getNextDecoder(readers)).getMaxMessageSize()).isEqualTo(size);
		assertThat(((FormHttpMessageReader) nextReader(readers)).getMaxInMemorySize()).isEqualTo(size);

		assertThat(((KotlinSerializationJsonDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((Jackson2JsonDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((Jackson2SmileDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((Jaxb2XmlDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);

		ServerSentEventHttpMessageReader reader = (ServerSentEventHttpMessageReader) nextReader(readers);
		assertThat(reader.getMaxInMemorySize()).isEqualTo(size);
		assertThat(((Jackson2JsonDecoder) reader.getDecoder()).getMaxInMemorySize()).isEqualTo(size);

		assertThat(((StringDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
	}

	@Test
	public void enableLoggingRequestDetails() {
		this.configurer.defaultCodecs().enableLoggingRequestDetails(true);

		List<HttpMessageWriter<?>> writers = this.configurer.getWriters();
		MultipartHttpMessageWriter multipartWriter = findCodec(writers, MultipartHttpMessageWriter.class);
		assertThat(multipartWriter.isEnableLoggingRequestDetails()).isTrue();

		FormHttpMessageWriter formWriter = (FormHttpMessageWriter) multipartWriter.getFormWriter();
		assertThat(formWriter).isNotNull();
		assertThat(formWriter.isEnableLoggingRequestDetails()).isTrue();
	}

	@Test
	public void clonedConfigurer() {
		ClientCodecConfigurer clone = this.configurer.clone();

		Jackson2JsonDecoder jackson2Decoder = new Jackson2JsonDecoder();
		clone.defaultCodecs().serverSentEventDecoder(jackson2Decoder);
		clone.defaultCodecs().multipartCodecs().encoder(new Jackson2SmileEncoder());
		clone.defaultCodecs().multipartCodecs().writer(new ResourceHttpMessageWriter());

		// Clone has the customizations

		Decoder<?> sseDecoder = findCodec(clone.getReaders(), ServerSentEventHttpMessageReader.class).getDecoder();
		List<HttpMessageWriter<?>> writers = findCodec(clone.getWriters(), MultipartHttpMessageWriter.class).getPartWriters();

		assertThat(sseDecoder).isSameAs(jackson2Decoder);
		assertThat(writers).hasSize(2);

		// Original does not have the customizations

		sseDecoder = findCodec(this.configurer.getReaders(), ServerSentEventHttpMessageReader.class).getDecoder();
		writers = findCodec(this.configurer.getWriters(), MultipartHttpMessageWriter.class).getPartWriters();

		assertThat(sseDecoder).isNotSameAs(jackson2Decoder);
		assertThat(writers).hasSize(12);
	}

	@Test // gh-24194
	public void cloneShouldNotDropMultipartCodecs() {

		ClientCodecConfigurer clone = this.configurer.clone();
		List<HttpMessageWriter<?>> writers =
				findCodec(clone.getWriters(), MultipartHttpMessageWriter.class).getPartWriters();

		assertThat(writers).hasSize(12);
	}

	@Test
	public void cloneShouldNotBeImpactedByChangesToOriginal() {

		ClientCodecConfigurer clone = this.configurer.clone();

		this.configurer.registerDefaults(false);
		this.configurer.customCodecs().register(new Jackson2JsonEncoder());

		List<HttpMessageWriter<?>> writers =
				findCodec(clone.getWriters(), MultipartHttpMessageWriter.class).getPartWriters();

		assertThat(writers).hasSize(12);
	}

	private Decoder<?> getNextDecoder(List<HttpMessageReader<?>> readers) {
		HttpMessageReader<?> reader = readers.get(this.index.getAndIncrement());
		assertThat(reader).isInstanceOf(DecoderHttpMessageReader.class);
		return ((DecoderHttpMessageReader<?>) reader).getDecoder();
	}

	private HttpMessageReader<?> nextReader(List<HttpMessageReader<?>> readers) {
		return readers.get(this.index.getAndIncrement());
	}

	private Encoder<?> getNextEncoder(List<HttpMessageWriter<?>> writers) {
		HttpMessageWriter<?> writer = writers.get(this.index.getAndIncrement());
		assertThat(writer.getClass()).isEqualTo(EncoderHttpMessageWriter.class);
		return ((EncoderHttpMessageWriter<?>) writer).getEncoder();
	}

	@SuppressWarnings("unchecked")
	private <T> T findCodec(List<?> codecs, Class<T> type) {
		return (T) codecs.stream()
				.map(c -> {
					if (c instanceof EncoderHttpMessageWriter) {
						return ((EncoderHttpMessageWriter<?>) c).getEncoder();
					}
					else if (c instanceof DecoderHttpMessageReader) {
						return ((DecoderHttpMessageReader<?>) c).getDecoder();
					}
					else {
						return c;
					}
				})
				.filter(type::isInstance).findFirst().get();
	}

	@SuppressWarnings("unchecked")
	private void assertStringDecoder(Decoder<?> decoder, boolean textOnly) {
		assertThat(decoder.getClass()).isEqualTo(StringDecoder.class);
		assertThat(decoder.canDecode(forClass(String.class), MimeTypeUtils.TEXT_PLAIN)).isTrue();
		Object expected = !textOnly;
		assertThat(decoder.canDecode(forClass(String.class), MediaType.TEXT_EVENT_STREAM)).isEqualTo(expected);

		byte[] bytes = "line1\nline2".getBytes(StandardCharsets.UTF_8);
		Flux<String> decoded = (Flux<String>) decoder.decode(
				Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes)),
				ResolvableType.forClass(String.class), MimeTypeUtils.TEXT_PLAIN, Collections.emptyMap());

		assertThat(decoded.collectList().block(Duration.ZERO)).isEqualTo(Arrays.asList("line1", "line2"));
	}

	private void assertStringEncoder(Encoder<?> encoder, boolean textOnly) {
		assertThat(encoder.getClass()).isEqualTo(CharSequenceEncoder.class);
		assertThat(encoder.canEncode(forClass(String.class), MimeTypeUtils.TEXT_PLAIN)).isTrue();
		Object expected = !textOnly;
		assertThat(encoder.canEncode(forClass(String.class), MediaType.TEXT_EVENT_STREAM)).isEqualTo(expected);
	}

	private void assertSseReader(List<HttpMessageReader<?>> readers) {
		HttpMessageReader<?> reader = readers.get(this.index.getAndIncrement());
		assertThat(reader.getClass()).isEqualTo(ServerSentEventHttpMessageReader.class);
		Decoder<?> decoder = ((ServerSentEventHttpMessageReader) reader).getDecoder();
		assertThat(decoder).isNotNull();
		assertThat(decoder.getClass()).isEqualTo(Jackson2JsonDecoder.class);
	}

}
