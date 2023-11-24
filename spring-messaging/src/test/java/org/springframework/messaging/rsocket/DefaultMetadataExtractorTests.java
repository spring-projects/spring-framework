/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.messaging.rsocket;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import io.netty.buffer.PooledByteBufAllocator;
import io.rsocket.Payload;
import io.rsocket.metadata.WellKnownMimeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDataBufferDecoder;
import org.springframework.core.codec.ByteArrayDecoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.messaging.rsocket.MetadataExtractor.ROUTE_KEY;
import static org.springframework.util.MimeTypeUtils.TEXT_HTML;
import static org.springframework.util.MimeTypeUtils.TEXT_PLAIN;
import static org.springframework.util.MimeTypeUtils.TEXT_XML;

/**
 * Unit tests for {@link DefaultMetadataExtractor}.
 * @author Rossen Stoyanchev
 */
class DefaultMetadataExtractorTests {

	private static MimeType COMPOSITE_METADATA =
			MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.getString());


	private RSocketStrategies strategies;

	private DefaultMetadataExtractor extractor;


	@BeforeEach
	void setUp() {
		DataBufferFactory bufferFactory = new LeakAwareNettyDataBufferFactory(PooledByteBufAllocator.DEFAULT);
		this.strategies = RSocketStrategies.builder().dataBufferFactory(bufferFactory).build();
		this.extractor = new DefaultMetadataExtractor(StringDecoder.allMimeTypes());
	}

	@AfterEach
	void tearDown() throws InterruptedException {
		DataBufferFactory bufferFactory = this.strategies.dataBufferFactory();
		((LeakAwareNettyDataBufferFactory) bufferFactory).checkForLeaks(Duration.ofSeconds(5));
	}


	@Test
	void compositeMetadataWithDefaultSettings() {
		MetadataEncoder metadataEncoder = new MetadataEncoder(COMPOSITE_METADATA, this.strategies)
				.route("toA")
				.metadata("text data", TEXT_PLAIN)
				.metadata("html data", TEXT_HTML)
				.metadata("xml data", TEXT_XML);

		DataBuffer metadata = metadataEncoder.encode().block();
		Payload payload = createPayload(metadata);
		Map<String, Object> result = this.extractor.extract(payload, COMPOSITE_METADATA);
		payload.release();

		assertThat(result).hasSize(1).containsEntry(ROUTE_KEY, "toA");
	}

	@Test
	void compositeMetadataWithMimeTypeRegistrations() {
		this.extractor.metadataToExtract(TEXT_PLAIN, String.class, "text-entry");
		this.extractor.metadataToExtract(TEXT_HTML, String.class, "html-entry");
		this.extractor.metadataToExtract(TEXT_XML, String.class, "xml-entry");

		MetadataEncoder metadataEncoder = new MetadataEncoder(COMPOSITE_METADATA, this.strategies)
				.route("toA")
				.metadata("text data", TEXT_PLAIN)
				.metadata("html data", TEXT_HTML)
				.metadata("xml data", TEXT_XML);

		DataBuffer metadata = metadataEncoder.encode().block();
		Payload payload = createPayload(metadata);
		Map<String, Object> result = this.extractor.extract(payload, COMPOSITE_METADATA);
		payload.release();

		assertThat(result).hasSize(4)
				.containsEntry(ROUTE_KEY, "toA")
				.containsEntry("text-entry", "text data")
				.containsEntry("html-entry", "html data")
				.containsEntry("xml-entry", "xml data");
	}

	@Test
	void route() {
		MimeType metaMimeType = MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.getString());
		MetadataEncoder metadataEncoder = new MetadataEncoder(metaMimeType, this.strategies).route("toA");
		DataBuffer metadata = metadataEncoder.encode().block();
		Payload payload = createPayload(metadata);
		Map<String, Object> result = this.extractor.extract(payload, metaMimeType);
		payload.release();

		assertThat(result).hasSize(1).containsEntry(ROUTE_KEY, "toA");
	}

	@Test
	void routeAsText() {
		this.extractor.metadataToExtract(TEXT_PLAIN, String.class, ROUTE_KEY);

		MetadataEncoder metadataEncoder = new MetadataEncoder(TEXT_PLAIN, this.strategies).route("toA");
		DataBuffer metadata = metadataEncoder.encode().block();
		Payload payload = createPayload(metadata);
		Map<String, Object> result = this.extractor.extract(payload, TEXT_PLAIN);
		payload.release();

		assertThat(result).hasSize(1).containsEntry(ROUTE_KEY, "toA");
	}

	@Test
	void routeWithCustomFormatting() {
		this.extractor.metadataToExtract(TEXT_PLAIN, String.class, (text, result) -> {
			String[] items = text.split(":");
			Assert.isTrue(items.length == 2, "Expected two items");
			result.put(ROUTE_KEY, items[0]);
			result.put("entry1", items[1]);
		});

		MetadataEncoder encoder = new MetadataEncoder(TEXT_PLAIN, this.strategies).metadata("toA:text data", null);
		DataBuffer metadata = encoder.encode().block();
		Payload payload = createPayload(metadata);
		Map<String, Object> result = this.extractor.extract(payload, TEXT_PLAIN);
		payload.release();

		assertThat(result).hasSize(2)
				.containsEntry(ROUTE_KEY, "toA")
				.containsEntry("entry1", "text data");
	}

	@Test
	void nonCompositeMetadataCanBeReadTwice() {
		DefaultMetadataExtractor extractor = new DefaultMetadataExtractor(new TestDecoder());
		extractor.metadataToExtract(TEXT_PLAIN, String.class, "name");

		MetadataEncoder encoder = new MetadataEncoder(TEXT_PLAIN, this.strategies).metadata("value", null);
		DataBuffer metadata = encoder.encode().block();
		Payload payload = createPayload(metadata);

		Map<String, Object> result = extractor.extract(payload, TEXT_PLAIN);
		assertThat(result).hasSize(1).containsEntry("name", "value");

		result = extractor.extract(payload, TEXT_PLAIN);
		assertThat(result).hasSize(1).containsEntry("name", "value");

		payload.release();
	}

	@Test
	void noDecoder() {
		DefaultMetadataExtractor extractor =
				new DefaultMetadataExtractor(Collections.singletonList(new ByteArrayDecoder())
		);

		assertThatIllegalArgumentException()
				.isThrownBy(() -> extractor.metadataToExtract(TEXT_PLAIN, String.class, "key"))
				.withMessage("No decoder for text/plain and java.lang.String");
	}


	private Payload createPayload(DataBuffer metadata) {
		return PayloadUtils.createPayload(this.strategies.dataBufferFactory().allocateBuffer(256), metadata);
	}


	/**
	 * Like StringDecoder but consumes the reader index in order to prove that
	 * extraction uses a slice and can be read twice.
	 */
	private static class TestDecoder extends AbstractDataBufferDecoder<String> {

		TestDecoder() {
			super(TEXT_PLAIN);
		}

		@Override
		public String decode(DataBuffer dataBuffer, ResolvableType elementType,
				@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

			byte[] bytes = new byte[dataBuffer.readableByteCount()];
			dataBuffer.read(bytes);
			DataBufferUtils.release(dataBuffer);
			return new String(bytes, StandardCharsets.UTF_8);
		}
	}

}
