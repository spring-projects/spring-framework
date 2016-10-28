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

package org.springframework.web.servlet.resource;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import javax.servlet.http.HttpServletResponse;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationManagerFactoryBean;
import org.springframework.web.servlet.HandlerMapping;

import static org.junit.Assert.*;

/**
 * Unit tests for ResourceHttpRequestHandler.
 *
 * @author Keith Donald
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class ResourceHttpRequestHandlerTests {

	private SimpleDateFormat dateFormat;

	private ResourceHttpRequestHandler handler;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;


	@Before
	public void setUp() throws Exception {
		dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

		List<Resource> paths = new ArrayList<>(2);
		paths.add(new ClassPathResource("test/", getClass()));
		paths.add(new ClassPathResource("testalternatepath/", getClass()));
		paths.add(new ClassPathResource("META-INF/resources/webjars/"));

		this.handler = new ResourceHttpRequestHandler();
		this.handler.setLocations(paths);
		this.handler.setCacheSeconds(3600);
		this.handler.setServletContext(new TestServletContext());
		this.handler.afterPropertiesSet();

		this.request = new MockHttpServletRequest("GET", "");
		this.response = new MockHttpServletResponse();
	}

	@Test
	public void getResource() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.handleRequest(this.request, this.response);

		assertEquals("text/css", this.response.getContentType());
		assertEquals(17, this.response.getContentLength());
		assertEquals("max-age=3600", this.response.getHeader("Cache-Control"));
		assertTrue(this.response.containsHeader("Last-Modified"));
		assertEquals(this.response.getHeader("Last-Modified"), resourceLastModifiedDate("test/foo.css"));
		assertEquals("bytes", this.response.getHeader("Accept-Ranges"));
		assertEquals(1, this.response.getHeaders("Accept-Ranges").size());
		assertEquals("h1 { color:red; }", this.response.getContentAsString());
	}

	@Test
	public void getResourceHttpHeader() throws Exception {
		this.request.setMethod("HEAD");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.handleRequest(this.request, this.response);

		assertEquals(200, this.response.getStatus());
		assertEquals("text/css", this.response.getContentType());
		assertEquals(17, this.response.getContentLength());
		assertEquals("max-age=3600", this.response.getHeader("Cache-Control"));
		assertTrue(this.response.containsHeader("Last-Modified"));
		assertEquals(this.response.getHeader("Last-Modified"), resourceLastModifiedDate("test/foo.css"));
		assertEquals("bytes", this.response.getHeader("Accept-Ranges"));
		assertEquals(1, this.response.getHeaders("Accept-Ranges").size());
		assertEquals(0, this.response.getContentAsByteArray().length);
	}

	@Test
	public void getResourceHttpOptions() throws Exception {
		this.request.setMethod("OPTIONS");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.handleRequest(this.request, this.response);

		assertEquals(200, this.response.getStatus());
		assertEquals("GET,HEAD,OPTIONS", this.response.getHeader("Allow"));
	}

	@Test
	public void getResourceNoCache() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.setCacheSeconds(0);
		this.handler.handleRequest(this.request, this.response);

		assertEquals("no-store", this.response.getHeader("Cache-Control"));
		assertTrue(this.response.containsHeader("Last-Modified"));
		assertEquals(this.response.getHeader("Last-Modified"), resourceLastModifiedDate("test/foo.css"));
		assertEquals("bytes", this.response.getHeader("Accept-Ranges"));
		assertEquals(1, this.response.getHeaders("Accept-Ranges").size());
	}

	@Test
	public void getVersionedResource() throws Exception {
		VersionResourceResolver versionResolver = new VersionResourceResolver()
				.addFixedVersionStrategy("versionString", "/**");
		this.handler.setResourceResolvers(Arrays.asList(versionResolver, new PathResourceResolver()));
		this.handler.afterPropertiesSet();

		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "versionString/foo.css");
		this.handler.handleRequest(this.request, this.response);

		assertEquals("\"versionString\"", this.response.getHeader("ETag"));
		assertEquals("bytes", this.response.getHeader("Accept-Ranges"));
		assertEquals(1, this.response.getHeaders("Accept-Ranges").size());
	}

	@Test
	@SuppressWarnings("deprecation")
	public void getResourceHttp10BehaviorCache() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.setCacheSeconds(3600);
		this.handler.setUseExpiresHeader(true);
		this.handler.setUseCacheControlHeader(true);
		this.handler.setAlwaysMustRevalidate(true);
		this.handler.handleRequest(this.request, this.response);

		assertEquals("max-age=3600, must-revalidate", this.response.getHeader("Cache-Control"));
		assertTrue(dateHeaderAsLong("Expires") >= System.currentTimeMillis() - 1000 + (3600 * 1000));
		assertTrue(this.response.containsHeader("Last-Modified"));
		assertEquals(this.response.getHeader("Last-Modified"), resourceLastModifiedDate("test/foo.css"));
		assertEquals("bytes", this.response.getHeader("Accept-Ranges"));
		assertEquals(1, this.response.getHeaders("Accept-Ranges").size());
	}

	@Test
	@SuppressWarnings("deprecation")
	public void getResourceHttp10BehaviorNoCache() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.setCacheSeconds(0);
		this.handler.setUseExpiresHeader(true);
		this.handler.setUseCacheControlNoStore(false);
		this.handler.setUseCacheControlHeader(true);
		this.handler.handleRequest(this.request, this.response);

		assertEquals("no-cache", this.response.getHeader("Pragma"));
		assertThat(this.response.getHeaderValues("Cache-Control"), Matchers.iterableWithSize(1));
		assertEquals("no-cache", this.response.getHeader("Cache-Control"));
		assertTrue(dateHeaderAsLong("Expires") <= System.currentTimeMillis());
		assertTrue(this.response.containsHeader("Last-Modified"));
		assertEquals(dateHeaderAsLong("Last-Modified") / 1000, resourceLastModified("test/foo.css") / 1000);
		assertEquals("bytes", this.response.getHeader("Accept-Ranges"));
		assertEquals(1, this.response.getHeaders("Accept-Ranges").size());
	}

	@Test
	public void getResourceWithHtmlMediaType() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.html");
		this.handler.handleRequest(this.request, this.response);

		assertEquals("text/html", this.response.getContentType());
		assertEquals("max-age=3600", this.response.getHeader("Cache-Control"));
		assertTrue(this.response.containsHeader("Last-Modified"));
		assertEquals(this.response.getHeader("Last-Modified"), resourceLastModifiedDate("test/foo.html"));
		assertEquals("bytes", this.response.getHeader("Accept-Ranges"));
		assertEquals(1, this.response.getHeaders("Accept-Ranges").size());
	}

	@Test
	public void getResourceFromAlternatePath() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "baz.css");
		this.handler.handleRequest(this.request, this.response);

		assertEquals("text/css", this.response.getContentType());
		assertEquals(17, this.response.getContentLength());
		assertEquals("max-age=3600", this.response.getHeader("Cache-Control"));
		assertTrue(this.response.containsHeader("Last-Modified"));
		assertEquals(this.response.getHeader("Last-Modified"), resourceLastModifiedDate("testalternatepath/baz.css"));
		assertEquals("bytes", this.response.getHeader("Accept-Ranges"));
		assertEquals(1, this.response.getHeaders("Accept-Ranges").size());
		assertEquals("h1 { color:red; }", this.response.getContentAsString());
	}

	@Test
	public void getResourceFromSubDirectory() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "js/foo.js");
		this.handler.handleRequest(this.request, this.response);

		assertEquals("text/javascript", this.response.getContentType());
		assertEquals("function foo() { console.log(\"hello world\"); }", this.response.getContentAsString());
	}

	@Test
	public void getResourceFromSubDirectoryOfAlternatePath() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "js/baz.js");
		this.handler.handleRequest(this.request, this.response);

		assertEquals("text/javascript", this.response.getContentType());
		assertEquals("function foo() { console.log(\"hello world\"); }", this.response.getContentAsString());
	}

	@Test // SPR-13658
	public void getResourceWithRegisteredMediaType() throws Exception {
		ContentNegotiationManagerFactoryBean factory = new ContentNegotiationManagerFactoryBean();
		factory.addMediaType("css", new MediaType("foo", "bar"));
		factory.afterPropertiesSet();
		ContentNegotiationManager manager = factory.getObject();

		List<Resource> paths = Collections.singletonList(new ClassPathResource("test/", getClass()));
		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setServletContext(new MockServletContext());
		handler.setLocations(paths);
		handler.setContentNegotiationManager(manager);
		handler.afterPropertiesSet();

		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		handler.handleRequest(this.request, this.response);

		assertEquals("foo/bar", this.response.getContentType());
		assertEquals("h1 { color:red; }", this.response.getContentAsString());
	}

	@Test // SPR-14577
	public void getMediaTypeWithFavorPathExtensionOff() throws Exception {
		ContentNegotiationManagerFactoryBean factory = new ContentNegotiationManagerFactoryBean();
		factory.setFavorPathExtension(false);
		factory.afterPropertiesSet();
		ContentNegotiationManager manager = factory.getObject();

		List<Resource> paths = Collections.singletonList(new ClassPathResource("test/", getClass()));
		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setServletContext(new MockServletContext());
		handler.setLocations(paths);
		handler.setContentNegotiationManager(manager);
		handler.afterPropertiesSet();

		this.request.addHeader("Accept", "application/json,text/plain,*/*");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.html");
		handler.handleRequest(this.request, this.response);

		assertEquals("text/html", this.response.getContentType());
	}

	@Test // SPR-14368
	public void getResourceWithMediaTypeResolvedThroughServletContext() throws Exception {
		MockServletContext servletContext = new MockServletContext() {

			@Override
			public String getMimeType(String filePath) {
				return "foo/bar";
			}

			@Override
			public String getVirtualServerName() {
				return null;
			}
		};

		List<Resource> paths = Collections.singletonList(new ClassPathResource("test/", getClass()));
		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setServletContext(servletContext);
		handler.setLocations(paths);
		handler.afterPropertiesSet();

		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		handler.handleRequest(this.request, this.response);

		assertEquals("foo/bar", this.response.getContentType());
		assertEquals("h1 { color:red; }", this.response.getContentAsString());
	}

	@Test
	public void invalidPath() throws Exception {
		for (HttpMethod method : HttpMethod.values()) {
			this.request = new MockHttpServletRequest("GET", "");
			this.response = new MockHttpServletResponse();
			testInvalidPath(method);
		}
	}

	private void testInvalidPath(HttpMethod httpMethod) throws Exception {
		this.request.setMethod(httpMethod.name());

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
		testInvalidPath(location, "/" + secretPath);
		testInvalidPath(location, "////../.." + secretPath);
		testInvalidPath(location, "/%2E%2E/testsecret/secret.txt");
		testInvalidPath(location, "/  " + secretPath);
		testInvalidPath(location, "url:" + secretPath);
	}

	private void testInvalidPath(Resource location, String requestPath) throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, requestPath);
		this.response = new MockHttpServletResponse();
		this.handler.handleRequest(this.request, this.response);
		if (!location.createRelative(requestPath).exists() && !requestPath.contains(":")) {
			fail(requestPath + " doesn't actually exist as a relative path");
		}
		assertEquals(HttpStatus.NOT_FOUND.value(), this.response.getStatus());
	}

	@Test
	public void ignoreInvalidEscapeSequence() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/%foo%/bar.txt");
		this.response = new MockHttpServletResponse();
		this.handler.handleRequest(this.request, this.response);
		assertEquals(404, this.response.getStatus());
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

		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setResourceResolvers(Collections.singletonList(pathResolver));
		handler.setServletContext(new MockServletContext());
		handler.setLocations(Arrays.asList(location1, location2));
		handler.afterPropertiesSet();

		Resource[] locations = pathResolver.getAllowedLocations();
		assertEquals(1, locations.length);
		assertEquals("test/", ((ClassPathResource) locations[0]).getPath());
	}

	@Test
	public void notModified() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.request.addHeader("If-Modified-Since", resourceLastModified("test/foo.css"));
		this.handler.handleRequest(this.request, this.response);
		assertEquals(HttpServletResponse.SC_NOT_MODIFIED, this.response.getStatus());
	}

	@Test
	public void modified() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.request.addHeader("If-Modified-Since", resourceLastModified("test/foo.css") / 1000 * 1000 - 1);
		this.handler.handleRequest(this.request, this.response);
		assertEquals(HttpServletResponse.SC_OK, this.response.getStatus());
		assertEquals("h1 { color:red; }", this.response.getContentAsString());
	}

	@Test
	public void directory() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "js/");
		this.handler.handleRequest(this.request, this.response);
		assertEquals(404, this.response.getStatus());
	}

	@Test
	public void directoryInJarFile() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "underscorejs/");
		this.handler.handleRequest(this.request, this.response);
		assertEquals(200, this.response.getStatus());
		assertEquals(0, this.response.getContentLength());
	}

	@Test
	public void missingResourcePath() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "");
		this.handler.handleRequest(this.request, this.response);
		assertEquals(404, this.response.getStatus());
	}

	@Test(expected = IllegalStateException.class)
	public void noPathWithinHandlerMappingAttribute() throws Exception {
		this.handler.handleRequest(this.request, this.response);
	}

	@Test(expected = HttpRequestMethodNotSupportedException.class)
	public void unsupportedHttpMethod() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.request.setMethod("POST");
		this.handler.handleRequest(this.request, this.response);
	}

	@Test
	public void resourceNotFound() throws Exception {
		for (HttpMethod method : HttpMethod.values()) {
			this.request = new MockHttpServletRequest("GET", "");
			this.response = new MockHttpServletResponse();
			resourceNotFound(method);
		}
	}

	private void resourceNotFound(HttpMethod httpMethod) throws Exception {
		this.request.setMethod(httpMethod.name());
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "not-there.css");
		this.handler.handleRequest(this.request, this.response);
		assertEquals(HttpStatus.NOT_FOUND.value(), this.response.getStatus());
	}

	@Test
	public void partialContentByteRange() throws Exception {
		this.request.addHeader("Range", "bytes=0-1");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertEquals(206, this.response.getStatus());
		assertEquals("text/plain", this.response.getContentType());
		assertEquals(2, this.response.getContentLength());
		assertEquals("bytes 0-1/10", this.response.getHeader("Content-Range"));
		assertEquals("So", this.response.getContentAsString());
		assertEquals("bytes", this.response.getHeader("Accept-Ranges"));
		assertEquals(1, this.response.getHeaders("Accept-Ranges").size());
	}

	@Test
	public void partialContentByteRangeNoEnd() throws Exception {
		this.request.addHeader("Range", "bytes=9-");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertEquals(206, this.response.getStatus());
		assertEquals("text/plain", this.response.getContentType());
		assertEquals(1, this.response.getContentLength());
		assertEquals("bytes 9-9/10", this.response.getHeader("Content-Range"));
		assertEquals(".", this.response.getContentAsString());
		assertEquals("bytes", this.response.getHeader("Accept-Ranges"));
		assertEquals(1, this.response.getHeaders("Accept-Ranges").size());
	}

	@Test
	public void partialContentByteRangeLargeEnd() throws Exception {
		this.request.addHeader("Range", "bytes=9-10000");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertEquals(206, this.response.getStatus());
		assertEquals("text/plain", this.response.getContentType());
		assertEquals(1, this.response.getContentLength());
		assertEquals("bytes 9-9/10", this.response.getHeader("Content-Range"));
		assertEquals(".", this.response.getContentAsString());
		assertEquals("bytes", this.response.getHeader("Accept-Ranges"));
		assertEquals(1, this.response.getHeaders("Accept-Ranges").size());
	}

	@Test
	public void partialContentSuffixRange() throws Exception {
		this.request.addHeader("Range", "bytes=-1");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertEquals(206, this.response.getStatus());
		assertEquals("text/plain", this.response.getContentType());
		assertEquals(1, this.response.getContentLength());
		assertEquals("bytes 9-9/10", this.response.getHeader("Content-Range"));
		assertEquals(".", this.response.getContentAsString());
		assertEquals("bytes", this.response.getHeader("Accept-Ranges"));
		assertEquals(1, this.response.getHeaders("Accept-Ranges").size());
	}

	@Test
	public void partialContentSuffixRangeLargeSuffix() throws Exception {
		this.request.addHeader("Range", "bytes=-11");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertEquals(206, this.response.getStatus());
		assertEquals("text/plain", this.response.getContentType());
		assertEquals(10, this.response.getContentLength());
		assertEquals("bytes 0-9/10", this.response.getHeader("Content-Range"));
		assertEquals("Some text.", this.response.getContentAsString());
		assertEquals("bytes", this.response.getHeader("Accept-Ranges"));
		assertEquals(1, this.response.getHeaders("Accept-Ranges").size());
	}

	@Test
	public void partialContentInvalidRangeHeader() throws Exception {
		this.request.addHeader("Range", "bytes= foo bar");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertEquals(416, this.response.getStatus());
		assertEquals("bytes */10", this.response.getHeader("Content-Range"));
		assertEquals("bytes", this.response.getHeader("Accept-Ranges"));
		assertEquals(1, this.response.getHeaders("Accept-Ranges").size());
	}

	@Test
	public void partialContentMultipleByteRanges() throws Exception {
		this.request.addHeader("Range", "bytes=0-1, 4-5, 8-9");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertEquals(206, this.response.getStatus());
		assertTrue(this.response.getContentType().startsWith("multipart/byteranges; boundary="));

		String boundary = "--" + this.response.getContentType().substring(31);

		String content = this.response.getContentAsString();
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
	}

	// SPR-14005
	@Test
	public void doOverwriteExistingCacheControlHeaders() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.response.setHeader("Cache-Control", "no-store");

		this.handler.handleRequest(this.request, this.response);

		assertEquals("max-age=3600", this.response.getHeader("Cache-Control"));
	}


	private long dateHeaderAsLong(String responseHeaderName) throws Exception {
		return dateFormat.parse(this.response.getHeader(responseHeaderName)).getTime();
	}

	private long resourceLastModified(String resourceName) throws IOException {
		return new ClassPathResource(resourceName, getClass()).getFile().lastModified();
	}

	private String resourceLastModifiedDate(String resourceName) throws IOException {
		long lastModified = new ClassPathResource(resourceName, getClass()).getFile().lastModified();
		return dateFormat.format(lastModified);
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
