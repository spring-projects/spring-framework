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

package org.springframework.web.client;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

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

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.SocketUtils;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class AbstractJettyServerTestCase {

	protected static String helloWorld = "H\u00e9llo W\u00f6rld";

	protected static String baseUrl;

	protected static MediaType contentType;

	private static Server jettyServer;

	@BeforeClass
	public static void startJettyServer() throws Exception {
		int port = SocketUtils.findAvailableTcpPort();
		jettyServer = new Server(port);
		baseUrl = "http://localhost:" + port;
		ServletContextHandler handler = new ServletContextHandler();
		byte[] bytes = helloWorld.getBytes("UTF-8");
		contentType = new MediaType("text", "plain", Collections
				.singletonMap("charset", "UTF-8"));
		handler.addServlet(new ServletHolder(new GetServlet(bytes, contentType)), "/get");
		handler.addServlet(new ServletHolder(new GetServlet(new byte[0], contentType)), "/get/nothing");
		handler.addServlet(new ServletHolder(new GetServlet(bytes, null)), "/get/nocontenttype");
		handler.addServlet(
				new ServletHolder(new PostServlet(helloWorld, baseUrl + "/post/1", bytes, contentType)),
				"/post");
		handler.addServlet(new ServletHolder(new StatusCodeServlet(204)), "/status/nocontent");
		handler.addServlet(new ServletHolder(new StatusCodeServlet(304)), "/status/notmodified");
		handler.addServlet(new ServletHolder(new ErrorServlet(404)), "/status/notfound");
		handler.addServlet(new ServletHolder(new ErrorServlet(500)), "/status/server");
		handler.addServlet(new ServletHolder(new UriServlet()), "/uri/*");
		handler.addServlet(new ServletHolder(new MultipartServlet()), "/multipart");
		handler.addServlet(new ServletHolder(new DeleteServlet()), "/delete");
		handler.addServlet(
				new ServletHolder(new PutServlet(helloWorld, bytes, contentType)),
				"/put");
		jettyServer.setHandler(handler);
		jettyServer.start();
	}

	@AfterClass
	public static void stopJettyServer() throws Exception {
		if (jettyServer != null) {
			jettyServer.stop();
		}
	}

	/** Servlet that sets the given status code. */
	@SuppressWarnings("serial")
	private static class StatusCodeServlet extends GenericServlet {

		private final int sc;

		private StatusCodeServlet(int sc) {
			this.sc = sc;
		}

		@Override
		public void service(ServletRequest request, ServletResponse response) throws
				ServletException, IOException {
			((HttpServletResponse) response).setStatus(sc);
		}
	}

	/** Servlet that returns an error message for a given status code. */
	@SuppressWarnings("serial")
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

	@SuppressWarnings("serial")
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
			if (contentType != null) {
				response.setContentType(contentType.toString());
			}
			response.setContentLength(buf.length);
			FileCopyUtils.copy(buf, response.getOutputStream());
		}
	}

	@SuppressWarnings("serial")
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

	@SuppressWarnings("serial")
	private static class PutServlet extends HttpServlet {

		private final String s;

		private PutServlet(String s, byte[] buf, MediaType contentType) {
			this.s = s;
		}

		@Override
		protected void doPut(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {
			assertTrue("Invalid request content-length", request.getContentLength() > 0);
			assertNotNull("No content-type", request.getContentType());
			String body = FileCopyUtils.copyToString(request.getReader());
			assertEquals("Invalid request body", s, body);
			response.setStatus(HttpServletResponse.SC_ACCEPTED);
		}
	}

	@SuppressWarnings("serial")
	private static class UriServlet extends HttpServlet {

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.setContentType("text/plain");
			resp.setCharacterEncoding("UTF-8");
			resp.getWriter().write(req.getRequestURI());
		}
	}

	@SuppressWarnings("serial")
	private static class MultipartServlet extends HttpServlet {

		@Override
		protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			assertTrue(ServletFileUpload.isMultipartContent(req));
			FileItemFactory factory = new DiskFileItemFactory();
			ServletFileUpload upload = new ServletFileUpload(factory);
			try {
				List<FileItem> items = upload.parseRequest(req);
				assertEquals(4, items.size());
				FileItem item = items.get(0);
				assertTrue(item.isFormField());
				assertEquals("name 1", item.getFieldName());
				assertEquals("value 1", item.getString());

				item = items.get(1);
				assertTrue(item.isFormField());
				assertEquals("name 2", item.getFieldName());
				assertEquals("value 2+1", item.getString());

				item = items.get(2);
				assertTrue(item.isFormField());
				assertEquals("name 2", item.getFieldName());
				assertEquals("value 2+2", item.getString());

				item = items.get(3);
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

	@SuppressWarnings("serial")
	private static class DeleteServlet extends HttpServlet {

		@Override
		protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {
			resp.setStatus(200);
		}

	}

}
