/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.mock.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

/**
 * @author Juergen Hoeller
 */
class MockMultipartHttpServletRequestTests {

	@Test
	void mockMultipartHttpServletRequestWithByteArray() throws IOException {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		assertThat(request.getFileNames().hasNext()).isFalse();
		assertThat(request.getFile("file1")).isNull();
		assertThat(request.getFile("file2")).isNull();
		assertThat(request.getFileMap()).isEmpty();

		request.addFile(new MockMultipartFile("file1", "myContent1".getBytes()));
		request.addFile(new MockMultipartFile("file2", "myOrigFilename", TEXT_PLAIN_VALUE, "myContent2".getBytes()));
		doTestMultipartHttpServletRequest(request);
	}

	@Test
	void mockMultipartHttpServletRequestWithInputStream() throws IOException {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(new MockMultipartFile("file1", new ByteArrayInputStream("myContent1".getBytes())));
		request.addFile(new MockMultipartFile("file2", "myOrigFilename", TEXT_PLAIN_VALUE, new ByteArrayInputStream(
			"myContent2".getBytes())));
		doTestMultipartHttpServletRequest(request);
	}

	@Test
	void mockMultiPartHttpServletRequestWithMixedData() {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(new MockMultipartFile("file", "myOrigFilename", TEXT_PLAIN_VALUE, "myContent2".getBytes()));

		MockPart metadataPart = new MockPart("metadata", null, "{\"foo\": \"bar\"}".getBytes(), APPLICATION_JSON);
		request.addPart(metadataPart);

		HttpHeaders fileHttpHeaders = request.getMultipartHeaders("file");
		assertThat(fileHttpHeaders).isNotNull();
		assertThat(fileHttpHeaders.getContentType()).isEqualTo(TEXT_PLAIN);

		HttpHeaders dataHttpHeaders = request.getMultipartHeaders("metadata");
		assertThat(dataHttpHeaders).isNotNull();
		assertThat(dataHttpHeaders.getContentType()).isEqualTo(APPLICATION_JSON);
	}

	private void doTestMultipartHttpServletRequest(MultipartHttpServletRequest request) throws IOException {
		Set<String> fileNames = new HashSet<>();
		Iterator<String> fileIter = request.getFileNames();
		while (fileIter.hasNext()) {
			fileNames.add(fileIter.next());
		}
		assertThat(fileNames).hasSize(2);
		assertThat(fileNames).contains("file1");
		assertThat(fileNames).contains("file2");
		MultipartFile file1 = request.getFile("file1");
		MultipartFile file2 = request.getFile("file2");
		Map<String, MultipartFile> fileMap = request.getFileMap();
		List<String> fileMapKeys = new ArrayList<>(fileMap.keySet());
		assertThat(fileMapKeys).hasSize(2);
		assertThat(fileMap.get("file1")).isEqualTo(file1);
		assertThat(fileMap.get("file2")).isEqualTo(file2);

		assertThat(file1.getName()).isEqualTo("file1");
		assertThat(file1.getOriginalFilename()).isEmpty();
		assertThat(file1.getContentType()).isNull();
		assertThat(ObjectUtils.nullSafeEquals("myContent1".getBytes(), file1.getBytes())).isTrue();
		assertThat(ObjectUtils.nullSafeEquals("myContent1".getBytes(),
			FileCopyUtils.copyToByteArray(file1.getInputStream()))).isTrue();
		assertThat(file2.getName()).isEqualTo("file2");
		assertThat(file2.getOriginalFilename()).isEqualTo("myOrigFilename");
		assertThat(file2.getContentType()).isEqualTo(TEXT_PLAIN_VALUE);
		assertThat(ObjectUtils.nullSafeEquals("myContent2".getBytes(), file2.getBytes())).isTrue();
		assertThat(ObjectUtils.nullSafeEquals("myContent2".getBytes(),
			FileCopyUtils.copyToByteArray(file2.getInputStream()))).isTrue();
	}

}
