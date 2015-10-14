/*
 * Copyright 2002-2015 the original author or authors.
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
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import org.springframework.util.StreamUtils;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
public abstract class AbstractJettyServerTestCase {

	private static Server jettyServer;

	protected static String baseUrl;

	@BeforeClass
	public static void startJettyServer() throws Exception {

		// Let server pick its own random, available port.
		jettyServer = new Server(0);

		ServletContextHandler handler = new ServletContextHandler();
		handler.setContextPath("/");

		handler.addServlet(new ServletHolder(new EchoServlet()), "/echo");
		handler.addServlet(new ServletHolder(new ParameterServlet()), "/params");
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

		Connector[] connectors = jettyServer.getConnectors();
		NetworkConnector connector = (NetworkConnector) connectors[0];
		baseUrl = "http://localhost:" + connector.getLocalPort();
	}

	@AfterClass
	public static void stopJettyServer() throws Exception {
		if (jettyServer != null) {
			jettyServer.stop();
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
		public void service(ServletRequest request, ServletResponse response) throws
				ServletException, IOException {
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
			response.setContentType(request.getContentType());
			response.setContentLength(request.getContentLength());
			for (Enumeration<String> e1 = request.getHeaderNames(); e1.hasMoreElements();) {
				String headerName = e1.nextElement();
				for (Enumeration<String> e2 = request.getHeaders(headerName); e2.hasMoreElements();) {
					String headerValue = e2.nextElement();
					response.addHeader(headerName, headerValue);
				}
			}
			StreamUtils.copy(request.getInputStream(), response.getOutputStream());
		}
	}


	@SuppressWarnings("serial")
	private static class ParameterServlet extends HttpServlet {

		@Override
		protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			Map<String, String[]> parameters = req.getParameterMap();
			assertEquals(2, parameters.size());

			String[] values = parameters.get("param1");
			assertEquals(1, values.length);
			assertEquals("value", values[0]);

			values = parameters.get("param2");
			assertEquals(2, values.length);
			assertEquals("value1", values[0]);
			assertEquals("value2", values[1]);

			resp.setStatus(200);
			resp.setContentLength(0);
		}
	}

}
