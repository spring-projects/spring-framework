/*
 * Copyright 2002-2024 the original author or authors.
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
