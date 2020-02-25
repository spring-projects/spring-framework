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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import reactor.core.publisher.Flux;

import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;

/**
 * {@link Function} to transform a JSON stream of arbitrary size, byte array
 * chunks into a {@code Flux<TokenBuffer>} where each token buffer is a
 * well-formed JSON object.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.0
 */
final class Jackson2Tokenizer {

	private final JsonParser parser;

	private final DeserializationContext deserializationContext;

	private final boolean tokenizeArrayElements;

	private final boolean forceUseOfBigDecimal;

	private final int maxInMemorySize;

	private int objectDepth;

	private int arrayDepth;

	private int byteCount;

	private TokenBuffer tokenBuffer;


	// TODO: change to ByteBufferFeeder when supported by Jackson
	// See https://github.com/FasterXML/jackson-core/issues/478
	private final ByteArrayFeeder inputFeeder;


	private Jackson2Tokenizer(JsonParser parser, DeserializationContext deserializationContext,
			boolean tokenizeArrayElements, boolean forceUseOfBigDecimal, int maxInMemorySize) {

		this.parser = parser;
		this.deserializationContext = deserializationContext;
		this.tokenizeArrayElements = tokenizeArrayElements;
		this.forceUseOfBigDecimal = forceUseOfBigDecimal;
		this.inputFeeder = (ByteArrayFeeder) this.parser.getNonBlockingInputFeeder();
		this.maxInMemorySize = maxInMemorySize;
		this.tokenBuffer = createToken();
	}



	private Flux<TokenBuffer> tokenize(DataBuffer dataBuffer) {
		int bufferSize = dataBuffer.readableByteCount();
		byte[] bytes = new byte[dataBuffer.readableByteCount()];
		dataBuffer.read(bytes);
		DataBufferUtils.release(dataBuffer);

		try {
			this.inputFeeder.feedInput(bytes, 0, bytes.length);
			List<TokenBuffer> result = parseTokenBufferFlux();
			assertInMemorySize(bufferSize, result);
			return Flux.fromIterable(result);
		}
		catch (JsonProcessingException ex) {
			return Flux.error(new DecodingException("JSON decoding error: " + ex.getOriginalMessage(), ex));
		}
		catch (IOException ex) {
			return Flux.error(ex);
		}
	}

	private Flux<TokenBuffer> endOfInput() {
		this.inputFeeder.endOfInput();
		try {
			List<TokenBuffer> result = parseTokenBufferFlux();
			return Flux.fromIterable(result);
		}
		catch (JsonProcessingException ex) {
			return Flux.error(new DecodingException("JSON decoding error: " + ex.getOriginalMessage(), ex));
		}
		catch (IOException ex) {
			return Flux.error(ex);
		}
	}

	private List<TokenBuffer> parseTokenBufferFlux() throws IOException {
		List<TokenBuffer> result = new ArrayList<>();

		// SPR-16151: Smile data format uses null to separate documents
		boolean previousNull = false;
		while (!this.parser.isClosed()) {
			JsonToken token = this.parser.nextToken();
			if (token == JsonToken.NOT_AVAILABLE ||
					token == null && previousNull) {
				break;
			}
			else if (token == null ) { // !previousNull
				previousNull = true;
				continue;
			}
			else {
				previousNull = false;
			}
			updateDepth(token);
			if (!this.tokenizeArrayElements) {
				processTokenNormal(token, result);
			}
			else {
				processTokenArray(token, result);
			}
		}
		return result;
	}

	private void updateDepth(JsonToken token) {
		switch (token) {
			case START_OBJECT:
				this.objectDepth++;
				break;
			case END_OBJECT:
				this.objectDepth--;
				break;
			case START_ARRAY:
				this.arrayDepth++;
				break;
			case END_ARRAY:
				this.arrayDepth--;
				break;
		}
	}

	private void processTokenNormal(JsonToken token, List<TokenBuffer> result) throws IOException {
		this.tokenBuffer.copyCurrentEvent(this.parser);

		if ((token.isStructEnd() || token.isScalarValue()) && this.objectDepth == 0 && this.arrayDepth == 0) {
			result.add(this.tokenBuffer);
			this.tokenBuffer = createToken();
		}
	}

	private void processTokenArray(JsonToken token, List<TokenBuffer> result) throws IOException {
		if (!isTopLevelArrayToken(token)) {
			this.tokenBuffer.copyCurrentEvent(this.parser);
		}

		if (this.objectDepth == 0 && (this.arrayDepth == 0 || this.arrayDepth == 1) &&
				(token == JsonToken.END_OBJECT || token.isScalarValue())) {
			result.add(this.tokenBuffer);
			this.tokenBuffer = createToken();
		}
	}

	private TokenBuffer createToken() {
		TokenBuffer tokenBuffer = new TokenBuffer(this.parser, this.deserializationContext);
		tokenBuffer.forceUseOfBigDecimal(this.forceUseOfBigDecimal);
		return tokenBuffer;
	}

	private boolean isTopLevelArrayToken(JsonToken token) {
		return this.objectDepth == 0 && ((token == JsonToken.START_ARRAY && this.arrayDepth == 1) ||
				(token == JsonToken.END_ARRAY && this.arrayDepth == 0));
	}

	private void assertInMemorySize(int currentBufferSize, List<TokenBuffer> result) {
		if (this.maxInMemorySize >= 0) {
			if (!result.isEmpty()) {
				this.byteCount = 0;
			}
			else if (currentBufferSize > Integer.MAX_VALUE - this.byteCount) {
				raiseLimitException();
			}
			else {
				this.byteCount += currentBufferSize;
				if (this.byteCount > this.maxInMemorySize) {
					raiseLimitException();
				}
			}
		}
	}

	private void raiseLimitException() {
		throw new DataBufferLimitException(
				"Exceeded limit on max bytes per JSON object: " + this.maxInMemorySize);
	}


	/**
	 * Tokenize the given {@code Flux<DataBuffer>} into {@code Flux<TokenBuffer>}.
	 * @param dataBuffers the source data buffers
	 * @param jsonFactory the factory to use
	 * @param objectMapper the current mapper instance
	 * @param tokenizeArrays if {@code true} and the "top level" JSON object is
	 * an array, each element is returned individually immediately after it is received
	 * @param forceUseOfBigDecimal if {@code true}, any floating point values encountered
	 * in source will use {@link java.math.BigDecimal}
	 * @param maxInMemorySize maximum memory size
	 * @return the resulting token buffers
	 */
	public static Flux<TokenBuffer> tokenize(Flux<DataBuffer> dataBuffers, JsonFactory jsonFactory,
			ObjectMapper objectMapper, boolean tokenizeArrays, boolean forceUseOfBigDecimal, int maxInMemorySize) {

		try {
			JsonParser parser = jsonFactory.createNonBlockingByteArrayParser();
			DeserializationContext context = objectMapper.getDeserializationContext();
			if (context instanceof DefaultDeserializationContext) {
				context = ((DefaultDeserializationContext) context).createInstance(
						objectMapper.getDeserializationConfig(), parser, objectMapper.getInjectableValues());
			}
			Jackson2Tokenizer tokenizer =
					new Jackson2Tokenizer(parser, context, tokenizeArrays, forceUseOfBigDecimal, maxInMemorySize);
			return dataBuffers.flatMap(tokenizer::tokenize, Flux::error, tokenizer::endOfInput);
		}
		catch (IOException ex) {
			return Flux.error(ex);
		}
	}

}
