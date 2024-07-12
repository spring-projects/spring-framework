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

package org.springframework.web.reactive.resource;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.testfixture.server.MockServerWebExchange;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.pattern.PathPatternParser;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ResourceWebHandler}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Brian Clozel
 */
class ResourceWebHandlerTests {

	private static final Duration TIMEOUT = Duration.ofSeconds(1);

	private static final ClassPathResource testResource = new ClassPathResource("test/", ResourceWebHandlerTests.class);
	private static final ClassPathResource testAlternatePathResource = new ClassPathResource("testalternatepath/", ResourceWebHandlerTests.class);
	private static final ClassPathResource webjarsResource = new ClassPathResource("META-INF/resources/webjars/");


	@Nested
	class ResourceHandlingTests {

		private ResourceWebHandler handler;

		@BeforeEach
		void setup() throws Exception {
			this.handler = new ResourceWebHandler();
			this.handler.setLocations(List.of(testResource, testAlternatePathResource, webjarsResource));
			this.handler.afterPropertiesSet();
		}

		@Test
		void servesResource() {
			MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
			setPathWithinHandlerMapping(exchange, "foo.css");
			setBestMachingPattern(exchange, "/**");
			this.handler.handle(exchange).block(TIMEOUT);

			HttpHeaders headers = exchange.getResponse().getHeaders();
			assertThat(headers.getContentType()).isEqualTo(MediaType.parseMediaType("text/css"));
			assertThat(headers.getContentLength()).isEqualTo(17);
			assertResponseBody(exchange, "h1 { color:red; }");
		}


		@Test
		void supportsHeadRequests() {
			MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.head(""));
			setPathWithinHandlerMapping(exchange, "foo.css");
			setBestMachingPattern(exchange, "/**");
			this.handler.handle(exchange).block(TIMEOUT);

			HttpHeaders headers = exchange.getResponse().getHeaders();
			assertThat(headers.getContentType()).isEqualTo(MediaType.parseMediaType("text/css"));
			assertThat(headers.getContentLength()).isEqualTo(17);
			assertResponseBodyIsEmpty(exchange);
		}

		@Test
		void supportsOptionsRequests() {
			MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.options(""));
			setPathWithinHandlerMapping(exchange, "foo.css");
			setBestMachingPattern(exchange, "/**");
			this.handler.handle(exchange).block(TIMEOUT);

			assertThat(exchange.getResponse().getStatusCode()).isNull();
			assertThat(exchange.getResponse().getHeaders().getFirst("Allow")).isEqualTo("GET,HEAD,OPTIONS");
		}

		@Test
		void servesHtmlResources() {
			MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
			setPathWithinHandlerMapping(exchange, "foo.html");
			setBestMachingPattern(exchange, "/**");
			this.handler.handle(exchange).block(TIMEOUT);

			HttpHeaders headers = exchange.getResponse().getHeaders();
			assertThat(headers.getContentType()).isEqualTo(MediaType.TEXT_HTML);
		}

		@Test
		void getResourceWithFullPathAsPattern() {
			MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/foo.css"));
			setPathWithinHandlerMapping(exchange, "");
			setBestMachingPattern(exchange, "/foo.css");
			this.handler.handle(exchange).block(TIMEOUT);
			HttpHeaders headers = exchange.getResponse().getHeaders();
			assertThat(headers.getContentType()).isEqualTo(MediaType.parseMediaType("text/css"));
			assertThat(headers.getContentLength()).isEqualTo(17);
			assertResponseBody(exchange, "h1 { color:red; }");
		}

		@Test
		void getResourceWithRegisteredMediaType() throws Exception {
			MediaType mediaType = new MediaType("foo", "bar");

			ResourceWebHandler handler = new ResourceWebHandler();
			handler.setLocations(List.of(new ClassPathResource("test/", getClass())));
			handler.setMediaTypes(Collections.singletonMap("bar", mediaType));
			handler.afterPropertiesSet();

			MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
			setPathWithinHandlerMapping(exchange, "foo.bar");
			setBestMachingPattern(exchange, "/**");
			handler.handle(exchange).block(TIMEOUT);

			HttpHeaders headers = exchange.getResponse().getHeaders();
			assertThat(headers.getContentType()).isEqualTo(mediaType);
			assertResponseBody(exchange, "foo bar foo bar foo bar");
		}

		@Test
			// SPR-14577
		void getMediaTypeWithFavorPathExtensionOff() throws Exception {
			List<Resource> paths = List.of(new ClassPathResource("test/", getClass()));
			ResourceWebHandler handler = new ResourceWebHandler();
			handler.setLocations(paths);
			handler.afterPropertiesSet();

			MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("")
					.header("Accept", "application/json,text/plain,*/*"));
			setPathWithinHandlerMapping(exchange, "foo.html");
			setBestMachingPattern(exchange, "/**");
			handler.handle(exchange).block(TIMEOUT);

			assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.TEXT_HTML);
		}

		@Test
		void unsupportedHttpMethod() {
			MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post(""));
			setPathWithinHandlerMapping(exchange, "foo.css");
			setBestMachingPattern(exchange, "/**");
			assertThatExceptionOfType(MethodNotAllowedException.class).isThrownBy(() ->
					this.handler.handle(exchange).block(TIMEOUT));
		}

		@ParameterizedTest
		@MethodSource("httpMethods")
		void resourceNotFound(HttpMethod method) {
			MockServerHttpRequest request = MockServerHttpRequest.method(method, "").build();
			MockServerWebExchange exchange = MockServerWebExchange.from(request);
			setPathWithinHandlerMapping(exchange, "not-there.css");
			setBestMachingPattern(exchange, "/**");
			Mono<Void> mono = this.handler.handle(exchange);

			StepVerifier.create(mono)
					.expectErrorSatisfies(err -> {
						assertThat(err).isInstanceOf(ResponseStatusException.class);
						assertThat(((ResponseStatusException) err).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
					}).verify(TIMEOUT);

			// SPR-17475
			AtomicReference<Throwable> exceptionRef = new AtomicReference<>();
			StepVerifier.create(mono).consumeErrorWith(exceptionRef::set).verify();
			StepVerifier.create(mono).consumeErrorWith(ex -> assertThat(ex).isNotSameAs(exceptionRef.get())).verify();
		}

		static Stream<HttpMethod> httpMethods() {
			return Arrays.stream(HttpMethod.values());
		}

	}


	@Nested
	class RangeRequestTests {

		private ResourceWebHandler handler;

		@BeforeEach
		void setup() throws Exception {
			this.handler = new ResourceWebHandler();
			this.handler.setLocations(List.of(testResource, testAlternatePathResource, webjarsResource));
			this.handler.afterPropertiesSet();
		}

		@Test
		void supportsRangeRequest() {
			MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
			setPathWithinHandlerMapping(exchange, "foo.css");
			setBestMachingPattern(exchange, "/**");
			this.handler.handle(exchange).block(TIMEOUT);

			HttpHeaders headers = exchange.getResponse().getHeaders();
			assertThat(headers.getFirst("Accept-Ranges")).isEqualTo("bytes");
			assertThat(headers.get("Accept-Ranges")).hasSize(1);
		}

		@Test
		void partialContentByteRange() {
			MockServerHttpRequest request = MockServerHttpRequest.get("").header("Range", "bytes=0-1").build();
			MockServerWebExchange exchange = MockServerWebExchange.from(request);
			setPathWithinHandlerMapping(exchange, "foo.txt");
			setBestMachingPattern(exchange, "/**");
			this.handler.handle(exchange).block(TIMEOUT);

			assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
			assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
			assertThat(exchange.getResponse().getHeaders().getContentLength()).isEqualTo(2);
			assertThat(exchange.getResponse().getHeaders().getFirst("Content-Range")).isEqualTo("bytes 0-1/10");
			assertThat(exchange.getResponse().getHeaders().getFirst("Accept-Ranges")).isEqualTo("bytes");
			assertThat(exchange.getResponse().getHeaders().get("Accept-Ranges")).hasSize(1);
			assertResponseBody(exchange, "So");
		}

		@Test
		void partialContentByteRangeNoEnd() {
			MockServerHttpRequest request = MockServerHttpRequest.get("").header("range", "bytes=9-").build();
			MockServerWebExchange exchange = MockServerWebExchange.from(request);
			setPathWithinHandlerMapping(exchange, "foo.txt");
			setBestMachingPattern(exchange, "/**");
			this.handler.handle(exchange).block(TIMEOUT);

			assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
			assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
			assertThat(exchange.getResponse().getHeaders().getContentLength()).isEqualTo(1);
			assertThat(exchange.getResponse().getHeaders().getFirst("Content-Range")).isEqualTo("bytes 9-9/10");
			assertThat(exchange.getResponse().getHeaders().getFirst("Accept-Ranges")).isEqualTo("bytes");
			assertThat(exchange.getResponse().getHeaders().get("Accept-Ranges")).hasSize(1);
			assertResponseBody(exchange, ".");
		}

		@Test
		void partialContentByteRangeLargeEnd() {
			MockServerHttpRequest request = MockServerHttpRequest.get("").header("range", "bytes=9-10000").build();
			MockServerWebExchange exchange = MockServerWebExchange.from(request);
			setPathWithinHandlerMapping(exchange, "foo.txt");
			setBestMachingPattern(exchange, "/**");
			this.handler.handle(exchange).block(TIMEOUT);

			assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
			assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
			assertThat(exchange.getResponse().getHeaders().getContentLength()).isEqualTo(1);
			assertThat(exchange.getResponse().getHeaders().getFirst("Content-Range")).isEqualTo("bytes 9-9/10");
			assertThat(exchange.getResponse().getHeaders().getFirst("Accept-Ranges")).isEqualTo("bytes");
			assertThat(exchange.getResponse().getHeaders().get("Accept-Ranges")).hasSize(1);
			assertResponseBody(exchange, ".");
		}

		@Test
		void partialContentSuffixRange() {
			MockServerHttpRequest request = MockServerHttpRequest.get("").header("range", "bytes=-1").build();
			MockServerWebExchange exchange = MockServerWebExchange.from(request);
			setPathWithinHandlerMapping(exchange, "foo.txt");
			setBestMachingPattern(exchange, "/**");
			this.handler.handle(exchange).block(TIMEOUT);

			assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
			assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
			assertThat(exchange.getResponse().getHeaders().getContentLength()).isEqualTo(1);
			assertThat(exchange.getResponse().getHeaders().getFirst("Content-Range")).isEqualTo("bytes 9-9/10");
			assertThat(exchange.getResponse().getHeaders().getFirst("Accept-Ranges")).isEqualTo("bytes");
			assertThat(exchange.getResponse().getHeaders().get("Accept-Ranges")).hasSize(1);
			assertResponseBody(exchange, ".");
		}

		@Test
		void partialContentSuffixRangeLargeSuffix() {
			MockServerHttpRequest request = MockServerHttpRequest.get("").header("range", "bytes=-11").build();
			MockServerWebExchange exchange = MockServerWebExchange.from(request);
			setPathWithinHandlerMapping(exchange, "foo.txt");
			setBestMachingPattern(exchange, "/**");
			this.handler.handle(exchange).block(TIMEOUT);

			assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
			assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
			assertThat(exchange.getResponse().getHeaders().getContentLength()).isEqualTo(10);
			assertThat(exchange.getResponse().getHeaders().getFirst("Content-Range")).isEqualTo("bytes 0-9/10");
			assertThat(exchange.getResponse().getHeaders().getFirst("Accept-Ranges")).isEqualTo("bytes");
			assertThat(exchange.getResponse().getHeaders().get("Accept-Ranges")).hasSize(1);
			assertResponseBody(exchange, "Some text.");
		}

		@Test
		void partialContentInvalidRangeHeader() {
			MockServerHttpRequest request = MockServerHttpRequest.get("").header("range", "bytes=foo bar").build();
			MockServerWebExchange exchange = MockServerWebExchange.from(request);
			setPathWithinHandlerMapping(exchange, "foo.txt");
			setBestMachingPattern(exchange, "/**");

			StepVerifier.create(this.handler.handle(exchange))
					.expectNextCount(0)
					.expectComplete()
					.verify();

			assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
			assertThat(exchange.getResponse().getHeaders().getFirst("Accept-Ranges")).isEqualTo("bytes");
		}

		@Test
		void partialContentMultipleByteRanges() {
			MockServerHttpRequest request = MockServerHttpRequest.get("").header("Range", "bytes=0-1, 4-5, 8-9").build();
			MockServerWebExchange exchange = MockServerWebExchange.from(request);
			setPathWithinHandlerMapping(exchange, "foo.txt");
			setBestMachingPattern(exchange, "/**");
			this.handler.handle(exchange).block(TIMEOUT);

			assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
			assertThat(exchange.getResponse().getHeaders().getContentType().toString()).startsWith(
					"multipart/byteranges;boundary=");

			String boundary = "--" + exchange.getResponse().getHeaders().getContentType().toString().substring(30);

			Mono<DataBuffer> reduced = Flux.from(exchange.getResponse().getBody())
					.reduce(DefaultDataBufferFactory.sharedInstance.allocateBuffer(256), (previous, current) -> {
						previous.write(current);
						DataBufferUtils.release(current);
						return previous;
					});

			StepVerifier.create(reduced)
					.consumeNextWith(buf -> {
						String content = buf.toString(UTF_8);
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

	}

	@Nested
	class HttpCachingTests {

		private ResourceWebHandler handler;

		@BeforeEach
		void setup() {
			this.handler = new ResourceWebHandler();
			this.handler.setLocations(List.of(testResource, testAlternatePathResource, webjarsResource));
		}

		@Test
		void defaultCachingHeaders() throws Exception {
			this.handler.afterPropertiesSet();
			MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
			setPathWithinHandlerMapping(exchange, "foo.css");
			setBestMachingPattern(exchange, "/**");
			this.handler.handle(exchange).block(TIMEOUT);

			HttpHeaders headers = exchange.getResponse().getHeaders();
			assertThat(headers.containsKey("Last-Modified")).isTrue();
			assertThat(resourceLastModifiedDate("test/foo.css") / 1000).isEqualTo(headers.getLastModified() / 1000);
		}

		@Test
		void configureCacheSeconds() throws Exception {
			this.handler.setCacheControl(CacheControl.maxAge(3600, TimeUnit.SECONDS));
			this.handler.afterPropertiesSet();

			MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
			setPathWithinHandlerMapping(exchange, "foo.css");
			setBestMachingPattern(exchange, "/**");
			this.handler.handle(exchange).block(TIMEOUT);

			HttpHeaders headers = exchange.getResponse().getHeaders();
			assertThat(headers.getCacheControl()).isEqualTo("max-age=3600");
		}

		@Test
		void configureCacheSecondsToZero() throws Exception {
			this.handler.setCacheControl(CacheControl.noStore());
			this.handler.afterPropertiesSet();

			MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
			setPathWithinHandlerMapping(exchange, "foo.css");
			setBestMachingPattern(exchange, "/**");
			this.handler.setCacheControl(CacheControl.noStore());
			this.handler.handle(exchange).block(TIMEOUT);

			MockServerHttpResponse response = exchange.getResponse();
			assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
			assertThat(response.getHeaders().containsKey("Last-Modified")).isTrue();
			assertThat(resourceLastModifiedDate("test/foo.css") / 1000).isEqualTo(response.getHeaders().getLastModified() / 1000);
		}

		@Test
		void configureVersionResourceResolver() throws Exception {
			VersionResourceResolver versionResolver = new VersionResourceResolver();
			versionResolver.addFixedVersionStrategy("versionString", "/**");
			this.handler.setResourceResolvers(List.of(versionResolver, new PathResourceResolver()));
			this.handler.afterPropertiesSet();

			MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
			setPathWithinHandlerMapping(exchange, "versionString/foo.css");
			setBestMachingPattern(exchange, "/**");
			this.handler.handle(exchange).block(TIMEOUT);

			assertThat(exchange.getResponse().getHeaders().getETag()).isEqualTo("W/\"versionString\"");
		}

		@Test
		void shouldRespondWithNotModifiedWhenModifiedSince() throws Exception {
			this.handler.afterPropertiesSet();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("").ifModifiedSince(resourceLastModified("test/foo.css")));

			setPathWithinHandlerMapping(exchange, "foo.css");
			setBestMachingPattern(exchange, "/**");
			this.handler.handle(exchange).block(TIMEOUT);
			assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
		}

		@Test
		void shouldRespondWithModifiedResource() throws Exception {
			this.handler.afterPropertiesSet();

			long timestamp = resourceLastModified("test/foo.css") / 1000 * 1000 - 1;
			MockServerHttpRequest request = MockServerHttpRequest.get("").ifModifiedSince(timestamp).build();
			MockServerWebExchange exchange = MockServerWebExchange.from(request);
			setPathWithinHandlerMapping(exchange, "foo.css");
			setBestMachingPattern(exchange, "/**");
			this.handler.handle(exchange).block(TIMEOUT);

			assertThat(exchange.getResponse().getStatusCode()).isNull();
			assertResponseBody(exchange, "h1 { color:red; }");
		}

		@Test
		void shouldRespondWithNotModifiedWhenEtag() throws Exception {
			this.handler.setEtagGenerator(resource -> "testEtag");
			this.handler.afterPropertiesSet();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("").ifNoneMatch( "\"testEtag\""));

			setPathWithinHandlerMapping(exchange, "foo.css");
			setBestMachingPattern(exchange, "/**");
			this.handler.handle(exchange).block(TIMEOUT);
			assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
		}

		@Test
		void shouldRespondWithModifiedResourceWhenEtagNoMatch() throws Exception {
			this.handler.setEtagGenerator(resource -> "noMatch");
			this.handler.afterPropertiesSet();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("").ifNoneMatch( "\"testEtag\""));

			setPathWithinHandlerMapping(exchange, "foo.css");
			setBestMachingPattern(exchange, "/**");
			this.handler.handle(exchange).block(TIMEOUT);
			assertThat(exchange.getResponse().getStatusCode()).isNull();
			assertResponseBody(exchange, "h1 { color:red; }");
		}

		@Test
		void shouldRespondWithNotModifiedWhenEtagAndLastModified() throws Exception {
			this.handler.setEtagGenerator(resource -> "testEtag");
			this.handler.afterPropertiesSet();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("")
							.ifModifiedSince(resourceLastModified("test/foo.css"))
							.ifNoneMatch( "\"testEtag\""));

			setPathWithinHandlerMapping(exchange, "foo.css");
			setBestMachingPattern(exchange, "/**");
			this.handler.handle(exchange).block(TIMEOUT);
			assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
		}

		@Test  // SPR-14005
		void doOverwriteExistingCacheControlHeaders() throws Exception {
			this.handler.setCacheControl(CacheControl.maxAge(3600, TimeUnit.SECONDS));
			this.handler.afterPropertiesSet();
			MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
			exchange.getResponse().getHeaders().setCacheControl(CacheControl.noStore().getHeaderValue());
			setPathWithinHandlerMapping(exchange, "foo.css");
			setBestMachingPattern(exchange, "/**");
			this.handler.handle(exchange).block(TIMEOUT);

			assertThat(exchange.getResponse().getHeaders().getCacheControl()).isEqualTo("max-age=3600");
		}

		@Test
		void ignoreLastModified() throws Exception {
			this.handler.afterPropertiesSet();
			MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
			setPathWithinHandlerMapping(exchange, "foo.css");
			setBestMachingPattern(exchange, "/**");
			this.handler.setUseLastModified(false);
			this.handler.handle(exchange).block(TIMEOUT);

			HttpHeaders headers = exchange.getResponse().getHeaders();
			assertThat(headers.getContentType()).isEqualTo(MediaType.parseMediaType("text/css"));
			assertThat(headers.getContentLength()).isEqualTo(17);
			assertThat(headers.containsKey("Last-Modified")).isFalse();
			assertResponseBody(exchange, "h1 { color:red; }");
		}

	}


	@Nested
	class ResourceLocationTests {

		private ResourceWebHandler handler;

		@BeforeEach
		void setup() {
			this.handler = new ResourceWebHandler();
			this.handler.setLocations(List.of(testResource, testAlternatePathResource, webjarsResource));
		}

		@Test
		void getResourceFromAlternatePath() throws Exception {
			this.handler.afterPropertiesSet();
			MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
			setPathWithinHandlerMapping(exchange, "baz.css");
			setBestMachingPattern(exchange, "/**");
			this.handler.handle(exchange).block(TIMEOUT);

			HttpHeaders headers = exchange.getResponse().getHeaders();
			assertThat(headers.getContentType()).isEqualTo(MediaType.parseMediaType("text/css"));
			assertThat(headers.getContentLength()).isEqualTo(17);
			assertResponseBody(exchange, "h1 { color:red; }");
		}

		@Test
		void getResourceFromSubDirectory() throws Exception {
			this.handler.afterPropertiesSet();
			MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
			setPathWithinHandlerMapping(exchange, "js/foo.js");
			setBestMachingPattern(exchange, "/**");
			this.handler.handle(exchange).block(TIMEOUT);

			assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("text/javascript"));
			assertResponseBody(exchange, "function foo() { console.log(\"hello world\"); }");
		}

		@Test
		void getResourceFromSubDirectoryOfAlternatePath() throws Exception {
			this.handler.afterPropertiesSet();
			MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
			setPathWithinHandlerMapping(exchange, "js/baz.js");
			setBestMachingPattern(exchange, "/**");
			this.handler.handle(exchange).block(TIMEOUT);

			HttpHeaders headers = exchange.getResponse().getHeaders();
			assertThat(headers.getContentType()).isEqualTo(MediaType.parseMediaType("text/javascript"));
			assertResponseBody(exchange, "function foo() { console.log(\"hello world\"); }");
		}

		@Test  // gh-27538, gh-27624
		void filterNonExistingLocations() throws Exception {
			this.handler.afterPropertiesSet();
			ResourceWebHandler handler = new ResourceWebHandler();
			handler.setLocations(List.of(testResource, testAlternatePathResource, new ClassPathResource("nosuchpath/", getClass())));
			handler.setOptimizeLocations(true);
			handler.afterPropertiesSet();

			List<Resource> actual = handler.getLocations();
			assertThat(actual).hasSize(2);
			assertThat(actual.get(0).getURL().toString()).endsWith("test/");
			assertThat(actual.get(1).getURL().toString()).endsWith("testalternatepath/");
		}

		@Test
		void shouldRejectInvalidPath() throws Exception {
			this.handler.afterPropertiesSet();
			// Use mock ResourceResolver: i.e. we're only testing upfront validations...

			Resource resource = mock();
			given(resource.getFilename()).willThrow(new AssertionError("Resource should not be resolved"));
			given(resource.getInputStream()).willThrow(new AssertionError("Resource should not be resolved"));
			ResourceResolver resolver = mock();
			given(resolver.resolveResource(any(), any(), any(), any())).willReturn(Mono.just(resource));

			ResourceWebHandler handler = new ResourceWebHandler();
			handler.setLocations(List.of(new ClassPathResource("test/", getClass())));
			handler.setResourceResolvers(List.of(resolver));
			handler.afterPropertiesSet();

			testInvalidPath("../testsecret/secret.txt", handler);
			testInvalidPath("test/../../testsecret/secret.txt", handler);
			testInvalidPath(":/../../testsecret/secret.txt", handler);

			Resource location = new UrlResource(getClass().getResource("./test/"));
			handler.setLocations(List.of(location));
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
			setBestMachingPattern(exchange, "/**");
			StepVerifier.create(handler.handle(exchange))
					.expectErrorSatisfies(err -> {
						assertThat(err).isInstanceOf(ResponseStatusException.class);
						assertThat(((ResponseStatusException) err).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
					}).verify(TIMEOUT);
		}

		@ParameterizedTest
		@MethodSource("httpMethods")
		void resolvePathWithTraversal(HttpMethod method) throws Exception {
			Resource location = new ClassPathResource("test/", getClass());
			this.handler.setLocations(List.of(location));

			testResolvePathWithTraversal(method, "../testsecret/secret.txt");
			testResolvePathWithTraversal(method, "test/../../testsecret/secret.txt");
			testResolvePathWithTraversal(method, ":/../../testsecret/secret.txt");

			location = new UrlResource(getClass().getResource("./test/"));
			this.handler.setLocations(List.of(location));
			Resource secretResource = new UrlResource(getClass().getResource("testsecret/secret.txt"));
			String secretPath = secretResource.getURL().getPath();

			testResolvePathWithTraversal(method, "file:" + secretPath);
			testResolvePathWithTraversal(method, "/file:" + secretPath);
			testResolvePathWithTraversal(method, "url:" + secretPath);
			testResolvePathWithTraversal(method, "/url:" + secretPath);
			testResolvePathWithTraversal(method, "////../.." + secretPath);
			testResolvePathWithTraversal(method, "/%2E%2E/testsecret/secret.txt");
			testResolvePathWithTraversal(method, "%2F%2F%2E%2E%2F%2Ftestsecret/secret.txt");
			testResolvePathWithTraversal(method, "url:" + secretPath);

			// The following tests fail with a MalformedURLException on Windows
			// testResolvePathWithTraversal(location, "/" + secretPath);
			// testResolvePathWithTraversal(location, "/  " + secretPath);
		}

		static Stream<HttpMethod> httpMethods() {
			return Arrays.stream(HttpMethod.values());
		}

		private void testResolvePathWithTraversal(HttpMethod httpMethod, String requestPath) {
			ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.method(httpMethod, ""));
			setPathWithinHandlerMapping(exchange, requestPath);
			setBestMachingPattern(exchange, "/**");
			StepVerifier.create(this.handler.handle(exchange))
					.expectErrorSatisfies(err -> {
						assertThat(err).isInstanceOf(ResponseStatusException.class);
						assertThat(((ResponseStatusException) err).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
					})
					.verify(TIMEOUT);
		}

		@Test
		void processPath() {
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
			assertThat(this.handler.processPath("   ")).isEmpty();
			assertThat(this.handler.processPath("/")).isEqualTo("/");
			assertThat(this.handler.processPath("///")).isEqualTo("/");
			assertThat(this.handler.processPath("/ /   / ")).isEqualTo("/");
		}

		@Test
		void initAllowedLocations() throws Exception {
			this.handler.afterPropertiesSet();
			PathResourceResolver resolver = (PathResourceResolver) this.handler.getResourceResolvers().get(0);
			Resource[] locations = resolver.getAllowedLocations();

			assertThat(locations).containsExactly(testResource, testAlternatePathResource, webjarsResource);
		}

		@Test
		void initAllowedLocationsWithExplicitConfiguration() throws Exception {
			PathResourceResolver pathResolver = new PathResourceResolver();
			pathResolver.setAllowedLocations(testResource);
			this.handler.setResourceResolvers(List.of(pathResolver));
			this.handler.setLocations(List.of(testResource, testAlternatePathResource));
			this.handler.afterPropertiesSet();

			assertThat(pathResolver.getAllowedLocations()).containsExactly(testResource);
		}

		@Test
		void shouldNotServeDirectory() throws Exception {
			this.handler.afterPropertiesSet();
			MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
			setPathWithinHandlerMapping(exchange, "js/");
			setBestMachingPattern(exchange, "/**");
			StepVerifier.create(this.handler.handle(exchange))
					.expectErrorSatisfies(err -> {
						assertThat(err).isInstanceOf(ResponseStatusException.class);
						assertThat(((ResponseStatusException) err).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
					}).verify(TIMEOUT);
		}

		@Test
		void shouldNotServeDirectoryInJarFile() throws Exception {
			this.handler.afterPropertiesSet();
			MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
			setPathWithinHandlerMapping(exchange, "underscorejs/");
			setBestMachingPattern(exchange, "/**");
			StepVerifier.create(this.handler.handle(exchange))
					.expectErrorSatisfies(err -> {
						assertThat(err).isInstanceOf(ResponseStatusException.class);
						assertThat(((ResponseStatusException) err).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
					}).verify(TIMEOUT);
		}

		@Test
		void servesResourcesFromFileSystem() throws Exception {
			String packagePath = ClassUtils.classPackageAsResourcePath(getClass());
			String path = Paths.get("src/test/resources", packagePath).normalize() + "/";

			this.handler.setLocations(List.of(new FileSystemResource(path)));
			this.handler.afterPropertiesSet();

			MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
			setPathWithinHandlerMapping(exchange, UriUtils.encodePath("test/foo with spaces.css", UTF_8));
			setBestMachingPattern(exchange, "/**");
			this.handler.handle(exchange).block(TIMEOUT);

			HttpHeaders headers = exchange.getResponse().getHeaders();
			assertThat(headers.getContentType()).isEqualTo(MediaType.parseMediaType("text/css"));
			assertThat(headers.getContentLength()).isEqualTo(17);
			assertResponseBody(exchange, "h1 { color:red; }");
		}

		@Test
		void shouldNotServeMissingResourcePath() throws Exception {
			this.handler.afterPropertiesSet();
			MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
			setPathWithinHandlerMapping(exchange, "");
			setBestMachingPattern(exchange, "/**");
			StepVerifier.create(this.handler.handle(exchange))
					.expectErrorSatisfies(err -> {
						assertThat(err).isInstanceOf(ResponseStatusException.class);
						assertThat(((ResponseStatusException) err).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
					}).verify(TIMEOUT);
		}

		@Test
		void noPathWithinHandlerMappingAttribute() throws Exception {
			this.handler.afterPropertiesSet();
			MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
			assertThatIllegalArgumentException().isThrownBy(() ->
					this.handler.handle(exchange).block(TIMEOUT));
		}


	}


	private void setPathWithinHandlerMapping(ServerWebExchange exchange, String path) {
		exchange.getAttributes().put(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE,
				PathContainer.parsePath(path));
	}

	private void setBestMachingPattern(ServerWebExchange exchange, String pattern) {
		exchange.getAttributes().put(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
				new PathPatternParser().parse(pattern));
	}

	private long resourceLastModified(String resourceName) throws IOException {
		return new ClassPathResource(resourceName, getClass()).getFile().lastModified();
	}

	private long resourceLastModifiedDate(String resourceName) throws IOException {
		return new ClassPathResource(resourceName, getClass()).getFile().lastModified();
	}

	private void assertResponseBody(MockServerWebExchange exchange, String responseBody) {
		StepVerifier.create(exchange.getResponse().getBody())
				.consumeNextWith(buf -> assertThat(buf.toString(UTF_8)).isEqualTo(responseBody))
				.expectComplete()
				.verify();
	}

	private void assertResponseBodyIsEmpty(MockServerWebExchange exchange) {
		StepVerifier.create(exchange.getResponse().getBody()).verifyComplete();
	}

}
