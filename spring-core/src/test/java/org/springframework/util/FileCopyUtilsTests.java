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

package org.springframework.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FileCopyUtils}.
 *
 * @author Juergen Hoeller
 * @since 12.03.2005
 */
class FileCopyUtilsTests {

	@Test
	void copyFromInputStream() throws IOException {
		byte[] content = "content".getBytes();
		ByteArrayInputStream in = new ByteArrayInputStream(content);
		ByteArrayOutputStream out = new ByteArrayOutputStream(content.length);
		int count = FileCopyUtils.copy(in, out);
		assertThat(count).isEqualTo(content.length);
		assertThat(Arrays.equals(content, out.toByteArray())).isTrue();
	}

	@Test
	void copyFromByteArray() throws IOException {
		byte[] content = "content".getBytes();
		ByteArrayOutputStream out = new ByteArrayOutputStream(content.length);
		FileCopyUtils.copy(content, out);
		assertThat(Arrays.equals(content, out.toByteArray())).isTrue();
	}

	@Test
	void copyToByteArray() throws IOException {
		byte[] content = "content".getBytes();
		ByteArrayInputStream in = new ByteArrayInputStream(content);
		byte[] result = FileCopyUtils.copyToByteArray(in);
		assertThat(Arrays.equals(content, result)).isTrue();
	}

	@Test
	void copyFromReader() throws IOException {
		String content = "content";
		StringReader in = new StringReader(content);
		StringWriter out = new StringWriter();
		int count = FileCopyUtils.copy(in, out);
		assertThat(count).isEqualTo(content.length());
		assertThat(out.toString()).isEqualTo(content);
	}

	@Test
	void copyFromString() throws IOException {
		String content = "content";
		StringWriter out = new StringWriter();
		FileCopyUtils.copy(content, out);
		assertThat(out.toString()).isEqualTo(content);
	}

	@Test
	void copyToString() throws IOException {
		String content = "content";
		StringReader in = new StringReader(content);
		String result = FileCopyUtils.copyToString(in);
		assertThat(result).isEqualTo(content);
	}

}
