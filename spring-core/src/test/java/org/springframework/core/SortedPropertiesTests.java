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

package org.springframework.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link SortedProperties}.
 *
 * @author Sam Brannen
 * @since 5.2
 */
class SortedPropertiesTests {

	@Test
	void keys() {
		assertKeys(createSortedProps());
	}

	@Test
	void keysFromPrototype() {
		assertKeys(createSortedPropsFromPrototype());
	}

	@Test
	void keySet() {
		assertKeySet(createSortedProps());
	}

	@Test
	void keySetFromPrototype() {
		assertKeySet(createSortedPropsFromPrototype());
	}

	@Test
	void entrySet() {
		assertEntrySet(createSortedProps());
	}

	@Test
	void entrySetFromPrototype() {
		assertEntrySet(createSortedPropsFromPrototype());
	}

	@Test
	void sortsPropertiesUsingOutputStream() throws IOException {
		SortedProperties sortedProperties = createSortedProps();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		sortedProperties.store(baos, "custom comment");

		String[] lines = lines(baos);
		assertThat(lines).hasSize(7);
		assertThat(lines[0]).isEqualTo("#custom comment");
		assertThat(lines[1]).as("timestamp").startsWith("#");

		assertPropsAreSorted(lines);
	}

	@Test
	void sortsPropertiesUsingWriter() throws IOException {
		SortedProperties sortedProperties = createSortedProps();

		StringWriter writer = new StringWriter();
		sortedProperties.store(writer, "custom comment");

		String[] lines = lines(writer);
		assertThat(lines).hasSize(7);
		assertThat(lines[0]).isEqualTo("#custom comment");
		assertThat(lines[1]).as("timestamp").startsWith("#");

		assertPropsAreSorted(lines);
	}

	@Test
	void sortsPropertiesAndOmitsCommentsUsingOutputStream() throws IOException {
		SortedProperties sortedProperties = createSortedProps(true);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		sortedProperties.store(baos, "custom comment");

		String[] lines = lines(baos);
		assertThat(lines).hasSize(5);

		assertPropsAreSorted(lines);
	}

	@Test
	void sortsPropertiesAndOmitsCommentsUsingWriter() throws IOException {
		SortedProperties sortedProperties = createSortedProps(true);

		StringWriter writer = new StringWriter();
		sortedProperties.store(writer, "custom comment");

		String[] lines = lines(writer);
		assertThat(lines).hasSize(5);

		assertPropsAreSorted(lines);
	}

	@Test
	void storingAsXmlSortsPropertiesAndOmitsComments() throws IOException {
		SortedProperties sortedProperties = createSortedProps(true);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		sortedProperties.storeToXML(baos, "custom comment");

		String[] lines = lines(baos);

		assertThat(lines).isNotEmpty();
		// Leniently match first line due to differences between JDK 8 and JDK 9+.
		String regex = "<\\?xml .*\\?>";
		assertThat(lines[0]).matches(regex);
		assertThat(lines).filteredOn(line -> !line.matches(regex)).containsExactly( //
			"<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">", //
			"<properties>", //
			"<entry key=\"color\">blue</entry>", //
			"<entry key=\"fragrance\">sweet</entry>", //
			"<entry key=\"fruit\">apple</entry>", //
			"<entry key=\"size\">medium</entry>", //
			"<entry key=\"vehicle\">car</entry>", //
			"</properties>" //
		);
	}

	private SortedProperties createSortedProps() {
		return createSortedProps(false);
	}

	private SortedProperties createSortedProps(boolean omitComments) {
		SortedProperties sortedProperties = new SortedProperties(omitComments);
		populateProperties(sortedProperties);
		return sortedProperties;
	}

	private SortedProperties createSortedPropsFromPrototype() {
		Properties properties = new Properties();
		populateProperties(properties);
		return new SortedProperties(properties, false);
	}

	private void populateProperties(Properties properties) {
		properties.setProperty("color", "blue");
		properties.setProperty("fragrance", "sweet");
		properties.setProperty("fruit", "apple");
		properties.setProperty("size", "medium");
		properties.setProperty("vehicle", "car");
	}

	private String[] lines(ByteArrayOutputStream baos) {
		return lines(new String(baos.toByteArray(), StandardCharsets.ISO_8859_1));
	}

	private String[] lines(StringWriter writer) {
		return lines(writer.toString());
	}

	private String[] lines(String input) {
		return input.trim().split(SortedProperties.EOL);
	}

	private void assertKeys(Properties properties) {
		assertThat(Collections.list(properties.keys())) //
				.containsExactly("color", "fragrance", "fruit", "size", "vehicle");
	}

	private void assertKeySet(Properties properties) {
		assertThat(properties.keySet()).containsExactly("color", "fragrance", "fruit", "size", "vehicle");
	}

	private void assertEntrySet(Properties properties) {
		assertThat(properties.entrySet()).containsExactly( //
			entry("color", "blue"), //
			entry("fragrance", "sweet"), //
			entry("fruit", "apple"), //
			entry("size", "medium"), //
			entry("vehicle", "car") //
		);
	}

	private void assertPropsAreSorted(String[] lines) {
		assertThat(stream(lines).filter(s -> !s.startsWith("#"))).containsExactly( //
			"color=blue", //
			"fragrance=sweet", //
			"fruit=apple", //
			"size=medium", //
			"vehicle=car"//
		);
	}

}
