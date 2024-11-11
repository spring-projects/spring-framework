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

import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.TypeReference;

/**
 * Tests for {@link ResourceHintsWriter}.
 *
 * @author Sebastien Deleuze
 * @author Brian Clozel
 */
class ResourceHintsWriterTests {

	@Test
	void empty() throws JSONException {
		ResourceHints hints = new ResourceHints();
		assertEquals("{}", hints);
	}

	@Test
	void registerExactMatch() throws JSONException {
		ResourceHints hints = new ResourceHints();
		hints.registerPattern("com/example/test.properties");
		hints.registerPattern("com/example/another.properties");
		assertEquals("""
				{
					"resources": {
						"includes": [
							{ "pattern": "\\\\Q/\\\\E" },
							{ "pattern": "\\\\Qcom\\\\E"},
							{ "pattern": "\\\\Qcom/example\\\\E"},
							{ "pattern": "\\\\Qcom/example/another.properties\\\\E"},
							{ "pattern": "\\\\Qcom/example/test.properties\\\\E"}
						]
					}
				}""", hints);
	}

	@Test
	void registerWildcardAtTheBeginningPattern() throws JSONException {
		ResourceHints hints = new ResourceHints();
		hints.registerPattern("*.properties");
		assertEquals("""
				{
					"resources": {
						"includes": [
							{ "pattern": ".*\\\\Q.properties\\\\E"},
							{ "pattern": "\\\\Q\\/\\\\E"}
						]
					}
				}""", hints);
	}

	@Test
	void registerWildcardInTheMiddlePattern() throws JSONException {
		ResourceHints hints = new ResourceHints();
		hints.registerPattern("com/example/*.properties");
		assertEquals("""
				{
					"resources": {
						"includes": [
							{ "pattern": "\\\\Q/\\\\E" },
							{ "pattern": "\\\\Qcom\\\\E"},
							{ "pattern": "\\\\Qcom/example\\\\E"},
							{ "pattern": "\\\\Qcom/example/\\\\E.*\\\\Q.properties\\\\E"}
						]
					}
				}""", hints);
	}

	@Test
	void registerWildcardAtTheEndPattern() throws JSONException {
		ResourceHints hints = new ResourceHints();
		hints.registerPattern("static/*");
		assertEquals("""
				{
					"resources": {
						"includes": [
							{ "pattern": "\\\\Q/\\\\E" },
							{ "pattern": "\\\\Qstatic\\\\E"},
							{ "pattern": "\\\\Qstatic/\\\\E.*"}
						]
					}
				}""", hints);
	}

	@Test
	void registerPatternWithIncludesAndExcludes() throws JSONException {
		ResourceHints hints = new ResourceHints();
		hints.registerPattern(hint -> hint.includes("com/example/*.properties").excludes("com/example/to-ignore.properties"));
		hints.registerPattern(hint -> hint.includes("org/other/*.properties").excludes("org/other/to-ignore.properties"));
		assertEquals("""
				{
					"resources": {
						"includes": [
							{ "pattern": "\\\\Q/\\\\E"},
							{ "pattern": "\\\\Qcom\\\\E"},
							{ "pattern": "\\\\Qcom/example\\\\E"},
							{ "pattern": "\\\\Qcom/example/\\\\E.*\\\\Q.properties\\\\E"},
							{ "pattern": "\\\\Qorg\\\\E"},
							{ "pattern": "\\\\Qorg/other\\\\E"},
							{ "pattern": "\\\\Qorg/other/\\\\E.*\\\\Q.properties\\\\E"}
						],
						"excludes": [
							{ "pattern": "\\\\Qcom/example/to-ignore.properties\\\\E"},
							{ "pattern": "\\\\Qorg/other/to-ignore.properties\\\\E"}
						]
					}
				}""", hints);
	}

	@Test
	void registerWithReachableTypeCondition() throws JSONException {
		ResourceHints hints = new ResourceHints();
		hints.registerPattern(builder -> builder.includes(TypeReference.of("com.example.Test"), "com/example/test.properties"));
		assertEquals("""
				{
					"resources": {
						"includes": [
							{ "condition": { "typeReachable": "com.example.Test"}, "pattern": "\\\\Q/\\\\E"},
							{ "condition": { "typeReachable": "com.example.Test"}, "pattern": "\\\\Qcom\\\\E"},
							{ "condition": { "typeReachable": "com.example.Test"}, "pattern": "\\\\Qcom/example\\\\E"},
							{ "condition": { "typeReachable": "com.example.Test"}, "pattern": "\\\\Qcom/example/test.properties\\\\E"}
						]
					}
				}""", hints);
	}

	@Test
	void registerType() throws JSONException {
		ResourceHints hints = new ResourceHints();
		hints.registerType(String.class);
		assertEquals("""
				{
					"resources": {
						"includes": [
							{ "pattern": "\\\\Q/\\\\E" },
							{ "pattern": "\\\\Qjava\\\\E" },
							{ "pattern": "\\\\Qjava/lang\\\\E" },
							{ "pattern": "\\\\Qjava/lang/String.class\\\\E" }
						]
					}
				}""", hints);
	}

	@Test
	void registerResourceBundle() throws JSONException {
		ResourceHints hints = new ResourceHints();
		hints.registerResourceBundle("com.example.message2");
		hints.registerResourceBundle("com.example.message");
		assertEquals("""
				{
					"bundles": [
						{ "name": "com.example.message"},
						{ "name": "com.example.message2"}
					]
				}""", hints);
	}

	private void assertEquals(String expectedString, ResourceHints hints) throws JSONException {
		StringWriter out = new StringWriter();
		BasicJsonWriter writer = new BasicJsonWriter(out, "\t");
		ResourceHintsWriter.INSTANCE.write(writer, hints);
		JSONAssert.assertEquals(expectedString, out.toString(), JSONCompareMode.STRICT);
	}

}
