/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.web.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.CommonsClientHttpRequestFactory;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/** @author Arjen Poutsma */
public class RestTemplateIntegrationTests {

	private RestTemplate template;

	private static Server jettyServer;

	private static String helloWorld = "H\u00e9llo W\u00f6rld";

	private static final String URI = "http://localhost:8889";

	private static MediaType contentType;

	@BeforeClass
	public static void startJettyServer() throws Exception {
		jettyServer = new Server(8889);
		Context jettyContext = new Context(jettyServer, "/");
		byte[] bytes = helloWorld.getBytes("UTF-8");
		contentType = new MediaType("text", "plain", Collections.singletonMap("charset", "utf-8"));
		jettyContext.addServlet(new ServletHolder(new GetServlet(bytes, contentType)), "/get");
		jettyContext.addServlet(new ServletHolder(new GetServlet(new byte[0], contentType)), "/get/nothing");
		jettyContext.addServlet(
				new ServletHolder(new PostServlet(helloWorld, URI + "/post/1", bytes, contentType)),
				"/post");
		jettyContext.addServlet(new ServletHolder(new ErrorServlet(404)), "/errors/notfound");
		jettyContext.addServlet(new ServletHolder(new ErrorServlet(500)), "/errors/server");
		jettyContext.addServlet(new ServletHolder(new UriServlet()), "/uri/*");
		jettyContext.addServlet(new ServletHolder(new MultipartServlet()), "/multipart");
		jettyServer.start();
	}

	@Before
	public void createTemplate() {
		template = new RestTemplate(new CommonsClientHttpRequestFactory());
	}

	@AfterClass
	public static void stopJettyServer() throws Exception {
		if (jettyServer != null) {
			jettyServer.stop();
		}
	}

	@Test
	public void getString() {
		String s = template.getForObject(URI + "/{method}", String.class, "get");
		assertEquals("Invalid content", helloWorld, s);
	}

	@Test
	public void getEntity() {
		ResponseEntity<String> entity = template.getForEntity(URI + "/{method}", String.class, "get");
		assertEquals("Invalid content", helloWorld, entity.getBody());
		assertFalse("No headers", entity.getHeaders().isEmpty());
		assertEquals("Invalid content-type", contentType, entity.getHeaders().getContentType());
		assertEquals("Invalid status code", HttpStatus.OK, entity.getStatusCode());
	}

	@Test
	public void getNoResponse() {
		String s = template.getForObject(URI + "/get/nothing", String.class);
		assertEquals("Invalid content", "", s);
	}

	@Test
	public void postForLocation() throws URISyntaxException {
		URI location = template.postForLocation(URI + "/{method}", helloWorld, "post");
		assertEquals("Invalid location", new URI(URI + "/post/1"), location);
	}

	@Test
	public void postForLocationEntity() throws URISyntaxException {
		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(new MediaType("text", "plain", Charset.forName("ISO-8859-15")));
		HttpEntity<String> entity = new HttpEntity<String>(helloWorld, entityHeaders);
		URI location = template.postForLocation(URI + "/{method}", entity, "post");
		assertEquals("Invalid location", new URI(URI + "/post/1"), location);
	}

	@Test
	public void postForObject() throws URISyntaxException {
		String s = template.postForObject(URI + "/{method}", helloWorld, String.class, "post");
		assertEquals("Invalid content", helloWorld, s);
	}

	@Test
	public void notFound() {
		try {
			template.execute(URI + "/errors/notfound", HttpMethod.GET, null, null);
			fail("HttpClientErrorException expected");
		}
		catch (HttpClientErrorException ex) {
			assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
			assertNotNull(ex.getStatusText());
			assertNotNull(ex.getResponseBodyAsString());
		}
	}

	@Test
	public void serverError() {
		try {
			template.execute(URI + "/errors/server", HttpMethod.GET, null, null);
			fail("HttpServerErrorException expected");
		}
		catch (HttpServerErrorException ex) {
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
			assertNotNull(ex.getStatusText());
			assertNotNull(ex.getResponseBodyAsString());
		}
	}

	@Test
	public void optionsForAllow() throws URISyntaxException {
		Set<HttpMethod> allowed = template.optionsForAllow(new URI(URI + "/get"));
		assertEquals("Invalid response",
				EnumSet.of(HttpMethod.GET, HttpMethod.OPTIONS, HttpMethod.HEAD, HttpMethod.TRACE), allowed);
	}

	@Test
	public void uri() throws InterruptedException, URISyntaxException {
		String result = template.getForObject(URI + "/uri/{query}", String.class, "Z\u00fcrich");
		assertEquals("Invalid request URI", "/uri/Z%C3%BCrich", result);

		result = template.getForObject(URI + "/uri/query={query}", String.class, "foo@bar");
		assertEquals("Invalid request URI", "/uri/query=foo@bar", result);

		result = template.getForObject(URI + "/uri/query={query}", String.class, "T\u014dky\u014d");
		assertEquals("Invalid request URI", "/uri/query=T%C5%8Dky%C5%8D", result);
	}

	@Test
	public void multipart() throws UnsupportedEncodingException {
		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<String, Object>();
		parts.add("name 1", "value 1");
		parts.add("name 2", "value 2+1");
		parts.add("name 2", "value 2+2");
		Resource logo = new ClassPathResource("/org/springframework/http/converter/logo.jpg");
		parts.add("logo", logo);

		template.postForLocation(URI + "/multipart", parts);
	}

	@Test
	public void exchangeGet() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("MyHeader", "MyValue");
		HttpEntity<?> requestEntity = new HttpEntity(requestHeaders);
		ResponseEntity<String> response =
				template.exchange(URI + "/{method}", HttpMethod.GET, requestEntity, String.class, "get");
		assertEquals("Invalid content", helloWorld, response.getBody());
	}

	@Test
	public void exchangePost() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("MyHeader", "MyValue");
		requestHeaders.setContentType(MediaType.TEXT_PLAIN);
		HttpEntity<String> requestEntity = new HttpEntity<String>(helloWorld, requestHeaders);
		HttpEntity<?> result = template.exchange(URI + "/{method}", HttpMethod.POST, requestEntity, null, "post");
		assertEquals("Invalid location", new URI(URI + "/post/1"), result.getHeaders().getLocation());
		assertFalse(result.hasBody());
	}

	/** Servlet that returns and error message for a given status code. */
	private static class ErrorServlet extends GenericServlet {

		private final int sc;

		private ErrorServlet(int sc) {
			this.sc = sc;
		}

		@Override
		public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
			((HttpServletResponse) response).sendError(sc);
		}
	}

	private static class GetServlet extends HttpServlet {

		private final byte[] buf;

		private final MediaType contentType;

		private GetServlet(byte[] buf, MediaType contentType) {
			this.buf = buf;
			this.contentType = contentType;
		}

		@Override
		protected void doGet(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {
			response.setContentType(contentType.toString());
			response.setContentLength(buf.length);
			FileCopyUtils.copy(buf, response.getOutputStream());
		}
	}

	private static class PostServlet extends HttpServlet {

		private final String s;

		private final String location;

		private final byte[] buf;

		private final MediaType contentType;

		private PostServlet(String s, String location, byte[] buf, MediaType contentType) {
			this.s = s;
			this.location = location;
			this.buf = buf;
			this.contentType = contentType;
		}

		@Override
		protected void doPost(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {
			assertTrue("Invalid request content-length", request.getContentLength() > 0);
			assertNotNull("No content-type", request.getContentType());
			String body = FileCopyUtils.copyToString(request.getReader());
			assertEquals("Invalid request body", s, body);
			response.setStatus(HttpServletResponse.SC_CREATED);
			response.setHeader("Location", location);
			response.setContentLength(buf.length);
			response.setContentType(contentType.toString());
			FileCopyUtils.copy(buf, response.getOutputStream());
		}
	}

	private static class UriServlet extends HttpServlet {

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.setContentType("text/plain");
			resp.setCharacterEncoding("UTF-8");
			resp.getWriter().write(req.getRequestURI());
		}
	}

	private static class MultipartServlet extends HttpServlet {

		@Override
		protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			assertTrue(ServletFileUpload.isMultipartContent(req));
			FileItemFactory factory = new DiskFileItemFactory();
			ServletFileUpload upload = new ServletFileUpload(factory);
			try {
				List items = upload.parseRequest(req);
				assertEquals(4, items.size());
				FileItem item = (FileItem) items.get(0);
				assertTrue(item.isFormField());
				assertEquals("name 1", item.getFieldName());
				assertEquals("value 1", item.getString());

				item = (FileItem) items.get(1);
				assertTrue(item.isFormField());
				assertEquals("name 2", item.getFieldName());
				assertEquals("value 2+1", item.getString());

				item = (FileItem) items.get(2);
				assertTrue(item.isFormField());
				assertEquals("name 2", item.getFieldName());
				assertEquals("value 2+2", item.getString());

				item = (FileItem) items.get(3);
				assertFalse(item.isFormField());
				assertEquals("logo", item.getFieldName());
				assertEquals("logo.jpg", item.getName());
				assertEquals("image/jpeg", item.getContentType());
			}
			catch (FileUploadException ex) {
				throw new ServletException(ex);
			}

		}
	}

}
