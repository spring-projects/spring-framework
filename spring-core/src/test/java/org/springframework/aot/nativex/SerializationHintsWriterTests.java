/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.aot.nativex;

import java.io.StringWriter;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import org.springframework.aot.hint.SerializationHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.env.Environment;

/**
 * Tests for {@link SerializationHintsWriter}.
 *
 * @author Sebastien Deleuze
 */
class SerializationHintsWriterTests {

	@Test
	void shouldWriteEmptyHint() throws JSONException {
		SerializationHints hints = new SerializationHints();
		assertEquals("[]", hints);
	}

	@Test
	void shouldWriteSingleHint() throws JSONException {
		SerializationHints hints = new SerializationHints().registerType(TypeReference.of(String.class));
		assertEquals("""
				[
					{ "name": "java.lang.String" }
				]""", hints);
	}

	@Test
	void shouldWriteMultipleHints() throws JSONException {
		SerializationHints hints = new SerializationHints()
				.registerType(TypeReference.of(Environment.class))
				.registerType(TypeReference.of(String.class));
		assertEquals("""
				[
					{ "name": "java.lang.String" },
					{ "name": "org.springframework.core.env.Environment" }
				]""", hints);
	}

	@Test
	void shouldWriteSingleHintWithCondition() throws JSONException {
		SerializationHints hints = new SerializationHints().registerType(TypeReference.of(String.class),
				builder -> builder.onReachableType(TypeReference.of("org.example.Test")));
		assertEquals("""
				[
					{ "condition": { "typeReachable": "org.example.Test" }, "name": "java.lang.String" }
				]""", hints);
	}

	private void assertEquals(String expectedString, SerializationHints hints) throws JSONException {
		StringWriter out = new StringWriter();
		BasicJsonWriter writer = new BasicJsonWriter(out, "\t");
		SerializationHintsWriter.INSTANCE.write(writer, hints);
		JSONAssert.assertEquals(expectedString, out.toString(), JSONCompareMode.STRICT);
	}

}
