/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.SocketUtils;
import org.springframework.util.StreamUtils;

import static org.junit.Assert.*;

public abstract class AbstractHttpRequestFactoryTestCase {

	protected ClientHttpRequestFactory factory;

	protected static String baseUrl;

	private static Server jettyServer;

	@BeforeClass
	public static void startJettyServer() throws Exception {
		int port = SocketUtils.findAvailableTcpPort();
		jettyServer = new Server(port);
		baseUrl = "http://localhost:" + port;

		ServletContextHandler handler = new ServletContextHandler();
		handler.setContextPath("/");

		handler.addServlet(new ServletHolder(new EchoServlet()), "/echo");
		handler.addServlet(new ServletHolder(new EchoServlet()), "/echo");
		handler.addServlet(new ServletHolder(new StatusServlet(200)), "/status/ok");
		handler.addServlet(new ServletHolder(new StatusServlet(404)), "/status/notfound");
		handler.addServlet(new ServletHolder(new MethodServlet("DELETE")), "/methods/delete");
		handler.addServlet(new ServletHolder(new MethodServlet("GET")), "/methods/get");
		handler.addServlet(new ServletHolder(new MethodServlet("HEAD")), "/methods/head");
		handler.addServlet(new ServletHolder(new MethodServlet("OPTIONS")), "/methods/options");
		handler.addServlet(new ServletHolder(new PostServlet()), "/methods/post");
		handler.addServlet(new ServletHolder(new MethodServlet("PUT")), "/methods/put");
		handler.addServlet(new ServletHolder(new MethodServlet("PATCH")), "/methods/patch");

		jettyServer.setHandler(handler);
		jettyServer.start();
	}

	@Before
	public final void createFactory() {
		factory = createRequestFactory();
	}

	protected abstract ClientHttpRequestFactory createRequestFactory();

	@AfterClass
	public static void stopJettyServer() throws Exception {
		if (jettyServer != null) {
			jettyServer.stop();
		}
	}

	@Test
	public void status() throws Exception {
		URI uri = new URI(baseUrl + "/status/notfound");
		ClientHttpRequest request = factory.createRequest(uri, HttpMethod.GET);
		assertEquals("Invalid HTTP method", HttpMethod.GET, request.getMethod());
		assertEquals("Invalid HTTP URI", uri, request.getURI());
		ClientHttpResponse response = request.execute();
		assertEquals("Invalid status code", HttpStatus.NOT_FOUND, response.getStatusCode());
	}

	@Test
	public void echo() throws Exception {
		ClientHttpRequest request = factory.createRequest(new URI(baseUrl + "/echo"), HttpMethod.PUT);
		assertEquals("Invalid HTTP method", HttpMethod.PUT, request.getMethod());
		String headerName = "MyHeader";
		String headerValue1 = "value1";
		request.getHeaders().add(headerName, headerValue1);
		String headerValue2 = "value2";
		request.getHeaders().add(headerName, headerValue2);
		final byte[] body = "Hello World".getBytes("UTF-8");
		request.getHeaders().setContentLength(body.length);
		if (request instanceof StreamingHttpOutputMessage) {
			StreamingHttpOutputMessage streamingRequest =
					(StreamingHttpOutputMessage) request;
			streamingRequest.setBody(new StreamingHttpOutputMessage.Body() {
				@Override
				public void writeTo(OutputStream outputStream) throws IOException {
					StreamUtils.copy(body, outputStream);
				}
			});
		}
		else {
			StreamUtils.copy(body, request.getBody());
		}
		ClientHttpResponse response = request.execute();
		try {
			assertEquals("Invalid status code", HttpStatus.OK, response.getStatusCode());
			assertTrue("Header not found", response.getHeaders().containsKey(headerName));
			assertEquals("Header value not found", Arrays.asList(headerValue1, headerValue2),
					response.getHeaders().get(headerName));
			byte[] result = FileCopyUtils.copyToByteArray(response.getBody());
			assertTrue("Invalid body", Arrays.equals(body, result));
		}
		finally {
			response.close();
		}
	}

	@Test(expected = IllegalStateException.class)
	public void multipleWrites() throws Exception {
		ClientHttpRequest request = factory.createRequest(new URI(baseUrl + "/echo"), HttpMethod.POST);
		final byte[] body = "Hello World".getBytes("UTF-8");
		if (request instanceof StreamingHttpOutputMessage) {
			StreamingHttpOutputMessage streamingRequest =
					(StreamingHttpOutputMessage) request;
			streamingRequest.setBody(new StreamingHttpOutputMessage.Body() {
				@Override
				public void writeTo(OutputStream outputStream) throws IOException {
					StreamUtils.copy(body, outputStream);
				}
			});
		}
		else {
			StreamUtils.copy(body, request.getBody());
		}

		ClientHttpResponse response = request.execute();
		try {
			FileCopyUtils.copy(body, request.getBody());
		}
		finally {
			response.close();
		}
	}

	@Test(expected = UnsupportedOperationException.class)
	public void headersAfterExecute() throws Exception {
		ClientHttpRequest request = factory.createRequest(new URI(baseUrl + "/echo"), HttpMethod.POST);
		request.getHeaders().add("MyHeader", "value");
		byte[] body = "Hello World".getBytes("UTF-8");
		FileCopyUtils.copy(body, request.getBody());
		ClientHttpResponse response = request.execute();
		try {
			request.getHeaders().add("MyHeader", "value");
		}
		finally {
			response.close();
		}
	}

	@Test
	public void httpMethods() throws Exception {
		assertHttpMethod("get", HttpMethod.GET);
		assertHttpMethod("head", HttpMethod.HEAD);
		assertHttpMethod("post", HttpMethod.POST);
		assertHttpMethod("put", HttpMethod.PUT);
		assertHttpMethod("options", HttpMethod.OPTIONS);
		assertHttpMethod("delete", HttpMethod.DELETE);
	}

	protected void assertHttpMethod(String path, HttpMethod method) throws Exception {
		ClientHttpResponse response = null;
		try {
			ClientHttpRequest request = factory.createRequest(new URI(baseUrl + "/methods/" + path), method);
			response = request.execute();
			assertEquals("Invalid response status", HttpStatus.OK, response.getStatusCode());
			assertEquals("Invalid method", path.toUpperCase(Locale.ENGLISH), request.getMethod().name());
		}
		finally {
			if (response != null) {
				response.close();
			}
		}
	}

	/**
	 * Servlet that sets a given status code.
	 */
	@SuppressWarnings("serial")
	private static class StatusServlet extends GenericServlet {

		private final int sc;

		private StatusServlet(int sc) {
			this.sc = sc;
		}

		@Override
		public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
			((HttpServletResponse) response).setStatus(sc);
		}
	}

	@SuppressWarnings("serial")
	private static class MethodServlet extends GenericServlet {

		private final String method;

		private MethodServlet(String method) {
			this.method = method;
		}

		@Override
		public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
			HttpServletRequest httpReq = (HttpServletRequest) req;
			assertEquals("Invalid HTTP method", method, httpReq.getMethod());
			res.setContentLength(0);
			((HttpServletResponse) res).setStatus(200);
		}
	}

	@SuppressWarnings("serial")
	private static class PostServlet extends MethodServlet {

		private PostServlet() {
			super("POST");
		}

		@Override
		public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
			super.service(req, res);
			long contentLength = req.getContentLength();
			if (contentLength != -1) {
				InputStream in = req.getInputStream();
				long byteCount = 0;
				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = in.read(buffer)) != -1) {
					byteCount += bytesRead;
				}
				assertEquals("Invalid content-length", contentLength, byteCount);
			}
		}
	}

	@SuppressWarnings("serial")
	private static class EchoServlet extends HttpServlet {

		@Override
		protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			echo(req, resp);
		}

		private void echo(HttpServletRequest request, HttpServletResponse response) throws IOException {
			response.setStatus(HttpServletResponse.SC_OK);
			for (Enumeration e1 = request.getHeaderNames(); e1.hasMoreElements();) {
				String headerName = (String) e1.nextElement();
				for (Enumeration e2 = request.getHeaders(headerName); e2.hasMoreElements();) {
					String headerValue = (String) e2.nextElement();
					response.addHeader(headerName, headerValue);
				}
			}
			FileCopyUtils.copy(request.getInputStream(), response.getOutputStream());
		}
	}

}
