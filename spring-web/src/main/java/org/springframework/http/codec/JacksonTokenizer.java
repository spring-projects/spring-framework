/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.http.codec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import reactor.core.publisher.Flux;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.async.ByteArrayFeeder;
import tools.jackson.core.async.ByteBufferFeeder;
import tools.jackson.core.async.NonBlockingInputFeeder;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.util.TokenBuffer;

import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;

/**
 * {@link Function} to transform a JSON stream of arbitrary size, byte array
 * chunks into a {@code Flux<TokenBuffer>} where each token buffer is a
 * well-formed JSON object with Jackson 3.x.
 *
 * @author Sebastien Deleuze
 * @since 7.0
 */
final class JacksonTokenizer {

	private final JsonParser parser;

	private final NonBlockingInputFeeder inputFeeder;

	private final boolean tokenizeArrayElements;

	private final boolean forceUseOfBigDecimal;

	private final int maxInMemorySize;

	private int objectDepth;

	private int arrayDepth;

	private int byteCount;

	private TokenBuffer tokenBuffer;


	private JacksonTokenizer(JsonParser parser, boolean tokenizeArrayElements, boolean forceUseOfBigDecimal, int maxInMemorySize) {
		this.parser = parser;
		this.inputFeeder = this.parser.nonBlockingInputFeeder();
		this.tokenizeArrayElements = tokenizeArrayElements;
		this.forceUseOfBigDecimal = forceUseOfBigDecimal;
		this.maxInMemorySize = maxInMemorySize;
		this.tokenBuffer = createToken();
	}


	private List<TokenBuffer> tokenize(DataBuffer dataBuffer) {
		try {
			int bufferSize = dataBuffer.readableByteCount();
			List<TokenBuffer> tokens = new ArrayList<>();
			if (this.inputFeeder instanceof ByteBufferFeeder byteBufferFeeder) {
				try (DataBuffer.ByteBufferIterator iterator = dataBuffer.readableByteBuffers()) {
					while (iterator.hasNext()) {
						byteBufferFeeder.feedInput(iterator.next());
						parseTokens(tokens);
					}
				}
			}
			else if (this.inputFeeder instanceof ByteArrayFeeder byteArrayFeeder) {
				byte[] bytes = new byte[bufferSize];
				dataBuffer.read(bytes);
				byteArrayFeeder.feedInput(bytes, 0, bufferSize);
				parseTokens(tokens);
			}
			assertInMemorySize(bufferSize, tokens);
			return tokens;
		}
		catch (JacksonException ex) {
			throw new DecodingException("JSON decoding error: " + ex.getOriginalMessage(), ex);
		}
		finally {
			DataBufferUtils.release(dataBuffer);
		}
	}

	private Flux<TokenBuffer> endOfInput() {
		return Flux.defer(() -> {
			this.inputFeeder.endOfInput();
			try {
				List<TokenBuffer> tokens = new ArrayList<>();
				parseTokens(tokens);
				return Flux.fromIterable(tokens);
			}
			catch (JacksonException ex) {
				throw new DecodingException("JSON decoding error: " + ex.getOriginalMessage(), ex);
			}
		});
	}

	private void parseTokens(List<TokenBuffer> tokens) {
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
				processTokenNormal(token, tokens);
			}
			else {
				processTokenArray(token, tokens);
			}
		}
	}

	private void updateDepth(JsonToken token) {
		switch (token) {
			case START_OBJECT -> this.objectDepth++;
			case END_OBJECT -> this.objectDepth--;
			case START_ARRAY -> this.arrayDepth++;
			case END_ARRAY -> this.arrayDepth--;
		}
	}

	private void processTokenNormal(JsonToken token, List<TokenBuffer> result) {
		this.tokenBuffer.copyCurrentEvent(this.parser);

		if ((token.isStructEnd() || token.isScalarValue()) && this.objectDepth == 0 && this.arrayDepth == 0) {
			result.add(this.tokenBuffer);
			this.tokenBuffer = createToken();
		}
	}

	private void processTokenArray(JsonToken token, List<TokenBuffer> result) {
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
		TokenBuffer tokenBuffer = TokenBuffer.forBuffering(this.parser, this.parser.objectReadContext());
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
	 * @param objectMapper the current mapper instance
	 * @param tokenizeArrays if {@code true} and the "top level" JSON object is
	 * an array, each element is returned individually immediately after it is received
	 * @param forceUseOfBigDecimal if {@code true}, any floating point values encountered
	 * in source will use {@link java.math.BigDecimal}
	 * @param maxInMemorySize maximum memory size
	 * @return the resulting token buffers
	 */
	public static Flux<TokenBuffer> tokenize(Flux<DataBuffer> dataBuffers,
			ObjectMapper objectMapper, boolean tokenizeArrays, boolean forceUseOfBigDecimal, int maxInMemorySize) {

		try {
			JsonParser parser;
			try {
				parser = objectMapper.createNonBlockingByteBufferParser();
			}
			catch (UnsupportedOperationException ex) {
				parser = objectMapper.createNonBlockingByteArrayParser();
			}
			JacksonTokenizer tokenizer =
					new JacksonTokenizer(parser, tokenizeArrays, forceUseOfBigDecimal, maxInMemorySize);
			return dataBuffers.concatMapIterable(tokenizer::tokenize).concatWith(tokenizer.endOfInput());
		}
		catch (JacksonException ex) {
			return Flux.error(ex);
		}
	}

}
