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
import com.fasterxml.jackson.databind.util.TokenBuffer;
import reactor.core.publisher.Flux;

import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;

/**
 * {@link Function} to transform a JSON stream of arbitrary size, byte array
 * chunks into a {@code Flux<TokenBuffer>} where each token buffer is a
 * well-formed JSON object.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class Jackson2Tokenizer {

	private final JsonParser parser;

	private final boolean tokenizeArrayElements;

	private TokenBuffer tokenBuffer;

	private int objectDepth;

	private int arrayDepth;

	// TODO: change to ByteBufferFeeder when supported by Jackson
	// See https://github.com/FasterXML/jackson-core/issues/478
	private final ByteArrayFeeder inputFeeder;


	private Jackson2Tokenizer(JsonParser parser, boolean tokenizeArrayElements) {
		this.parser = parser;
		this.tokenizeArrayElements = tokenizeArrayElements;
		this.tokenBuffer = new TokenBuffer(parser);
		this.inputFeeder = (ByteArrayFeeder) this.parser.getNonBlockingInputFeeder();
	}


	private Flux<TokenBuffer> tokenize(DataBuffer dataBuffer) {
		byte[] bytes = new byte[dataBuffer.readableByteCount()];
		dataBuffer.read(bytes);
		DataBufferUtils.release(dataBuffer);

		try {
			this.inputFeeder.feedInput(bytes, 0, bytes.length);
			return parseTokenBufferFlux();
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
			return parseTokenBufferFlux();
		}
		catch (JsonProcessingException ex) {
			return Flux.error(new DecodingException("JSON decoding error: " + ex.getOriginalMessage(), ex));
		}
		catch (IOException ex) {
			return Flux.error(ex);
		}
	}

	private Flux<TokenBuffer> parseTokenBufferFlux() throws IOException {
		List<TokenBuffer> result = new ArrayList<>();

		while (true) {
			JsonToken token = this.parser.nextToken();
			// SPR-16151: Smile data format uses null to separate documents
			if (token == JsonToken.NOT_AVAILABLE ||
					(token == null && (token = this.parser.nextToken()) == null)) {
				break;
			}
			updateDepth(token);
			if (!this.tokenizeArrayElements) {
				processTokenNormal(token, result);
			}
			else {
				processTokenArray(token, result);
			}
		}
		return Flux.fromIterable(result);
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
			this.tokenBuffer = new TokenBuffer(this.parser);
		}
	}

	private void processTokenArray(JsonToken token, List<TokenBuffer> result) throws IOException {
		if (!isTopLevelArrayToken(token)) {
			this.tokenBuffer.copyCurrentEvent(this.parser);
		}

		if (this.objectDepth == 0 && (this.arrayDepth == 0 || this.arrayDepth == 1) &&
				(token == JsonToken.END_OBJECT || token.isScalarValue())) {
			result.add(this.tokenBuffer);
			this.tokenBuffer = new TokenBuffer(this.parser);
		}
	}

	private boolean isTopLevelArrayToken(JsonToken token) {
		return this.objectDepth == 0 && ((token == JsonToken.START_ARRAY && this.arrayDepth == 1) ||
				(token == JsonToken.END_ARRAY && this.arrayDepth == 0));
	}


	/**
	 * Tokenize the given {@code Flux<DataBuffer>} into {@code Flux<TokenBuffer>}.
	 * @param dataBuffers the source data buffers
	 * @param jsonFactory the factory to use
	 * @param tokenizeArrayElements if {@code true} and the "top level" JSON object is
	 * an array, each element is returned individually immediately after it is received
	 * @return the resulting token buffers
	 */
	public static Flux<TokenBuffer> tokenize(
			Flux<DataBuffer> dataBuffers, JsonFactory jsonFactory, boolean tokenizeArrayElements) {

		try {
			JsonParser parser = jsonFactory.createNonBlockingByteArrayParser();
			Jackson2Tokenizer tokenizer = new Jackson2Tokenizer(parser, tokenizeArrayElements);
			return dataBuffers.flatMap(tokenizer::tokenize, Flux::error, tokenizer::endOfInput);
		}
		catch (IOException ex) {
			return Flux.error(ex);
		}
	}

}
