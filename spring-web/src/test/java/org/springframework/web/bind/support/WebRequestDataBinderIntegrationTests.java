/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.bind.support;

import java.util.List;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Brian Clozel
 * @author Sam Brannen
 */
@TestInstance(Lifecycle.PER_CLASS)
class WebRequestDataBinderIntegrationTests {

	private final PartsServlet partsServlet = new PartsServlet();

	private final PartListServlet partListServlet = new PartListServlet();

	private final RestTemplate template = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

	private Server jettyServer;

	private String baseUrl;


	@BeforeAll
	void startJettyServer() throws Exception {
		// Let server pick its own random, available port.
		jettyServer = new Server(0);

		ServletContextHandler handler = new ServletContextHandler();

		MultipartConfigElement multipartConfig = new MultipartConfigElement("");

		ServletHolder holder = new ServletHolder(partsServlet);
		holder.getRegistration().setMultipartConfig(multipartConfig);
		handler.addServlet(holder, "/parts");

		holder = new ServletHolder(partListServlet);
		holder.getRegistration().setMultipartConfig(multipartConfig);
		handler.addServlet(holder, "/partlist");

		jettyServer.setHandler(handler);
		jettyServer.start();

		Connector[] connectors = jettyServer.getConnectors();
		NetworkConnector connector = (NetworkConnector) connectors[0];
		baseUrl = "http://localhost:" + connector.getLocalPort();
	}

	@AfterAll
	void stopJettyServer() throws Exception {
		if (jettyServer != null) {
			jettyServer.stop();
		}
	}


	@Test
	void partsBinding() {
		PartsBean bean = new PartsBean();
		partsServlet.setBean(bean);

		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
		Resource firstPart = new ClassPathResource("/org/springframework/http/converter/logo.jpg");
		parts.add("firstPart", firstPart);
		parts.add("secondPart", "secondValue");

		template.postForLocation(baseUrl + "/parts", parts);

		assertThat(bean.getFirstPart()).isNotNull();
		assertThat(bean.getSecondPart()).isNotNull();
	}

	@Test
	void partListBinding() {
		PartListBean bean = new PartListBean();
		partListServlet.setBean(bean);

		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
		parts.add("partList", "first value");
		parts.add("partList", "second value");
		Resource logo = new ClassPathResource("/org/springframework/http/converter/logo.jpg");
		parts.add("partList", logo);

		template.postForLocation(baseUrl + "/partlist", parts);

		assertThat(bean.getPartList()).isNotNull();
		assertThat(bean.getPartList().size()).isEqualTo(parts.get("partList").size());
	}


	@SuppressWarnings("serial")
	private abstract static class AbstractStandardMultipartServlet<T> extends HttpServlet {

		private T bean;

		@Override
		public void service(HttpServletRequest request, HttpServletResponse response) {
			WebRequestDataBinder binder = new WebRequestDataBinder(bean);
			ServletWebRequest webRequest = new ServletWebRequest(request, response);
			binder.bind(webRequest);
			response.setStatus(HttpServletResponse.SC_OK);
		}

		void setBean(T bean) {
			this.bean = bean;
		}
	}


	private static class PartsBean {

		private Part firstPart;

		private Part secondPart;

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

		private List<Part> partList;

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
