/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.subscriber.ScriptedSubscriber;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.accept.CompositeContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.web.reactive.HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE;

/**
 * Unit tests for {@link ResourceWebHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class ResourceWebHandlerTests {

	private ResourceWebHandler handler;

	private ServerWebExchange exchange;

	private MockServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, "");

	private MockServerHttpResponse response = new MockServerHttpResponse();

	private WebSessionManager sessionManager = new DefaultWebSessionManager();

	private DataBufferFactory bufferFactory = new DefaultDataBufferFactory();


	@Before
	public void setUp() throws Exception {
		List<Resource> paths = new ArrayList<>(2);
		paths.add(new ClassPathResource("test/", getClass()));
		paths.add(new ClassPathResource("testalternatepath/", getClass()));
		paths.add(new ClassPathResource("META-INF/resources/webjars/"));

		this.handler = new ResourceWebHandler();
		this.handler.setLocations(paths);
		this.handler.setCacheControl(CacheControl.maxAge(3600, TimeUnit.SECONDS));
		this.handler.afterPropertiesSet();
		this.handler.afterSingletonsInstantiated();

		this.exchange = new DefaultServerWebExchange(this.request, this.response, this.sessionManager);
	}


	@Test
	public void getResource() throws Exception {
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.handle(this.exchange).blockMillis(5000);

		HttpHeaders headers = this.response.getHeaders();
		assertEquals(MediaType.parseMediaType("text/css"), headers.getContentType());
		assertEquals(17, headers.getContentLength());
		assertEquals("max-age=3600", headers.getCacheControl());
		assertTrue(headers.containsKey("Last-Modified"));
		assertEquals(headers.getLastModified() / 1000, resourceLastModifiedDate("test/foo.css") / 1000);
		assertEquals("bytes", headers.getFirst("Accept-Ranges"));
		assertEquals(1, headers.get("Accept-Ranges").size());
		assertResponseBody("h1 { color:red; }");
	}

	@Test
	public void getResourceHttpHeader() throws Exception {
		this.request.setHttpMethod(HttpMethod.HEAD);
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.handle(this.exchange).blockMillis(5000);

		assertNull(this.response.getStatusCode());
		HttpHeaders headers = this.response.getHeaders();
		assertEquals(MediaType.parseMediaType("text/css"), headers.getContentType());
		assertEquals(17, headers.getContentLength());
		assertEquals("max-age=3600", headers.getCacheControl());
		assertTrue(headers.containsKey("Last-Modified"));
		assertEquals(headers.getLastModified() / 1000, resourceLastModifiedDate("test/foo.css") / 1000);
		assertEquals("bytes", headers.getFirst("Accept-Ranges"));
		assertEquals(1, headers.get("Accept-Ranges").size());
		assertNull(this.response.getBody());
	}

	@Test
	public void getResourceHttpOptions() throws Exception {
		this.request.setHttpMethod(HttpMethod.OPTIONS);
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.handle(this.exchange).blockMillis(5000);

		assertNull(this.response.getStatusCode());
		assertEquals("GET,HEAD,OPTIONS", this.response.getHeaders().getFirst("Allow"));
	}

	@Test
	public void getResourceNoCache() throws Exception {
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.setCacheControl(CacheControl.noStore());
		this.handler.handle(this.exchange).blockMillis(5000);

		assertEquals("no-store", this.response.getHeaders().getCacheControl());
		assertTrue(this.response.getHeaders().containsKey("Last-Modified"));
		assertEquals(this.response.getHeaders().getLastModified() / 1000, resourceLastModifiedDate("test/foo.css") / 1000);
		assertEquals("bytes", this.response.getHeaders().getFirst("Accept-Ranges"));
		assertEquals(1, this.response.getHeaders().get("Accept-Ranges").size());
	}

	@Test
	public void getVersionedResource() throws Exception {
		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.addFixedVersionStrategy("versionString", "/**");
		this.handler.setResourceResolvers(Arrays.asList(versionResolver, new PathResourceResolver()));
		this.handler.afterPropertiesSet();
		this.handler.afterSingletonsInstantiated();

		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "versionString/foo.css");
		this.handler.handle(this.exchange).blockMillis(5000);

		assertEquals("\"versionString\"", this.response.getHeaders().getETag());
		assertEquals("bytes", this.response.getHeaders().getFirst("Accept-Ranges"));
		assertEquals(1, this.response.getHeaders().get("Accept-Ranges").size());
	}

	@Test
	public void getResourceWithHtmlMediaType() throws Exception {
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.html");
		this.handler.handle(this.exchange).blockMillis(5000);

		HttpHeaders headers = this.response.getHeaders();
		assertEquals(MediaType.TEXT_HTML, headers.getContentType());
		assertEquals("max-age=3600", headers.getCacheControl());
		assertTrue(headers.containsKey("Last-Modified"));
		assertEquals(headers.getLastModified() / 1000, resourceLastModifiedDate("test/foo.html") / 1000);
		assertEquals("bytes", headers.getFirst("Accept-Ranges"));
		assertEquals(1, headers.get("Accept-Ranges").size());
	}

	@Test
	public void getResourceFromAlternatePath() throws Exception {
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "baz.css");
		this.handler.handle(this.exchange).blockMillis(5000);

		HttpHeaders headers = this.response.getHeaders();
		assertEquals(MediaType.parseMediaType("text/css"), headers.getContentType());
		assertEquals(17, headers.getContentLength());
		assertEquals("max-age=3600", headers.getCacheControl());
		assertTrue(headers.containsKey("Last-Modified"));
		assertEquals(headers.getLastModified() / 1000, resourceLastModifiedDate("testalternatepath/baz.css") / 1000);
		assertEquals("bytes", headers.getFirst("Accept-Ranges"));
		assertEquals(1, headers.get("Accept-Ranges").size());
		assertResponseBody("h1 { color:red; }");
	}

	@Test
	public void getResourceFromSubDirectory() throws Exception {
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "js/foo.js");
		this.handler.handle(this.exchange).blockMillis(5000);

		assertEquals(MediaType.parseMediaType("text/javascript"), this.response.getHeaders().getContentType());
		assertResponseBody("function foo() { console.log(\"hello world\"); }");
	}

	@Test
	public void getResourceFromSubDirectoryOfAlternatePath() throws Exception {
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "js/baz.js");
		this.handler.handle(this.exchange).blockMillis(5000);

		assertEquals(MediaType.parseMediaType("text/javascript"), this.response.getHeaders().getContentType());
		assertResponseBody("function foo() { console.log(\"hello world\"); }");
	}

	@Test // SPR-13658
	public void getResourceWithRegisteredMediaType() throws Exception {
		CompositeContentTypeResolver contentTypeResolver = new RequestedContentTypeResolverBuilder()
				.mediaType("css", new MediaType("foo", "bar"))
				.build();

		List<Resource> paths = Collections.singletonList(new ClassPathResource("test/", getClass()));
		ResourceWebHandler handler = new ResourceWebHandler();
		handler.setLocations(paths);
		handler.setContentTypeResolver(contentTypeResolver);
		handler.afterPropertiesSet();
		handler.afterSingletonsInstantiated();

		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		handler.handle(this.exchange).blockMillis(5000);

		assertEquals(MediaType.parseMediaType("foo/bar"), this.response.getHeaders().getContentType());
		assertResponseBody("h1 { color:red; }");
	}

	@Test // SPR-14577
	public void getMediaTypeWithFavorPathExtensionOff() throws Exception {
		CompositeContentTypeResolver contentTypeResolver = new RequestedContentTypeResolverBuilder()
				.favorPathExtension(false)
				.build();

		List<Resource> paths = Collections.singletonList(new ClassPathResource("test/", getClass()));
		ResourceWebHandler handler = new ResourceWebHandler();
		handler.setLocations(paths);
		handler.setContentTypeResolver(contentTypeResolver);
		handler.afterPropertiesSet();
		handler.afterSingletonsInstantiated();

		this.request.addHeader("Accept", "application/json,text/plain,*/*");
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.html");
		handler.handle(this.exchange).blockMillis(5000);

		assertEquals(MediaType.TEXT_HTML, this.response.getHeaders().getContentType());
	}

	@Test
	public void invalidPath() throws Exception {
		for (HttpMethod method : HttpMethod.values()) {
			this.request = new MockServerHttpRequest(HttpMethod.GET, "");
			this.response = new MockServerHttpResponse();
			this.sessionManager = new DefaultWebSessionManager();
			this.exchange = new DefaultServerWebExchange(this.request, this.response, this.sessionManager);
			testInvalidPath(method);
		}
	}

	private void testInvalidPath(HttpMethod httpMethod) throws Exception {
		this.request.setHttpMethod(httpMethod);

		Resource location = new ClassPathResource("test/", getClass());
		this.handler.setLocations(Collections.singletonList(location));

		testInvalidPath(location, "../testsecret/secret.txt");
		testInvalidPath(location, "test/../../testsecret/secret.txt");
		testInvalidPath(location, ":/../../testsecret/secret.txt");

		location = new UrlResource(getClass().getResource("./test/"));
		this.handler.setLocations(Collections.singletonList(location));
		Resource secretResource = new UrlResource(getClass().getResource("testsecret/secret.txt"));
		String secretPath = secretResource.getURL().getPath();

		testInvalidPath(location, "file:" + secretPath);
		testInvalidPath(location, "/file:" + secretPath);
		testInvalidPath(location, "url:" + secretPath);
		testInvalidPath(location, "/url:" + secretPath);
		testInvalidPath(location, "////../.." + secretPath);
		testInvalidPath(location, "/%2E%2E/testsecret/secret.txt");
		testInvalidPath(location, "url:" + secretPath);

		// The following tests fail with a MalformedURLException on Windows
		// testInvalidPath(location, "/" + secretPath);
		// testInvalidPath(location, "/  " + secretPath);
	}

	private void testInvalidPath(Resource location, String requestPath) throws Exception {
		this.response = new MockServerHttpResponse();
		this.exchange = new DefaultServerWebExchange(this.request, this.response, this.sessionManager);
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, requestPath);
		this.handler.handle(this.exchange).blockMillis(5000);
		if (!location.createRelative(requestPath).exists() && !requestPath.contains(":")) {
			fail(requestPath + " doesn't actually exist as a relative path");
		}
		assertEquals(HttpStatus.NOT_FOUND, this.response.getStatusCode());
	}

	@Test
	public void ignoreInvalidEscapeSequence() throws Exception {
		this.response = new MockServerHttpResponse();
		this.exchange = new DefaultServerWebExchange(this.request, this.response, this.sessionManager);
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/%foo%/bar.txt");
		this.handler.handle(this.exchange).blockMillis(5000);
		assertEquals(HttpStatus.NOT_FOUND, this.response.getStatusCode());
	}

	@Test
	public void processPath() throws Exception {
		assertSame("/foo/bar", this.handler.processPath("/foo/bar"));
		assertSame("foo/bar", this.handler.processPath("foo/bar"));

		// leading whitespace control characters (00-1F)
		assertEquals("/foo/bar", this.handler.processPath("  /foo/bar"));
		assertEquals("/foo/bar", this.handler.processPath((char) 1 + "/foo/bar"));
		assertEquals("/foo/bar", this.handler.processPath((char) 31 + "/foo/bar"));
		assertEquals("foo/bar", this.handler.processPath("  foo/bar"));
		assertEquals("foo/bar", this.handler.processPath((char) 31 + "foo/bar"));

		// leading control character 0x7F (DEL)
		assertEquals("/foo/bar", this.handler.processPath((char) 127 + "/foo/bar"));
		assertEquals("/foo/bar", this.handler.processPath((char) 127 + "/foo/bar"));

		// leading control and '/' characters
		assertEquals("/foo/bar", this.handler.processPath("  /  foo/bar"));
		assertEquals("/foo/bar", this.handler.processPath("  /  /  foo/bar"));
		assertEquals("/foo/bar", this.handler.processPath("  // /// ////  foo/bar"));
		assertEquals("/foo/bar", this.handler.processPath((char) 1 + " / " + (char) 127 + " // foo/bar"));

		// root or empty path
		assertEquals("", this.handler.processPath("   "));
		assertEquals("/", this.handler.processPath("/"));
		assertEquals("/", this.handler.processPath("///"));
		assertEquals("/", this.handler.processPath("/ /   / "));
	}

	@Test
	public void initAllowedLocations() throws Exception {
		PathResourceResolver resolver = (PathResourceResolver) this.handler.getResourceResolvers().get(0);
		Resource[] locations = resolver.getAllowedLocations();

		assertEquals(3, locations.length);
		assertEquals("test/", ((ClassPathResource) locations[0]).getPath());
		assertEquals("testalternatepath/", ((ClassPathResource) locations[1]).getPath());
		assertEquals("META-INF/resources/webjars/", ((ClassPathResource) locations[2]).getPath());
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
		handler.afterSingletonsInstantiated();

		Resource[] locations = pathResolver.getAllowedLocations();
		assertEquals(1, locations.length);
		assertEquals("test/", ((ClassPathResource) locations[0]).getPath());
	}

	@Test
	public void notModified() throws Exception {
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.request.getHeaders().setIfModifiedSince(resourceLastModified("test/foo.css"));
		this.handler.handle(this.exchange).blockMillis(5000);
		assertEquals(HttpStatus.NOT_MODIFIED, this.response.getStatusCode());
	}

	@Test
	public void modified() throws Exception {
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.request.getHeaders().setIfModifiedSince(resourceLastModified("test/foo.css") / 1000 * 1000 - 1);
		this.handler.handle(this.exchange).blockMillis(5000);

		assertNull(this.response.getStatusCode());
		assertResponseBody("h1 { color:red; }");
	}

	@Test
	public void directory() throws Exception {
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "js/");
		this.handler.handle(this.exchange).blockMillis(5000);
		assertEquals(HttpStatus.NOT_FOUND, this.response.getStatusCode());
	}

	@Test
	public void directoryInJarFile() throws Exception {
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "underscorejs/");
		this.handler.handle(this.exchange).blockMillis(5000);

		assertNull(this.response.getStatusCode());
		assertEquals(0, this.response.getHeaders().getContentLength());
	}

	@Test
	public void missingResourcePath() throws Exception {
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "");
		this.handler.handle(this.exchange).blockMillis(5000);
		assertEquals(HttpStatus.NOT_FOUND, this.response.getStatusCode());
	}

	@Test(expected = IllegalStateException.class)
	public void noPathWithinHandlerMappingAttribute() throws Exception {
		this.handler.handle(this.exchange).blockMillis(5000);
	}

	@Test(expected = MethodNotAllowedException.class)
	public void unsupportedHttpMethod() throws Exception {
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.request.setHttpMethod(HttpMethod.POST);
		this.handler.handle(this.exchange).blockMillis(5000);
	}

	@Test
	public void resourceNotFound() throws Exception {
		for (HttpMethod method : HttpMethod.values()) {
			this.request = new MockServerHttpRequest(HttpMethod.GET, "");
			this.response = new MockServerHttpResponse();
			this.exchange = new DefaultServerWebExchange(this.request, this.response, this.sessionManager);
			resourceNotFound(method);
		}
	}

	private void resourceNotFound(HttpMethod httpMethod) throws Exception {
		this.request.setHttpMethod(httpMethod);
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "not-there.css");
		this.handler.handle(this.exchange).blockMillis(5000);
		assertEquals(HttpStatus.NOT_FOUND, this.response.getStatusCode());
	}

	@Test
	public void partialContentByteRange() throws Exception {
		this.request.addHeader("Range", "bytes=0-1");
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handle(this.exchange).blockMillis(5000);

		assertEquals(HttpStatus.PARTIAL_CONTENT, this.response.getStatusCode());
		assertEquals(MediaType.TEXT_PLAIN, this.response.getHeaders().getContentType());
		assertEquals(2, this.response.getHeaders().getContentLength());
		assertEquals("bytes 0-1/10", this.response.getHeaders().getFirst("Content-Range"));
		assertEquals("bytes", this.response.getHeaders().getFirst("Accept-Ranges"));
		assertEquals(1, this.response.getHeaders().get("Accept-Ranges").size());
		assertResponseBody("So");
	}

	@Test
	public void partialContentByteRangeNoEnd() throws Exception {
		this.request.addHeader("Range", "bytes=9-");
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handle(this.exchange).blockMillis(5000);

		assertEquals(HttpStatus.PARTIAL_CONTENT, this.response.getStatusCode());
		assertEquals(MediaType.TEXT_PLAIN, this.response.getHeaders().getContentType());
		assertEquals(1, this.response.getHeaders().getContentLength());
		assertEquals("bytes 9-9/10", this.response.getHeaders().getFirst("Content-Range"));
		assertEquals("bytes", this.response.getHeaders().getFirst("Accept-Ranges"));
		assertEquals(1, this.response.getHeaders().get("Accept-Ranges").size());
		assertResponseBody(".");
	}

	@Test
	public void partialContentByteRangeLargeEnd() throws Exception {
		this.request.addHeader("Range", "bytes=9-10000");
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handle(this.exchange).blockMillis(5000);

		assertEquals(HttpStatus.PARTIAL_CONTENT, this.response.getStatusCode());
		assertEquals(MediaType.TEXT_PLAIN, this.response.getHeaders().getContentType());
		assertEquals(1, this.response.getHeaders().getContentLength());
		assertEquals("bytes 9-9/10", this.response.getHeaders().getFirst("Content-Range"));
		assertEquals("bytes", this.response.getHeaders().getFirst("Accept-Ranges"));
		assertEquals(1, this.response.getHeaders().get("Accept-Ranges").size());
		assertResponseBody(".");
	}

	@Test
	public void partialContentSuffixRange() throws Exception {
		this.request.addHeader("Range", "bytes=-1");
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handle(this.exchange).blockMillis(5000);

		assertEquals(HttpStatus.PARTIAL_CONTENT, this.response.getStatusCode());
		assertEquals(MediaType.TEXT_PLAIN, this.response.getHeaders().getContentType());
		assertEquals(1, this.response.getHeaders().getContentLength());
		assertEquals("bytes 9-9/10", this.response.getHeaders().getFirst("Content-Range"));
		assertEquals("bytes", this.response.getHeaders().getFirst("Accept-Ranges"));
		assertEquals(1, this.response.getHeaders().get("Accept-Ranges").size());
		assertResponseBody(".");
	}

	@Test
	public void partialContentSuffixRangeLargeSuffix() throws Exception {
		this.request.addHeader("Range", "bytes=-11");
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handle(this.exchange).blockMillis(5000);

		assertEquals(HttpStatus.PARTIAL_CONTENT, this.response.getStatusCode());
		assertEquals(MediaType.TEXT_PLAIN, this.response.getHeaders().getContentType());
		assertEquals(10, this.response.getHeaders().getContentLength());
		assertEquals("bytes 0-9/10", this.response.getHeaders().getFirst("Content-Range"));
		assertEquals("bytes", this.response.getHeaders().getFirst("Accept-Ranges"));
		assertEquals(1, this.response.getHeaders().get("Accept-Ranges").size());
		assertResponseBody("Some text.");
	}

	@Test
	public void partialContentInvalidRangeHeader() throws Exception {
		this.request.addHeader("Range", "bytes= foo bar");
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");

		ScriptedSubscriber.create()
				.expectNextCount(0)
				.expectComplete()
				.verify(this.handler.handle(this.exchange));

		assertEquals(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, this.response.getStatusCode());
		assertEquals("bytes", this.response.getHeaders().getFirst("Accept-Ranges"));
	}

	@Test
	public void partialContentMultipleByteRanges() throws Exception {
		this.request.addHeader("Range", "bytes=0-1, 4-5, 8-9");
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handle(this.exchange).blockMillis(5000);

		assertEquals(HttpStatus.PARTIAL_CONTENT, this.response.getStatusCode());
		assertTrue(this.response.getHeaders().getContentType().toString()
				.startsWith("multipart/byteranges;boundary="));

		String boundary = "--" + this.response.getHeaders().getContentType().toString().substring(30);

		Mono<DataBuffer> reduced = Flux.from(this.response.getBody())
				.reduce(this.bufferFactory.allocateBuffer(), (previous, current) -> {
					previous.write(current);
					DataBufferUtils.release(current);
					return previous;
				});

		ScriptedSubscriber.<DataBuffer>create()
				.consumeNextWith(buf -> {
					String content = DataBufferTestUtils.dumpString(buf, StandardCharsets.UTF_8);
					String[] ranges = StringUtils.tokenizeToStringArray(content, "\r\n", false, true);

					assertEquals(boundary, ranges[0]);
					assertEquals("Content-Type: text/plain", ranges[1]);
					assertEquals("Content-Range: bytes 0-1/10", ranges[2]);
					assertEquals("So", ranges[3]);

					assertEquals(boundary, ranges[4]);
					assertEquals("Content-Type: text/plain", ranges[5]);
					assertEquals("Content-Range: bytes 4-5/10", ranges[6]);
					assertEquals(" t", ranges[7]);

					assertEquals(boundary, ranges[8]);
					assertEquals("Content-Type: text/plain", ranges[9]);
					assertEquals("Content-Range: bytes 8-9/10", ranges[10]);
					assertEquals("t.", ranges[11]);
				})
				.expectComplete()
				.verify(reduced);
	}

	@Test  // SPR-14005
	public void doOverwriteExistingCacheControlHeaders() throws Exception {
		this.exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.response.getHeaders().setCacheControl(CacheControl.noStore().getHeaderValue());
		this.handler.handle(this.exchange).blockMillis(5000);

		assertEquals("max-age=3600", this.response.getHeaders().getCacheControl());
	}


	private long resourceLastModified(String resourceName) throws IOException {
		return new ClassPathResource(resourceName, getClass()).getFile().lastModified();
	}

	private long resourceLastModifiedDate(String resourceName) throws IOException {
		return new ClassPathResource(resourceName, getClass()).getFile().lastModified();
	}

	private void assertResponseBody(String responseBody) {
		ScriptedSubscriber.<DataBuffer>create()
				.consumeNextWith(buf -> assertEquals(responseBody,
						DataBufferTestUtils.dumpString(buf, StandardCharsets.UTF_8)))
				.expectComplete()
				.verify(this.response.getBody());
	}

}
