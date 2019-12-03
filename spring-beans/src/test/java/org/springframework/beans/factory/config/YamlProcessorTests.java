/*
 * Copyright 2002-2019 the original author or authors.
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
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.scanner.ScannerException;

import org.springframework.core.io.ByteArrayResource;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link YamlProcessor}.
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class YamlProcessorTests {

	private final YamlProcessor processor = new YamlProcessor() {};


	@Test
	public void arrayConvertedToIndexedBeanReference() {
		this.processor.setResources(new ByteArrayResource("foo: bar\nbar: [1,2,3]".getBytes()));
		this.processor.process((properties, map) -> {
			assertThat(properties.size()).isEqualTo(4);
			assertThat(properties.get("foo")).isEqualTo("bar");
			assertThat(properties.getProperty("foo")).isEqualTo("bar");
			assertThat(properties.get("bar[0]")).isEqualTo(1);
			assertThat(properties.getProperty("bar[0]")).isEqualTo("1");
			assertThat(properties.get("bar[1]")).isEqualTo(2);
			assertThat(properties.getProperty("bar[1]")).isEqualTo("2");
			assertThat(properties.get("bar[2]")).isEqualTo(3);
			assertThat(properties.getProperty("bar[2]")).isEqualTo("3");
		});
	}

	@Test
	public void stringResource() {
		this.processor.setResources(new ByteArrayResource("foo # a document that is a literal".getBytes()));
		this.processor.process((properties, map) -> assertThat(map.get("document")).isEqualTo("foo"));
	}

	@Test
	public void badDocumentStart() {
		this.processor.setResources(new ByteArrayResource("foo # a document\nbar: baz".getBytes()));
		assertThatExceptionOfType(ParserException.class)
			.isThrownBy(() -> this.processor.process((properties, map) -> {}))
			.withMessageContaining("line 2, column 1");
	}

	@Test
	public void badResource() {
		this.processor.setResources(new ByteArrayResource("foo: bar\ncd\nspam:\n  foo: baz".getBytes()));
		assertThatExceptionOfType(ScannerException.class)
			.isThrownBy(() -> this.processor.process((properties, map) -> {}))
			.withMessageContaining("line 3, column 1");
	}

	@Test
	public void mapConvertedToIndexedBeanReference() {
		this.processor.setResources(new ByteArrayResource("foo: bar\nbar:\n spam: bucket".getBytes()));
		this.processor.process((properties, map) -> {
			assertThat(properties.get("bar.spam")).isEqualTo("bucket");
			assertThat(properties).hasSize(2);
		});
	}

	@Test
	public void integerKeyBehaves() {
		this.processor.setResources(new ByteArrayResource("foo: bar\n1: bar".getBytes()));
		this.processor.process((properties, map) -> {
			assertThat(properties.get("[1]")).isEqualTo("bar");
			assertThat(properties).hasSize(2);
		});
	}

	@Test
	public void integerDeepKeyBehaves() {
		this.processor.setResources(new ByteArrayResource("foo:\n  1: bar".getBytes()));
		this.processor.process((properties, map) -> {
			assertThat(properties.get("foo[1]")).isEqualTo("bar");
			assertThat(properties).hasSize(1);
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void flattenedMapIsSameAsPropertiesButOrdered() {
		this.processor.setResources(new ByteArrayResource("cat: dog\nfoo: bar\nbar:\n spam: bucket".getBytes()));
		this.processor.process((properties, map) -> {
			Map<String, Object> flattenedMap = processor.getFlattenedMap(map);
			assertThat(flattenedMap).isInstanceOf(LinkedHashMap.class);

			assertThat(properties).hasSize(3);
			assertThat(flattenedMap).hasSize(3);

			assertThat(properties.get("bar.spam")).isEqualTo("bucket");
			assertThat(flattenedMap.get("bar.spam")).isEqualTo("bucket");

			Map<String, Object> bar = (Map<String, Object>) map.get("bar");
			assertThat(bar.get("spam")).isEqualTo("bucket");

			List<Object> keysFromProperties = properties.keySet().stream().collect(toList());
			List<String> keysFromFlattenedMap = flattenedMap.keySet().stream().collect(toList());
			assertThat(keysFromProperties).containsExactlyInAnyOrderElementsOf(keysFromFlattenedMap);
			// Keys in the Properties object are sorted.
			assertThat(keysFromProperties).containsExactly("bar.spam", "cat", "foo");
			// But the flattened map retains the order from the input.
			assertThat(keysFromFlattenedMap).containsExactly("cat", "foo", "bar.spam");
		});
	}

}
