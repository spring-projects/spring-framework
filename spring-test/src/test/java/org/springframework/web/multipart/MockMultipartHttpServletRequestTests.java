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

package org.springframework.web.multipart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ObjectUtils;

/**
 * @author Juergen Hoeller
 */
public class MockMultipartHttpServletRequestTests {

	@Test
	public void mockMultipartHttpServletRequestWithByteArray() throws IOException {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		assertFalse(request.getFileNames().hasNext());
		assertNull(request.getFile("file1"));
		assertNull(request.getFile("file2"));
		assertTrue(request.getFileMap().isEmpty());

		request.addFile(new MockMultipartFile("file1", "myContent1".getBytes()));
		request.addFile(new MockMultipartFile("file2", "myOrigFilename", "text/plain", "myContent2".getBytes()));
		doTestMultipartHttpServletRequest(request);
	}

	@Test
	public void mockMultipartHttpServletRequestWithInputStream() throws IOException {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(new MockMultipartFile("file1", new ByteArrayInputStream("myContent1".getBytes())));
		request.addFile(new MockMultipartFile("file2", "myOrigFilename", "text/plain", new ByteArrayInputStream(
			"myContent2".getBytes())));
		doTestMultipartHttpServletRequest(request);
	}

	private void doTestMultipartHttpServletRequest(MultipartHttpServletRequest request) throws IOException {
		Set<String> fileNames = new HashSet<String>();
		Iterator<String> fileIter = request.getFileNames();
		while (fileIter.hasNext()) {
			fileNames.add(fileIter.next());
		}
		assertEquals(2, fileNames.size());
		assertTrue(fileNames.contains("file1"));
		assertTrue(fileNames.contains("file2"));
		MultipartFile file1 = request.getFile("file1");
		MultipartFile file2 = request.getFile("file2");
		Map<String, MultipartFile> fileMap = request.getFileMap();
		List<String> fileMapKeys = new LinkedList<String>(fileMap.keySet());
		assertEquals(2, fileMapKeys.size());
		assertEquals(file1, fileMap.get("file1"));
		assertEquals(file2, fileMap.get("file2"));

		assertEquals("file1", file1.getName());
		assertEquals("", file1.getOriginalFilename());
		assertNull(file1.getContentType());
		assertTrue(ObjectUtils.nullSafeEquals("myContent1".getBytes(), file1.getBytes()));
		assertTrue(ObjectUtils.nullSafeEquals("myContent1".getBytes(),
			FileCopyUtils.copyToByteArray(file1.getInputStream())));
		assertEquals("file2", file2.getName());
		assertEquals("myOrigFilename", file2.getOriginalFilename());
		assertEquals("text/plain", file2.getContentType());
		assertTrue(ObjectUtils.nullSafeEquals("myContent2".getBytes(), file2.getBytes()));
		assertTrue(ObjectUtils.nullSafeEquals("myContent2".getBytes(),
			FileCopyUtils.copyToByteArray(file2.getInputStream())));
	}

}
