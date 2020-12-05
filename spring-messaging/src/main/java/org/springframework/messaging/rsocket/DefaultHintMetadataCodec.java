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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.rsocket.metadata.CompositeMetadata;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import static org.springframework.messaging.rsocket.DefaultRSocketRequester.CODEC_HINTS_MIME;

/**
 *
 * Default implement for {@link HintMetadataCodec}.
 *
 * @author Rudy Steiner
 * @since 5.3.2
 **/
public class DefaultHintMetadataCodec implements HintMetadataCodec {
	private static final Log logger = LogFactory.getLog(DefaultHintMetadataCodec.class);
	private static final Map<String, Object> EMPTY_HINTS = Collections.emptyMap();
	private final NettyDataBufferFactory nettyDataBufferFactory ;
	private final DataBufferFactory bufferFactory;
	private ByteBufAllocator byteBufAllocator;
	private List<Encoder<?>> encoders;
	private List<Decoder<?>> decoders;

	public DefaultHintMetadataCodec(DataBufferFactory dataBufferFactory, MetadataExtractor metadataExtractor,
									List<Encoder<?>> encoders, List<Decoder<?>> decoders){
		Assert.notNull(dataBufferFactory, "'dataBufferFactory' must not be null");
		Assert.notNull(encoders, "'encoders' must not be null");
		Assert.notNull(decoders, "'decoders' must not be null");
		this.bufferFactory = dataBufferFactory;
		this.encoders = encoders;
		this.decoders = decoders;
		this.byteBufAllocator = allocator();
		this.nettyDataBufferFactory = new NettyDataBufferFactory(this.byteBufAllocator);
		if (metadataExtractor instanceof MetadataExtractorRegistry) {
			try {
				MetadataExtractorRegistry registry = (MetadataExtractorRegistry) metadataExtractor;
				registry.metadataToExtract(CODEC_HINTS_MIME, ByteBuf.class, this::decodeHints);
			}
			catch (Throwable ex){
				logger.error("Register hints extractor failed", ex);
			}
		}
		else if(logger.isDebugEnabled()){
			logger.debug("Failed to register extractor for "+ CODEC_HINTS_MIME.toString());
		}
	}
	@Override
	public void encodeHints(Map<String, Object> hints, Consumer<ByteBuf> consumer) {
		CompositeByteBuf composite = this.byteBufAllocator.compositeBuffer();
		hints.forEach( (key, value) -> {
			Assert.notNull(key, "'key' must not be null");
			Assert.notNull(value, "'value' must not be null");
			encodeHint(composite,key,value);
		});
		consumer.accept(composite);
	}



	@Override
	public void decodeHints(ByteBuf hints, Map<String, Object> result) {
		for (CompositeMetadata.Entry entry : new CompositeMetadata(hints, false)) {
			try {
				consumeHint(entry.getMimeType(),(key, resolvableType)->{
					Assert.isTrue(result.get(resolvableType) == null,"Duplicate key:" + key);
					decodeHint(key,resolvableType,entry.getContent(),result);
				});
			}
			catch (Throwable ex){
				logger.error("Extract hints exception", ex);
			}
		}
	}

	private void encodeHint(CompositeByteBuf composite,String key,Object value){
		io.rsocket.metadata.CompositeMetadataCodec.encodeAndAddMetadata(composite, this.byteBufAllocator,
				keyValueClass(key, value),
				PayloadUtils.asByteBuf(encodeData(value, ResolvableType.forInstance(value),null, MimeTypeUtils.ALL)));
	}

	private void decodeHint(String key, ResolvableType resolvableType , ByteBuf value, Map<String,Object> result){
		Decoder<Object> decoder = this.decoder( resolvableType, MimeTypeUtils.ALL);
		if (Objects.isNull(decoder) && logger.isDebugEnabled()) {
			logger.debug("No decoder for hint,key:" + key + ",class resolvable type:" + resolvableType);
		}
		result.put(key, decoder.decode(this.nettyDataBufferFactory.wrap(value.retain()), resolvableType,
				MimeTypeUtils.ALL, null));
	}

	@SuppressWarnings("unchecked")
	private <T> DataBuffer encodeData(T value, ResolvableType elementType, @Nullable Encoder<?> encoder, MimeType dataMimeType) {
		if (encoder == null) {
			elementType = ResolvableType.forInstance(value);
			encoder = this.encoder(elementType, dataMimeType);
		}
		return ((Encoder<T>) encoder).encodeValue(
				value, this.bufferFactory, elementType, dataMimeType,EMPTY_HINTS);
	}
	private ByteBufAllocator allocator() {
		return this.bufferFactory instanceof NettyDataBufferFactory ?
				((NettyDataBufferFactory) this.bufferFactory).getByteBufAllocator() : ByteBufAllocator.DEFAULT;
	}
	/**
	 * Composite hint key and class name of value object.
	 **/
	private String keyValueClass(String key, Object object){
		return key +"/" +object.getClass().getName();
	}

	/**
	 * Consume hint keyValueClass.
	 **/
	private void consumeHint(String keyValueClass, BiConsumer<String, ResolvableType> keyValueResolveType) throws ClassNotFoundException{
		String[]  keyClass = keyValueClass.split("/");
		Assert.isTrue(keyClass.length ==2 ,"Invalid key value class name:"+keyValueClass);
		keyValueResolveType.accept(keyClass[0],ResolvableType.forClass(Class.forName(keyClass[1])));
	}

	@SuppressWarnings("unchecked")
	private  <T> Decoder<T> decoder(ResolvableType elementType, @Nullable MimeType mimeType) {
		for (Decoder<?> decoder : this.decoders) {
			if (decoder.canDecode(elementType, mimeType)) {
				return (Decoder<T>) decoder;
			}
		}
		throw new IllegalArgumentException("No decoder for " + elementType);
	}

	@SuppressWarnings("unchecked")
	private  <T> Encoder<T> encoder(ResolvableType elementType, @Nullable MimeType mimeType) {
		for (Encoder<?> encoder : this.encoders) {
			if (encoder.canEncode(elementType, mimeType)) {
				return (Encoder<T>) encoder;
			}
		}
		throw new IllegalArgumentException("No encoder for " + elementType);
	}

}
