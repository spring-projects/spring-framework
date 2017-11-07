/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.codec.json;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.HttpMessageDecoder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Abstract base class for Jackson JSON 2.9 decoding.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class AbstractJackson2Decoder extends Jackson2CodecSupport implements HttpMessageDecoder<Object> {

	/**
	 * Constructor with a Jackson {@link ObjectMapper} to use.
	 */
	protected AbstractJackson2Decoder(ObjectMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
	}


	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		JavaType javaType = getObjectMapper().getTypeFactory().constructType(elementType.getType());
		// Skip String: CharSequenceDecoder + "*/*" comes after
		return (!CharSequence.class.isAssignableFrom(elementType.resolve(Object.class)) &&
				getObjectMapper().canDeserialize(javaType) && supportsMimeType(mimeType));
	}

	@Override
	public Flux<Object> decode(Publisher<DataBuffer> input, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		Flux<TokenBuffer> tokens = tokenize(input, true);
		return decodeInternal(tokens, elementType, mimeType, hints);
	}

	@Override
	public Mono<Object> decodeToMono(Publisher<DataBuffer> input, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		Flux<TokenBuffer> tokens = tokenize(input, false);
		return decodeInternal(tokens, elementType, mimeType, hints).singleOrEmpty();
	}

	private Flux<TokenBuffer> tokenize(Publisher<DataBuffer> input, boolean tokenizeArrayElements) {
		try {
			JsonFactory factory = getObjectMapper().getFactory();
			JsonParser parser = factory.createNonBlockingByteArrayParser();
			Jackson2Tokenizer tokenizer = new Jackson2Tokenizer(parser, tokenizeArrayElements);
			return Flux.from(input).flatMap(tokenizer).doFinally(t -> tokenizer.endOfInput());
		}
		catch (IOException ex) {
			return Flux.error(new UncheckedIOException(ex));
		}
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

		return tokens.map(tokenBuffer -> {
			try {
				return reader.readValue(tokenBuffer.asParser(getObjectMapper()));
			}
			catch (InvalidDefinitionException ex) {
				throw new CodecException("Type definition error: " + ex.getType(), ex);
			}
			catch (JsonProcessingException ex) {
				throw new DecodingException("JSON decoding error: " + ex.getOriginalMessage(), ex);
			}
			catch (IOException ex) {
				throw new DecodingException("I/O error while parsing input stream", ex);
			}
		});
	}


	// HttpMessageDecoder...

	@Override
	public Map<String, Object> getDecodeHints(ResolvableType actualType, ResolvableType elementType,
			ServerHttpRequest request, ServerHttpResponse response) {

		return getHints(actualType);
	}

	@Override
	protected <A extends Annotation> A getAnnotation(MethodParameter parameter, Class<A> annotType) {
		return parameter.getParameterAnnotation(annotType);
	}

}
