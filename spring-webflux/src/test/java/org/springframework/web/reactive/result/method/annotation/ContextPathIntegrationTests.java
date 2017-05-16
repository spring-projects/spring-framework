/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.web.reactive.result.method.annotation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.bootstrap.ReactorHttpServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.config.EnableWebFlux;

import static org.junit.Assert.*;

/**
 * Integration tests that demonstrate running multiple applications under
 * different context paths.
 *
 * @author Rossen Stoyanchev
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ContextPathIntegrationTests {

	private ReactorHttpServer server;


	@Before
	public void setup() throws Exception {
		AnnotationConfigApplicationContext context1 = new AnnotationConfigApplicationContext();
		context1.register(WebApp1Config.class);
		context1.refresh();

		AnnotationConfigApplicationContext context2 = new AnnotationConfigApplicationContext();
		context2.register(WebApp2Config.class);
		context2.refresh();

		HttpHandler webApp1Handler = DispatcherHandler.toHttpHandler(context1);
		HttpHandler webApp2Handler = DispatcherHandler.toHttpHandler(context2);

		this.server = new ReactorHttpServer();

		this.server.registerHttpHandler("/webApp1", webApp1Handler);
		this.server.registerHttpHandler("/webApp2", webApp2Handler);

		this.server.afterPropertiesSet();
		this.server.start();
	}

	@After
	public void shutdown() throws Exception {
		this.server.stop();
	}


	@Test
	public void basic() throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		String actual;

		actual = restTemplate.getForObject(createUrl("/webApp1/test"), String.class);
		assertEquals("Tested in /webApp1", actual);

		actual = restTemplate.getForObject(createUrl("/webApp2/test"), String.class);
		assertEquals("Tested in /webApp2", actual);
	}

	private String createUrl(String path) {
		return "http://localhost:" + this.server.getPort() + path;
	}


	@EnableWebFlux
	@Configuration
	static class WebApp1Config {

		@Bean
		public TestController testController() {
			return new TestController();
		}
	}


	@EnableWebFlux
	@Configuration
	static class WebApp2Config {

		@Bean
		public TestController testController() {
			return new TestController();
		}
	}


	@RestController
	static class TestController {

		@GetMapping("/test")
		public String handle(ServerHttpRequest request) {
			return "Tested in " + request.getContextPath();
		}
	}

}
