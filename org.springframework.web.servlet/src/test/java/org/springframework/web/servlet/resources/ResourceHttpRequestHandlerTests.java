package org.springframework.web.servlet.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;
import org.springframework.web.servlet.resources.ResourceHttpRequestHandler;


public class ResourceHttpRequestHandlerTests {

	private ResourceHttpRequestHandler handler;
	
	@Before
	public void setUp() {
		handler = new ResourceHttpRequestHandler(new ClassPathResource("test/", getClass()));
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
	public void getResourceBundle() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/foo.css,bar.css");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.handleRequest(request, response);
		assertEquals("text/css", response.getContentType());
		assertEquals(36, response.getContentLength());
		assertTrue(((Long)response.getHeader("Expires")) > System.currentTimeMillis() + (31556926 * 1000) - 10000);
		assertEquals("max-age=31556926", response.getHeader("Cache-Control"));
		assertTrue(response.containsHeader("Last-Modified"));
		assertEquals(response.getHeader("Last-Modified"), new ClassPathResource("test/bar.css", getClass()).getFile().lastModified());
		assertEquals("h1 { color:red; }h2 { color:white; }", response.getContentAsString());
	}

	@Test
	public void getResourceBundleDifferentTypes() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/foo.css,/js/bar.js");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.handleRequest(request, response);
		assertEquals("text/css", response.getContentType());
		assertEquals("h1 { color:red; }function foo() { console.log(\"hello bar\"); }", response.getContentAsString());
	}
	
	@Test
	public void getResourceFromSubDirectory() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/js/foo.js");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.handleRequest(request, response);
		System.out.println(response.getContentAsString());
		assertEquals("text/javascript", response.getContentType());
		assertEquals("function foo() { console.log(\"hello world\"); }", response.getContentAsString());
	}
	
	@Test
	public void getResourceBundleDifferentTypesIncludingDirectory() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/foo.css,/js,/js/foo.js");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.handleRequest(request, response);
		assertEquals("text/css", response.getContentType());
		assertEquals("h1 { color:red; }function foo() { console.log(\"hello world\"); }", response.getContentAsString());
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

	@Test(expected=NoSuchRequestHandlingMethodException.class)
	public void missingResourcePath() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.handleRequest(request, response);
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
