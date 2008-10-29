/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;

import junit.framework.TestCase;

/**
 * Unit tests for the FileCopyUtils class.
 *
 * @author Juergen Hoeller
 * @since 12.03.2005
 */
public class FileCopyUtilsTests extends TestCase {

	public void testCopyFromInputStream() throws IOException {
		byte[] content = "content".getBytes();
		ByteArrayInputStream in = new ByteArrayInputStream(content);
		ByteArrayOutputStream out = new ByteArrayOutputStream(content.length);
		int count = FileCopyUtils.copy(in, out);
		assertEquals(content.length, count);
		assertTrue(Arrays.equals(content, out.toByteArray()));
	}

	public void testCopyFromByteArray() throws IOException {
		byte[] content = "content".getBytes();
		ByteArrayOutputStream out = new ByteArrayOutputStream(content.length);
		FileCopyUtils.copy(content, out);
		assertTrue(Arrays.equals(content, out.toByteArray()));
	}

	public void testCopyToByteArray() throws IOException {
		byte[] content = "content".getBytes();
		ByteArrayInputStream in = new ByteArrayInputStream(content);
		byte[] result = FileCopyUtils.copyToByteArray(in);
		assertTrue(Arrays.equals(content, result));
	}

	public void testCopyFromReader() throws IOException {
		String content = "content";
		StringReader in = new StringReader(content);
		StringWriter out = new StringWriter();
		int count = FileCopyUtils.copy(in, out);
		assertEquals(content.length(), count);
		assertEquals(content, out.toString());
	}

	public void testCopyFromString() throws IOException {
		String content = "content";
		StringWriter out = new StringWriter();
		FileCopyUtils.copy(content, out);
		assertEquals(content, out.toString());
	}

	public void testCopyToString() throws IOException {
		String content = "content";
		StringReader in = new StringReader(content);
		String result = FileCopyUtils.copyToString(in);
		assertEquals(content, result);
	}

}
