/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.index;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.context.index.Metadata.*;

/**
 * Tests for {@link PropertiesMarshaller}.
 *
 * @author Stephane Nicoll
 */
public class PropertiesMarshallerTests {

	@Test
	public void readWrite() throws IOException {
		CandidateComponentsMetadata metadata = new CandidateComponentsMetadata();
		metadata.add(createItem("com.foo", "first", "second"));
		metadata.add(createItem("com.bar", "first"));

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		PropertiesMarshaller.write(metadata, outputStream);
		CandidateComponentsMetadata readMetadata = PropertiesMarshaller.read(
				new ByteArrayInputStream(outputStream.toByteArray()));
		assertThat(readMetadata, hasComponent("com.foo", "first", "second"));
		assertThat(readMetadata, hasComponent("com.bar", "first"));
		assertThat(readMetadata.getItems(), hasSize(2));
	}

	private static ItemMetadata createItem(String type, String... stereotypes) {
		return new ItemMetadata(type, new HashSet<>(Arrays.asList(stereotypes)));
	}

}
