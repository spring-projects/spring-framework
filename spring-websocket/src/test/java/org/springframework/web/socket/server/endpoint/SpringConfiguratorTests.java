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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.junit.Assert.*;

public class SpringConfiguratorTests {

	private MockServletContext servletContext;

	private ContextLoader contextLoader;

	private AnnotationConfigWebApplicationContext webAppContext;

	private SpringConfigurator configurator;


	@Before
	public void setup() {
		this.servletContext = new MockServletContext();

		this.webAppContext = new AnnotationConfigWebApplicationContext();
		this.webAppContext.register(Config.class);

		this.contextLoader = new ContextLoader(this.webAppContext);
		this.contextLoader.initWebApplicationContext(this.servletContext);

		this.configurator = new SpringConfigurator();
	}

	@After
	public void destroy() {
		this.contextLoader.closeWebApplicationContext(this.servletContext);
	}


	@Test
	public void getEndpointInstancePerConnection() throws Exception {
		PerConnectionEchoEndpoint endpoint = this.configurator.getEndpointInstance(PerConnectionEchoEndpoint.class);
		assertNotNull(endpoint);
	}

	@Test
	public void getEndpointInstanceSingletonByType() throws Exception {
		EchoEndpoint expected = this.webAppContext.getBean(EchoEndpoint.class);
		EchoEndpoint actual = this.configurator.getEndpointInstance(EchoEndpoint.class);
		assertSame(expected, actual);
	}

	@Test
	public void getEndpointInstanceSingletonByComponentName() throws Exception {
		AlternativeEchoEndpoint expected = this.webAppContext.getBean(AlternativeEchoEndpoint.class);
		AlternativeEchoEndpoint actual = this.configurator.getEndpointInstance(AlternativeEchoEndpoint.class);
		assertSame(expected, actual);
	}


	@Configuration
	@ComponentScan(basePackageClasses=SpringConfiguratorTests.class)
	static class Config {

		@Bean
		public EchoEndpoint echoEndpoint() {
			return new EchoEndpoint(echoService());
		}

		@Bean
		public EchoService echoService() {
			return new EchoService();
		}
	}

	private static class EchoEndpoint extends Endpoint {

		@SuppressWarnings("unused")
		private final EchoService service;

		@Autowired
		public EchoEndpoint(EchoService service) {
			this.service = service;
		}

		@Override
		public void onOpen(Session session, EndpointConfig config) {
		}
	}

	@Component("echoEndpoint")
	private static class AlternativeEchoEndpoint extends Endpoint {

		@SuppressWarnings("unused")
		private final EchoService service;

		@Autowired
		public AlternativeEchoEndpoint(EchoService service) {
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
