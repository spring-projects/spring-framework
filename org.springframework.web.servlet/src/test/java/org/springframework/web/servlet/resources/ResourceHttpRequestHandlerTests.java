package org.springframework.web.servlet.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.HandlerMapping;

public class ResourceHttpRequestHandlerTests {

	private ResourceHttpRequestHandler handler;
	
	@Before
	public void setUp() {
		List<Resource> resourcePaths = new ArrayList<Resource>();
		resourcePaths.add(new ClassPathResource("test/", getClass()));
		resourcePaths.add(new ClassPathResource("testalternatepath/", getClass()));
		handler = new ResourceHttpRequestHandler(resourcePaths);
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
		assertTrue(((Long)response.getHeader("Expires")) > System.currentTimeMillis() + (31556926 * 1000) - 10000);
		assertEquals("max-age=31556926", response.getHeader("Cache-Control"));
		assertTrue(response.containsHeader("Last-Modified"));
		assertEquals(response.getHeader("Last-Modified"), new ClassPathResource("test/foo.css", getClass()).getFile().lastModified());
		assertEquals("h1 { color:red; }", response.getContentAsString());
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
		assertTrue(((Long)response.getHeader("Expires")) > System.currentTimeMillis() + (31556926 * 1000) - 10000);
		assertEquals("max-age=31556926", response.getHeader("Cache-Control"));
		assertTrue(response.containsHeader("Last-Modified"));
		assertEquals(response.getHeader("Last-Modified"), new ClassPathResource("testalternatepath/baz.css", getClass()).getFile().lastModified());
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
	public void lastModified() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/foo.css");
		long expected = new ClassPathResource("test/foo.css", getClass()).lastModified();
		long lastModified = handler.getLastModified(request);
		assertEquals(expected, lastModified);
	}
	
	@Test
	public void modified() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/foo.css");
		request.addHeader("If-Modified-Since", new ClassPathResource("test/foo.css", getClass()).getFile().lastModified() - 1);
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
	
	@Test(expected=IllegalStateException.class)
	public void noPathWithinHandlerMappingAttribute() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.handleRequest(request, response);
	}
	
	@Test(expected=HttpRequestMethodNotSupportedException.class)
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
}
