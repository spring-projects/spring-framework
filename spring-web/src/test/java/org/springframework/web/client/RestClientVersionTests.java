/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.web.client;

import java.io.IOException;
import java.util.function.Consumer;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.client.JdkClientHttpRequestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * {@link RestClient} tests for sending API versions.
 * @author Rossen Stoyanchev
 */
public class RestClientVersionTests {

	private final MockWebServer server = new MockWebServer();

	private final RestClient.Builder restClientBuilder = RestClient.builder()
			.requestFactory(new JdkClientHttpRequestFactory())
			.baseUrl(this.server.url("/").toString());


	@BeforeEach
	void setUp() {
		MockResponse response = new MockResponse();
		response.setHeader("Content-Type", "text/plain").setBody("body");
		this.server.enqueue(response);
	}

	@AfterEach
	void shutdown() throws IOException {
		this.server.shutdown();
	}


	@Test
	void header() {
		performRequest(DefaultApiVersionInserter.fromHeader("X-API-Version"));
		expectRequest(request -> assertThat(request.getHeader("X-API-Version")).isEqualTo("1.2"));
	}

	@Test
	void queryParam() {
		performRequest(DefaultApiVersionInserter.fromQueryParam("api-version"));
		expectRequest(request -> assertThat(request.getPath()).isEqualTo("/path?api-version=1.2"));
	}

	@Test
	void pathSegmentIndexLessThanSize() {
		performRequest(DefaultApiVersionInserter.fromPathSegment(0).withVersionFormatter(v -> "v" + v));
		expectRequest(request -> assertThat(request.getPath()).isEqualTo("/v1.2/path"));
	}

	@Test
	void pathSegmentIndexEqualToSize() {
		performRequest(DefaultApiVersionInserter.fromPathSegment(1).withVersionFormatter(v -> "v" + v));
		expectRequest(request -> assertThat(request.getPath()).isEqualTo("/path/v1.2"));
	}

	@Test
	void pathSegmentIndexGreaterThanSize() {
		assertThatIllegalStateException()
				.isThrownBy(() -> performRequest(DefaultApiVersionInserter.fromPathSegment(2)))
				.withMessage("Cannot insert version into '/path' at path segment index 2");
		}

	private void performRequest(DefaultApiVersionInserter.Builder builder) {
		ApiVersionInserter versionInserter = builder.build();
		RestClient restClient = restClientBuilder.apiVersionInserter(versionInserter).build();

		restClient.get()
				.uri("/path")
				.apiVersion(1.2)
				.retrieve()
				.body(String.class);
	}

	private void expectRequest(Consumer<RecordedRequest> consumer) {
		try {
			consumer.accept(this.server.takeRequest());
		}
		catch (InterruptedException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
