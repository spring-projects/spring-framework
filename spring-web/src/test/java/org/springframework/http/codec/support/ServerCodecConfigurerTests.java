/*
 * Copyright 2002-2019 the original author or authors.
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
import org.springframework.core.codec.ResourceDecoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.FormHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageReader;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.ServerSentEventHttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.json.Jackson2SmileDecoder;
import org.springframework.http.codec.json.Jackson2SmileEncoder;
import org.springframework.http.codec.multipart.MultipartHttpMessageReader;
import org.springframework.http.codec.multipart.SynchronossPartHttpMessageReader;
import org.springframework.http.codec.protobuf.ProtobufDecoder;
import org.springframework.http.codec.protobuf.ProtobufHttpMessageWriter;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.ResolvableType.forClass;

/**
 * Unit tests for {@link ServerCodecConfigurer}.
 *
 * @author Rossen Stoyanchev
 */
public class ServerCodecConfigurerTests {

	private final ServerCodecConfigurer configurer = new DefaultServerCodecConfigurer();

	private final AtomicInteger index = new AtomicInteger(0);


	@Test
	public void defaultReaders() {
		List<HttpMessageReader<?>> readers = this.configurer.getReaders();
		assertThat(readers.size()).isEqualTo(13);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(ByteArrayDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(ByteBufferDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(DataBufferDecoder.class);
		assertThat(readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(ResourceHttpMessageReader.class);
		assertStringDecoder(getNextDecoder(readers), true);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(ProtobufDecoder.class);
		assertThat(readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(FormHttpMessageReader.class);
		assertThat(readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(SynchronossPartHttpMessageReader.class);
		assertThat(readers.get(this.index.getAndIncrement()).getClass()).isEqualTo(MultipartHttpMessageReader.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(Jackson2JsonDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(Jackson2SmileDecoder.class);
		assertThat(getNextDecoder(readers).getClass()).isEqualTo(Jaxb2XmlDecoder.class);
		assertStringDecoder(getNextDecoder(readers), false);
	}

	@Test
	public void defaultWriters() {
		List<HttpMessageWriter<?>> writers = this.configurer.getWriters();
		assertThat(writers.size()).isEqualTo(11);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(ByteArrayEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(ByteBufferEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(DataBufferEncoder.class);
		assertThat(writers.get(index.getAndIncrement()).getClass()).isEqualTo(ResourceHttpMessageWriter.class);
		assertStringEncoder(getNextEncoder(writers), true);
		assertThat(writers.get(index.getAndIncrement()).getClass()).isEqualTo(ProtobufHttpMessageWriter.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(Jackson2JsonEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(Jackson2SmileEncoder.class);
		assertThat(getNextEncoder(writers).getClass()).isEqualTo(Jaxb2XmlEncoder.class);
		assertSseWriter(writers);
		assertStringEncoder(getNextEncoder(writers), false);
	}

	@Test
	public void jackson2EncoderOverride() {
		Jackson2JsonEncoder encoder = new Jackson2JsonEncoder();
		this.configurer.defaultCodecs().jackson2JsonEncoder(encoder);

		assertThat(this.configurer.getWriters().stream()
				.filter(writer -> ServerSentEventHttpMessageWriter.class.equals(writer.getClass()))
				.map(writer -> (ServerSentEventHttpMessageWriter) writer)
				.findFirst()
				.map(ServerSentEventHttpMessageWriter::getEncoder)
				.filter(e -> e == encoder).orElse(null)).isSameAs(encoder);
	}

	@Test
	public void maxInMemorySize() {
		int size = 99;
		this.configurer.defaultCodecs().maxInMemorySize(size);

		List<HttpMessageReader<?>> readers = this.configurer.getReaders();
		assertThat(((ByteArrayDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((ByteBufferDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((DataBufferDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((ResourceDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((StringDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((ProtobufDecoder) getNextDecoder(readers)).getMaxMessageSize()).isEqualTo(size);
		assertThat(((FormHttpMessageReader) nextReader(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((SynchronossPartHttpMessageReader) nextReader(readers)).getMaxInMemorySize()).isEqualTo(size);

		MultipartHttpMessageReader multipartReader = (MultipartHttpMessageReader) nextReader(readers);
		SynchronossPartHttpMessageReader reader = (SynchronossPartHttpMessageReader) multipartReader.getPartReader();
		assertThat((reader).getMaxInMemorySize()).isEqualTo(size);

		assertThat(((Jackson2JsonDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((Jackson2SmileDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((Jaxb2XmlDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((StringDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
	}

	@Test
	public void maxInMemorySizeWithCustomCodecs() {

		int size = 99;
		this.configurer.defaultCodecs().maxInMemorySize(size);
		this.configurer.registerDefaults(false);

		CodecConfigurer.CustomCodecs customCodecs = this.configurer.customCodecs();
		customCodecs.register(new ByteArrayDecoder());
		customCodecs.registerWithDefaultConfig(new ByteArrayDecoder());
		customCodecs.register(new Jackson2JsonDecoder());
		customCodecs.registerWithDefaultConfig(new Jackson2JsonDecoder());

		this.configurer.defaultCodecs().enableLoggingRequestDetails(true);

		List<HttpMessageReader<?>> readers = this.configurer.getReaders();
		assertThat(((ByteArrayDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(256 * 1024);
		assertThat(((ByteArrayDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
		assertThat(((Jackson2JsonDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(256 * 1024);
		assertThat(((Jackson2JsonDecoder) getNextDecoder(readers)).getMaxInMemorySize()).isEqualTo(size);
	}

	@Test
	public void enableRequestLoggingDetails() {
		this.configurer.defaultCodecs().enableLoggingRequestDetails(true);

		List<HttpMessageReader<?>> readers = this.configurer.getReaders();
		assertThat(findCodec(readers, FormHttpMessageReader.class).isEnableLoggingRequestDetails()).isTrue();

		MultipartHttpMessageReader multipartReader = findCodec(readers, MultipartHttpMessageReader.class);
		assertThat(multipartReader.isEnableLoggingRequestDetails()).isTrue();

		SynchronossPartHttpMessageReader reader = (SynchronossPartHttpMessageReader) multipartReader.getPartReader();
		assertThat(reader.isEnableLoggingRequestDetails()).isTrue();
	}

	@Test
	public void enableRequestLoggingDetailsWithCustomCodecs() {

		this.configurer.registerDefaults(false);
		this.configurer.defaultCodecs().enableLoggingRequestDetails(true);

		CodecConfigurer.CustomCodecs customCodecs = this.configurer.customCodecs();
		customCodecs.register(new FormHttpMessageReader());
		customCodecs.registerWithDefaultConfig(new FormHttpMessageReader());

		List<HttpMessageReader<?>> readers = this.configurer.getReaders();
		assertThat(((FormHttpMessageReader) readers.get(0)).isEnableLoggingRequestDetails()).isFalse();
		assertThat(((FormHttpMessageReader) readers.get(1)).isEnableLoggingRequestDetails()).isTrue();
	}

	@Test
	public void cloneConfigurer() {
		ServerCodecConfigurer clone = this.configurer.clone();

		MultipartHttpMessageReader reader = new MultipartHttpMessageReader(new SynchronossPartHttpMessageReader());
		Jackson2JsonEncoder encoder = new Jackson2JsonEncoder();
		clone.defaultCodecs().multipartReader(reader);
		clone.defaultCodecs().serverSentEventEncoder(encoder);

		// Clone has the customizations

		HttpMessageReader<?> actualReader =
				findCodec(clone.getReaders(), MultipartHttpMessageReader.class);

		ServerSentEventHttpMessageWriter actualWriter =
				findCodec(clone.getWriters(), ServerSentEventHttpMessageWriter.class);

		assertThat(actualReader).isSameAs(reader);
		assertThat(actualWriter.getEncoder()).isSameAs(encoder);

		// Original does not have the customizations

		actualReader = findCodec(this.configurer.getReaders(), MultipartHttpMessageReader.class);
		actualWriter = findCodec(this.configurer.getWriters(), ServerSentEventHttpMessageWriter.class);

		assertThat(actualReader).isNotSameAs(reader);
		assertThat(actualWriter.getEncoder()).isNotSameAs(encoder);
	}

	private Decoder<?> getNextDecoder(List<HttpMessageReader<?>> readers) {
		HttpMessageReader<?> reader = nextReader(readers);
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
		return (T) codecs.stream().filter(type::isInstance).findFirst().get();
	}

	@SuppressWarnings("unchecked")
	private void assertStringDecoder(Decoder<?> decoder, boolean textOnly) {
		assertThat(decoder.getClass()).isEqualTo(StringDecoder.class);
		assertThat(decoder.canDecode(forClass(String.class), MimeTypeUtils.TEXT_PLAIN)).isTrue();
		Object expected = !textOnly;
		assertThat(decoder.canDecode(forClass(String.class), MediaType.TEXT_EVENT_STREAM)).isEqualTo(expected);

		Flux<String> flux = (Flux<String>) decoder.decode(
				Flux.just(new DefaultDataBufferFactory().wrap("line1\nline2".getBytes(StandardCharsets.UTF_8))),
				ResolvableType.forClass(String.class), MimeTypeUtils.TEXT_PLAIN, Collections.emptyMap());

		assertThat(flux.collectList().block(Duration.ZERO)).isEqualTo(Arrays.asList("line1", "line2"));
	}

	private void assertStringEncoder(Encoder<?> encoder, boolean textOnly) {
		assertThat(encoder.getClass()).isEqualTo(CharSequenceEncoder.class);
		assertThat(encoder.canEncode(forClass(String.class), MimeTypeUtils.TEXT_PLAIN)).isTrue();
		Object expected = !textOnly;
		assertThat(encoder.canEncode(forClass(String.class), MediaType.TEXT_EVENT_STREAM)).isEqualTo(expected);
	}

	private void assertSseWriter(List<HttpMessageWriter<?>> writers) {
		HttpMessageWriter<?> writer = writers.get(this.index.getAndIncrement());
		assertThat(writer.getClass()).isEqualTo(ServerSentEventHttpMessageWriter.class);
		Encoder<?> encoder = ((ServerSentEventHttpMessageWriter) writer).getEncoder();
		assertThat(encoder).isNotNull();
		assertThat(encoder.getClass()).isEqualTo(Jackson2JsonEncoder.class);
	}

}
