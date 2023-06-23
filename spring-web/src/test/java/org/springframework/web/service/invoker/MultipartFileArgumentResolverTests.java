/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.service.invoker;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.testfixture.servlet.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MultipartFileArgumentResolver}.
 * Tests for base class functionality of this resolver can be found in {@link NamedValueArgumentResolverTests}.
 *
 * @author Olga Maciaszek-Sharma
 */
@SuppressWarnings("unchecked")
class MultipartFileArgumentResolverTests {

	private final TestHttpClientAdapter clientAdapter = new TestHttpClientAdapter();

	private TestClient client;

	@BeforeEach
	void setUp() {
		HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builder(this.clientAdapter).build();
		this.client = proxyFactory.createClient(TestClient.class);
	}

	@Test
	void multipartFile() {
		String fileName = "testFileName";
		String originalFileName = "originalTestFileName";
		MultipartFile testFile = new MockMultipartFile(fileName, originalFileName,
				MediaType.APPLICATION_JSON_VALUE, "test".getBytes());
		this.client.postMultipartFile(testFile);

		Object body = clientAdapter.getRequestValues().getBodyValue();

		assertThat(body).isInstanceOf(MultiValueMap.class);
		MultiValueMap<String, HttpEntity<?>> map = (MultiValueMap<String, HttpEntity<?>>) body;
		assertThat(map.size()).isEqualTo(1);
		assertThat(map.getFirst("file")).isNotNull();
		HttpEntity<?> fileEntity = map.getFirst("file");
		assertThat(fileEntity.getBody()).isEqualTo(testFile.getResource());
		HttpHeaders headers = fileEntity.getHeaders();
		assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
		ContentDisposition contentDisposition = headers.getContentDisposition();
		assertThat(contentDisposition.getType()).isEqualTo("form-data");
		assertThat(contentDisposition.getName()).isEqualTo("file");
		assertThat(contentDisposition.getFilename()).isEqualTo(originalFileName);
	}

	@Test
	void optionalMultipartFile() {
		this.client.postOptionalMultipartFile(Optional.empty(), "anotherPart");

		Object body = clientAdapter.getRequestValues().getBodyValue();

		assertThat(body).isInstanceOf(MultiValueMap.class);
		MultiValueMap<String, HttpEntity<?>> map = (MultiValueMap<String, HttpEntity<?>>) body;
		assertThat(map.size()).isEqualTo(1);
		assertThat(map.getFirst("anotherPart")).isNotNull();
	}

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	private interface TestClient {

		@PostExchange
		void postMultipartFile(MultipartFile file);

		@PostExchange
		void postOptionalMultipartFile(Optional<MultipartFile> file, @RequestPart String anotherPart);

	}
}
