/*
 * Copyright 2002-present the original author or authors.
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
import org.springframework.http.codec.cbor.KotlinSerializationCborDecoder;
import org.springframework.http.codec.cbor.KotlinSerializationCborEncoder;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader;
import org.springframework.http.codec.multipart.MultipartHttpMessageReader;
import org.springframework.http.codec.multipart.MultipartHttpMessageWriter;
import org.springframework.http.codec.multipart.PartEventHttpMessageReader;
import org.springframework.http.codec.multipart.PartEventHttpMessageWriter;
import org.springframework.http.codec.multipart.PartHttpMessageWriter;
import org.springframework.http.codec.protobuf.KotlinSerializationProtobufDecoder;
import org.springframework.http.codec.protobuf.KotlinSerializationProtobufEncoder;
import org.springframework.http.codec.protobuf.ProtobufDecoder;
import org.springframework.http.codec.protobuf.ProtobufHttpMessageWriter;
import org.springframework.http.codec.smile.JacksonSmileDecoder;
import org.springframework.http.codec.smile.JacksonSmileEncoder;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.ResolvableType.forClass;

/**
 * Tests for {@link ClientCodecConfigurer}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
class ClientCodecConfigurerTests {

	private final ClientCodecConfigurer configurer = new DefaultClientCodecConfigurer();

	private final AtomicInteger index = new AtomicInteger();


	@Test
	void defaultReaders() {
		List<HttpMessageReader<?>> readers = this.configurer.getReaders();
		assertThat(readers).hasSize(18);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(ByteArrayDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(ByteBufferDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(DataBufferDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(NettyByteBufDecoder.class);
		assertThat(readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(ResourceHttpMessageReader.class);
		assertStringDecoder(getNextDecoder(readers), true);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(ProtobufDecoder.class);
		// SPR-16804
		assertThat(readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(FormHttpMessageReader.class);
		assertThat(readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(DefaultPartHttpMessageReader.class);
		assertThat(readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(MultipartHttpMessageReader.class);
		assertThat(readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(PartEventHttpMessageReader.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(KotlinSerializationCborDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(KotlinSerializationProtobufDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(JacksonJsonDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(JacksonSmileDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(Jaxb2XmlDecoder.class);
		assertSseReader(readers);
		assertStringDecoder(getNextDecoder(readers), false);
	}

	@Test
	void defaultWriters() {
		List<HttpMessageWriter<?>> writers = this.configurer.getWriters();
		assertThat(writers).hasSize(16);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(ByteArrayEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(ByteBufferEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(DataBufferEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(NettyByteBufEncoder.class);
		assertThat(writers.get(index.getAndIncrement()).getClass()).isEqualTo(ResourceHttpMessageWriter.class);
		assertStringEncoder(getNextEncoder(writers), true);
		assertThat(writers.get(index.getAndIncrement()).getClass()).isEqualTo(ProtobufHttpMessageWriter.class);
		assertThat(writers.get(this.index.getAndIncrement()).getClass()).isEqualTo(MultipartHttpMessageWriter.class);
		assertThat(writers.get(this.index.getAndIncrement()).getClass()).isEqualTo(PartEventHttpMessageWriter.class);
		assertThat(writers.get(this.index.getAndIncrement()).getClass()).isEqualTo(PartHttpMessageWriter.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(KotlinSerializationCborEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(KotlinSerializationProtobufEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(JacksonJsonEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(JacksonSmileEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(Jaxb2XmlEncoder.class);
		assertStringEncoder(getNextEncoder(writers), false);
	}

	@Test
	void jacksonCodecCustomization() {
		JacksonJsonDecoder decoder = new JacksonJsonDecoder();
		JacksonJsonEncoder encoder = new JacksonJsonEncoder();
		this.configurer.defaultCodecs().jacksonJsonDecoder(decoder);
		this.configurer.defaultCodecs().jacksonJsonEncoder(encoder);

		List<HttpMessageReader<?>> readers = this.configurer.getReaders();
		JacksonJsonDecoder actualDecoder = findCodec(readers, JacksonJsonDecoder.class);
		assertThat(actualDecoder).isSameAs(decoder);
		assertThat(findCodec(readers, ServerSentEventHttpMessageReader.class).getDecoder()).isSameAs(decoder);

		List<HttpMessageWriter<?>> writers = this.configurer.getWriters();
		JacksonJsonEncoder actualEncoder = findCodec(writers, JacksonJsonEncoder.class);
		assertThat(actualEncoder).isSameAs(encoder);

		MultipartHttpMessageWriter multipartWriter = findCodec(writers, MultipartHttpMessageWriter.class);
		actualEncoder = findCodec(multipartWriter.getPartWriters(), JacksonJsonEncoder.class);
		assertThat(actualEncoder).isSameAs(encoder);
	}

	@Test
	void maxInMemorySize() {
		int size = 99;
		this.configurer.defaultCodecs().maxInMemorySize(size);
		List<HttpMessageReader<?>> readers = this.configurer.getReaders();
		assertThat(readers).hasSize(18);
		assertThat(((ByteArrayDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((ByteBufferDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((DataBufferDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((NettyByteBufDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((ResourceDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((StringDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((ProtobufDecoder) getNextDecoder(readers)).getMaxMessageSize()).isEqualTo(size);
		assertThat(((FormHttpMessageReader) nextReader(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((DefaultPartHttpMessageReader) nextReader(readers)).getMaxInMemorySize()).isEqualTo(size);
		nextReader(readers);
		assertThat(((PartEventHttpMessageReader) nextReader(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((KotlinSerializationCborDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((KotlinSerializationProtobufDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((JacksonJsonDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((JacksonSmileDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((Jaxb2XmlDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);

		ServerSentEventHttpMessageReader reader = (ServerSentEventHttpMessageReader) nextReader(readers);
		assertThat(reader.getMaxInMemorySize()).isEqualTo(size);
		assertThat(((JacksonJsonDecoder) reader.getDecoder()).getMaxInMemorySize()).isEqualTo(size);

		assertThat(((StringDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
	}

	@Test
	void enableLoggingRequestDetails() {
		this.configurer.defaultCodecs().enableLoggingRequestDetails(true);

		List<HttpMessageWriter<?>> writers = this.configurer.getWriters();
		MultipartHttpMessageWriter multipartWriter = findCodec(writers, MultipartHttpMessageWriter.class);
		assertThat(multipartWriter.isEnableLoggingRequestDetails()).isTrue();

		FormHttpMessageWriter formWriter = (FormHttpMessageWriter) multipartWriter.getFormWriter();
		assertThat(formWriter).isNotNull();
		assertThat(formWriter.isEnableLoggingRequestDetails()).isTrue();
	}

	@Test
	void clonedConfigurer() {
		ClientCodecConfigurer clone = this.configurer.clone();

		JacksonJsonDecoder jacksonDecoder = new JacksonJsonDecoder();
		clone.defaultCodecs().serverSentEventDecoder(jacksonDecoder);
		clone.defaultCodecs().multipartCodecs().encoder(new JacksonSmileEncoder());
		clone.defaultCodecs().multipartCodecs().writer(new ResourceHttpMessageWriter());

		// Clone has the customizations

		Decoder<?> sseDecoder = findCodec(clone.getReaders(), ServerSentEventHttpMessageReader.class).getDecoder();
		List<HttpMessageWriter<?>> writers = findCodec(clone.getWriters(), MultipartHttpMessageWriter.class).getPartWriters();

		assertThat(sseDecoder).isSameAs(jacksonDecoder);
		assertThat(writers).hasSize(2);

		// Original does not have the customizations

		sseDecoder = findCodec(this.configurer.getReaders(), ServerSentEventHttpMessageReader.class).getDecoder();
		writers = findCodec(this.configurer.getWriters(), MultipartHttpMessageWriter.class).getPartWriters();

		assertThat(sseDecoder).isNotSameAs(jacksonDecoder);
		assertThat(writers).hasSize(16);
	}

	@Test // gh-24194
	public void cloneShouldNotDropMultipartCodecs() {

		ClientCodecConfigurer clone = this.configurer.clone();
		List<HttpMessageWriter<?>> writers =
				findCodec(clone.getWriters(), MultipartHttpMessageWriter.class).getPartWriters();

		assertThat(writers).hasSize(16);
	}

	@Test
	void cloneShouldNotBeImpactedByChangesToOriginal() {

		ClientCodecConfigurer clone = this.configurer.clone();

		this.configurer.registerDefaults(false);
		this.configurer.customCodecs().register(new JacksonJsonEncoder());

		List<HttpMessageWriter<?>> writers =
				findCodec(clone.getWriters(), MultipartHttpMessageWriter.class).getPartWriters();

		assertThat(writers).hasSize(16);
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
		assertThat(decoder.getClass()).isEqualTo(JacksonJsonDecoder.class);
	}

}
