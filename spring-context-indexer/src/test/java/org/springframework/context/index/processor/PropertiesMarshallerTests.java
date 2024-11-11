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

package org.springframework.context.index.processor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesMarshaller}.
 *
 * @author Stephane Nicoll
 * @author Vedran Pavic
 */
class PropertiesMarshallerTests {

	@Test
	void readWrite() throws IOException {
		CandidateComponentsMetadata metadata = new CandidateComponentsMetadata();
		metadata.add(createItem("com.foo", "first", "second"));
		metadata.add(createItem("com.bar", "first"));

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		PropertiesMarshaller.write(metadata, outputStream);
		CandidateComponentsMetadata readMetadata = PropertiesMarshaller.read(
				new ByteArrayInputStream(outputStream.toByteArray()));
		assertThat(readMetadata).has(Metadata.of("com.foo", "first", "second"));
		assertThat(readMetadata).has(Metadata.of("com.bar", "first"));
		assertThat(readMetadata.getItems()).hasSize(2);
	}

	@Test
	void metadataIsWrittenDeterministically() throws IOException {
		CandidateComponentsMetadata metadata = new CandidateComponentsMetadata();
		metadata.add(createItem("com.b", "type"));
		metadata.add(createItem("com.c", "type"));
		metadata.add(createItem("com.a", "type"));

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		PropertiesMarshaller.write(metadata, outputStream);
		String contents = outputStream.toString(StandardCharsets.ISO_8859_1);
		assertThat(contents.split(System.lineSeparator())).containsExactly("com.a=type", "com.b=type", "com.c=type");
	}

	private static ItemMetadata createItem(String type, String... stereotypes) {
		return new ItemMetadata(type, new HashSet<>(Arrays.asList(stereotypes)));
	}

}
