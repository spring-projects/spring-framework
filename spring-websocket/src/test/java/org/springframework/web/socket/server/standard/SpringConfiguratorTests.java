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

package org.springframework.web.socket.server.standard;

import javax.websocket.server.ServerEndpoint;

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
	public void getEndpointPerConnection() throws Exception {
		PerConnectionEchoEndpoint endpoint = this.configurator.getEndpointInstance(PerConnectionEchoEndpoint.class);
		assertNotNull(endpoint);
	}

	@Test
	public void getEndpointSingletonByType() throws Exception {
		EchoEndpoint expected = this.webAppContext.getBean(EchoEndpoint.class);
		EchoEndpoint actual = this.configurator.getEndpointInstance(EchoEndpoint.class);
		assertSame(expected, actual);
	}

	@Test
	public void getEndpointSingletonByComponentName() throws Exception {
		ComponentEchoEndpoint expected = this.webAppContext.getBean(ComponentEchoEndpoint.class);
		ComponentEchoEndpoint actual = this.configurator.getEndpointInstance(ComponentEchoEndpoint.class);
		assertSame(expected, actual);
	}


	@Configuration
	@ComponentScan(basePackageClasses=SpringConfiguratorTests.class)
	static class Config {

		@Bean
		public EchoEndpoint javaConfigEndpoint() {
			return new EchoEndpoint(echoService());
		}

		@Bean
		public EchoService echoService() {
			return new EchoService();
		}
	}

	@ServerEndpoint("/echo")
	private static class EchoEndpoint {

		@SuppressWarnings("unused")
		private final EchoService service;

		@Autowired
		public EchoEndpoint(EchoService service) {
			this.service = service;
		}
	}

	@Component("myComponentEchoEndpoint")
	@ServerEndpoint("/echo")
	private static class ComponentEchoEndpoint {

		@SuppressWarnings("unused")
		private final EchoService service;

		@Autowired
		public ComponentEchoEndpoint(EchoService service) {
			this.service = service;
		}
	}

	@ServerEndpoint("/echo")
	private static class PerConnectionEchoEndpoint {

		@SuppressWarnings("unused")
		private final EchoService service;

		@Autowired
		public PerConnectionEchoEndpoint(EchoService service) {
			this.service = service;
		}
	}

	private static class EchoService {	}

}
