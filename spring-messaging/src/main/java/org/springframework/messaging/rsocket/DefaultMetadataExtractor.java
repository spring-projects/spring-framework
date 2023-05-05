/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.Payload;
import io.rsocket.metadata.CompositeMetadata;
import io.rsocket.metadata.RoutingMetadata;
import io.rsocket.metadata.WellKnownMimeType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

/**
 * Default {@link MetadataExtractor} implementation that relies on
 * {@link Decoder}s to deserialize the content of metadata entries.
 * <p>By default only {@code "message/x.rsocket.routing.v0"} is extracted and
 * saved under {@link MetadataExtractor#ROUTE_KEY}. Use {@code metadataToExtract}
 * methods to specify other metadata mime types of interest to extract.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class DefaultMetadataExtractor implements MetadataExtractor, MetadataExtractorRegistry {

	private static final Log logger = LogFactory.getLog(DefaultMetadataExtractor.class);


	private final List<Decoder<?>> decoders;

	private final Map<String, EntryExtractor<?>> registrations = new HashMap<>();


	/**
	 * Constructor with decoders for de-serializing metadata entries.
	 */
	public DefaultMetadataExtractor(Decoder<?>... decoders) {
		this(Arrays.asList(decoders));
	}

	/**
	 * Constructor with list of decoders for de-serializing metadata entries.
	 */
	public DefaultMetadataExtractor(List<Decoder<?>> decoders) {
		this.decoders = List.copyOf(decoders);
	}


	/**
	 * Return a read-only list with the configured decoders.
	 */
	public List<? extends Decoder<?>> getDecoders() {
		return this.decoders;
	}

	@Override
	public <T> void metadataToExtract(
			MimeType mimeType, Class<T> targetType, BiConsumer<T, Map<String, Object>> mapper) {

		registerMetadata(mimeType, ResolvableType.forClass(targetType), mapper);
	}

	@Override
	public <T> void metadataToExtract(
			MimeType mimeType, ParameterizedTypeReference<T> type, BiConsumer<T, Map<String, Object>> mapper) {

		registerMetadata(mimeType, ResolvableType.forType(type), mapper);
	}

	@SuppressWarnings("unchecked")
	private <T> void registerMetadata(
			MimeType mimeType, ResolvableType targetType, BiConsumer<T, Map<String, Object>> mapper) {

		for (Decoder<?> decoder : this.decoders) {
			if (decoder.canDecode(targetType, mimeType)) {
				this.registrations.put(mimeType.toString(),
						new EntryExtractor<>((Decoder<T>) decoder, mimeType, targetType, mapper));
				return;
			}
		}
		throw new IllegalArgumentException("No decoder for " + mimeType + " and " + targetType);
	}


	@Override
	public Map<String, Object> extract(Payload payload, MimeType metadataMimeType) {
		Map<String, Object> result = new HashMap<>();
		if (metadataMimeType.toString().equals(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.toString())) {
			for (CompositeMetadata.Entry entry : new CompositeMetadata(payload.metadata(), false)) {
				extractEntry(entry.getContent(), entry.getMimeType(), result);
			}
		}
		else {
			extractEntry(payload.metadata().slice(), metadataMimeType.toString(), result);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Values extracted from metadata: " + result +
					" with registrations for " + this.registrations.keySet() + ".");
		}
		return result;
	}

	private void extractEntry(ByteBuf content, @Nullable String mimeType, Map<String, Object> result) {
		if (content.readableBytes() == 0) {
			return;
		}
		EntryExtractor<?> extractor = this.registrations.get(mimeType);
		if (extractor != null) {
			extractor.extract(content, result);
			return;
		}
		if (mimeType != null && mimeType.equals(WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.getString())) {
			Iterator<String> iterator = new RoutingMetadata(content).iterator();
			if (iterator.hasNext()) {
				result.put(MetadataExtractor.ROUTE_KEY, iterator.next());
			}
		}
	}


	private static class EntryExtractor<T> {

		// We only need this to wrap ByteBufs
		private final static NettyDataBufferFactory bufferFactory =
				new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);


		private final Decoder<T> decoder;

		private final MimeType mimeType;

		private final ResolvableType targetType;

		private final BiConsumer<T, Map<String, Object>> accumulator;


		EntryExtractor(Decoder<T> decoder, MimeType mimeType, ResolvableType targetType,
				BiConsumer<T, Map<String, Object>> accumulator) {

			this.decoder = decoder;
			this.mimeType = mimeType;
			this.targetType = targetType;
			this.accumulator = accumulator;
		}


		public void extract(ByteBuf content, Map<String, Object> result) {
			NettyDataBuffer dataBuffer = bufferFactory.wrap(content.retain());
			T value = this.decoder.decode(dataBuffer, this.targetType, this.mimeType, Collections.emptyMap());
			this.accumulator.accept(value, result);
		}


		@Override
		public String toString() {
			return "\"" + this.mimeType + "\" => " + this.targetType;
		}
	}

}
