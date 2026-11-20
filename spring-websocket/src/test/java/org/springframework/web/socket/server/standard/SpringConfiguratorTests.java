/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.server.standard;

import jakarta.websocket.server.ServerEndpoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;

class SpringConfiguratorTests {

	private MockServletContext servletContext;

	private ContextLoader contextLoader;

	private AnnotationConfigWebApplicationContext webAppContext;

	private SpringConfigurator configurator;


	@BeforeEach
	void setup() {
		this.servletContext = new MockServletContext();

		this.webAppContext = new AnnotationConfigWebApplicationContext();
		this.webAppContext.register(Config.class);

		this.contextLoader = new ContextLoader(this.webAppContext);
		this.contextLoader.initWebApplicationContext(this.servletContext);

		this.configurator = new SpringConfigurator();
	}

	@AfterEach
	void destroy() {
		this.contextLoader.closeWebApplicationContext(this.servletContext);
	}


	@Test
	void getEndpointPerConnection() throws Exception {
		PerConnectionEchoEndpoint endpoint = this.configurator.getEndpointInstance(PerConnectionEchoEndpoint.class);
		assertThat(endpoint).isNotNull();
	}

	@Test
	void getEndpointSingletonByType() throws Exception {
		EchoEndpoint expected = this.webAppContext.getBean(EchoEndpoint.class);
		EchoEndpoint actual = this.configurator.getEndpointInstance(EchoEndpoint.class);
		assertThat(actual).isSameAs(expected);
	}

	@Test
	void getEndpointSingletonByComponentName() throws Exception {
		ComponentEchoEndpoint expected = this.webAppContext.getBean(ComponentEchoEndpoint.class);
		ComponentEchoEndpoint actual = this.configurator.getEndpointInstance(ComponentEchoEndpoint.class);
		assertThat(actual).isSameAs(expected);
	}


	@Configuration
	@Import(ComponentEchoEndpoint.class)
	static class Config {

		@Bean
		EchoEndpoint javaConfigEndpoint() {
			return new EchoEndpoint(echoService());
		}

		@Bean
		EchoService echoService() {
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

	private static class EchoService { }

}
