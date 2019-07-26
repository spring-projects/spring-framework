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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.rsocket.Payload;
import io.rsocket.metadata.CompositeMetadata;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Default {@link MetadataExtractor} implementation that relies on {@link Decoder}s
 * to deserialize the content of metadata entries.
 *
 * <p>By default only {@code "message/x.rsocket.routing.v0""} is extracted and
 * saved under {@link MetadataExtractor#ROUTE_KEY}. Use the
 * {@code metadataToExtract} methods to specify other metadata mime types of
 * interest to extract.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class DefaultMetadataExtractor implements MetadataExtractor {

	private final List<Decoder<?>> decoders = new ArrayList<>();

	private final Map<String, MetadataProcessor<?>> processors = new HashMap<>();


	/**
	 * Configure the decoders to use for de-serializing metadata entries.
	 * <p>By default this is not set.
	 * <p>When this extractor is passed into {@link RSocketStrategies.Builder} or
	 * {@link org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler
	 * RSocketMessageHandler}, the decoders may be left not set, and they will
	 * be initialized from the decoders already configured there.
	 */
	public void setDecoders(List<? extends Decoder<?>> decoders) {
		this.decoders.clear();
		if (!decoders.isEmpty()) {
			this.decoders.addAll(decoders);
			updateProcessors();
		}
	}

	@SuppressWarnings("unchecked")
	private <T> void updateProcessors() {
		for (MetadataProcessor<?> info : this.processors.values()) {
			Decoder<T> decoder = decoderFor(info.mimeType(), info.targetType());
			Assert.isTrue(decoder != null, "No decoder for " + info);
			info = ((MetadataProcessor<T>) info).setDecoder(decoder);
			this.processors.put(info.mimeType().toString(), info);
		}
	}

	@Nullable
	@SuppressWarnings("unchecked")
	private <T> Decoder<T> decoderFor(MimeType mimeType, ResolvableType type) {
		for (Decoder<?> decoder : this.decoders) {
			if (decoder.canDecode(type, mimeType)) {
				return (Decoder<T>) decoder;
			}
		}
		return null;
	}

	/**
	 * Return the {@link #setDecoders(List) configured} decoders.
	 */
	public List<? extends Decoder<?>> getDecoders() {
		return this.decoders;
	}


	/**
	 * Decode metadata entries with the given {@link MimeType} to the specified
	 * target class, and store the decoded value in the output map under the
	 * given name.
	 * @param mimeType the mime type of metadata entries to extract
	 * @param targetType the target value type to decode to
	 * @param name assign a name for the decoded value; if not provided, then
	 * the mime type is used as the key
	 */
	public void metadataToExtract(
			MimeType mimeType, Class<?> targetType, @Nullable String name) {

		String key = name != null ? name : mimeType.toString();
		metadataToExtract(mimeType, targetType, (value, map) -> map.put(key, value));
	}

	/**
	 * Variant of {@link #metadataToExtract(MimeType, Class, String)} that accepts
	 * {@link ParameterizedTypeReference} instead of {@link Class} for
	 * specifying a target type with generic parameters.
	 */
	public void metadataToExtract(
			MimeType mimeType, ParameterizedTypeReference<?> targetType, @Nullable String name) {

		String key = name != null ? name : mimeType.toString();
		metadataToExtract(mimeType, targetType, (value, map) -> map.put(key, value));
	}

	/**
	 * Variant of {@link #metadataToExtract(MimeType, Class, String)} that allows
	 * custom logic to be used to map the decoded value to any number of values
	 * in the output map.
	 * @param mimeType the mime type of metadata entries to extract
	 * @param targetType the target value type to decode to
	 * @param mapper custom logic to add the decoded value to the output map
	 * @param <T> the target value type
	 */
	public <T> void metadataToExtract(
			MimeType mimeType, Class<T> targetType, BiConsumer<T, Map<String, Object>> mapper) {

		metadataToExtract(mimeType, mapper, ResolvableType.forClass(targetType));
	}

	/**
	 * Variant of {@link #metadataToExtract(MimeType, Class, BiConsumer)} that
	 * accepts {@link ParameterizedTypeReference} instead of {@link Class} for
	 * specifying a target type with generic parameters.
	 * @param mimeType the mime type of metadata entries to extract
	 * @param targetType the target value type to decode to
	 * @param mapper custom logic to add the decoded value to the output map
	 * @param <T> the target value type
	 */
	public <T> void metadataToExtract(
			MimeType mimeType, ParameterizedTypeReference<T> targetType,
			BiConsumer<T, Map<String, Object>> mapper) {

		metadataToExtract(mimeType, mapper, ResolvableType.forType(targetType));
	}

	private <T> void metadataToExtract(
			MimeType mimeType, BiConsumer<T, Map<String, Object>> mapper, ResolvableType elementType) {

		Decoder<T> decoder = decoderFor(mimeType, elementType);
		Assert.isTrue(this.decoders.isEmpty() || decoder != null, () -> "No decoder for " + mimeType);
		MetadataProcessor<T> info = new MetadataProcessor<>(mimeType, elementType, mapper, decoder);
		this.processors.put(mimeType.toString(), info);
	}


	@Override
	public Map<String, Object> extract(Payload payload, MimeType metadataMimeType) {
		Map<String, Object> result = new HashMap<>();
		if (metadataMimeType.equals(COMPOSITE_METADATA)) {
			for (CompositeMetadata.Entry entry : new CompositeMetadata(payload.metadata(), false)) {
				processEntry(entry.getContent(), entry.getMimeType(), result);
			}
		}
		else {
			processEntry(payload.metadata(), metadataMimeType.toString(), result);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private <T> void processEntry(ByteBuf content, @Nullable String mimeType, Map<String, Object> result) {
		MetadataProcessor<T> info = (MetadataProcessor<T>) this.processors.get(mimeType);
		if (info != null) {
			info.process(content, result);
			return;
		}
		if (MetadataExtractor.ROUTING.toString().equals(mimeType)) {
			// TODO: use rsocket-core API when available
			result.put(MetadataExtractor.ROUTE_KEY, content.toString(StandardCharsets.UTF_8));
		}
	}


	private static class MetadataProcessor<T> {

		private final static NettyDataBufferFactory bufferFactory =
				new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT);


		private final MimeType mimeType;

		private final ResolvableType targetType;

		private final BiConsumer<T, Map<String, Object>> accumulator;

		@Nullable
		private final Decoder<T> decoder;


		MetadataProcessor(MimeType mimeType, ResolvableType targetType,
				BiConsumer<T, Map<String, Object>> accumulator, @Nullable Decoder<T> decoder) {

			this.mimeType = mimeType;
			this.targetType = targetType;
			this.accumulator = accumulator;
			this.decoder = decoder;
		}

		MetadataProcessor(MetadataProcessor<T> other, Decoder<T> decoder) {
			this.mimeType = other.mimeType;
			this.targetType = other.targetType;
			this.accumulator = other.accumulator;
			this.decoder = decoder;
		}


		public MimeType mimeType() {
			return this.mimeType;
		}

		public ResolvableType targetType() {
			return this.targetType;
		}

		public MetadataProcessor<T> setDecoder(Decoder<T> decoder) {
			return this.decoder != decoder ? new MetadataProcessor<>(this, decoder) : this;
		}


		public void process(ByteBuf content, Map<String, Object> result) {
			if (this.decoder == null) {
				throw new IllegalStateException("No decoder for " + this);
			}
			NettyDataBuffer dataBuffer = bufferFactory.wrap(content.retain());
			T value = this.decoder.decode(dataBuffer, this.targetType, this.mimeType, Collections.emptyMap());
			this.accumulator.accept(value, result);
		}


		@Override
		public String toString() {
			return "MetadataProcessor mimeType=" + this.mimeType + ", targetType=" + this.targetType;
		}
	}

}
