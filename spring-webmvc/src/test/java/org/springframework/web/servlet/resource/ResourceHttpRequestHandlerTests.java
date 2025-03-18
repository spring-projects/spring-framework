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

package org.springframework.web.servlet.resource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ResourceHttpRequestHandler}.
 *
 * @author Keith Donald
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 */
@ExtendWith(GzipSupport.class)
class ResourceHttpRequestHandlerTests {

	private static final ClassPathResource testResource = new ClassPathResource("test/", ResourceHttpRequestHandlerTests.class);
	private static final ClassPathResource testAlternatePathResource = new ClassPathResource("testalternatepath/", ResourceHttpRequestHandlerTests.class);
	private static final ClassPathResource webjarsResource = new ClassPathResource("META-INF/resources/webjars/");

	@Nested
	class ResourceHandlingTests {

		private ResourceHttpRequestHandler handler;

		private MockHttpServletRequest request;

		private MockHttpServletResponse response;


		@BeforeEach
		void setup() throws Exception {
			TestServletContext servletContext = new TestServletContext();
			this.handler = new ResourceHttpRequestHandler();
			this.handler.setLocations(List.of(testResource, testAlternatePathResource, webjarsResource));
			this.handler.setServletContext(servletContext);
			this.handler.afterPropertiesSet();
			this.request = new MockHttpServletRequest(servletContext, "GET", "");
			this.response = new MockHttpServletResponse();
		}

		@Test
		void servesResource() throws Exception {
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
			this.handler.handleRequest(this.request, this.response);

			assertThat(this.response.getContentType()).isEqualTo("text/css");
			assertThat(this.response.getContentLength()).isEqualTo(17);
			assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
		}

		@Test
		void supportsHeadRequests() throws Exception {
			this.request.setMethod("HEAD");
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
			this.handler.handleRequest(this.request, this.response);

			assertThat(this.response.getStatus()).isEqualTo(200);
			assertThat(this.response.getContentType()).isEqualTo("text/css");
			assertThat(this.response.getContentLength()).isEqualTo(17);
			assertThat(this.response.getContentAsByteArray()).isEmpty();
		}

		@Test
		void supportsOptionsRequests() throws Exception {
			this.request.setMethod("OPTIONS");
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
			this.handler.handleRequest(this.request, this.response);

			assertThat(this.response.getStatus()).isEqualTo(200);
			assertThat(this.response.getHeader("Allow")).isEqualTo("GET,HEAD,OPTIONS");
		}

		@Test
		void servesHtmlResources() throws Exception {
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.html");
			this.handler.handleRequest(this.request, this.response);

			assertThat(this.response.getContentType()).isEqualTo("text/html");
		}

		@Test  // SPR-13658
		void getResourceWithRegisteredMediaType() throws Exception {
			List<Resource> paths = List.of(new ClassPathResource("test/", getClass()));
			ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
			handler.setServletContext(new MockServletContext());
			handler.setMediaTypes(Map.of("bar", new MediaType("foo", "bar")));
			handler.setLocations(paths);
			handler.afterPropertiesSet();

			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.bar");
			handler.handleRequest(this.request, this.response);

			assertThat(this.response.getContentType()).isEqualTo("foo/bar");
			assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
		}

		@Test // SPR-14577
		void getMediaTypeWithFavorPathExtensionOff() throws Exception {
			List<Resource> paths = List.of(new ClassPathResource("test/", getClass()));
			ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
			handler.setServletContext(new MockServletContext());
			handler.setLocations(paths);
			handler.afterPropertiesSet();

			this.request.addHeader("Accept", "application/json,text/plain,*/*");
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.html");
			handler.handleRequest(this.request, this.response);

			assertThat(this.response.getContentType()).isEqualTo("text/html");
		}

		@Test  // SPR-14368
		void getResourceWithMediaTypeResolvedThroughServletContext() throws Exception {
			MockServletContext servletContext = new MockServletContext() {
				@Override
				public String getMimeType(String filePath) {
					return "foo/bar";
				}
			};

			List<Resource> paths = List.of(new ClassPathResource("test/", getClass()));
			ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
			handler.setServletContext(servletContext);
			handler.setLocations(paths);
			handler.afterPropertiesSet();

			MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "");
			request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
			handler.handleRequest(request, this.response);

			assertThat(this.response.getContentType()).isEqualTo("foo/bar");
			assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
		}

		@Test
		void unsupportedHttpMethod() {
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
			this.request.setMethod("POST");
			assertThatExceptionOfType(HttpRequestMethodNotSupportedException.class).isThrownBy(() ->
					this.handler.handleRequest(this.request, this.response));
		}

		@Test
		void testResourceNotFound() {
			for (HttpMethod method : HttpMethod.values()) {
				this.request = new MockHttpServletRequest("GET", "");
				this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "not-there.css");
				this.request.setMethod(method.name());
				this.response = new MockHttpServletResponse();
				assertNotFound();
			}
		}

		private void assertNotFound() {
			assertThatThrownBy(() -> this.handler.handleRequest(this.request, this.response))
					.isInstanceOf(NoResourceFoundException.class);
		}

	}


	@Nested
	class RangeRequestTests {


		private ResourceHttpRequestHandler handler;

		private MockHttpServletRequest request;

		private MockHttpServletResponse response;


		@BeforeEach
		void setup() throws Exception {
			TestServletContext servletContext = new TestServletContext();
			this.handler = new ResourceHttpRequestHandler();
			this.handler.setLocations(List.of(testResource, testAlternatePathResource, webjarsResource));
			this.handler.setServletContext(servletContext);
			this.handler.afterPropertiesSet();
			this.request = new MockHttpServletRequest(servletContext, "GET", "");
			this.response = new MockHttpServletResponse();
		}

		@Test
		void supportsRangeRequest() throws Exception {
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
			this.handler.handleRequest(this.request, this.response);

			assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
			assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
		}

		@Test
		void partialContentByteRange() throws Exception {
			this.request.addHeader("Range", "bytes=0-1");
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
			this.handler.handleRequest(this.request, this.response);

			assertThat(this.response.getStatus()).isEqualTo(206);
			assertThat(this.response.getContentType()).isEqualTo("text/plain");
			assertThat(this.response.getContentLength()).isEqualTo(2);
			assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 0-1/10");
			assertThat(this.response.getContentAsString()).isEqualTo("So");
			assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
			assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
		}

		@Test
		void partialContentByteRangeNoEnd() throws Exception {
			this.request.addHeader("Range", "bytes=9-");
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
			this.handler.handleRequest(this.request, this.response);

			assertThat(this.response.getStatus()).isEqualTo(206);
			assertThat(this.response.getContentType()).isEqualTo("text/plain");
			assertThat(this.response.getContentLength()).isEqualTo(1);
			assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 9-9/10");
			assertThat(this.response.getContentAsString()).isEqualTo(".");
			assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
			assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
		}

		@Test
		void partialContentByteRangeLargeEnd() throws Exception {
			this.request.addHeader("Range", "bytes=9-10000");
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
			this.handler.handleRequest(this.request, this.response);

			assertThat(this.response.getStatus()).isEqualTo(206);
			assertThat(this.response.getContentType()).isEqualTo("text/plain");
			assertThat(this.response.getContentLength()).isEqualTo(1);
			assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 9-9/10");
			assertThat(this.response.getContentAsString()).isEqualTo(".");
			assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
			assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
		}

		@Test
		void partialContentSuffixRange() throws Exception {
			this.request.addHeader("Range", "bytes=-1");
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
			this.handler.handleRequest(this.request, this.response);

			assertThat(this.response.getStatus()).isEqualTo(206);
			assertThat(this.response.getContentType()).isEqualTo("text/plain");
			assertThat(this.response.getContentLength()).isEqualTo(1);
			assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 9-9/10");
			assertThat(this.response.getContentAsString()).isEqualTo(".");
			assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
			assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
		}

		@Test
		void partialContentSuffixRangeLargeSuffix() throws Exception {
			this.request.addHeader("Range", "bytes=-11");
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
			this.handler.handleRequest(this.request, this.response);

			assertThat(this.response.getStatus()).isEqualTo(206);
			assertThat(this.response.getContentType()).isEqualTo("text/plain");
			assertThat(this.response.getContentLength()).isEqualTo(10);
			assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 0-9/10");
			assertThat(this.response.getContentAsString()).isEqualTo("Some text.");
			assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
			assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
		}

		@Test
		void partialContentInvalidRangeHeader() throws Exception {
			this.request.addHeader("Range", "bytes= foo bar");
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
			this.handler.handleRequest(this.request, this.response);

			assertThat(this.response.getStatus()).isEqualTo(416);
			assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes */10");
			assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
			assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
		}

		@Test
		void partialContentMultipleByteRanges() throws Exception {
			this.request.addHeader("Range", "bytes=0-1, 4-5, 8-9");
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
			this.handler.handleRequest(this.request, this.response);

			assertThat(this.response.getStatus()).isEqualTo(206);
			assertThat(this.response.getContentType()).startsWith("multipart/byteranges; boundary=");

			String boundary = "--" + this.response.getContentType().substring(31);

			String content = this.response.getContentAsString();
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
		}

		@Test  // gh-25976
		void partialContentByteRangeWithEncodedResource(GzipSupport.GzippedFiles gzippedFiles) throws Exception {
			String path = "js/foo.js";
			gzippedFiles.create(path);

			ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
			handler.setResourceResolvers(List.of(new EncodedResourceResolver(), new PathResourceResolver()));
			handler.setLocations(List.of(testResource));
			handler.setServletContext(new MockServletContext());
			handler.afterPropertiesSet();

			this.request.addHeader("Accept-Encoding", "gzip");
			this.request.addHeader("Range", "bytes=0-1");
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, path);
			handler.handleRequest(this.request, this.response);

			assertThat(this.response.getStatus()).isEqualTo(206);
			assertThat(this.response.getHeaderNames()).containsExactlyInAnyOrder(
					"Content-Type", "Content-Length", "Content-Range", "Accept-Ranges",
					"Last-Modified", "Content-Encoding", "Vary");

			assertThat(this.response.getContentType()).isEqualTo("text/javascript");
			assertThat(this.response.getContentLength()).isEqualTo(2);
			assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 0-1/66");
			assertThat(this.response.getHeaderValues("Accept-Ranges")).containsExactly("bytes");
			assertThat(this.response.getHeaderValues("Content-Encoding")).containsExactly("gzip");
			assertThat(this.response.getHeaderValues("Vary")).containsExactly("Accept-Encoding");
		}

		@Test  // gh-25976
		void partialContentWithHttpHead() throws Exception {
			this.request.setMethod("HEAD");
			this.request.addHeader("Range", "bytes=0-1");
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
			this.handler.handleRequest(this.request, this.response);

			assertThat(this.response.getStatus()).isEqualTo(206);
			assertThat(this.response.getContentType()).isEqualTo("text/plain");
			assertThat(this.response.getContentLength()).isEqualTo(2);
			assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 0-1/10");
			assertThat(this.response.getHeaderValues("Accept-Ranges")).containsExactly("bytes");
		}

	}

	@Nested
	class HttpCachingTests {

		private ResourceHttpRequestHandler handler;

		private MockHttpServletRequest request;

		private MockHttpServletResponse response;


		@BeforeEach
		void setup() {
			TestServletContext servletContext = new TestServletContext();
			this.handler = new ResourceHttpRequestHandler();
			this.handler.setLocations(List.of(testResource, testAlternatePathResource, webjarsResource));
			this.handler.setServletContext(servletContext);
			this.request = new MockHttpServletRequest(servletContext, "GET", "");
			this.response = new MockHttpServletResponse();
		}

		@Test
		void defaultCachingHeaders() throws Exception {
			this.handler.afterPropertiesSet();
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
			this.handler.handleRequest(this.request, this.response);

			assertThat(this.response.containsHeader("Last-Modified")).isTrue();
			assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("test/foo.css") / 1000);
		}

		@Test
		void configureCacheSeconds() throws Exception {
			this.handler.setCacheSeconds(3600);
			this.handler.afterPropertiesSet();
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
			this.handler.handleRequest(this.request, this.response);

			assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
		}


		@Test
		void configureCacheSecondsToZero() throws Exception {
			this.handler.setCacheSeconds(0);
			this.handler.afterPropertiesSet();
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
			this.handler.handleRequest(this.request, this.response);

			assertThat(this.response.getHeader("Cache-Control")).isEqualTo("no-store");
		}

		@Test
		void configureVersionResourceResolver() throws Exception {
			VersionResourceResolver versionResolver = new VersionResourceResolver()
					.addFixedVersionStrategy("versionString", "/**");
			this.handler.setResourceResolvers(List.of(versionResolver, new PathResourceResolver()));
			this.handler.afterPropertiesSet();

			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "versionString/foo.css");
			this.handler.handleRequest(this.request, this.response);

			assertThat(this.response.getHeader("ETag")).isEqualTo("W/\"versionString\"");
		}

		@Test
		void shouldRespondWithNotModifiedWhenModifiedSince() throws Exception {
			this.handler.setCacheSeconds(3600);
			this.handler.afterPropertiesSet();
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
			this.request.addHeader("If-Modified-Since", resourceLastModified("test/foo.css"));
			this.handler.handleRequest(this.request, this.response);
			assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_MODIFIED);
			assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
		}

		@Test
		void shouldRespondWithModifiedResource() throws Exception {
			this.handler.afterPropertiesSet();
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
			this.request.addHeader("If-Modified-Since", resourceLastModified("test/foo.css") / 1000 * 1000 - 1);
			this.handler.handleRequest(this.request, this.response);
			assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
			assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
		}

		@Test
		void shouldRespondWithNotModifiedWhenEtag() throws Exception {
			this.handler.setCacheSeconds(3600);
			this.handler.setEtagGenerator(resource -> "testEtag");
			this.handler.afterPropertiesSet();
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
			this.request.addHeader("If-None-Match", "\"testEtag\"");
			this.handler.handleRequest(this.request, this.response);
			assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_MODIFIED);
			assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
		}

		@Test
		void shouldRespondWithModifiedResourceWhenEtagNoMatch() throws Exception {
			this.handler.setEtagGenerator(resource -> "noMatch");
			this.handler.afterPropertiesSet();
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
			this.request.addHeader("If-None-Match", "\"testEtag\"");
			this.handler.handleRequest(this.request, this.response);
			assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
			assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
		}

		@Test
		void shouldRespondWithNotModifiedWhenEtagAndLastModified() throws Exception {
			this.handler.setEtagGenerator(resource -> "testEtag");
			this.handler.afterPropertiesSet();
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
			this.request.addHeader("If-None-Match", "\"testEtag\"");
			this.request.addHeader("If-Modified-Since", resourceLastModified("test/foo.css"));
			this.handler.handleRequest(this.request, this.response);
			assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_MODIFIED);
		}

		@Test  // SPR-14005
		void overwritesExistingCacheControlHeaders() throws Exception {
			this.handler.setCacheSeconds(3600);
			this.handler.afterPropertiesSet();
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
			this.response.setHeader("Cache-Control", "no-store");

			this.handler.handleRequest(this.request, this.response);

			assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
		}

		@Test
		void ignoreLastModified() throws Exception {
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
			this.handler.setUseLastModified(false);
			this.handler.afterPropertiesSet();
			this.handler.handleRequest(this.request, this.response);

			assertThat(this.response.getContentType()).isEqualTo("text/css");
			assertThat(this.response.getContentLength()).isEqualTo(17);
			assertThat(this.response.containsHeader("Last-Modified")).isFalse();
			assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
		}


		private long resourceLastModified(String resourceName) throws IOException {
			return new ClassPathResource(resourceName, getClass()).getFile().lastModified();
		}

	}


	@Nested
	class ResourceLocationTests {

		private ResourceHttpRequestHandler handler;

		private MockHttpServletRequest request;

		private MockHttpServletResponse response;


		@BeforeEach
		void setup() {
			TestServletContext servletContext = new TestServletContext();
			this.handler = new ResourceHttpRequestHandler();
			this.handler.setLocations(List.of(testResource, testAlternatePathResource, webjarsResource));
			this.handler.setServletContext(servletContext);
			this.request = new MockHttpServletRequest(servletContext, "GET", "");
			this.response = new MockHttpServletResponse();
		}

		@Test
		void servesResourcesFromAlternatePath() throws Exception {
			this.handler.afterPropertiesSet();
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "baz.css");
			this.handler.handleRequest(this.request, this.response);

			assertThat(this.response.getContentType()).isEqualTo("text/css");
			assertThat(this.response.getContentLength()).isEqualTo(17);
			assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
		}

		@Test
		void servesResourcesFromSubDirectory() throws Exception {
			this.handler.afterPropertiesSet();
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "js/foo.js");
			this.handler.handleRequest(this.request, this.response);

			assertThat(this.response.getContentType()).isEqualTo("text/javascript");
			assertThat(this.response.getContentAsString()).isEqualTo("function foo() { console.log(\"hello world\"); }");
		}

		@Test
		void servesResourcesFromSubDirectoryOfAlternatePath() throws Exception {
			this.handler.afterPropertiesSet();
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "js/baz.js");
			this.handler.handleRequest(this.request, this.response);

			assertThat(this.response.getContentType()).isEqualTo("text/javascript");
			assertThat(this.response.getContentAsString()).isEqualTo("function foo() { console.log(\"hello world\"); }");
		}

		@Test  // gh-27538, gh-27624
		void filterNonExistingLocations() throws Exception {
			List<Resource> inputLocations = List.of(testResource, testAlternatePathResource,
					new ClassPathResource("nosuchpath/", ResourceHttpRequestHandlerTests.class));
			this.handler.setLocations(inputLocations);
			this.handler.setOptimizeLocations(true);
			this.handler.afterPropertiesSet();

			List<Resource> actual = handler.getLocations();
			assertThat(actual).hasSize(2);
			assertThat(actual.get(0).getURL().toString()).endsWith("test/");
			assertThat(actual.get(1).getURL().toString()).endsWith("testalternatepath/");
		}

		@Test
		void shouldRejectInvalidPath() throws Exception {
			// Use mock ResourceResolver: i.e. we're only testing upfront validations...
			Resource resource = mock();
			given(resource.getFilename()).willThrow(new AssertionError("Resource should not be resolved"));
			given(resource.getInputStream()).willThrow(new AssertionError("Resource should not be resolved"));
			ResourceResolver resolver = mock();
			given(resolver.resolveResource(any(), any(), any(), any())).willReturn(resource);

			this.handler.setLocations(List.of(testResource));
			this.handler.setResourceResolvers(List.of(resolver));
			this.handler.setServletContext(new TestServletContext());
			this.handler.afterPropertiesSet();

			testInvalidPath("../testsecret/secret.txt");
			testInvalidPath("test/../../testsecret/secret.txt");
			testInvalidPath(":/../../testsecret/secret.txt");
			testInvalidPath("/testsecret/test/../secret.txt");

			Resource location = new UrlResource(ResourceHttpRequestHandlerTests.class.getResource("./test/"));
			this.handler.setLocations(List.of(location));
			Resource secretResource = new UrlResource(ResourceHttpRequestHandlerTests.class.getResource("testsecret/secret.txt"));
			String secretPath = secretResource.getURL().getPath();

			testInvalidPath("file:" + secretPath);
			testInvalidPath("/file:" + secretPath);
			testInvalidPath("url:" + secretPath);
			testInvalidPath("/url:" + secretPath);
			testInvalidPath("/../.." + secretPath);
			testInvalidPath("/%2E%2E/testsecret/secret.txt");
			testInvalidPath("/%2E%2E/testsecret/secret.txt");
		}

		private void testInvalidPath(String requestPath) {
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, requestPath);
			this.response = new MockHttpServletResponse();
			assertNotFound();
		}

		private void assertNotFound() {
			assertThatThrownBy(() -> this.handler.handleRequest(this.request, this.response))
					.isInstanceOf(NoResourceFoundException.class);
		}

		@Test
		void shouldRejectPathWithTraversal() throws Exception {
			this.handler.afterPropertiesSet();
			for (HttpMethod method : HttpMethod.values()) {
				this.request = new MockHttpServletRequest("GET", "");
				this.response = new MockHttpServletResponse();
				shouldRejectPathWithTraversal(method);
			}
		}

		private void shouldRejectPathWithTraversal(HttpMethod httpMethod) throws Exception {
			this.request.setMethod(httpMethod.name());

			Resource location = new ClassPathResource("test/", getClass());
			this.handler.setLocations(List.of(location));

			testResolvePathWithTraversal(location, "../testsecret/secret.txt");
			testResolvePathWithTraversal(location, "test/../../testsecret/secret.txt");
			testResolvePathWithTraversal(location, ":/../../testsecret/secret.txt");

			location = new UrlResource(ResourceHttpRequestHandlerTests.class.getResource("./test/"));
			this.handler.setLocations(List.of(location));
			Resource secretResource = new UrlResource(ResourceHttpRequestHandlerTests.class.getResource("testsecret/secret.txt"));
			String secretPath = secretResource.getURL().getPath();

			testResolvePathWithTraversal(location, "file:" + secretPath);
			testResolvePathWithTraversal(location, "/file:" + secretPath);
			testResolvePathWithTraversal(location, "url:" + secretPath);
			testResolvePathWithTraversal(location, "/url:" + secretPath);
			testResolvePathWithTraversal(location, "/" + secretPath);
			testResolvePathWithTraversal(location, "////../.." + secretPath);
			testResolvePathWithTraversal(location, "/%2E%2E/testsecret/secret.txt");
			testResolvePathWithTraversal(location, "%2F%2F%2E%2E%2F%2Ftestsecret/secret.txt");
			testResolvePathWithTraversal(location, "/  " + secretPath);
		}

		private void testResolvePathWithTraversal(Resource location, String requestPath) {
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, requestPath);
			this.response = new MockHttpServletResponse();
			assertNotFound();
		}

		@Test
		void ignoreInvalidEscapeSequence() throws Exception {
			this.handler.afterPropertiesSet();
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/%foo%/bar.txt");
			this.response = new MockHttpServletResponse();
			assertNotFound();
		}

		@Test
		void processPath() {
			// Unchanged
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
			assertThat(this.handler.processPath("\\/ \\/   \\/ ")).isEqualTo("/");

			// duplicate slash or backslash
			assertThat(this.handler.processPath("//foo/ /bar//baz//")).isEqualTo("/foo/ /bar/baz/");
			assertThat(this.handler.processPath("\\\\foo\\ \\bar\\\\baz\\\\")).isEqualTo("/foo/ /bar/baz/");
			assertThat(this.handler.processPath("foo\\\\/\\////bar")).isEqualTo("foo/bar");

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
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "js/");
			assertNotFound();
		}

		@Test
		void shouldNotServeDirectoryInJarFile() throws Exception {
			this.handler.afterPropertiesSet();
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "underscorejs/");
			assertNotFound();
		}

		@Test
		void shouldNotServeMissingResourcePath() throws Exception {
			this.handler.afterPropertiesSet();
			this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "");
			assertNotFound();
		}

		@Test
		void noPathWithinHandlerMappingAttribute() throws Exception {
			this.handler.afterPropertiesSet();
			assertThatIllegalStateException().isThrownBy(() ->
					this.handler.handleRequest(this.request, this.response));
		}

		@Test
		void servletContextRootValidation() {
			StaticWebApplicationContext context = new StaticWebApplicationContext() {
				@Override
				public Resource getResource(String location) {
					return new FileSystemResource("/");
				}
			};

			ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
			handler.setLocationValues(List.of("/"));
			handler.setApplicationContext(context);

			assertThatIllegalStateException().isThrownBy(handler::afterPropertiesSet)
					.withMessage("The String-based location \"/\" should be relative to the web application root but " +
							"resolved to a Resource of type: class org.springframework.core.io.FileSystemResource. " +
							"If this is intentional, please pass it as a pre-configured Resource via setLocations.");
		}

	}


	private static class TestServletContext extends MockServletContext {

		@Override
		public String getMimeType(String filePath) {
			if (filePath.endsWith(".css")) {
				return "text/css";
			}
			else if (filePath.endsWith(".js")) {
				return "text/javascript";
			}
			else {
				return super.getMimeType(filePath);
			}
		}
	}

}
