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
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.mockito.Mockito.*;

/**
 * Test fixture for {@link ServerEndpointExporter}.
 *
 * @author Rossen Stoyanchev
 */
public class ServerEndpointExporterTests {

	private ServerContainer serverContainer;

	private ServerEndpointExporter exporter;

	private AnnotationConfigWebApplicationContext webAppContext;


	@Before
	public void setup() {
		this.serverContainer = mock(ServerContainer.class);

		MockServletContext servletContext = new MockServletContext();
		servletContext.setAttribute("javax.websocket.server.ServerContainer", this.serverContainer);

		this.webAppContext = new AnnotationConfigWebApplicationContext();
		this.webAppContext.register(Config.class);
		this.webAppContext.setServletContext(servletContext);
		this.webAppContext.refresh();

		this.exporter = new ServerEndpointExporter();
		this.exporter.setApplicationContext(this.webAppContext);
	}


	@Test
	public void addAnnotatedEndpointBean() throws Exception {

		this.exporter.setAnnotatedEndpointClasses(AnnotatedDummyEndpoint.class);
		this.exporter.afterPropertiesSet();

		verify(this.serverContainer).addEndpoint(AnnotatedDummyEndpoint.class);
		verify(this.serverContainer).addEndpoint(AnnotatedDummyEndpointBean.class);
	}

	@Test
	public void addServerEndpointConfigBean() throws Exception {

		ServerEndpointRegistration endpointRegistration = new ServerEndpointRegistration("/dummy", new DummyEndpoint());
		this.exporter.postProcessAfterInitialization(endpointRegistration, "dummyEndpoint");

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
