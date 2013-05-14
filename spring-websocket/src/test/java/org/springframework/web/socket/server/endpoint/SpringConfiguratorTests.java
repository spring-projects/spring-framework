/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.server.endpoint;

import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.junit.Assert.*;


public class SpringConfiguratorTests {


	private MockServletContext servletContext;

	private ContextLoader contextLoader;

	private AnnotationConfigWebApplicationContext webAppContext;


	@Before
	public void setup() {
		this.servletContext = new MockServletContext();

		this.webAppContext = new AnnotationConfigWebApplicationContext();
		this.webAppContext.register(Config.class);

		this.contextLoader = new ContextLoader(webAppContext);
		this.contextLoader.initWebApplicationContext(this.servletContext);
	}

	@After
	public void destroy() {
		this.contextLoader.closeWebApplicationContext(this.servletContext);
	}


	@Test
	public void getEndpointInstanceCreateBean() throws Exception {

		PerConnectionEchoEndpoint endpoint = new SpringConfigurator().getEndpointInstance(PerConnectionEchoEndpoint.class);

		assertNotNull(endpoint);
	}

	@Test
	public void getEndpointInstanceUseBean() throws Exception {

		EchoEndpointBean expected = this.webAppContext.getBean(EchoEndpointBean.class);
		EchoEndpointBean actual = new SpringConfigurator().getEndpointInstance(EchoEndpointBean.class);

		assertSame(expected, actual);
	}


	@Configuration
	static class Config {

		@Bean
		public EchoEndpointBean echoEndpointBean() {
			return new EchoEndpointBean(echoService());
		}

		@Bean
		public EchoService echoService() {
			return new EchoService();
		}
	}

	private static class EchoEndpointBean extends Endpoint {

		@SuppressWarnings("unused")
		private final EchoService service;

		@Autowired
		public EchoEndpointBean(EchoService service) {
			this.service = service;
		}

		@Override
		public void onOpen(Session session, EndpointConfig config) {
		}
	}

	private static class PerConnectionEchoEndpoint extends Endpoint {

		@SuppressWarnings("unused")
		private final EchoService service;

		@Autowired
		public PerConnectionEchoEndpoint(EchoService service) {
			this.service = service;
		}

		@Override
		public void onOpen(Session session, EndpointConfig config) {
		}
	}

	private static class EchoService {	}

}
