/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.socket.server.standard;

import jakarta.servlet.ServletContext;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ServerEndpointExporter}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
class ServerEndpointExporterTests {

	private ServerContainer serverContainer = mock();

	private ServletContext servletContext = new MockServletContext();

	private ServerEndpointExporter exporter = new ServerEndpointExporter();

	private AnnotationConfigWebApplicationContext webAppContext;


	@BeforeEach
	void setup() {
		this.servletContext.setAttribute("jakarta.websocket.server.ServerContainer", this.serverContainer);

		this.webAppContext = new AnnotationConfigWebApplicationContext();
		this.webAppContext.register(Config.class);
		this.webAppContext.setServletContext(this.servletContext);
		this.webAppContext.refresh();
	}


	@Test
	void addAnnotatedEndpointClasses() throws Exception {
		this.exporter.setAnnotatedEndpointClasses(AnnotatedDummyEndpoint.class);
		this.exporter.setApplicationContext(this.webAppContext);
		this.exporter.afterPropertiesSet();
		this.exporter.afterSingletonsInstantiated();

		verify(this.serverContainer).addEndpoint(AnnotatedDummyEndpoint.class);
		verify(this.serverContainer).addEndpoint(AnnotatedDummyEndpointBean.class);
	}

	@Test
	void addAnnotatedEndpointClassesWithServletContextOnly() throws Exception {
		this.exporter.setAnnotatedEndpointClasses(AnnotatedDummyEndpoint.class, AnnotatedDummyEndpointBean.class);
		this.exporter.setServletContext(this.servletContext);
		this.exporter.afterPropertiesSet();
		this.exporter.afterSingletonsInstantiated();

		verify(this.serverContainer).addEndpoint(AnnotatedDummyEndpoint.class);
		verify(this.serverContainer).addEndpoint(AnnotatedDummyEndpointBean.class);
	}

	@Test
	void addAnnotatedEndpointClassesWithExplicitServerContainerOnly() throws Exception {
		this.exporter.setAnnotatedEndpointClasses(AnnotatedDummyEndpoint.class, AnnotatedDummyEndpointBean.class);
		this.exporter.setServerContainer(this.serverContainer);
		this.exporter.afterPropertiesSet();
		this.exporter.afterSingletonsInstantiated();

		verify(this.serverContainer).addEndpoint(AnnotatedDummyEndpoint.class);
		verify(this.serverContainer).addEndpoint(AnnotatedDummyEndpointBean.class);
	}

	@Test
	void addServerEndpointConfigBean() throws Exception {
		ServerEndpointRegistration endpointRegistration = new ServerEndpointRegistration("/dummy", new DummyEndpoint());
		this.webAppContext.getBeanFactory().registerSingleton("dummyEndpoint", endpointRegistration);

		this.exporter.setApplicationContext(this.webAppContext);
		this.exporter.afterPropertiesSet();
		this.exporter.afterSingletonsInstantiated();

		verify(this.serverContainer).addEndpoint(endpointRegistration);
	}

	@Test
	void addServerEndpointConfigBeanWithExplicitServletContext() throws Exception {
		ServerEndpointRegistration endpointRegistration = new ServerEndpointRegistration("/dummy", new DummyEndpoint());
		this.webAppContext.getBeanFactory().registerSingleton("dummyEndpoint", endpointRegistration);

		this.exporter.setServletContext(this.servletContext);
		this.exporter.setApplicationContext(this.webAppContext);
		this.exporter.afterPropertiesSet();
		this.exporter.afterSingletonsInstantiated();

		verify(this.serverContainer).addEndpoint(endpointRegistration);
	}

	@Test
	void addServerEndpointConfigBeanWithExplicitServerContainer() throws Exception {
		ServerEndpointRegistration endpointRegistration = new ServerEndpointRegistration("/dummy", new DummyEndpoint());
		this.webAppContext.getBeanFactory().registerSingleton("dummyEndpoint", endpointRegistration);
		this.servletContext.removeAttribute("jakarta.websocket.server.ServerContainer");

		this.exporter.setServerContainer(this.serverContainer);
		this.exporter.setApplicationContext(this.webAppContext);
		this.exporter.afterPropertiesSet();
		this.exporter.afterSingletonsInstantiated();

		verify(this.serverContainer).addEndpoint(endpointRegistration);
	}


	private static class DummyEndpoint extends Endpoint {

		@Override
		public void onOpen(Session session, EndpointConfig config) {
		}
	}


	@ServerEndpoint("/path")
	private static class AnnotatedDummyEndpoint {
	}


	@ServerEndpoint("/path")
	private static class AnnotatedDummyEndpointBean {
	}


	@Configuration
	static class Config {

		@Bean
		AnnotatedDummyEndpointBean annotatedEndpoint1() {
			return new AnnotatedDummyEndpointBean();
		}
	}

}
