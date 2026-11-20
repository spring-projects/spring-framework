/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
