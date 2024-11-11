/*
 * Copyright 2002-2024 the original author or authors.
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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.core.testfixture.io.buffer.AbstractLeakCheckingTests;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
class Jackson2TokenizerTests extends AbstractLeakCheckingTests {

	private JsonFactory jsonFactory;

	private ObjectMapper objectMapper;


	@BeforeEach
	void createParser() {
		this.jsonFactory = new JsonFactory();
		this.objectMapper = new ObjectMapper(this.jsonFactory);
	}


	@Test
	void doNotTokenizeArrayElements() {
		testTokenize(
				singletonList("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}"),
				singletonList("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}"), false);

		testTokenize(
				asList(
						"{\"foo\": \"foofoo\"",
						", \"bar\": \"barbar\"}"
				),
				singletonList("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}"), false);

		testTokenize(
				singletonList("[{\"foo\": \"foofoo\", \"bar\": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]"),
				singletonList("[{\"foo\": \"foofoo\", \"bar\": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]"),
				false);

		testTokenize(
				singletonList("[{\"foo\": \"bar\"},{\"foo\": \"baz\"}]"),
				singletonList("[{\"foo\": \"bar\"},{\"foo\": \"baz\"}]"), false);

		testTokenize(
				asList(
						"[{\"foo\": \"foofoo\", \"bar\"",
						": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]"
				),
				singletonList("[{\"foo\": \"foofoo\", \"bar\": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]"),
				false);

		testTokenize(
				asList(
						"[",
						"{\"id\":1,\"name\":\"Robert\"}", ",",
						"{\"id\":2,\"name\":\"Raide\"}", ",",
						"{\"id\":3,\"name\":\"Ford\"}", "]"
				),
				singletonList("[{\"id\":1,\"name\":\"Robert\"},{\"id\":2,\"name\":\"Raide\"},{\"id\":3,\"name\":\"Ford\"}]"),
				false);

		// SPR-16166: top-level JSON values
		testTokenize(asList("\"foo", "bar\""), singletonList("\"foobar\""), false);
		testTokenize(asList("12", "34"), singletonList("1234"), false);
		testTokenize(asList("12.", "34"), singletonList("12.34"), false);

		// note that we do not test for null, true, or false, which are also valid top-level values,
		// but are unsupported by JSONassert
	}

	@Test
	void tokenizeArrayElements() {
		testTokenize(
				singletonList("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}"),
				singletonList("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}"), true);

		testTokenize(
				asList(
						"{\"foo\": \"foofoo\"",
						", \"bar\": \"barbar\"}"
				),
				singletonList("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}"), true);

		testTokenize(
				singletonList("[{\"foo\": \"foofoo\", \"bar\": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]"),
				asList(
						"{\"foo\": \"foofoo\", \"bar\": \"barbar\"}",
						"{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}"
				),
				true);

		testTokenize(
				singletonList("[{\"foo\": \"bar\"},{\"foo\": \"baz\"}]"),
				asList(
						"{\"foo\": \"bar\"}",
						"{\"foo\": \"baz\"}"
				),
				true);

		// SPR-15803: nested array
		testTokenize(
				singletonList("[" +
						"{\"id\":\"0\",\"start\":[-999999999,1,1],\"end\":[999999999,12,31]}," +
						"{\"id\":\"1\",\"start\":[-999999999,1,1],\"end\":[999999999,12,31]}," +
						"{\"id\":\"2\",\"start\":[-999999999,1,1],\"end\":[999999999,12,31]}" +
						"]"),
				asList(
						"{\"id\":\"0\",\"start\":[-999999999,1,1],\"end\":[999999999,12,31]}",
						"{\"id\":\"1\",\"start\":[-999999999,1,1],\"end\":[999999999,12,31]}",
						"{\"id\":\"2\",\"start\":[-999999999,1,1],\"end\":[999999999,12,31]}"
				),
				true);

		// SPR-15803: nested array, no top-level array
		testTokenize(
				singletonList("{\"speakerIds\":[\"tastapod\"],\"language\":\"ENGLISH\"}"),
				singletonList("{\"speakerIds\":[\"tastapod\"],\"language\":\"ENGLISH\"}"), true);

		testTokenize(
				asList(
						"[{\"foo\": \"foofoo\", \"bar\"",
						": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]"
				),
				asList(
						"{\"foo\": \"foofoo\", \"bar\": \"barbar\"}",
						"{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}"), true);

		testTokenize(
				asList(
						"[",
						"{\"id\":1,\"name\":\"Robert\"}",
						",",
						"{\"id\":2,\"name\":\"Raide\"}",
						",",
						"{\"id\":3,\"name\":\"Ford\"}",
						"]"
				),
				asList(
						"{\"id\":1,\"name\":\"Robert\"}",
						"{\"id\":2,\"name\":\"Raide\"}",
						"{\"id\":3,\"name\":\"Ford\"}"
				),
				true);

		// SPR-16166: top-level JSON values
		testTokenize(asList("\"foo", "bar\""), singletonList("\"foobar\""), true);
		testTokenize(asList("12", "34"), singletonList("1234"), true);
		testTokenize(asList("12.", "34"), singletonList("12.34"), true);

		// SPR-16407
		testTokenize(asList("[1", ",2,", "3]"), asList("1", "2", "3"), true);
	}

	@Test
	void tokenizeStream() {

		// NDJSON (Newline Delimited JSON), JSON Lines
		testTokenize(
				asList(
						"{\"id\":1,\"name\":\"Robert\"}",
						"\n",
						"{\"id\":2,\"name\":\"Raide\"}",
						"\n",
						"{\"id\":3,\"name\":\"Ford\"}"
				),
				asList(
						"{\"id\":1,\"name\":\"Robert\"}",
						"{\"id\":2,\"name\":\"Raide\"}",
						"{\"id\":3,\"name\":\"Ford\"}"
				),
				true);

		// JSON Sequence with newline separator
		testTokenize(
				asList(
						"\n",
						"{\"id\":1,\"name\":\"Robert\"}",
						"\n",
						"{\"id\":2,\"name\":\"Raide\"}",
						"\n",
						"{\"id\":3,\"name\":\"Ford\"}"
				),
				asList(
						"{\"id\":1,\"name\":\"Robert\"}",
						"{\"id\":2,\"name\":\"Raide\"}",
						"{\"id\":3,\"name\":\"Ford\"}"
				),
				true);
	}

	private void testTokenize(List<String> input, List<String> output, boolean tokenize) {
		StepVerifier.FirstStep<String> builder = StepVerifier.create(decode(input, tokenize, -1));
		output.forEach(expected -> builder.assertNext(actual -> {
			try {
				JSONAssert.assertEquals(expected, actual, true);
			}
			catch (JSONException ex) {
				throw new RuntimeException(ex);
			}
		}));
		builder.verifyComplete();
	}

	@Test
	void testLimit() {
		List<String> source = asList(
				"[",
				"{", "\"id\":1,\"name\":\"Dan\"", "},",
				"{", "\"id\":2,\"name\":\"Ron\"", "},",
				"{", "\"id\":3,\"name\":\"Bartholomew\"", "}",
				"]"
		);

		String expected = String.join("", source);
		int maxInMemorySize = expected.length();

		StepVerifier.create(decode(source, false, maxInMemorySize))
				.expectNext(expected)
				.verifyComplete();

		StepVerifier.create(decode(source, false, maxInMemorySize - 2))
				.verifyError(DataBufferLimitException.class);
	}

	@Test
	void testLimitTokenized() {

		List<String> source = asList(
				"[",
				"{", "\"id\":1, \"name\":\"Dan\"", "},",
				"{", "\"id\":2, \"name\":\"Ron\"", "},",
				"{", "\"id\":3, \"name\":\"Bartholomew\"", "}",
				"]"
		);

		String expected = "{\"id\":3,\"name\":\"Bartholomew\"}";
		int maxInMemorySize = expected.length();

		StepVerifier.create(decode(source, true, maxInMemorySize))
				.expectNext("{\"id\":1,\"name\":\"Dan\"}")
				.expectNext("{\"id\":2,\"name\":\"Ron\"}")
				.expectNext(expected)
				.verifyComplete();

		StepVerifier.create(decode(source, true, maxInMemorySize - 1))
				.expectNext("{\"id\":1,\"name\":\"Dan\"}")
				.expectNext("{\"id\":2,\"name\":\"Ron\"}")
				.verifyError(DataBufferLimitException.class);
	}

	@Test
	void errorInStream() {
		DataBuffer buffer = stringBuffer("{\"id\":1,\"name\":");
		Flux<DataBuffer> source = Flux.just(buffer).concatWith(Flux.error(new RuntimeException()));
		Flux<TokenBuffer> result = Jackson2Tokenizer.tokenize(source, this.jsonFactory, this.objectMapper, true,
				false, -1);

		StepVerifier.create(result)
				.expectError(RuntimeException.class)
				.verify();
	}

	@Test  // SPR-16521
	public void jsonEOFExceptionIsWrappedAsDecodingError() {
		Flux<DataBuffer> source = Flux.just(stringBuffer("{\"status\": \"noClosingQuote}"));
		Flux<TokenBuffer> tokens = Jackson2Tokenizer.tokenize(source, this.jsonFactory, this.objectMapper, false,
				false, -1);

		StepVerifier.create(tokens)
				.expectError(DecodingException.class)
				.verify();
	}

	@Test
	void useBigDecimalForFloats() {
		Flux<DataBuffer> source = Flux.just(stringBuffer("1E+2"));
		Flux<TokenBuffer> tokens = Jackson2Tokenizer.tokenize(
				source, this.jsonFactory, this.objectMapper, false, true, -1);

		StepVerifier.create(tokens)
				.assertNext(tokenBuffer -> {
					try {
						JsonParser parser = tokenBuffer.asParser();
						JsonToken token = parser.nextToken();
						assertThat(token).isEqualTo(JsonToken.VALUE_NUMBER_FLOAT);
						JsonParser.NumberType numberType = parser.getNumberType();
						assertThat(numberType).isEqualTo(JsonParser.NumberType.BIG_DECIMAL);
					}
					catch (IOException ex) {
						fail(ex.getMessage(), ex);
					}
				})
				.verifyComplete();
	}

	// gh-31747
	@Test
	void compositeNettyBuffer() {
		ByteBufAllocator allocator = UnpooledByteBufAllocator.DEFAULT;
		ByteBuf firstByteBuf = allocator.buffer();
		firstByteBuf.writeBytes("{\"foo\": \"foofoo\"".getBytes(StandardCharsets.UTF_8));
		ByteBuf secondBuf = allocator.buffer();
		secondBuf.writeBytes(", \"bar\": \"barbar\"}".getBytes(StandardCharsets.UTF_8));
		CompositeByteBuf composite = allocator.compositeBuffer();
		composite.addComponent(true, firstByteBuf);
		composite.addComponent(true, secondBuf);

		NettyDataBufferFactory bufferFactory = new NettyDataBufferFactory(allocator);
		Flux<DataBuffer> source = Flux.just(bufferFactory.wrap(composite));
		Flux<TokenBuffer> tokens = Jackson2Tokenizer.tokenize(source, this.jsonFactory, this.objectMapper, false, false, -1);

		Flux<String> strings = tokens.map(this::tokenToString);

		StepVerifier.create(strings)
				.assertNext(s -> assertThat(s).isEqualTo("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}"))
				.verifyComplete();
	}


	private Flux<String> decode(List<String> source, boolean tokenize, int maxInMemorySize) {

		Flux<TokenBuffer> tokens = Jackson2Tokenizer.tokenize(
				Flux.fromIterable(source).map(this::stringBuffer),
				this.jsonFactory, this.objectMapper, tokenize, false, maxInMemorySize);

		return tokens.map(this::tokenToString);
	}

	private String tokenToString(TokenBuffer tokenBuffer) {
		try {
			TreeNode root = this.objectMapper.readTree(tokenBuffer.asParser());
			return this.objectMapper.writeValueAsString(root);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private DataBuffer stringBuffer(String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
		buffer.write(bytes);
		return buffer;
	}

}
