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
package org.springframework.messaging.rsocket.annotation.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import io.netty.buffer.ByteBuf;
import io.rsocket.Payload;
import io.rsocket.metadata.CompositeMetadata;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.rsocket.RSocketStrategies;
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

	private final RSocketStrategies rsocketStrategies;

	private final Map<String, EntryProcessor<?>> entryProcessors = new HashMap<>();


	/**
	 * Default constructor with {@link RSocketStrategies}.
	 */
	public DefaultMetadataExtractor(RSocketStrategies strategies) {
		Assert.notNull(strategies, "RSocketStrategies is required");
		this.rsocketStrategies = strategies;
		// TODO: remove when rsocket-core API available
		metadataToExtract(MessagingRSocket.ROUTING, String.class, ROUTE_KEY);
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
			MimeType mimeType, Class<T> targetType,
			BiConsumer<T, Map<String, Object>> mapper) {

		EntryProcessor<T> spec = new EntryProcessor<>(mimeType, targetType, mapper);
		this.entryProcessors.put(mimeType.toString(), spec);
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

		EntryProcessor<T> spec = new EntryProcessor<>(mimeType, targetType, mapper);
		this.entryProcessors.put(mimeType.toString(), spec);
	}


	@Override
	public Map<String, Object> extract(Payload payload, MimeType metadataMimeType) {
		Map<String, Object> result = new HashMap<>();
		if (metadataMimeType.equals(MessagingRSocket.COMPOSITE_METADATA)) {
			for (CompositeMetadata.Entry entry : new CompositeMetadata(payload.metadata(), false)) {
				processEntry(entry.getContent(), entry.getMimeType(), result);
			}
		}
		else {
			processEntry(payload.metadata(), metadataMimeType.toString(), result);
		}
		return result;
	}

	private void processEntry(ByteBuf content, @Nullable String mimeType, Map<String, Object> result) {
		EntryProcessor<?> entryProcessor = this.entryProcessors.get(mimeType);
		if (entryProcessor != null) {
			content.retain();
			entryProcessor.process(content, result);
			return;
		}
		if (MessagingRSocket.ROUTING.toString().equals(mimeType)) {
			// TODO: use rsocket-core API when available
		}
	}


	/**
	 * Helps to decode a metadata entry and add the resulting value to the
	 * output map.
	 */
	private class EntryProcessor<T> {

		private final MimeType mimeType;

		private final ResolvableType targetType;

		private final BiConsumer<T, Map<String, Object>> accumulator;

		private final Decoder<T> decoder;


		public EntryProcessor(
				MimeType mimeType, Class<T> targetType,
				BiConsumer<T, Map<String, Object>> accumulator) {

			this(mimeType, ResolvableType.forClass(targetType), accumulator);
		}

		public EntryProcessor(
				MimeType mimeType, ParameterizedTypeReference<T> targetType,
				BiConsumer<T, Map<String, Object>> accumulator) {

			this(mimeType, ResolvableType.forType(targetType), accumulator);
		}

		private EntryProcessor(
				MimeType mimeType, ResolvableType targetType,
				BiConsumer<T, Map<String, Object>> accumulator) {

			this.mimeType = mimeType;
			this.targetType = targetType;
			this.accumulator = accumulator;
			this.decoder = rsocketStrategies.decoder(targetType, mimeType);
		}


		public void process(ByteBuf byteBuf, Map<String, Object> result) {
			DataBufferFactory factory = rsocketStrategies.dataBufferFactory();
			DataBuffer buffer =  factory instanceof NettyDataBufferFactory ?
					((NettyDataBufferFactory) factory).wrap(byteBuf) :
					factory.wrap(byteBuf.nioBuffer());

			T value = this.decoder.decode(buffer, this.targetType, this.mimeType, Collections.emptyMap());
			this.accumulator.accept(value, result);
		}
	}

}
