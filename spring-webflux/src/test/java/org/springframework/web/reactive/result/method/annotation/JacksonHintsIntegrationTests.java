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

package org.springframework.web.reactive.result.method.annotation;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastien Deleuze
 */
class JacksonHintsIntegrationTests extends AbstractRequestMappingIntegrationTests {

	@Override
	protected ApplicationContext initApplicationContext() {
		AnnotationConfigApplicationContext wac = new AnnotationConfigApplicationContext();
		wac.register(WebConfig.class);
		wac.refresh();
		return wac;
	}


	@ParameterizedHttpServerTest
	void jsonViewResponse(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		String expected = "{\"withView1\":\"with\"}";
		assertThat(performGet("/response/raw", MediaType.APPLICATION_JSON, String.class).getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	void jsonViewWithMonoResponse(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		String expected = "{\"withView1\":\"with\"}";
		assertThat(performGet("/response/mono", MediaType.APPLICATION_JSON, String.class).getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest  // SPR-16098
	void jsonViewWithMonoResponseEntity(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		String expected = "{\"withView1\":\"with\"}";
		assertThat(performGet("/response/entity", MediaType.APPLICATION_JSON, String.class).getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	void jsonViewWithFluxResponse(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		String expected = "[{\"withView1\":\"with\"},{\"withView1\":\"with\"}]";
		assertThat(performGet("/response/flux", MediaType.APPLICATION_JSON, String.class).getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	void jsonViewWithRequest(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		String expected = "{\"withView1\":\"with\",\"withView2\":null,\"withoutView\":null}";
		assertThat(performPost("/request/raw", MediaType.APPLICATION_JSON,
				new JacksonViewBean("with", "with", "without"), MediaType.APPLICATION_JSON, String.class).getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	void jsonViewWithMonoRequest(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		String expected = "{\"withView1\":\"with\",\"withView2\":null,\"withoutView\":null}";
		assertThat(performPost("/request/mono", MediaType.APPLICATION_JSON,
				new JacksonViewBean("with", "with", "without"), MediaType.APPLICATION_JSON, String.class).getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest  // SPR-16098
	void jsonViewWithEntityMonoRequest(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		String expected = "{\"withView1\":\"with\",\"withView2\":null,\"withoutView\":null}";
		assertThat(performPost("/request/entity/mono", MediaType.APPLICATION_JSON,
				new JacksonViewBean("with", "with", "without"),
				MediaType.APPLICATION_JSON, String.class).getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest  // SPR-16098
	void jsonViewWithEntityFluxRequest(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		String expected = "[" +
				"{\"withView1\":\"with\",\"withView2\":null,\"withoutView\":null}," +
				"{\"withView1\":\"with\",\"withView2\":null,\"withoutView\":null}]";
		assertThat(performPost("/request/entity/flux", MediaType.APPLICATION_JSON,
				Arrays.asList(new JacksonViewBean("with", "with", "without"),
						new JacksonViewBean("with", "with", "without")),
				MediaType.APPLICATION_JSON, String.class).getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	void jsonViewWithFluxRequest(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		String expected = "[" +
				"{\"withView1\":\"with\",\"withView2\":null,\"withoutView\":null}," +
				"{\"withView1\":\"with\",\"withView2\":null,\"withoutView\":null}]";
		List<JacksonViewBean> beans = Arrays.asList(
				new JacksonViewBean("with", "with", "without"),
				new JacksonViewBean("with", "with", "without"));
		assertThat(performPost("/request/flux", MediaType.APPLICATION_JSON, beans,
				MediaType.APPLICATION_JSON, String.class).getBody()).isEqualTo(expected);
	}


	@Configuration
	@ComponentScan(resourcePattern = "**/JacksonHintsIntegrationTests*.class")
	@EnableWebFlux
	@SuppressWarnings({"unused", "WeakerAccess"})
	static class WebConfig {
	}


	@RestController
	@SuppressWarnings("unused")
	private static class JsonViewRestController {

		@GetMapping("/response/raw")
		@JsonView(MyJacksonView1.class)
		public JacksonViewBean rawResponse() {
			return new JacksonViewBean("with", "with", "without");
		}

		@GetMapping("/response/mono")
		@JsonView(MyJacksonView1.class)
		public Mono<JacksonViewBean> monoResponse() {
			return Mono.just(new JacksonViewBean("with", "with", "without"));
		}

		@GetMapping("/response/entity")
		@JsonView(MyJacksonView1.class)
		public Mono<ResponseEntity<JacksonViewBean>> monoResponseEntity() {
			return Mono.just(ResponseEntity.ok(new JacksonViewBean("with", "with", "without")));
		}

		@GetMapping("/response/flux")
		@JsonView(MyJacksonView1.class)
		public Flux<JacksonViewBean> fluxResponse() {
			return Flux.just(new JacksonViewBean("with", "with", "without"), new JacksonViewBean("with", "with", "without"));
		}

		@PostMapping("/request/raw")
		public JacksonViewBean rawRequest(@JsonView(MyJacksonView1.class) @RequestBody JacksonViewBean bean) {
			return bean;
		}

		@PostMapping("/request/mono")
		public Mono<JacksonViewBean> monoRequest(@JsonView(MyJacksonView1.class) @RequestBody Mono<JacksonViewBean> mono) {
			return mono;
		}

		@PostMapping("/request/entity/mono")
		public Mono<JacksonViewBean> entityMonoRequest(@JsonView(MyJacksonView1.class) HttpEntity<Mono<JacksonViewBean>> entityMono) {
			return entityMono.getBody();
		}

		@PostMapping("/request/entity/flux")
		public Flux<JacksonViewBean> entityFluxRequest(@JsonView(MyJacksonView1.class) HttpEntity<Flux<JacksonViewBean>> entityFlux) {
			return entityFlux.getBody();
		}

		@PostMapping("/request/flux")
		public Flux<JacksonViewBean> fluxRequest(@JsonView(MyJacksonView1.class) @RequestBody Flux<JacksonViewBean> flux) {
			return flux;
		}

	}

	private interface MyJacksonView1 {}

	private interface MyJacksonView2 {}


	@SuppressWarnings("unused")
	private static class JacksonViewBean {

		@JsonView(MyJacksonView1.class)
		private String withView1;

		@JsonView(MyJacksonView2.class)
		private String withView2;

		private String withoutView;


		public JacksonViewBean() {
		}

		public JacksonViewBean(String withView1, String withView2, String withoutView) {
			this.withView1 = withView1;
			this.withView2 = withView2;
			this.withoutView = withoutView;
		}

		public String getWithView1() {
			return withView1;
		}

		public void setWithView1(String withView1) {
			this.withView1 = withView1;
		}

		public String getWithView2() {
			return withView2;
		}

		public void setWithView2(String withView2) {
			this.withView2 = withView2;
		}

		public String getWithoutView() {
			return withoutView;
		}

		public void setWithoutView(String withoutView) {
			this.withoutView = withoutView;
		}
	}

}
