/*
 * Copyright 2002-2009 the original author or authors.
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.Set;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.CommonsClientHttpRequestFactory;
import org.springframework.util.FileCopyUtils;

/** @author Arjen Poutsma */
public class RestTemplateIntegrationTests {

	private RestTemplate template;

	private static Server jettyServer;

	private static String helloWorld = "H\u00e9llo W\u00f6rld";

	private static final String URI = "http://localhost:8889";

	@BeforeClass
	public static void startJettyServer() throws Exception {
		jettyServer = new Server(8889);
		Context jettyContext = new Context(jettyServer, "/");
		byte[] bytes = helloWorld.getBytes("UTF-8");
		String contentType = "text/plain;charset=utf-8";
		jettyContext.addServlet(new ServletHolder(new GetServlet(bytes, contentType)), "/get");
		jettyContext.addServlet(new ServletHolder(new GetServlet(new byte[0], contentType)), "/get/nothing");
		jettyContext.addServlet(
				new ServletHolder(new PostServlet(helloWorld, URI + "/post/1", bytes, contentType)),
				"/post");
		jettyContext.addServlet(new ServletHolder(new ErrorServlet(404)), "/errors/notfound");
		jettyContext.addServlet(new ServletHolder(new ErrorServlet(500)), "/errors/server");
		jettyContext.addServlet(new ServletHolder(new UriServlet()), "/uri/*");
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
	public void postForObject() throws URISyntaxException {
		String s = template.postForObject(URI + "/{method}", helloWorld, String.class, "post");
		assertEquals("Invalid content", helloWorld, s);
	}

	@Test(expected = HttpClientErrorException.class)
	public void notFound() {
		template.execute(URI + "/errors/notfound", HttpMethod.GET, null, null);
	}

	@Test(expected = HttpServerErrorException.class)
	public void serverError() {
		template.execute(URI + "/errors/server", HttpMethod.GET, null, null);
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
		assertEquals("Invalid request URI", "/uri/Z%FCrich", result);
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

		private final String contentType;

		private GetServlet(byte[] buf, String contentType) {
			this.buf = buf;
			this.contentType = contentType;
		}

		@Override
		protected void doGet(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {
			response.setContentType(contentType);
			response.setContentLength(buf.length);
			FileCopyUtils.copy(buf, response.getOutputStream());
		}
	}

	private static class PostServlet extends HttpServlet {

		private final String s;

		private final String location;

		private final byte[] buf;

		private final String contentType;

		private PostServlet(String s, String location, byte[] buf, String contentType) {
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
			response.setContentType(contentType);
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

}
