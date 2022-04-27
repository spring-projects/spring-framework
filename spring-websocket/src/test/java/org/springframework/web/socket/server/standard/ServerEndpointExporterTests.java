/*
 * Copyright 2002-2019 the original author or authors.
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

import javax.servlet.ServletContext;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test fixture for {@link ServerEndpointExporter}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class ServerEndpointExporterTests {

	private ServerContainer serverContainer;

	private ServletContext servletContext;

	private AnnotationConfigWebApplicationContext webAppContext;

	private ServerEndpointExporter exporter;


	@BeforeEach
	public void setup() {
		this.serverContainer = mock(ServerContainer.class);

		this.servletContext = new MockServletContext();
		this.servletContext.setAttribute("javax.websocket.server.ServerContainer", this.serverContainer);

		this.webAppContext = new AnnotationConfigWebApplicationContext();
		this.webAppContext.register(Config.class);
		this.webAppContext.setServletContext(this.servletContext);
		this.webAppContext.refresh();

		this.exporter = new ServerEndpointExporter();
	}


	@Test
	public void addAnnotatedEndpointClasses() throws Exception {
		this.exporter.setAnnotatedEndpointClasses(AnnotatedDummyEndpoint.class);
		this.exporter.setApplicationContext(this.webAppContext);
		this.exporter.afterPropertiesSet();
		this.exporter.afterSingletonsInstantiated();

		verify(this.serverContainer).addEndpoint(AnnotatedDummyEndpoint.class);
		verify(this.serverContainer).addEndpoint(AnnotatedDummyEndpointBean.class);
	}

	@Test
	public void addAnnotatedEndpointClassesWithServletContextOnly() throws Exception {
		this.exporter.setAnnotatedEndpointClasses(AnnotatedDummyEndpoint.class, AnnotatedDummyEndpointBean.class);
		this.exporter.setServletContext(this.servletContext);
		this.exporter.afterPropertiesSet();
		this.exporter.afterSingletonsInstantiated();

		verify(this.serverContainer).addEndpoint(AnnotatedDummyEndpoint.class);
		verify(this.serverContainer).addEndpoint(AnnotatedDummyEndpointBean.class);
	}

	@Test
	public void addAnnotatedEndpointClassesWithExplicitServerContainerOnly() throws Exception {
		this.exporter.setAnnotatedEndpointClasses(AnnotatedDummyEndpoint.class, AnnotatedDummyEndpointBean.class);
		this.exporter.setServerContainer(this.serverContainer);
		this.exporter.afterPropertiesSet();
		this.exporter.afterSingletonsInstantiated();

		verify(this.serverContainer).addEndpoint(AnnotatedDummyEndpoint.class);
		verify(this.serverContainer).addEndpoint(AnnotatedDummyEndpointBean.class);
	}

	@Test
	public void addServerEndpointConfigBean() throws Exception {
		ServerEndpointRegistration endpointRegistration = new ServerEndpointRegistration("/dummy", new DummyEndpoint());
		this.webAppContext.getBeanFactory().registerSingleton("dummyEndpoint", endpointRegistration);

		this.exporter.setApplicationContext(this.webAppContext);
		this.exporter.afterPropertiesSet();
		this.exporter.afterSingletonsInstantiated();

		verify(this.serverContainer).addEndpoint(endpointRegistration);
	}

	@Test
	public void addServerEndpointConfigBeanWithExplicitServletContext() throws Exception {
		ServerEndpointRegistration endpointRegistration = new ServerEndpointRegistration("/dummy", new DummyEndpoint());
		this.webAppContext.getBeanFactory().registerSingleton("dummyEndpoint", endpointRegistration);

		this.exporter.setServletContext(this.servletContext);
		this.exporter.setApplicationContext(this.webAppContext);
		this.exporter.afterPropertiesSet();
		this.exporter.afterSingletonsInstantiated();

		verify(this.serverContainer).addEndpoint(endpointRegistration);
	}

	@Test
	public void addServerEndpointConfigBeanWithExplicitServerContainer() throws Exception {
		ServerEndpointRegistration endpointRegistration = new ServerEndpointRegistration("/dummy", new DummyEndpoint());
		this.webAppContext.getBeanFactory().registerSingleton("dummyEndpoint", endpointRegistration);
		this.servletContext.removeAttribute("javax.websocket.server.ServerContainer");

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
		public AnnotatedDummyEndpointBean annotatedEndpoint1() {
			return new AnnotatedDummyEndpointBean();
		}
	}

}
