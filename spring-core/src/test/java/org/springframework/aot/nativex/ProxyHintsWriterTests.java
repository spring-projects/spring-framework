/*
 * Copyright 2002-2022 the original author or authors.
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
import java.util.function.Consumer;
import java.util.function.Function;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import org.springframework.aot.hint.ProxyHints;
import org.springframework.aot.hint.TypeReference;

/**
 * Tests for {@link ProxyHintsWriter}.
 *
 * @author Sebastien Deleuze
 */
public class ProxyHintsWriterTests {

	@Test
	void empty() throws JSONException {
		ProxyHints hints = new ProxyHints();
		assertEquals("[]", hints);
	}

	@Test
	void shouldWriteOneEntry() throws JSONException {
		ProxyHints hints = new ProxyHints();
		hints.registerJdkProxy(Function.class);
		assertEquals("""
				[
					{ "interfaces": [ "java.util.function.Function" ] }
				]""", hints);
	}

	@Test
	void shouldWriteMultipleEntries() throws JSONException {
		ProxyHints hints = new ProxyHints();
		hints.registerJdkProxy(Function.class);
		hints.registerJdkProxy(Function.class, Consumer.class);
		assertEquals("""
				[
					{ "interfaces": [ "java.util.function.Function" ] },
					{ "interfaces": [ "java.util.function.Function", "java.util.function.Consumer" ] }
				]""", hints);
	}

	@Test
	void shouldWriteInnerClass() throws JSONException {
		ProxyHints hints = new ProxyHints();
		hints.registerJdkProxy(Inner.class);
		assertEquals("""
				[
					{ "interfaces": [ "org.springframework.aot.nativex.ProxyHintsWriterTests$Inner" ] }
				]""", hints);
	}

	@Test
	void shouldWriteCondition() throws JSONException {
		ProxyHints hints = new ProxyHints();
		hints.registerJdkProxy(builder -> builder.proxiedInterfaces(Function.class)
				.onReachableType(TypeReference.of("org.example.Test")));
		assertEquals("""
				[
					{ "condition": { "typeReachable": "org.example.Test"}, "interfaces": [ "java.util.function.Function" ] }
				]""", hints);
	}

	private void assertEquals(String expectedString, ProxyHints hints) throws JSONException {
		StringWriter out = new StringWriter();
		BasicJsonWriter writer = new BasicJsonWriter(out, "\t");
		ProxyHintsWriter.INSTANCE.write(writer, hints);
		JSONAssert.assertEquals(expectedString, out.toString(), JSONCompareMode.NON_EXTENSIBLE);
	}

	interface Inner {

	}

}
