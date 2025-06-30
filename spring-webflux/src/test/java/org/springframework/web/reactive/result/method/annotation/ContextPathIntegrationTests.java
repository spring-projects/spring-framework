/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.AbstractHttpServer;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.JettyCoreHttpServer;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.JettyHttpServer;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.ReactorHttpServer;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.TomcatHttpServer;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.UndertowHttpServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

/**
 * Integration tests related to the use of context paths.
 *
 * @author Rossen Stoyanchev
 */
class ContextPathIntegrationTests {

	static Stream<Arguments> httpServers() {
		return Stream.of(
				argumentSet("Jetty", new JettyHttpServer()),
				argumentSet("Jetty Core", new JettyCoreHttpServer()),
				argumentSet("Reactor Netty", new ReactorHttpServer()),
				argumentSet("Tomcat", new TomcatHttpServer()),
				argumentSet("Undertow", new UndertowHttpServer())
		);
	}

	@ParameterizedTest
	@MethodSource("httpServers")
	void multipleWebFluxApps(AbstractHttpServer server) throws Exception {
		AnnotationConfigApplicationContext context1 = new AnnotationConfigApplicationContext(WebAppConfig.class);
		AnnotationConfigApplicationContext context2 = new AnnotationConfigApplicationContext(WebAppConfig.class);

		HttpHandler webApp1Handler = WebHttpHandlerBuilder.applicationContext(context1).build();
		HttpHandler webApp2Handler = WebHttpHandlerBuilder.applicationContext(context2).build();

		server.registerHttpHandler("/webApp1", webApp1Handler);
		server.registerHttpHandler("/webApp2", webApp2Handler);
		server.afterPropertiesSet();
		server.start();

		try {
			RestTemplate restTemplate = new RestTemplate();
			String actual;

			String url = "http://localhost:" + server.getPort() + "/webApp1/test";
			actual = restTemplate.getForObject(url, String.class);
			assertThat(actual).isEqualTo("Tested in /webApp1");

			url = "http://localhost:" + server.getPort() + "/webApp2/test";
			actual = restTemplate.getForObject(url, String.class);
			assertThat(actual).isEqualTo("Tested in /webApp2");
		}
		finally {
			server.stop();
		}
	}

	@Test
	void servletPathMapping() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(WebAppConfig.class);

		TomcatHttpServer server = new TomcatHttpServer();
		server.setContextPath("/app");
		server.setServletMapping("/api/*");

		HttpHandler httpHandler = WebHttpHandlerBuilder.applicationContext(context).build();
		server.setHandler(httpHandler);

		server.afterPropertiesSet();
		server.start();

		try {
			String url = "http://localhost:" + server.getPort() + "/app/api/test";
			String actual = new RestTemplate().getForObject(url, String.class);
			assertThat(actual).isEqualTo("Tested in /app/api");
		}
		finally {
			server.stop();
		}
	}


	@EnableWebFlux
	@Configuration
	static class WebAppConfig {

		@Bean
		TestController testController() {
			return new TestController();
		}
	}


	@RestController
	static class TestController {

		@GetMapping("/test")
		String handle(ServerHttpRequest request) {
			return "Tested in " + request.getPath().contextPath().value();
		}
	}

}
