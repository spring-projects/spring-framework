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

import org.junit.jupiter.api.Test;

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
 * Tests for base class functionality of this resolver can be found in
 * {@link NamedValueArgumentResolverTests}.
 *
 * @author Olga Maciaszek-Sharma
 */
@SuppressWarnings("unchecked")
class MultipartFileArgumentResolverTests {

	private final TestExchangeAdapter client = new TestExchangeAdapter();

	private final MultipartService multipartService =
			HttpServiceProxyFactory.builderFor(this.client).build().createClient(MultipartService.class);


	@Test
	void multipartFile() {
		String fileName = "testFileName";
		String originalFileName = "originalTestFileName";
		MultipartFile testFile = new MockMultipartFile(fileName, originalFileName, "text/plain", "test".getBytes());

		this.multipartService.postMultipartFile(testFile);
		Object value = this.client.getRequestValues().getBodyValue();

		assertThat(value).isInstanceOf(MultiValueMap.class);
		MultiValueMap<String, HttpEntity<?>> map = (MultiValueMap<String, HttpEntity<?>>) value;
		assertThat(map).hasSize(1);

		HttpEntity<?> entity = map.getFirst("file");
		assertThat(entity).isNotNull();
		assertThat(entity.getBody()).isEqualTo(testFile.getResource());

		HttpHeaders headers = entity.getHeaders();
		assertThat(headers.getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
		assertThat(headers.getContentDisposition().getType()).isEqualTo("form-data");
		assertThat(headers.getContentDisposition().getName()).isEqualTo("file");
		assertThat(headers.getContentDisposition().getFilename()).isEqualTo(originalFileName);
	}

	@Test
	void optionalMultipartFile() {
		this.multipartService.postOptionalMultipartFile(Optional.empty(), "anotherPart");
		Object value = client.getRequestValues().getBodyValue();

		assertThat(value).isInstanceOf(MultiValueMap.class);
		MultiValueMap<String, HttpEntity<?>> map = (MultiValueMap<String, HttpEntity<?>>) value;
		assertThat(map).containsOnlyKeys("anotherPart");
	}


	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	private interface MultipartService {

		@PostExchange
		void postMultipartFile(MultipartFile file);

		@PostExchange
		void postOptionalMultipartFile(Optional<MultipartFile> file, @RequestPart String anotherPart);

	}

}
