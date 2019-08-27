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
package org.springframework.messaging.rsocket;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.metadata.CompositeMetadata;
import io.rsocket.metadata.WellKnownMimeType;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class MetadataEncoderTests {

	private static MimeType COMPOSITE_METADATA =
			MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.getString());


	private final RSocketStrategies strategies = RSocketStrategies.create();


	@Test
	public void compositeMetadataWithRoute() {
		DataBuffer buffer = new MetadataEncoder(COMPOSITE_METADATA, this.strategies)
				.route("toA")
				.encode();

		CompositeMetadata entries = new CompositeMetadata(((NettyDataBuffer) buffer).getNativeBuffer(), false);
		Iterator<CompositeMetadata.Entry> iterator = entries.iterator();

		assertThat(iterator.hasNext()).isTrue();
		CompositeMetadata.Entry entry = iterator.next();
		assertThat(entry.getMimeType()).isEqualTo(WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.getString());
		assertThat(entry.getContent().toString(StandardCharsets.UTF_8)).isEqualTo("toA");

		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void compositeMetadataWithRouteAndText() {

		DataBuffer buffer = new MetadataEncoder(COMPOSITE_METADATA, this.strategies)
				.route("toA")
				.metadata("My metadata", MimeTypeUtils.TEXT_PLAIN)
				.encode();

		CompositeMetadata entries = new CompositeMetadata(((NettyDataBuffer) buffer).getNativeBuffer(), false);
		Iterator<CompositeMetadata.Entry> iterator = entries.iterator();

		assertThat(iterator.hasNext()).isTrue();
		CompositeMetadata.Entry entry = iterator.next();
		assertThat(entry.getMimeType()).isEqualTo(WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.getString());
		assertThat(entry.getContent().toString(StandardCharsets.UTF_8)).isEqualTo("toA");

		assertThat(iterator.hasNext()).isTrue();
		entry = iterator.next();
		assertThat(entry.getMimeType()).isEqualTo(MimeTypeUtils.TEXT_PLAIN.toString());
		assertThat(entry.getContent().toString(StandardCharsets.UTF_8)).isEqualTo("My metadata");

		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void routeWithRoutingMimeType() {
		MimeType metaMimeType = MimeTypeUtils.parseMimeType(
				WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.getString());

		DataBuffer buffer =
				new MetadataEncoder(metaMimeType, this.strategies)
						.route("toA")
						.encode();

		assertThat(dumpString(buffer)).isEqualTo("toA");
	}

	@Test
	public void routeWithTextPlainMimeType() {
		DataBuffer buffer =
				new MetadataEncoder(MimeTypeUtils.TEXT_PLAIN, this.strategies)
						.route("toA")
						.encode();

		assertThat(dumpString(buffer)).isEqualTo("toA");
	}

	@Test
	public void routeWithVars() {
		DataBuffer buffer =
				new MetadataEncoder(MimeTypeUtils.TEXT_PLAIN, this.strategies)
						.route("a.{b}.{c}", "BBB", "C.C.C")
						.encode();

		assertThat(dumpString(buffer)).isEqualTo("a.BBB.C%2EC%2EC");
	}

	@Test
	public void metadataWithTextPlainMimeType() {
		DataBuffer buffer =
				new MetadataEncoder(MimeTypeUtils.TEXT_PLAIN, this.strategies)
						.metadata("toA", null)
						.encode();

		assertThat(dumpString(buffer)).isEqualTo("toA");
	}

	@Test
	public void compositeRequiredForMultipleEntries() {

		// Route, metadata
		MetadataEncoder encoder1 = new MetadataEncoder(MimeTypeUtils.TEXT_PLAIN, this.strategies);
		encoder1.route("toA");

		assertThatThrownBy(() -> encoder1.metadata("My metadata", MimeTypeUtils.TEXT_PLAIN))
				.hasMessage("Composite metadata required for multiple metadata entries.");

		// Metadata, route
		MetadataEncoder encoder2 = new MetadataEncoder(MimeTypeUtils.TEXT_PLAIN, this.strategies);
		encoder2.metadata("My metadata", MimeTypeUtils.TEXT_PLAIN);

		assertThatThrownBy(() -> encoder2.route("toA"))
				.hasMessage("Composite metadata required for multiple metadata entries.");

		// Route and metadata
		MetadataEncoder encoder3 = new MetadataEncoder(MimeTypeUtils.TEXT_PLAIN, this.strategies);
		Map<Object, MimeType> metadata = Collections.singletonMap("My metadata", MimeTypeUtils.TEXT_PLAIN);

		assertThatThrownBy(() -> encoder3.metadataAndOrRoute(metadata, "toA", new Object[0]))
				.hasMessage("Composite metadata required for multiple metadata entries.");
	}

	@Test
	public void mimeTypeRequiredForCompositeEntries() {
		MetadataEncoder encoder = new MetadataEncoder(COMPOSITE_METADATA, this.strategies);

		assertThatThrownBy(() -> encoder.metadata("toA", null))
				.hasMessage("MimeType is required for composite metadata entries.");
	}

	@Test
	public void mimeTypeDoesNotMatchConnectionMetadataMimeType() {
		MetadataEncoder encoder = new MetadataEncoder(MimeTypeUtils.TEXT_PLAIN, this.strategies);

		assertThatThrownBy(() -> encoder.metadata("toA", MimeTypeUtils.APPLICATION_JSON))
				.hasMessage("Mime type is optional (may be null) " +
						"but was provided and does not match the connection metadata mime type.");
	}

	@Test
	public void defaultDataBufferFactory() {
		DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();
		RSocketStrategies strategies = RSocketStrategies.builder().dataBufferFactory(bufferFactory).build();

		DataBuffer buffer = new MetadataEncoder(COMPOSITE_METADATA, strategies)
				.route("toA")
				.encode();

		ByteBuf byteBuf = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT)
				.wrap(buffer.asByteBuffer())
				.getNativeBuffer();

		CompositeMetadata entries = new CompositeMetadata(byteBuf, false);
		Iterator<CompositeMetadata.Entry> iterator = entries.iterator();

		assertThat(iterator.hasNext()).isTrue();
		CompositeMetadata.Entry entry = iterator.next();
		assertThat(entry.getMimeType()).isEqualTo(WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.getString());
		assertThat(entry.getContent().toString(StandardCharsets.UTF_8)).isEqualTo("toA");

		assertThat(iterator.hasNext()).isFalse();
	}


	private String dumpString(DataBuffer buffer) {
		return DataBufferTestUtils.dumpString(buffer, StandardCharsets.UTF_8);
	}

}
