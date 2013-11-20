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

package org.springframework.web.bind.support;

import java.io.IOException;
import java.util.List;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.SocketUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.Assert.*;


/**
 * @author Brian Clozel
 */
public class WebRequestDataBinderIntegrationTests {

	protected static String baseUrl;

	protected static MediaType contentType;

	private static Server jettyServer;

	private RestTemplate template;

	private static PartsServlet partsServlet;

	private static PartListServlet partListServlet;


	@Before
	public void createTemplate() {
		template = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
	}

	@BeforeClass
	public static void startJettyServer() throws Exception {
		int port = SocketUtils.findAvailableTcpPort();
		jettyServer = new Server(port);
		baseUrl = "http://localhost:" + port;
		ServletContextHandler handler = new ServletContextHandler();

		partsServlet = new PartsServlet();
		partListServlet = new PartListServlet();

		MultipartConfigElement multipartConfig = new MultipartConfigElement("");

		ServletHolder holder = new ServletHolder(partsServlet);
		holder.getRegistration().setMultipartConfig(multipartConfig);
		handler.addServlet(holder, "/parts");

		holder = new ServletHolder(partListServlet);
		holder.getRegistration().setMultipartConfig(multipartConfig);
		handler.addServlet(holder, "/partlist");
		jettyServer.setHandler(handler);
		jettyServer.start();
	}

	@AfterClass
	public static void stopJettyServer() throws Exception {
		if (jettyServer != null) {
			jettyServer.stop();
		}
	}


	@Test
	public void testPartsBinding() {

		PartsBean bean = new PartsBean();
		partsServlet.setBean(bean);

		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<String, Object>();
		Resource firstPart = new ClassPathResource("/org/springframework/http/converter/logo.jpg");
		parts.add("firstPart", firstPart);
		parts.add("secondPart", "secondValue");

		template.postForLocation(baseUrl + "/parts", parts);

		assertNotNull(bean.getFirstPart());
		assertNotNull(bean.getSecondPart());
	}

	@Test
	public void testPartListBinding() {

		PartListBean bean = new PartListBean();
		partListServlet.setBean(bean);

		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<String, Object>();
		parts.add("partList", "first value");
		parts.add("partList", "second value");
		Resource logo = new ClassPathResource("/org/springframework/http/converter/logo.jpg");
		parts.add("partList", logo);

		template.postForLocation(baseUrl + "/partlist", parts);

		assertNotNull(bean.getPartList());
		assertEquals(parts.get("partList").size(), bean.getPartList().size());
	}


	@SuppressWarnings("serial")
	private abstract static class AbstractStandardMultipartServlet<T> extends HttpServlet {

		private T bean;

		@Override
		public void service(HttpServletRequest request, HttpServletResponse response) throws
				ServletException, IOException {

			WebRequestDataBinder binder = new WebRequestDataBinder(bean);
			ServletWebRequest webRequest = new ServletWebRequest(request, response);

			binder.bind(webRequest);

			response.setStatus(HttpServletResponse.SC_OK);
		}

		public void setBean(T bean) {
			this.bean = bean;
		}
	}

	private static class PartsBean {

		public Part firstPart;

		public Part secondPart;

		public Part getFirstPart() {
			return firstPart;
		}

		@SuppressWarnings("unused")
		public void setFirstPart(Part firstPart) {
			this.firstPart = firstPart;
		}

		public Part getSecondPart() {
			return secondPart;
		}

		@SuppressWarnings("unused")
		public void setSecondPart(Part secondPart) {
			this.secondPart = secondPart;
		}
	}

	@SuppressWarnings("serial")
	private static class PartsServlet extends AbstractStandardMultipartServlet<PartsBean> {
	}

	private static class PartListBean {

		public List<Part> partList;

		public List<Part> getPartList() {
			return partList;
		}

		@SuppressWarnings("unused")
		public void setPartList(List<Part> partList) {
			this.partList = partList;
		}
	}

	@SuppressWarnings("serial")
	private static class PartListServlet extends AbstractStandardMultipartServlet<PartListBean> {
	}

}
