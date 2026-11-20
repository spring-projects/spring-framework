/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
