/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.client;

import java.io.IOException;
import java.util.function.Consumer;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * {@link RestClient} tests for sending API versions.
 * @author Rossen Stoyanchev
 */
class RestClientVersionTests {

	private final MockWebServer server = new MockWebServer();

	private RestClient.Builder restClientBuilder;


	@BeforeEach
	void setUp() throws IOException {
		this.server.start();
		this.restClientBuilder = RestClient.builder()
				.requestFactory(new JdkClientHttpRequestFactory())
				.baseUrl(this.server.url("/").toString());
		MockResponse response = new MockResponse.Builder()
				.setHeader("Content-Type", "text/plain")
				.body("body")
				.build();
		this.server.enqueue(response);
	}

	@AfterEach
	void shutdown() {
		this.server.close();
	}


	@Test
	void header() {
		performRequest(ApiVersionInserter.useHeader("API-Version"));
		expectRequest(request -> assertThat(request.getHeaders().get("API-Version")).isEqualTo("1.2"));
	}

	@Test
	void queryParam() {
		performRequest(ApiVersionInserter.useQueryParam("api-version"));
		expectRequest(request -> assertThat(request.getTarget()).isEqualTo("/path?api-version=1.2"));
	}

	@Test
	void mediaTypeParam() {
		performRequest(ApiVersionInserter.useMediaTypeParam("v"));
		expectRequest(request -> assertThat(request.getHeaders().get("Content-Type")).isEqualTo("application/json;v=1.2"));
	}

	@Test
	void pathSegmentIndexLessThanSize() {
		performRequest(ApiVersionInserter.builder().usePathSegment(0).withVersionFormatter(v -> "v" + v).build());
		expectRequest(request -> assertThat(request.getTarget()).isEqualTo("/v1.2/path"));
	}

	@Test
	void pathSegmentIndexEqualToSize() {
		performRequest(ApiVersionInserter.builder().usePathSegment(1).withVersionFormatter(v -> "v" + v).build());
		expectRequest(request -> assertThat(request.getTarget()).isEqualTo("/path/v1.2"));
	}

	@Test
	void pathSegmentIndexGreaterThanSize() {
		assertThatIllegalStateException()
				.isThrownBy(() -> performRequest(ApiVersionInserter.usePathSegment(2)))
				.withMessage("Cannot insert version into '/path' at path segment index 2");
	}

	@Test
	void defaultVersion() {
		ApiVersionInserter inserter = ApiVersionInserter.useHeader("API-Version");
		RestClient restClient = restClientBuilder.defaultApiVersion(1.2).apiVersionInserter(inserter).build();
		restClient.get().uri("/path").retrieve().body(String.class);

		expectRequest(request -> assertThat(request.getHeaders().get("API-Version")).isEqualTo("1.2"));
	}

	@Test
	void noVersion() {
		ApiVersionInserter inserter = ApiVersionInserter.useHeader("API-Version");
		RestClient restClient = restClientBuilder.defaultApiVersion(1.2).apiVersionInserter(inserter).build();
		restClient.get().uri("/path").apiVersion(null).retrieve().body(String.class);

		expectRequest(request -> assertThat(request.getHeaders().get("API-Version")).isNull());
	}

	private void performRequest(ApiVersionInserter versionInserter) {
		restClientBuilder.apiVersionInserter(versionInserter).build()
				.post().uri("/path")
				.contentType(MediaType.APPLICATION_JSON)
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
