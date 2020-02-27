/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.reactive.resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.testfixture.io.buffer.DataBufferTestUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ResourceWebHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class ResourceWebHandlerTests {

	private static final Duration TIMEOUT = Duration.ofSeconds(1);


	private ResourceWebHandler handler;

	private DataBufferFactory bufferFactory = new DefaultDataBufferFactory();


	@BeforeEach
	public void setup() throws Exception {
		List<Resource> locations = new ArrayList<>(2);
		locations.add(new ClassPathResource("test/", getClass()));
		locations.add(new ClassPathResource("testalternatepath/", getClass()));
		locations.add(new ClassPathResource("META-INF/resources/webjars/"));

		this.handler = new ResourceWebHandler();
		this.handler.setLocations(locations);
		this.handler.setCacheControl(CacheControl.maxAge(3600, TimeUnit.SECONDS));
		this.handler.afterPropertiesSet();
	}


	@Test
	public void getResource() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
		setPathWithinHandlerMapping(exchange, "foo.css");
		this.handler.handle(exchange).block(TIMEOUT);

		HttpHeaders headers = exchange.getResponse().getHeaders();
		assertThat(headers.getContentType()).isEqualTo(MediaType.parseMediaType("text/css"));
		assertThat(headers.getContentLength()).isEqualTo(17);
		assertThat(headers.getCacheControl()).isEqualTo("max-age=3600");
		assertThat(headers.containsKey("Last-Modified")).isTrue();
		assertThat(resourceLastModifiedDate("test/foo.css") / 1000).isEqualTo(headers.getLastModified() / 1000);
		assertThat(headers.getFirst("Accept-Ranges")).isEqualTo("bytes");
		assertThat(headers.get("Accept-Ranges").size()).isEqualTo(1);
		assertResponseBody(exchange, "h1 { color:red; }");
	}

	@Test
	public void getResourceHttpHeader() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.head(""));
		setPathWithinHandlerMapping(exchange, "foo.css");
		this.handler.handle(exchange).block(TIMEOUT);

		assertThat((Object) exchange.getResponse().getStatusCode()).isNull();
		HttpHeaders headers = exchange.getResponse().getHeaders();
		assertThat(headers.getContentType()).isEqualTo(MediaType.parseMediaType("text/css"));
		assertThat(headers.getContentLength()).isEqualTo(17);
		assertThat(headers.getCacheControl()).isEqualTo("max-age=3600");
		assertThat(headers.containsKey("Last-Modified")).isTrue();
		assertThat(resourceLastModifiedDate("test/foo.css") / 1000).isEqualTo(headers.getLastModified() / 1000);
		assertThat(headers.getFirst("Accept-Ranges")).isEqualTo("bytes");
		assertThat(headers.get("Accept-Ranges").size()).isEqualTo(1);

		StepVerifier.create(exchange.getResponse().getBody())
				.expectErrorMatches(ex -> ex.getMessage().startsWith("No content was written"))
				.verify();
	}

	@Test
	public void getResourceHttpOptions() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.options(""));
		setPathWithinHandlerMapping(exchange, "foo.css");
		this.handler.handle(exchange).block(TIMEOUT);

		assertThat(exchange.getResponse().getStatusCode()).isNull();
		assertThat(exchange.getResponse().getHeaders().getFirst("Allow")).isEqualTo("GET,HEAD,OPTIONS");
	}

	@Test
	public void getResourceNoCache() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
		setPathWithinHandlerMapping(exchange, "foo.css");
		this.handler.setCacheControl(CacheControl.noStore());
		this.handler.handle(exchange).block(TIMEOUT);

		MockServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
		assertThat(response.getHeaders().containsKey("Last-Modified")).isTrue();
		assertThat(resourceLastModifiedDate("test/foo.css") / 1000).isEqualTo(response.getHeaders().getLastModified() / 1000);
		assertThat(response.getHeaders().getFirst("Accept-Ranges")).isEqualTo("bytes");
		assertThat(response.getHeaders().get("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	public void getVersionedResource() throws Exception {
		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.addFixedVersionStrategy("versionString", "/**");
		this.handler.setResourceResolvers(Arrays.asList(versionResolver, new PathResourceResolver()));
		this.handler.afterPropertiesSet();

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
		setPathWithinHandlerMapping(exchange, "versionString/foo.css");
		this.handler.handle(exchange).block(TIMEOUT);

		assertThat(exchange.getResponse().getHeaders().getETag()).isEqualTo("\"versionString\"");
		assertThat(exchange.getResponse().getHeaders().getFirst("Accept-Ranges")).isEqualTo("bytes");
		assertThat(exchange.getResponse().getHeaders().get("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	public void getResourceWithHtmlMediaType() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
		setPathWithinHandlerMapping(exchange, "foo.html");
		this.handler.handle(exchange).block(TIMEOUT);

		HttpHeaders headers = exchange.getResponse().getHeaders();
		assertThat(headers.getContentType()).isEqualTo(MediaType.TEXT_HTML);
		assertThat(headers.getCacheControl()).isEqualTo("max-age=3600");
		assertThat(headers.containsKey("Last-Modified")).isTrue();
		assertThat(resourceLastModifiedDate("test/foo.html") / 1000).isEqualTo(headers.getLastModified() / 1000);
		assertThat(headers.getFirst("Accept-Ranges")).isEqualTo("bytes");
		assertThat(headers.get("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	public void getResourceFromAlternatePath() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
		setPathWithinHandlerMapping(exchange, "baz.css");
		this.handler.handle(exchange).block(TIMEOUT);

		HttpHeaders headers = exchange.getResponse().getHeaders();
		assertThat(headers.getContentType()).isEqualTo(MediaType.parseMediaType("text/css"));
		assertThat(headers.getContentLength()).isEqualTo(17);
		assertThat(headers.getCacheControl()).isEqualTo("max-age=3600");
		assertThat(headers.containsKey("Last-Modified")).isTrue();
		assertThat(resourceLastModifiedDate("testalternatepath/baz.css") / 1000).isEqualTo(headers.getLastModified() / 1000);
		assertThat(headers.getFirst("Accept-Ranges")).isEqualTo("bytes");
		assertThat(headers.get("Accept-Ranges").size()).isEqualTo(1);
		assertResponseBody(exchange, "h1 { color:red; }");
	}

	@Test
	public void getResourceFromSubDirectory() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
		setPathWithinHandlerMapping(exchange, "js/foo.js");
		this.handler.handle(exchange).block(TIMEOUT);

		assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("application/javascript"));
		assertResponseBody(exchange, "function foo() { console.log(\"hello world\"); }");
	}

	@Test
	public void getResourceFromSubDirectoryOfAlternatePath() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
		setPathWithinHandlerMapping(exchange, "js/baz.js");
		this.handler.handle(exchange).block(TIMEOUT);

		HttpHeaders headers = exchange.getResponse().getHeaders();
		assertThat(headers.getContentType()).isEqualTo(MediaType.parseMediaType("application/javascript"));
		assertResponseBody(exchange, "function foo() { console.log(\"hello world\"); }");
	}

	@Test // SPR-14577
	public void getMediaTypeWithFavorPathExtensionOff() throws Exception {
		List<Resource> paths = Collections.singletonList(new ClassPathResource("test/", getClass()));
		ResourceWebHandler handler = new ResourceWebHandler();
		handler.setLocations(paths);
		handler.afterPropertiesSet();

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("")
				.header("Accept", "application/json,text/plain,*/*"));
		setPathWithinHandlerMapping(exchange, "foo.html");
		handler.handle(exchange).block(TIMEOUT);

		assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.TEXT_HTML);
	}

	@Test
	public void testInvalidPath() throws Exception {

		// Use mock ResourceResolver: i.e. we're only testing upfront validations...

		Resource resource = mock(Resource.class);
		given(resource.getFilename()).willThrow(new AssertionError("Resource should not be resolved"));
		given(resource.getInputStream()).willThrow(new AssertionError("Resource should not be resolved"));
		ResourceResolver resolver = mock(ResourceResolver.class);
		given(resolver.resolveResource(any(), any(), any(), any())).willReturn(Mono.just(resource));

		ResourceWebHandler handler = new ResourceWebHandler();
		handler.setLocations(Collections.singletonList(new ClassPathResource("test/", getClass())));
		handler.setResourceResolvers(Collections.singletonList(resolver));
		handler.afterPropertiesSet();

		testInvalidPath("../testsecret/secret.txt", handler);
		testInvalidPath("test/../../testsecret/secret.txt", handler);
		testInvalidPath(":/../../testsecret/secret.txt", handler);

		Resource location = new UrlResource(getClass().getResource("./test/"));
		this.handler.setLocations(Collections.singletonList(location));
		Resource secretResource = new UrlResource(getClass().getResource("testsecret/secret.txt"));
		String secretPath = secretResource.getURL().getPath();

		testInvalidPath("file:" + secretPath, handler);
		testInvalidPath("/file:" + secretPath, handler);
		testInvalidPath("url:" + secretPath, handler);
		testInvalidPath("/url:" + secretPath, handler);
		testInvalidPath("/../.." + secretPath, handler);
		testInvalidPath("/%2E%2E/testsecret/secret.txt", handler);
		testInvalidPath("/%2E%2E/testsecret/secret.txt", handler);
		testInvalidPath("%2F%2F%2E%2E%2F%2F%2E%2E" + secretPath, handler);
	}

	private void testInvalidPath(String requestPath, ResourceWebHandler handler) {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
		setPathWithinHandlerMapping(exchange, requestPath);

		StepVerifier.create(handler.handle(exchange))
				.expectErrorSatisfies(err -> {
					assertThat(err).isInstanceOf(ResponseStatusException.class);
					assertThat(((ResponseStatusException) err).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
				}).verify(TIMEOUT);
	}

	@Test
	public void testResolvePathWithTraversal() throws Exception {
		for (HttpMethod method : HttpMethod.values()) {
			testResolvePathWithTraversal(method);
		}
	}

	private void testResolvePathWithTraversal(HttpMethod httpMethod) throws Exception {
		Resource location = new ClassPathResource("test/", getClass());
		this.handler.setLocations(Collections.singletonList(location));

		testResolvePathWithTraversal(httpMethod, "../testsecret/secret.txt", location);
		testResolvePathWithTraversal(httpMethod, "test/../../testsecret/secret.txt", location);
		testResolvePathWithTraversal(httpMethod, ":/../../testsecret/secret.txt", location);

		location = new UrlResource(getClass().getResource("./test/"));
		this.handler.setLocations(Collections.singletonList(location));
		Resource secretResource = new UrlResource(getClass().getResource("testsecret/secret.txt"));
		String secretPath = secretResource.getURL().getPath();

		testResolvePathWithTraversal(httpMethod, "file:" + secretPath, location);
		testResolvePathWithTraversal(httpMethod, "/file:" + secretPath, location);
		testResolvePathWithTraversal(httpMethod, "url:" + secretPath, location);
		testResolvePathWithTraversal(httpMethod, "/url:" + secretPath, location);
		testResolvePathWithTraversal(httpMethod, "////../.." + secretPath, location);
		testResolvePathWithTraversal(httpMethod, "/%2E%2E/testsecret/secret.txt", location);
		testResolvePathWithTraversal(httpMethod, "%2F%2F%2E%2E%2F%2Ftestsecret/secret.txt", location);
		testResolvePathWithTraversal(httpMethod, "url:" + secretPath, location);

		// The following tests fail with a MalformedURLException on Windows
		// testResolvePathWithTraversal(location, "/" + secretPath);
		// testResolvePathWithTraversal(location, "/  " + secretPath);
	}

	private void testResolvePathWithTraversal(HttpMethod httpMethod, String requestPath, Resource location)
			throws Exception {

		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.method(httpMethod, ""));
		setPathWithinHandlerMapping(exchange, requestPath);
		StepVerifier.create(this.handler.handle(exchange))
				.expectErrorSatisfies(err -> {
					assertThat(err).isInstanceOf(ResponseStatusException.class);
					assertThat(((ResponseStatusException) err).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
				})
				.verify(TIMEOUT);
		if (!location.createRelative(requestPath).exists() && !requestPath.contains(":")) {
			fail(requestPath + " doesn't actually exist as a relative path");
		}
	}

	@Test
	public void processPath() {
		assertThat(this.handler.processPath("/foo/bar")).isSameAs("/foo/bar");
		assertThat(this.handler.processPath("foo/bar")).isSameAs("foo/bar");

		// leading whitespace control characters (00-1F)
		assertThat(this.handler.processPath("  /foo/bar")).isEqualTo("/foo/bar");
		assertThat(this.handler.processPath((char) 1 + "/foo/bar")).isEqualTo("/foo/bar");
		assertThat(this.handler.processPath((char) 31 + "/foo/bar")).isEqualTo("/foo/bar");
		assertThat(this.handler.processPath("  foo/bar")).isEqualTo("foo/bar");
		assertThat(this.handler.processPath((char) 31 + "foo/bar")).isEqualTo("foo/bar");

		// leading control character 0x7F (DEL)
		assertThat(this.handler.processPath((char) 127 + "/foo/bar")).isEqualTo("/foo/bar");
		assertThat(this.handler.processPath((char) 127 + "/foo/bar")).isEqualTo("/foo/bar");

		// leading control and '/' characters
		assertThat(this.handler.processPath("  /  foo/bar")).isEqualTo("/foo/bar");
		assertThat(this.handler.processPath("  /  /  foo/bar")).isEqualTo("/foo/bar");
		assertThat(this.handler.processPath("  // /// ////  foo/bar")).isEqualTo("/foo/bar");
		assertThat(this.handler.processPath((char) 1 + " / " + (char) 127 + " // foo/bar")).isEqualTo("/foo/bar");

		// root or empty path
		assertThat(this.handler.processPath("   ")).isEqualTo("");
		assertThat(this.handler.processPath("/")).isEqualTo("/");
		assertThat(this.handler.processPath("///")).isEqualTo("/");
		assertThat(this.handler.processPath("/ /   / ")).isEqualTo("/");
	}

	@Test
	public void initAllowedLocations() {
		PathResourceResolver resolver = (PathResourceResolver) this.handler.getResourceResolvers().get(0);
		Resource[] locations = resolver.getAllowedLocations();

		assertThat(locations.length).isEqualTo(3);
		assertThat(((ClassPathResource) locations[0]).getPath()).isEqualTo("test/");
		assertThat(((ClassPathResource) locations[1]).getPath()).isEqualTo("testalternatepath/");
		assertThat(((ClassPathResource) locations[2]).getPath()).isEqualTo("META-INF/resources/webjars/");
	}

	@Test
	public void initAllowedLocationsWithExplicitConfiguration() throws Exception {
		ClassPathResource location1 = new ClassPathResource("test/", getClass());
		ClassPathResource location2 = new ClassPathResource("testalternatepath/", getClass());

		PathResourceResolver pathResolver = new PathResourceResolver();
		pathResolver.setAllowedLocations(location1);

		ResourceWebHandler handler = new ResourceWebHandler();
		handler.setResourceResolvers(Collections.singletonList(pathResolver));
		handler.setLocations(Arrays.asList(location1, location2));
		handler.afterPropertiesSet();

		Resource[] locations = pathResolver.getAllowedLocations();
		assertThat(locations.length).isEqualTo(1);
		assertThat(((ClassPathResource) locations[0]).getPath()).isEqualTo("test/");
	}

	@Test
	public void notModified() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("").ifModifiedSince(resourceLastModified("test/foo.css")));

		setPathWithinHandlerMapping(exchange, "foo.css");
		this.handler.handle(exchange).block(TIMEOUT);
		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
	}

	@Test
	public void modified() throws Exception {
		long timestamp = resourceLastModified("test/foo.css") / 1000 * 1000 - 1;
		MockServerHttpRequest request = MockServerHttpRequest.get("").ifModifiedSince(timestamp).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		setPathWithinHandlerMapping(exchange, "foo.css");
		this.handler.handle(exchange).block(TIMEOUT);

		assertThat((Object) exchange.getResponse().getStatusCode()).isNull();
		assertResponseBody(exchange, "h1 { color:red; }");
	}

	@Test
	public void directory() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
		setPathWithinHandlerMapping(exchange, "js/");
		StepVerifier.create(this.handler.handle(exchange))
				.expectErrorSatisfies(err -> {
					assertThat(err).isInstanceOf(ResponseStatusException.class);
					assertThat(((ResponseStatusException) err).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
				}).verify(TIMEOUT);
	}

	@Test
	public void directoryInJarFile() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
		setPathWithinHandlerMapping(exchange, "underscorejs/");
		StepVerifier.create(this.handler.handle(exchange))
				.expectErrorSatisfies(err -> {
					assertThat(err).isInstanceOf(ResponseStatusException.class);
					assertThat(((ResponseStatusException) err).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
				}).verify(TIMEOUT);
	}

	@Test
	public void missingResourcePath() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
		setPathWithinHandlerMapping(exchange, "");
		StepVerifier.create(this.handler.handle(exchange))
				.expectErrorSatisfies(err -> {
					assertThat(err).isInstanceOf(ResponseStatusException.class);
					assertThat(((ResponseStatusException) err).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
				}).verify(TIMEOUT);
	}

	@Test
	public void noPathWithinHandlerMappingAttribute() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
		assertThatIllegalArgumentException().isThrownBy(() ->
				this.handler.handle(exchange).block(TIMEOUT));
	}

	@Test
	public void unsupportedHttpMethod() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post(""));
		setPathWithinHandlerMapping(exchange, "foo.css");
		assertThatExceptionOfType(MethodNotAllowedException.class).isThrownBy(() ->
				this.handler.handle(exchange).block(TIMEOUT));
	}

	@Test
	public void resourceNotFound() throws Exception {
		for (HttpMethod method : HttpMethod.values()) {
			resourceNotFound(method);
		}
	}

	private void resourceNotFound(HttpMethod httpMethod) {
		MockServerHttpRequest request = MockServerHttpRequest.method(httpMethod, "").build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		setPathWithinHandlerMapping(exchange, "not-there.css");
		Mono<Void> mono = this.handler.handle(exchange);

		StepVerifier.create(mono)
				.expectErrorSatisfies(err -> {
					assertThat(err).isInstanceOf(ResponseStatusException.class);
					assertThat(((ResponseStatusException) err).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
				}).verify(TIMEOUT);

		// SPR-17475
		AtomicReference<Throwable> exceptionRef = new AtomicReference<>();
		StepVerifier.create(mono).consumeErrorWith(exceptionRef::set).verify();
		StepVerifier.create(mono).consumeErrorWith(ex -> assertThat(ex).isNotSameAs(exceptionRef.get())).verify();
	}

	@Test
	public void partialContentByteRange() {
		MockServerHttpRequest request = MockServerHttpRequest.get("").header("Range", "bytes=0-1").build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		setPathWithinHandlerMapping(exchange, "foo.txt");
		this.handler.handle(exchange).block(TIMEOUT);

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
		assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
		assertThat(exchange.getResponse().getHeaders().getContentLength()).isEqualTo(2);
		assertThat(exchange.getResponse().getHeaders().getFirst("Content-Range")).isEqualTo("bytes 0-1/10");
		assertThat(exchange.getResponse().getHeaders().getFirst("Accept-Ranges")).isEqualTo("bytes");
		assertThat(exchange.getResponse().getHeaders().get("Accept-Ranges").size()).isEqualTo(1);
		assertResponseBody(exchange, "So");
	}

	@Test
	public void partialContentByteRangeNoEnd() {
		MockServerHttpRequest request = MockServerHttpRequest.get("").header("range", "bytes=9-").build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		setPathWithinHandlerMapping(exchange, "foo.txt");
		this.handler.handle(exchange).block(TIMEOUT);

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
		assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
		assertThat(exchange.getResponse().getHeaders().getContentLength()).isEqualTo(1);
		assertThat(exchange.getResponse().getHeaders().getFirst("Content-Range")).isEqualTo("bytes 9-9/10");
		assertThat(exchange.getResponse().getHeaders().getFirst("Accept-Ranges")).isEqualTo("bytes");
		assertThat(exchange.getResponse().getHeaders().get("Accept-Ranges").size()).isEqualTo(1);
		assertResponseBody(exchange, ".");
	}

	@Test
	public void partialContentByteRangeLargeEnd() {
		MockServerHttpRequest request = MockServerHttpRequest.get("").header("range", "bytes=9-10000").build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		setPathWithinHandlerMapping(exchange, "foo.txt");
		this.handler.handle(exchange).block(TIMEOUT);

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
		assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
		assertThat(exchange.getResponse().getHeaders().getContentLength()).isEqualTo(1);
		assertThat(exchange.getResponse().getHeaders().getFirst("Content-Range")).isEqualTo("bytes 9-9/10");
		assertThat(exchange.getResponse().getHeaders().getFirst("Accept-Ranges")).isEqualTo("bytes");
		assertThat(exchange.getResponse().getHeaders().get("Accept-Ranges").size()).isEqualTo(1);
		assertResponseBody(exchange, ".");
	}

	@Test
	public void partialContentSuffixRange() {
		MockServerHttpRequest request = MockServerHttpRequest.get("").header("range", "bytes=-1").build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		setPathWithinHandlerMapping(exchange, "foo.txt");
		this.handler.handle(exchange).block(TIMEOUT);

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
		assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
		assertThat(exchange.getResponse().getHeaders().getContentLength()).isEqualTo(1);
		assertThat(exchange.getResponse().getHeaders().getFirst("Content-Range")).isEqualTo("bytes 9-9/10");
		assertThat(exchange.getResponse().getHeaders().getFirst("Accept-Ranges")).isEqualTo("bytes");
		assertThat(exchange.getResponse().getHeaders().get("Accept-Ranges").size()).isEqualTo(1);
		assertResponseBody(exchange, ".");
	}

	@Test
	public void partialContentSuffixRangeLargeSuffix() {
		MockServerHttpRequest request = MockServerHttpRequest.get("").header("range", "bytes=-11").build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		setPathWithinHandlerMapping(exchange, "foo.txt");
		this.handler.handle(exchange).block(TIMEOUT);

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
		assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
		assertThat(exchange.getResponse().getHeaders().getContentLength()).isEqualTo(10);
		assertThat(exchange.getResponse().getHeaders().getFirst("Content-Range")).isEqualTo("bytes 0-9/10");
		assertThat(exchange.getResponse().getHeaders().getFirst("Accept-Ranges")).isEqualTo("bytes");
		assertThat(exchange.getResponse().getHeaders().get("Accept-Ranges").size()).isEqualTo(1);
		assertResponseBody(exchange, "Some text.");
	}

	@Test
	public void partialContentInvalidRangeHeader() {
		MockServerHttpRequest request = MockServerHttpRequest.get("").header("range", "bytes=foo bar").build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		setPathWithinHandlerMapping(exchange, "foo.txt");

		StepVerifier.create(this.handler.handle(exchange))
				.expectNextCount(0)
				.expectComplete()
				.verify();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
		assertThat(exchange.getResponse().getHeaders().getFirst("Accept-Ranges")).isEqualTo("bytes");
	}

	@Test
	public void partialContentMultipleByteRanges() {
		MockServerHttpRequest request = MockServerHttpRequest.get("").header("Range", "bytes=0-1, 4-5, 8-9").build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		setPathWithinHandlerMapping(exchange, "foo.txt");
		this.handler.handle(exchange).block(TIMEOUT);

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
		assertThat(exchange.getResponse().getHeaders().getContentType().toString()
		.startsWith("multipart/byteranges;boundary=")).isTrue();

		String boundary = "--" + exchange.getResponse().getHeaders().getContentType().toString().substring(30);

		Mono<DataBuffer> reduced = Flux.from(exchange.getResponse().getBody())
				.reduce(this.bufferFactory.allocateBuffer(), (previous, current) -> {
					previous.write(current);
					DataBufferUtils.release(current);
					return previous;
				});

		StepVerifier.create(reduced)
				.consumeNextWith(buf -> {
					String content = DataBufferTestUtils.dumpString(buf, StandardCharsets.UTF_8);
					String[] ranges = StringUtils.tokenizeToStringArray(content, "\r\n", false, true);

					assertThat(ranges[0]).isEqualTo(boundary);
					assertThat(ranges[1]).isEqualTo("Content-Type: text/plain");
					assertThat(ranges[2]).isEqualTo("Content-Range: bytes 0-1/10");
					assertThat(ranges[3]).isEqualTo("So");

					assertThat(ranges[4]).isEqualTo(boundary);
					assertThat(ranges[5]).isEqualTo("Content-Type: text/plain");
					assertThat(ranges[6]).isEqualTo("Content-Range: bytes 4-5/10");
					assertThat(ranges[7]).isEqualTo(" t");

					assertThat(ranges[8]).isEqualTo(boundary);
					assertThat(ranges[9]).isEqualTo("Content-Type: text/plain");
					assertThat(ranges[10]).isEqualTo("Content-Range: bytes 8-9/10");
					assertThat(ranges[11]).isEqualTo("t.");
				})
				.expectComplete()
				.verify();
	}

	@Test  // SPR-14005
	public void doOverwriteExistingCacheControlHeaders() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
		exchange.getResponse().getHeaders().setCacheControl(CacheControl.noStore().getHeaderValue());
		setPathWithinHandlerMapping(exchange, "foo.css");
		this.handler.handle(exchange).block(TIMEOUT);

		assertThat(exchange.getResponse().getHeaders().getCacheControl()).isEqualTo("max-age=3600");
	}


	private void setPathWithinHandlerMapping(ServerWebExchange exchange, String path) {
		exchange.getAttributes().put(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE,
				PathContainer.parsePath(path));
	}

	private long resourceLastModified(String resourceName) throws IOException {
		return new ClassPathResource(resourceName, getClass()).getFile().lastModified();
	}

	private long resourceLastModifiedDate(String resourceName) throws IOException {
		return new ClassPathResource(resourceName, getClass()).getFile().lastModified();
	}

	private void assertResponseBody(MockServerWebExchange exchange, String responseBody) {
		StepVerifier.create(exchange.getResponse().getBody())
				.consumeNextWith(buf -> assertThat(DataBufferTestUtils.dumpString(buf, StandardCharsets.UTF_8)).isEqualTo(responseBody))
				.expectComplete()
				.verify();
	}

}
