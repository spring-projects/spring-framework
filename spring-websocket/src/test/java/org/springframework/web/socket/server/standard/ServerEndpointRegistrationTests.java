/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.server.standard;

import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for {@link ServerEndpointRegistration}.
 *
 * @author Rossen Stoyanchev
 */
class ServerEndpointRegistrationTests {


	@Test
	void endpointPerConnection() throws Exception {

		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(Config.class);

		ServerEndpointRegistration registration = new ServerEndpointRegistration("/path", EchoEndpoint.class);
		registration.setBeanFactory(context.getBeanFactory());

		EchoEndpoint endpoint = registration.getConfigurator().getEndpointInstance(EchoEndpoint.class);

		assertThat(endpoint).isNotNull();
	}

	@Test
	void endpointSingleton() throws Exception {

		EchoEndpoint endpoint = new EchoEndpoint(new EchoService());
		ServerEndpointRegistration registration = new ServerEndpointRegistration("/path", endpoint);

		EchoEndpoint actual = registration.getConfigurator().getEndpointInstance(EchoEndpoint.class);

		assertThat(actual).isSameAs(endpoint);
	}


	@Configuration
	static class Config {

		@Bean
		EchoService echoService() {
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

	private static class EchoService { }

}
