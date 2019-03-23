/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.scanner.ScannerException;

import org.springframework.core.io.ByteArrayResource;

import static org.junit.Assert.*;
import static org.springframework.beans.factory.config.YamlProcessor.*;

/**
 * Tests for {@link YamlProcessor}.
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 */
public class YamlProcessorTests {

	private final YamlProcessor processor = new YamlProcessor() {};

	@Rule
	public ExpectedException exception = ExpectedException.none();


	@Test
	public void arrayConvertedToIndexedBeanReference() {
		this.processor.setResources(new ByteArrayResource("foo: bar\nbar: [1,2,3]".getBytes()));
		this.processor.process(new MatchCallback() {
			@Override
			public void process(Properties properties, Map<String, Object> map) {
				assertEquals(4, properties.size());
				assertEquals("bar", properties.get("foo"));
				assertEquals("bar", properties.getProperty("foo"));
				assertEquals(1, properties.get("bar[0]"));
				assertEquals("1", properties.getProperty("bar[0]"));
				assertEquals(2, properties.get("bar[1]"));
				assertEquals("2", properties.getProperty("bar[1]"));
				assertEquals(3, properties.get("bar[2]"));
				assertEquals("3", properties.getProperty("bar[2]"));
			}
		});
	}

	@Test
	public void testStringResource() throws Exception {
		this.processor.setResources(new ByteArrayResource("foo # a document that is a literal".getBytes()));
		this.processor.process(new MatchCallback() {
			@Override
			public void process(Properties properties, Map<String, Object> map) {
				assertEquals("foo", map.get("document"));
			}
		});
	}

	@Test
	public void testBadDocumentStart() throws Exception {
		this.processor.setResources(new ByteArrayResource("foo # a document\nbar: baz".getBytes()));
		this.exception.expect(ParserException.class);
		this.exception.expectMessage("line 2, column 1");
		this.processor.process(new MatchCallback() {
			@Override
			public void process(Properties properties, Map<String, Object> map) {
			}
		});
	}

	@Test
	public void testBadResource() throws Exception {
		this.processor.setResources(new ByteArrayResource("foo: bar\ncd\nspam:\n  foo: baz".getBytes()));
		this.exception.expect(ScannerException.class);
		this.exception.expectMessage("line 3, column 1");
		this.processor.process(new MatchCallback() {
			@Override
			public void process(Properties properties, Map<String, Object> map) {
			}
		});
	}

	@Test
	public void mapConvertedToIndexedBeanReference() {
		this.processor.setResources(new ByteArrayResource("foo: bar\nbar:\n spam: bucket".getBytes()));
		this.processor.process(new MatchCallback() {
			@Override
			public void process(Properties properties, Map<String, Object> map) {
				// System.err.println(properties);
				assertEquals("bucket", properties.get("bar.spam"));
				assertEquals(2, properties.size());
			}
		});
	}

	@Test
	public void integerKeyBehaves() {
		this.processor.setResources(new ByteArrayResource("foo: bar\n1: bar".getBytes()));
		this.processor.process(new MatchCallback() {
			@Override
			public void process(Properties properties, Map<String, Object> map) {
				assertEquals("bar", properties.get("[1]"));
				assertEquals(2, properties.size());
			}
		});
	}

	@Test
	public void integerDeepKeyBehaves() {
		this.processor.setResources(new ByteArrayResource("foo:\n  1: bar".getBytes()));
		this.processor.process(new MatchCallback() {
			@Override
			public void process(Properties properties, Map<String, Object> map) {
				assertEquals("bar", properties.get("foo[1]"));
				assertEquals(1, properties.size());
			}
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void flattenedMapIsSameAsPropertiesButOrdered() {
		this.processor.setResources(new ByteArrayResource("foo: bar\nbar:\n spam: bucket".getBytes()));
		this.processor.process(new MatchCallback() {
			@Override
			public void process(Properties properties, Map<String, Object> map) {
				assertEquals("bucket", properties.get("bar.spam"));
				assertEquals(2, properties.size());
				Map<String, Object> flattenedMap = processor.getFlattenedMap(map);
				assertEquals("bucket", flattenedMap.get("bar.spam"));
				assertEquals(2, flattenedMap.size());
				assertTrue(flattenedMap instanceof LinkedHashMap);
				Map<String, Object> bar = (Map<String, Object>) map.get("bar");
				assertEquals("bucket", bar.get("spam"));
			}
		});
	}

}
