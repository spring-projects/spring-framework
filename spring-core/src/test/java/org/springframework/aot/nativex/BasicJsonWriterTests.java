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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BasicJsonWriter}.
 *
 * @author Stephane Nicoll
 */
class BasicJsonWriterTests {

	private final StringWriter out = new StringWriter();

	private final BasicJsonWriter json = new BasicJsonWriter(out, "\t");

	@Test
	void writeObject() {
		Map<String, Object> attributes = orderedMap("test", "value");
		attributes.put("another", true);
		this.json.writeObject(attributes);
		assertThat(out.toString()).isEqualTo("""
				{
					"test": "value",
					"another": true
				}
				""");
	}

	@Test
	void writeObjectWitNestedObject() {
		Map<String, Object> attributes = orderedMap("test", "value");
		attributes.put("nested", orderedMap("enabled", false));
		this.json.writeObject(attributes);
		assertThat(out.toString()).isEqualTo("""
				{
					"test": "value",
					"nested": {
						"enabled": false
					}
				}
				""");
	}

	@Test
	void writeObjectWitNestedArrayOfString() {
		Map<String, Object> attributes = orderedMap("test", "value");
		attributes.put("nested", List.of("test", "value", "another"));
		this.json.writeObject(attributes);
		assertThat(out.toString()).isEqualTo("""
				{
					"test": "value",
					"nested": [
						"test",
						"value",
						"another"
					]
				}
				""");
	}

	@Test
	void writeObjectWitNestedArrayOfObject() {
		Map<String, Object> attributes = orderedMap("test", "value");
		LinkedHashMap<String, Object> secondNested = orderedMap("name", "second");
		secondNested.put("enabled", false);
		attributes.put("nested", List.of(orderedMap("name", "first"), secondNested, orderedMap("name", "third")));
		this.json.writeObject(attributes);
		assertThat(out.toString()).isEqualTo("""
				{
					"test": "value",
					"nested": [
						{
							"name": "first"
						},
						{
							"name": "second",
							"enabled": false
						},
						{
							"name": "third"
						}
					]
				}
				""");
	}

	@Test
	void writeObjectWithNestedEmptyArray() {
		Map<String, Object> attributes = orderedMap("test", "value");
		attributes.put("nested", Collections.emptyList());
		this.json.writeObject(attributes);
		assertThat(out.toString()).isEqualTo("""
				{
					"test": "value",
					"nested": [ ]
				}
				""");
	}

	@Test
	void writeObjectWithNestedEmptyObject() {
		Map<String, Object> attributes = orderedMap("test", "value");
		attributes.put("nested", Collections.emptyMap());
		this.json.writeObject(attributes);
		assertThat(out.toString()).isEqualTo("""
				{
					"test": "value",
					"nested": { }
				}
				""");
	}

	@Test
	void writeWithEscapeDoubleQuote() {
		assertEscapedValue("foo\"bar", "foo\\\"bar");
	}

	@Test
	void writeWithEscapeBackslash() {
		assertEscapedValue("foo\"bar", "foo\\\"bar");
	}

	@Test
	void writeWithEscapeBackspace() {
		assertEscapedValue("foo\bbar", "foo\\bbar");
	}

	@Test
	void writeWithEscapeFormFeed() {
		assertEscapedValue("foo\fbar", "foo\\fbar");
	}

	@Test
	void writeWithEscapeNewline() {
		assertEscapedValue("foo\nbar", "foo\\nbar");
	}

	@Test
	void writeWithEscapeCarriageReturn() {
		assertEscapedValue("foo\rbar", "foo\\rbar");
	}

	@Test
	void writeWithEscapeTab() {
		assertEscapedValue("foo\tbar", "foo\\tbar");
	}

	@Test
	void writeWithEscapeUnicode() {
		assertEscapedValue("foo\u001Fbar", "foo\\u001fbar");
	}

	void assertEscapedValue(String value, String expectedEscapedValue) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		attributes.put("test", value);
		this.json.writeObject(attributes);
		assertThat(out.toString()).contains("\"test\": \"" + expectedEscapedValue + "\"");
	}

	private static LinkedHashMap<String, Object> orderedMap(String key, Object value) {
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put(key, value);
		return map;
	}

}
