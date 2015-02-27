/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.HandlerMapping;

import static org.junit.Assert.*;

/**
 * @author Keith Donald
 * @author Jeremy Grelle
 */
public class ResourceHttpRequestHandlerTests {

	private ResourceHttpRequestHandler handler;

	@Before
	public void setUp() {
		List<Resource> resourcePaths = new ArrayList<Resource>();
		resourcePaths.add(new ClassPathResource("test/", getClass()));
		resourcePaths.add(new ClassPathResource("testalternatepath/", getClass()));
		handler = new ResourceHttpRequestHandler();
		handler.setLocations(resourcePaths);
		handler.setCacheSeconds(3600);
		handler.setServletContext(new TestServletContext());
	}

	@Test
	public void getResource() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/foo.css");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.handleRequest(request, response);
		assertEquals("text/css", response.getContentType());
		assertEquals(17, response.getContentLength());
		assertTrue(Long.valueOf(response.getHeader("Expires")) >= System.currentTimeMillis() - 1000 + (3600 * 1000));
		assertEquals("max-age=3600, must-revalidate", response.getHeader("Cache-Control"));
		assertTrue(response.containsHeader("Last-Modified"));
		assertEquals(Long.valueOf(response.getHeader("Last-Modified")).longValue(),
				new ClassPathResource("test/foo.css", getClass()).getFile().lastModified());
		assertEquals("h1 { color:red; }", response.getContentAsString());
	}

	@Test
	public void getResourceWithHtmlMediaType() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/foo.html");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.handleRequest(request, response);
		assertEquals("text/html", response.getContentType());
		assertTrue(Long.valueOf(response.getHeader("Expires")) >= System.currentTimeMillis() - 1000 + (3600 * 1000));
		assertEquals("max-age=3600, must-revalidate", response.getHeader("Cache-Control"));
		assertTrue(response.containsHeader("Last-Modified"));
		assertEquals(Long.valueOf(response.getHeader("Last-Modified")).longValue(),
				new ClassPathResource("test/foo.html", getClass()).getFile().lastModified());
	}

	@Test
	public void getResourceFromAlternatePath() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/baz.css");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.handleRequest(request, response);
		assertEquals("text/css", response.getContentType());
		assertEquals(17, response.getContentLength());
		assertTrue(Long.valueOf(response.getHeader("Expires")) >= System.currentTimeMillis() - 1000 + (3600 * 1000));
		assertEquals("max-age=3600, must-revalidate", response.getHeader("Cache-Control"));
		assertTrue(response.containsHeader("Last-Modified"));
		assertEquals(Long.valueOf(response.getHeader("Last-Modified")).longValue(),
				new ClassPathResource("testalternatepath/baz.css", getClass()).getFile().lastModified());
		assertEquals("h1 { color:red; }", response.getContentAsString());
	}

	@Test
	public void getResourceFromSubDirectory() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/js/foo.js");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.handleRequest(request, response);
		assertEquals("text/javascript", response.getContentType());
		assertEquals("function foo() { console.log(\"hello world\"); }", response.getContentAsString());
	}

	@Test
	public void getResourceFromSubDirectoryOfAlternatePath() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/js/baz.js");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.handleRequest(request, response);
		assertEquals("text/javascript", response.getContentType());
		assertEquals("function foo() { console.log(\"hello world\"); }", response.getContentAsString());
	}

	@Test
	public void invalidPath() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();

		Resource location = new ClassPathResource("test/", getClass());
		this.handler.setLocations(Arrays.asList(location));

		testInvalidPath(location, "../testsecret/secret.txt", request, response);
		testInvalidPath(location, "test/../../testsecret/secret.txt", request, response);
		testInvalidPath(location, ":/../../testsecret/secret.txt", request, response);

		location = new UrlResource(getClass().getResource("./test/"));
		this.handler.setLocations(Arrays.asList(location));
		Resource secretResource = new UrlResource(getClass().getResource("testsecret/secret.txt"));
		String secretPath = secretResource.getURL().getPath();

		testInvalidPath(location, "file:" + secretPath, request, response);
		testInvalidPath(location, "/file:" + secretPath, request, response);
		testInvalidPath(location, "url:" + secretPath, request, response);
		testInvalidPath(location, "/url:" + secretPath, request, response);
		testInvalidPath(location, "/" + secretPath, request, response);
		testInvalidPath(location, "////../.." + secretPath, request, response);
		testInvalidPath(location, "/%2E%2E/testsecret/secret.txt", request, response);
		testInvalidPath(location, "/%2e%2e/testsecret/secret.txt", request, response);
		testInvalidPath(location, " " + secretPath, request, response);
		testInvalidPath(location, "/  " + secretPath, request, response);
		testInvalidPath(location, "url:" + secretPath, request, response);
	}

	private void testInvalidPath(Resource location, String requestPath,
			MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {

		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, requestPath);
		response = new MockHttpServletResponse();
		this.handler.handleRequest(request, response);
		if (!location.createRelative(requestPath).exists() && !requestPath.contains(":")) {
			fail(requestPath + " doesn't actually exist as a relative path");
		}
		assertEquals(404, response.getStatus());
	}

	@Test
	public void ignoreInvalidEscapeSequence() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/%foo%/bar.txt");
		response = new MockHttpServletResponse();
		this.handler.handleRequest(request, response);
		assertEquals(404, response.getStatus());
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
	public void notModified() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/foo.css");
		request.addHeader("If-Modified-Since", new ClassPathResource("test/foo.css", getClass()).getFile().lastModified());
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.handleRequest(request, response);
		assertEquals(HttpServletResponse.SC_NOT_MODIFIED, response.getStatus());
	}

	@Test
	public void modified() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/foo.css");
		request.addHeader("If-Modified-Since",
				new ClassPathResource("test/foo.css", getClass()).getFile().lastModified() / 1000 * 1000 - 1);
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.handleRequest(request, response);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		assertEquals("h1 { color:red; }", response.getContentAsString());
	}

	@Test
	public void directory() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/js/");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.handleRequest(request, response);
		assertEquals(404, response.getStatus());
	}

	@Test
	public void missingResourcePath() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.handleRequest(request, response);
		assertEquals(404, response.getStatus());
	}

	@Test(expected = IllegalStateException.class)
	public void noPathWithinHandlerMappingAttribute() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.handleRequest(request, response);
	}

	@Test(expected = HttpRequestMethodNotSupportedException.class)
	public void unsupportedHttpMethod() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/foo.css");
		request.setMethod("POST");
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.handleRequest(request, response);
	}

	@Test
	public void resourceNotFound() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/not-there.css");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.handleRequest(request, response);
		assertEquals(404, response.getStatus());
	}


	// SPR-12747
	@Test
	public void getResourceWithResourceLocation() throws Exception {
		List<Resource> resourcePaths = new ArrayList<Resource>();
		resourcePaths.add(new ClassPathResource("test/foo.css", getClass()));
		this.handler.setLocations(resourcePaths);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/foo.css");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.handleRequest(request, response);
		assertEquals("text/css", response.getContentType());
		assertEquals(17, response.getContentLength());
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
