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

package org.springframework.beans.factory.config;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.yaml.snakeyaml.constructor.ConstructorException;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.scanner.ScannerException;

import org.springframework.core.io.ByteArrayResource;

import static org.junit.Assert.*;

/**
 * Tests for {@link YamlProcessor}.
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class YamlProcessorTests {

	private final YamlProcessor processor = new YamlProcessor() {};

	@Rule
	public ExpectedException exception = ExpectedException.none();


	@Test
	public void arrayConvertedToIndexedBeanReference() {
		setYaml("foo: bar\nbar: [1,2,3]");
		this.processor.process((properties, map) -> {
			assertEquals(4, properties.size());
			assertEquals("bar", properties.get("foo"));
			assertEquals("bar", properties.getProperty("foo"));
			assertEquals(1, properties.get("bar[0]"));
			assertEquals("1", properties.getProperty("bar[0]"));
			assertEquals(2, properties.get("bar[1]"));
			assertEquals("2", properties.getProperty("bar[1]"));
			assertEquals(3, properties.get("bar[2]"));
			assertEquals("3", properties.getProperty("bar[2]"));
		});
	}

	@Test
	public void testStringResource() {
		setYaml("foo # a document that is a literal");
		this.processor.process((properties, map) -> assertEquals("foo", map.get("document")));
	}

	@Test
	public void testBadDocumentStart() {
		setYaml("foo # a document\nbar: baz");
		this.exception.expect(ParserException.class);
		this.exception.expectMessage("line 2, column 1");
		this.processor.process((properties, map) -> {});
	}

	@Test
	public void testBadResource() {
		setYaml("foo: bar\ncd\nspam:\n  foo: baz");
		this.exception.expect(ScannerException.class);
		this.exception.expectMessage("line 3, column 1");
		this.processor.process((properties, map) -> {});
	}

	@Test
	public void mapConvertedToIndexedBeanReference() {
		setYaml("foo: bar\nbar:\n spam: bucket");
		this.processor.process((properties, map) -> {
			assertEquals("bucket", properties.get("bar.spam"));
			assertEquals(2, properties.size());
		});
	}

	@Test
	public void integerKeyBehaves() {
		setYaml("foo: bar\n1: bar");
		this.processor.process((properties, map) -> {
			assertEquals("bar", properties.get("[1]"));
			assertEquals(2, properties.size());
		});
	}

	@Test
	public void integerDeepKeyBehaves() {
		setYaml("foo:\n  1: bar");
		this.processor.process((properties, map) -> {
			assertEquals("bar", properties.get("foo[1]"));
			assertEquals(1, properties.size());
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void flattenedMapIsSameAsPropertiesButOrdered() {
		setYaml("foo: bar\nbar:\n spam: bucket");
		this.processor.process((properties, map) -> {
			assertEquals("bucket", properties.get("bar.spam"));
			assertEquals(2, properties.size());
			Map<String, Object> flattenedMap = processor.getFlattenedMap(map);
			assertEquals("bucket", flattenedMap.get("bar.spam"));
			assertEquals(2, flattenedMap.size());
			assertTrue(flattenedMap instanceof LinkedHashMap);
			Map<String, Object> bar = (Map<String, Object>) map.get("bar");
			assertEquals("bucket", bar.get("spam"));
		});
	}

	@Test
	public void customTypeSupportedByDefault() throws Exception {
		URL url = new URL("https://localhost:9000/");
		setYaml("value: !!java.net.URL [\"" + url + "\"]");

		this.processor.process((properties, map) -> {
			assertEquals(1, properties.size());
			assertEquals(1, map.size());
			assertEquals(url, properties.get("value"));
			assertEquals(url, map.get("value"));
		});
	}

	@Test
	public void customTypesSupportedDueToExplicitConfiguration() throws Exception {
		this.processor.setSupportedTypes(URL.class, String.class);

		URL url = new URL("https://localhost:9000/");
		setYaml("value: !!java.net.URL [!!java.lang.String [\"" + url + "\"]]");

		this.processor.process((properties, map) -> {
			assertEquals(1, properties.size());
			assertEquals(1, map.size());
			assertEquals(url, properties.get("value"));
			assertEquals(url, map.get("value"));
		});
	}

	@Test
	public void customTypeNotSupportedDueToExplicitConfiguration() {
		this.processor.setSupportedTypes(List.class);

		setYaml("value: !!java.net.URL [\"https://localhost:9000/\"]");

		this.exception.expect(ConstructorException.class);
		this.exception.expectMessage("Unsupported type encountered in YAML document: java.net.URL");
		this.processor.process((properties, map) -> {});
	}

	private void setYaml(String yaml) {
		this.processor.setResources(new ByteArrayResource(yaml.getBytes()));
	}

}
