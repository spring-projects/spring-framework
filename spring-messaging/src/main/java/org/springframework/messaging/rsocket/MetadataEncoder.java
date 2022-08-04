/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.rsocket.metadata.WellKnownMimeType;
import reactor.core.publisher.Mono;

import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.util.ObjectUtils;

/**
 * Helps to collect metadata values and mime types, and encode them.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
final class MetadataEncoder {

	/** For route variable replacement. */
	private static final Pattern VARS_PATTERN = Pattern.compile("\\{(.+?)}");

	private static final Object NO_VALUE = new Object();


	private final MimeType metadataMimeType;

	private final RSocketStrategies strategies;

	private final boolean isComposite;

	private final ByteBufAllocator allocator;

	@Nullable
	private String route;

	private final List<MetadataEntry> metadataEntries = new ArrayList<>(4);

	private boolean hasAsyncValues;


	MetadataEncoder(MimeType metadataMimeType, RSocketStrategies strategies) {
		Assert.notNull(metadataMimeType, "'metadataMimeType' is required");
		Assert.notNull(strategies, "RSocketStrategies is required");
		this.metadataMimeType = metadataMimeType;
		this.strategies = strategies;
		this.isComposite = this.metadataMimeType.toString().equals(
				WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.getString());
		this.allocator = bufferFactory() instanceof NettyDataBufferFactory ?
				((NettyDataBufferFactory) bufferFactory()).getByteBufAllocator() : ByteBufAllocator.DEFAULT;
	}


	private DataBufferFactory bufferFactory() {
		return this.strategies.dataBufferFactory();
	}


	/**
	 * Set the route to a remote handler as described in
	 * {@link RSocketRequester#route(String, Object...)}.
	 */
	public MetadataEncoder route(String route, Object... routeVars) {
		this.route = expand(route, routeVars);
		assertMetadataEntryCount();
		return this;
	}

	private static String expand(String route, Object... routeVars) {
		if (ObjectUtils.isEmpty(routeVars)) {
			return route;
		}
		StringBuffer sb = new StringBuffer();
		int index = 0;
		Matcher matcher = VARS_PATTERN.matcher(route);
		while (matcher.find()) {
			Assert.isTrue(index < routeVars.length, () -> "No value for variable '" + matcher.group(1) + "'");
			String value = routeVars[index].toString();
			value = value.contains(".") ? value.replaceAll("\\.", "%2E") : value;
			matcher.appendReplacement(sb, value);
			index++;
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	private void assertMetadataEntryCount() {
		if (!this.isComposite) {
			int count = this.route != null ? this.metadataEntries.size() + 1 : this.metadataEntries.size();
			Assert.isTrue(count < 2, "Composite metadata required for multiple metadata entries.");
		}
	}

	/**
	 * Add a metadata entry. If called more than once or in addition to route,
	 * composite metadata must be in use.
	 */
	public MetadataEncoder metadata(Object metadata, @Nullable MimeType mimeType) {
		if (this.isComposite) {
			Assert.notNull(mimeType, "MimeType is required for composite metadata entries.");
		}
		else if (mimeType == null) {
			mimeType = this.metadataMimeType;
		}
		else if (!this.metadataMimeType.equals(mimeType)) {
			throw new IllegalArgumentException(
					"Mime type is optional when not using composite metadata, but it was provided " +
							"and does not match the connection metadata mime type '" + this.metadataMimeType + "'.");
		}
		ReactiveAdapter adapter = this.strategies.reactiveAdapterRegistry().getAdapter(metadata.getClass());
		if (adapter != null) {
			Assert.isTrue(!adapter.isMultiValue(), "Expected single value: " + metadata);
			metadata = Mono.from(adapter.toPublisher(metadata)).defaultIfEmpty(NO_VALUE);
			this.hasAsyncValues = true;
		}
		this.metadataEntries.add(new MetadataEntry(metadata, mimeType));
		assertMetadataEntryCount();
		return this;
	}

	/**
	 * Add route and/or metadata, both optional.
	 */
	public MetadataEncoder metadataAndOrRoute(@Nullable Map<Object, MimeType> metadata,
			@Nullable String route, @Nullable Object[] vars) {

		if (route != null) {
			this.route = expand(route, vars != null ? vars : new Object[0]);
		}
		if (!CollectionUtils.isEmpty(metadata)) {
			for (Map.Entry<Object, MimeType> entry : metadata.entrySet()) {
				metadata(entry.getKey(), entry.getValue());
			}
		}
		assertMetadataEntryCount();
		return this;
	}


	/**
	 * Encode the collected metadata entries to a {@code DataBuffer}.
	 * @see PayloadUtils#createPayload(DataBuffer, DataBuffer)
	 */
	public Mono<DataBuffer> encode() {
		return this.hasAsyncValues ?
				resolveAsyncMetadata().map(this::encodeEntries) :
				Mono.fromCallable(() -> encodeEntries(this.metadataEntries));
	}

	@SuppressWarnings("deprecation")
	private DataBuffer encodeEntries(List<MetadataEntry> entries) {
		if (this.isComposite) {
			CompositeByteBuf composite = this.allocator.compositeBuffer();
			try {
				if (this.route != null) {
					io.rsocket.metadata.CompositeMetadataCodec.encodeAndAddMetadata(composite, this.allocator,
							WellKnownMimeType.MESSAGE_RSOCKET_ROUTING, encodeRoute());
				}
				entries.forEach(entry -> {
					Object value = entry.value();
					io.rsocket.metadata.CompositeMetadataCodec.encodeAndAddMetadata(
							composite, this.allocator, entry.mimeType().toString(),
							value instanceof ByteBuf ? (ByteBuf) value : PayloadUtils.asByteBuf(encodeEntry(entry)));
				});
				return asDataBuffer(composite);
				}
			catch (Throwable ex) {
				composite.release();
				throw ex;
			}
		}
		else if (this.route != null) {
			Assert.isTrue(entries.isEmpty(), "Composite metadata required for route and other entries");
			String routingMimeType = WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.getString();
			return this.metadataMimeType.toString().equals(routingMimeType) ?
					asDataBuffer(encodeRoute()) :
					encodeEntry(this.route, this.metadataMimeType);
		}
		else {
			Assert.isTrue(entries.size() == 1, "Composite metadata required for multiple entries");
			MetadataEntry entry = entries.get(0);
			if (!this.metadataMimeType.equals(entry.mimeType())) {
				throw new IllegalArgumentException(
						"Connection configured for metadata mime type " +
								"'" + this.metadataMimeType + "', but actual is `" + entries + "`");
			}
			return encodeEntry(entry);
		}
	}

	private ByteBuf encodeRoute() {
		return io.rsocket.metadata.TaggingMetadataCodec.createRoutingMetadata(
				this.allocator, Collections.singletonList(this.route)).getContent();
	}

	private <T> DataBuffer encodeEntry(MetadataEntry entry) {
		return encodeEntry(entry.value(), entry.mimeType());
	}

	@SuppressWarnings("unchecked")
	private <T> DataBuffer encodeEntry(Object value, MimeType mimeType) {
		if (value instanceof ByteBuf) {
			return asDataBuffer((ByteBuf) value);
		}
		ResolvableType type = ResolvableType.forInstance(value);
		Encoder<T> encoder = this.strategies.encoder(type, mimeType);
		Assert.notNull(encoder, () -> "No encoder for metadata " + value + ", mimeType '" + mimeType + "'");
		return encoder.encodeValue((T) value, bufferFactory(), type, mimeType, Collections.emptyMap());
	}

	private DataBuffer asDataBuffer(ByteBuf byteBuf) {
		if (bufferFactory() instanceof NettyDataBufferFactory) {
			return ((NettyDataBufferFactory) bufferFactory()).wrap(byteBuf);
		}
		else {
			DataBuffer buffer = bufferFactory().wrap(byteBuf.nioBuffer());
			byteBuf.release();
			return buffer;
		}
	}

	private Mono<List<MetadataEntry>> resolveAsyncMetadata() {
		Assert.state(this.hasAsyncValues, "No asynchronous values to resolve");
		List<Mono<?>> valueMonos = new ArrayList<>();
		this.metadataEntries.forEach(entry -> {
			Object v = entry.value();
			valueMonos.add(v instanceof Mono ? (Mono<?>) v : Mono.just(v));
		});
		return Mono.zip(valueMonos, values -> {
			List<MetadataEntry> result = new ArrayList<>(values.length);
			for (int i = 0; i < values.length; i++) {
				if (values[i] != NO_VALUE) {
					result.add(new MetadataEntry(values[i], this.metadataEntries.get(i).mimeType()));
				}
			}
			return result;
		});
	}


	/**
	 * Holder for the metadata value and mime type.
	 * @since 5.2.2
	 */
	private static class MetadataEntry {

		private final Object value;

		private final MimeType mimeType;

		MetadataEntry(Object value, MimeType mimeType) {
			this.value = value;
			this.mimeType = mimeType;
		}

		public Object value() {
			return this.value;
		}

		public MimeType mimeType() {
			return this.mimeType;
		}
	}

}
