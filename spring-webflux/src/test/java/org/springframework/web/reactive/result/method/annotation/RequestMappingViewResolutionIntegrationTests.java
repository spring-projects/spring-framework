/*
 * Copyright 2002-2022 the original author or authors.
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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Optional;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.ViewResolverRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @RequestMapping} integration tests with view resolution scenarios.
 *
 * @author Rossen Stoyanchev
 */
class RequestMappingViewResolutionIntegrationTests extends AbstractRequestMappingIntegrationTests {

	@Override
	protected ApplicationContext initApplicationContext() {
		return new AnnotationConfigApplicationContext(WebConfig.class);
	}


	@ParameterizedHttpServerTest
	void html(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		String expected = "<html><body>Hello: Jason!</body></html>";
		assertThat(performGet("/html?name=Jason", MediaType.TEXT_HTML, String.class).getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	void etagCheckWithNotModifiedResponse(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		URI uri = URI.create("http://localhost:" + this.port + "/html");
		RequestEntity<Void> request = RequestEntity.get(uri).ifNoneMatch("\"deadb33f8badf00d\"").build();
		ResponseEntity<String> response = getRestTemplate().exchange(request, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
		assertThat(response.getBody()).isNull();
	}

	@ParameterizedHttpServerTest  // SPR-15291
	void redirect(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
			@Override
			protected void prepareConnection(HttpURLConnection conn, String method) throws IOException {
				super.prepareConnection(conn, method);
				conn.setInstanceFollowRedirects(false);
			}
		};

		URI uri = URI.create("http://localhost:" + this.port + "/redirect");
		RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaType.ALL).build();
		@SuppressWarnings("resource")
		ResponseEntity<Void> response = new RestTemplate(factory).exchange(request, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SEE_OTHER);
		assertThat(response.getHeaders().getLocation().toString()).isEqualTo("/");
	}


	@Configuration
	@EnableWebFlux
	@ComponentScan(resourcePattern = "**/RequestMappingViewResolutionIntegrationTests$*.class")
	@SuppressWarnings({"unused", "WeakerAccess"})
	static class WebConfig implements WebFluxConfigurer {

		@Override
		public void configureViewResolvers(ViewResolverRegistry registry) {
			registry.freeMarker();
		}

		@Bean
		public FreeMarkerConfigurer freeMarkerConfig() {
			FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
			configurer.setPreferFileSystemAccess(false);
			configurer.setTemplateLoaderPath("classpath*:org/springframework/web/reactive/view/freemarker/");
			return configurer;
		}
	}


	@Controller
	@SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
	private static class TestController {

		@GetMapping("/html")
		public String getHtmlPage(Optional<String> name, Model model, ServerWebExchange exchange) {
			if (exchange.checkNotModified("deadb33f8badf00d")) {
				return null;
			}
			model.addAttribute("hello", "Hello: " + name.orElse("<no name>") + "!");
			return "test";
		}

		@GetMapping("/redirect")
		public String redirect() {
			return "redirect:/";
		}
	}

}
