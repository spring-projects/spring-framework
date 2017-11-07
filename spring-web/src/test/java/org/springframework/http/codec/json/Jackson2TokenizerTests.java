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
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

/**
 * @author Arjen Poutsma
 */
public class Jackson2TokenizerTests extends AbstractDataBufferAllocatingTestCase {

	private JsonParser jsonParser;

	private Jackson2Tokenizer tokenizer;

	private ObjectMapper objectMapper;

	@Before
	public void createParser() throws IOException {
		JsonFactory factory = new JsonFactory();
		this.jsonParser = factory.createNonBlockingByteArrayParser();
		this.objectMapper = new ObjectMapper(factory);
	}

	@Test
	public void doNotTokenizeArrayElements() {
		this.tokenizer = new Jackson2Tokenizer(this.jsonParser, false);

		testTokenize(
				singletonList("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}"),
				singletonList("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}"));

		testTokenize(
				asList("{\"foo\": \"foofoo\"",
						", \"bar\": \"barbar\"}"),
				singletonList("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}"));

		testTokenize(
				singletonList("[{\"foo\": \"foofoo\", \"bar\": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]"),
				singletonList("[{\"foo\": \"foofoo\", \"bar\": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]"));

		testTokenize(
				singletonList("[{\"foo\": \"bar\"},{\"foo\": \"baz\"}]"),
				singletonList("[{\"foo\": \"bar\"},{\"foo\": \"baz\"}]"));

		testTokenize(
				asList("[{\"foo\": \"foofoo\", \"bar\"",
						": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]"),
				singletonList("[{\"foo\": \"foofoo\", \"bar\": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]"));

		testTokenize(
				asList("[",
						"{\"id\":1,\"name\":\"Robert\"}",
						",",
						"{\"id\":2,\"name\":\"Raide\"}",
						",",
						"{\"id\":3,\"name\":\"Ford\"}",
						"]"),
				singletonList("[{\"id\":1,\"name\":\"Robert\"},{\"id\":2,\"name\":\"Raide\"},{\"id\":3,\"name\":\"Ford\"}]"));
	}

	@Test
	public void tokenizeArrayElements() {
		this.tokenizer = new Jackson2Tokenizer(this.jsonParser, true);

		testTokenize(
				singletonList("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}"),
				singletonList("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}"));

		testTokenize(
				asList("{\"foo\": \"foofoo\"",
						", \"bar\": \"barbar\"}"),
				singletonList("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}"));

		testTokenize(
				singletonList("[{\"foo\": \"foofoo\", \"bar\": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]"),
				asList("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}",
						"{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}"));

		testTokenize(
				singletonList("[{\"foo\": \"bar\"},{\"foo\": \"baz\"}]"),
				asList("{\"foo\": \"bar\"}",
						"{\"foo\": \"baz\"}"));

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
						"{\"id\":\"2\",\"start\":[-999999999,1,1],\"end\":[999999999,12,31]}")
		);

		// SPR-15803: nested array, no top-level array
		testTokenize(
				singletonList("{\"speakerIds\":[\"tastapod\"],\"language\":\"ENGLISH\"}"),
				singletonList("{\"speakerIds\":[\"tastapod\"],\"language\":\"ENGLISH\"}"));

		testTokenize(
				asList("[{\"foo\": \"foofoo\", \"bar\"",
						": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]"),
				asList("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}",
						"{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}"));

		testTokenize(
				asList("[",
						"{\"id\":1,\"name\":\"Robert\"}",
						",",
						"{\"id\":2,\"name\":\"Raide\"}",
						",",
						"{\"id\":3,\"name\":\"Ford\"}",
						"]"),
				asList("{\"id\":1,\"name\":\"Robert\"}",
						"{\"id\":2,\"name\":\"Raide\"}",
						"{\"id\":3,\"name\":\"Ford\"}"));
	}

	private void testTokenize(List<String> source, List<String> expected) {
		Flux<DataBuffer> sourceFlux = Flux.fromIterable(source)
				.map(this::stringBuffer);

		Flux<String> result = sourceFlux
				.flatMap(this.tokenizer)
				.map(tokenBuffer -> {
					try {
						TreeNode root = this.objectMapper.readTree(tokenBuffer.asParser());
						return this.objectMapper.writeValueAsString(root);
					}
					catch (IOException ex) {
						throw new UncheckedIOException(ex);
					}
				});

		StepVerifier.FirstStep<String> builder = StepVerifier.create(result);
		for (String s : expected) {
			builder.assertNext(new JSONAssertConsumer(s));
		}
		builder.verifyComplete();
	}

	private static class JSONAssertConsumer implements Consumer<String> {

		private final String expected;

		public JSONAssertConsumer(String expected) {
			this.expected = expected;
		}

		@Override
		public void accept(String s) {
			try {
				JSONAssert.assertEquals(this.expected, s, true);
			}
			catch (JSONException ex) {
				throw new RuntimeException(ex);
			}
		}
	}


}