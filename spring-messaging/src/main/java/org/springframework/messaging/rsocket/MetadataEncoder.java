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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.rsocket.metadata.CompositeMetadataFlyweight;
import io.rsocket.metadata.TaggingMetadataFlyweight;
import io.rsocket.metadata.WellKnownMimeType;

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
	private static final Pattern VARS_PATTERN = Pattern.compile("\\{([^/]+?)}");


	private final MimeType metadataMimeType;

	private final RSocketStrategies strategies;

	private final boolean isComposite;

	private final ByteBufAllocator allocator;

	@Nullable
	private String route;

	private final Map<Object, MimeType> metadata = new LinkedHashMap<>(4);


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
		return sb.toString();
	}

	private void assertMetadataEntryCount() {
		if (!this.isComposite) {
			int count = this.route != null ? this.metadata.size() + 1 : this.metadata.size();
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
			throw new IllegalArgumentException("Mime type is optional (may be null) " +
					"but was provided and does not match the connection metadata mime type.");
		}
		this.metadata.put(metadata, mimeType);
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
	public DataBuffer encode() {
		if (this.isComposite) {
			CompositeByteBuf composite = this.allocator.compositeBuffer();
			try {
				if (this.route != null) {
					CompositeMetadataFlyweight.encodeAndAddMetadata(composite, this.allocator,
							WellKnownMimeType.MESSAGE_RSOCKET_ROUTING, encodeRoute());
				}
				this.metadata.forEach((value, mimeType) -> {
					ByteBuf metadata = (value instanceof ByteBuf ?
							(ByteBuf) value : PayloadUtils.asByteBuf(encodeEntry(value, mimeType)));
					CompositeMetadataFlyweight.encodeAndAddMetadata(
							composite, this.allocator, mimeType.toString(), metadata);
				});
				return asDataBuffer(composite);
				}
			catch (Throwable ex) {
				composite.release();
				throw ex;
			}
		}
		else if (this.route != null) {
			Assert.isTrue(this.metadata.isEmpty(), "Composite metadata required for route and other entries");
			String routingMimeType = WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.getString();
			return this.metadataMimeType.toString().equals(routingMimeType) ?
					asDataBuffer(encodeRoute()) :
					encodeEntry(this.route, this.metadataMimeType);
		}
		else {
			Assert.isTrue(this.metadata.size() == 1, "Composite metadata required for multiple entries");
			Map.Entry<Object, MimeType> entry = this.metadata.entrySet().iterator().next();
			if (!this.metadataMimeType.equals(entry.getValue())) {
				throw new IllegalArgumentException(
						"Connection configured for metadata mime type " +
								"'" + this.metadataMimeType + "', but actual is `" + this.metadata + "`");
			}
			return encodeEntry(entry.getKey(), entry.getValue());
		}
	}

	private ByteBuf encodeRoute() {
		return TaggingMetadataFlyweight.createRoutingMetadata(
				this.allocator, Collections.singletonList(this.route)).getContent();
	}

	@SuppressWarnings("unchecked")
	private <T> DataBuffer encodeEntry(Object metadata, MimeType mimeType) {
		if (metadata instanceof ByteBuf) {
			return asDataBuffer((ByteBuf) metadata);
		}
		ResolvableType type = ResolvableType.forInstance(metadata);
		Encoder<T> encoder = this.strategies.encoder(type, mimeType);
		Assert.notNull(encoder, () -> "No encoder for metadata " + metadata + ", mimeType '" + mimeType + "'");
		return encoder.encodeValue((T) metadata, bufferFactory(), type, mimeType, Collections.emptyMap());
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
}
