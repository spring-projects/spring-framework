/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.method.annotation;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @RequestMapping} integration focusing on controller method parameters.
 * Also see:
 * <ul>
 * <li>{@link RequestMappingDataBindingIntegrationTests}
 * <li>{@link RequestMappingMessageConversionIntegrationTests}
 * </ul>
 * @author Rossen Stoyanchev
 */
class ControllerInputIntegrationTests extends AbstractRequestMappingIntegrationTests {

	@Override
	protected ApplicationContext initApplicationContext() {
		return new AnnotationConfigApplicationContext(WebConfig.class, TestRestController.class);
	}


	@ParameterizedHttpServerTest
	void handleWithParam(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		String expected = "Hello George!";
		assertThat(performGet("/param?name=George", new HttpHeaders(), String.class).getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest  // SPR-15140
	void handleWithEncodedParam(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		String expected = "Hello  + \u00e0!";
		assertThat(performGet("/param?name=%20%2B+%C3%A0", new HttpHeaders(), String.class).getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	void matrixVariable(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		String expected = "p=11, q2=22, q4=44";
		String url = "/first;p=11/second;q=22/third-fourth;q=44";
		assertThat(performGet(url, new HttpHeaders(), String.class).getBody()).isEqualTo(expected);
	}


	@Configuration
	@EnableWebFlux
	static class WebConfig {
	}


	@RestController
	@SuppressWarnings("unused")
	private static class TestRestController {

		@GetMapping("/param")
		public Publisher<String> param(@RequestParam String name) {
			return Flux.just("Hello ", name, "!");
		}

		@GetMapping("/{one}/{two}/{three}-{four}")
		public String matrixVar(
				@MatrixVariable int p,
				@MatrixVariable(name = "q", pathVar = "two") int q2,
				@MatrixVariable(name = "q", pathVar = "four") int q4) {

			return "p=" + p + ", q2=" + q2 + ", q4=" + q4;
		}
	}

}
