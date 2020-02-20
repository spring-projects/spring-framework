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

package org.springframework.http.codec.json;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.codec.HttpMessageDecoder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Abstract base class for Jackson 2.9 decoding, leveraging non-blocking parsing.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 5.0
 * @see <a href="https://github.com/FasterXML/jackson-core/issues/57" target="_blank">Add support for non-blocking ("async") JSON parsing</a>
 */
public abstract class AbstractJackson2Decoder extends Jackson2CodecSupport implements HttpMessageDecoder<Object> {

	private int maxInMemorySize = -1;


	/**
	 * Until https://github.com/FasterXML/jackson-core/issues/476 is resolved,
	 * we need to ensure buffer recycling is off.
	 */
	private final JsonFactory jsonFactory;


	/**
	 * Constructor with a Jackson {@link ObjectMapper} to use.
	 */
	protected AbstractJackson2Decoder(ObjectMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
		this.jsonFactory = mapper.getFactory().copy()
				.disable(JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING);
	}


	/**
	 * Set the max number of bytes that can be buffered by this decoder. This
	 * is either the size of the entire input when decoding as a whole, or the
	 * size of one top-level JSON object within a JSON stream. When the limit
	 * is exceeded, {@link DataBufferLimitException} is raised.
	 * <p>By default in 5.1 this is set to -1, unlimited. In 5.2 the default
	 * value for this limit is set to 256K.
	 * @param byteCount the max number of bytes to buffer, or -1 for unlimited
	 * @since 5.1.11
	 */
	public void setMaxInMemorySize(int byteCount) {
		this.maxInMemorySize = byteCount;
	}

	/**
	 * Return the {@link #setMaxInMemorySize configured} byte count limit.
	 * @since 5.1.11
	 */
	public int getMaxInMemorySize() {
		return this.maxInMemorySize;
	}


	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		JavaType javaType = getObjectMapper().getTypeFactory().constructType(elementType.getType());
		// Skip String: CharSequenceDecoder + "*/*" comes after
		return (!CharSequence.class.isAssignableFrom(elementType.toClass()) &&
				getObjectMapper().canDeserialize(javaType) && supportsMimeType(mimeType));
	}

	@Override
	public Flux<Object> decode(Publisher<DataBuffer> input, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		boolean forceUseOfBigDecimal = getObjectMapper().isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
		if (BigDecimal.class.equals(elementType.getType())) {
			forceUseOfBigDecimal = true;
		}

		Flux<DataBuffer> processed = processInput(input, elementType, mimeType, hints);
		Flux<TokenBuffer> tokens = Jackson2Tokenizer.tokenize(processed, this.jsonFactory, getObjectMapper(),
				true, forceUseOfBigDecimal, getMaxInMemorySize());
		return decodeInternal(tokens, elementType, mimeType, hints);
	}

	/**
	 * Process the input publisher into a flux. Default implementation returns
	 * {@link Flux#from(Publisher)}, but subclasses can choose to to customize
	 * this behaviour.
	 * @param input the {@code DataBuffer} input stream to process
	 * @param elementType the expected type of elements in the output stream
	 * @param mimeType the MIME type associated with the input stream (optional)
	 * @param hints additional information about how to do encode
	 * @return the processed flux
	 * @since 5.1.14
	 */
	protected Flux<DataBuffer> processInput(Publisher<DataBuffer> input, ResolvableType elementType,
				@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return Flux.from(input);
	}

	@Override
	public Mono<Object> decodeToMono(Publisher<DataBuffer> input, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		boolean forceUseOfBigDecimal = getObjectMapper().isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
		if (BigDecimal.class.equals(elementType.getType())) {
			forceUseOfBigDecimal = true;
		}

		Flux<DataBuffer> processed = processInput(input, elementType, mimeType, hints);
		Flux<TokenBuffer> tokens = Jackson2Tokenizer.tokenize(processed, this.jsonFactory, getObjectMapper(),
				false, forceUseOfBigDecimal, getMaxInMemorySize());
		return decodeInternal(tokens, elementType, mimeType, hints).singleOrEmpty();
	}

	private Flux<Object> decodeInternal(Flux<TokenBuffer> tokens, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		Assert.notNull(tokens, "'tokens' must not be null");
		Assert.notNull(elementType, "'elementType' must not be null");

		MethodParameter param = getParameter(elementType);
		Class<?> contextClass = (param != null ? param.getContainingClass() : null);
		JavaType javaType = getJavaType(elementType.getType(), contextClass);
		Class<?> jsonView = (hints != null ? (Class<?>) hints.get(Jackson2CodecSupport.JSON_VIEW_HINT) : null);

		ObjectReader reader = (jsonView != null ?
				getObjectMapper().readerWithView(jsonView).forType(javaType) :
				getObjectMapper().readerFor(javaType));

		return tokens.flatMap(tokenBuffer -> {
			try {
				Object value = reader.readValue(tokenBuffer.asParser(getObjectMapper()));
				if (!Hints.isLoggingSuppressed(hints)) {
					LogFormatUtils.traceDebug(logger, traceOn -> {
						String formatted = LogFormatUtils.formatValue(value, !traceOn);
						return Hints.getLogPrefix(hints) + "Decoded [" + formatted + "]";
					});
				}
				return Mono.justOrEmpty(value);
			}
			catch (InvalidDefinitionException ex) {
				return Mono.error(new CodecException("Type definition error: " + ex.getType(), ex));
			}
			catch (JsonProcessingException ex) {
				return Mono.error(new DecodingException("JSON decoding error: " + ex.getOriginalMessage(), ex));
			}
			catch (IOException ex) {
				return Mono.error(new DecodingException("I/O error while parsing input stream", ex));
			}
		});
	}


	// HttpMessageDecoder

	@Override
	public Map<String, Object> getDecodeHints(ResolvableType actualType, ResolvableType elementType,
			ServerHttpRequest request, ServerHttpResponse response) {

		return getHints(actualType);
	}

	@Override
	public List<MimeType> getDecodableMimeTypes() {
		return getMimeTypes();
	}


	// Jackson2CodecSupport

	@Override
	protected <A extends Annotation> A getAnnotation(MethodParameter parameter, Class<A> annotType) {
		return parameter.getParameterAnnotation(annotType);
	}

}
