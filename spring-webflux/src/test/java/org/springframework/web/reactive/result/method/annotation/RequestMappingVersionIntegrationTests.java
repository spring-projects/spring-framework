/*
 * Copyright 2002-2025 the original author or authors.
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

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.config.ApiVersionConfigurer;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code @RequestMapping} integration focusing on API versioning.
 * @author Rossen Stoyanchev
 */
public class RequestMappingVersionIntegrationTests extends AbstractRequestMappingIntegrationTests {

	@Override
	protected ApplicationContext initApplicationContext() {
		AnnotationConfigApplicationContext wac = new AnnotationConfigApplicationContext();
		wac.register(WebConfig.class, TestController.class);
		wac.refresh();
		return wac;
	}


	@ParameterizedHttpServerTest
	void initialVersion(HttpServer httpServer) throws Exception {
		startServer(httpServer);
		assertThat(exchangeWithVersion("1.0").getBody()).isEqualTo("none");
		assertThat(exchangeWithVersion("1.1").getBody()).isEqualTo("none");
	}

	@ParameterizedHttpServerTest
	void baselineVersion(HttpServer httpServer) throws Exception {
		startServer(httpServer);
		assertThat(exchangeWithVersion("1.2").getBody()).isEqualTo("1.2");
		assertThat(exchangeWithVersion("1.3").getBody()).isEqualTo("1.2");
	}

	@ParameterizedHttpServerTest
	void fixedVersion(HttpServer httpServer) throws Exception {
		startServer(httpServer);
		assertThat(exchangeWithVersion("1.5").getBody()).isEqualTo("1.5");
		assertThatThrownBy(() -> exchangeWithVersion("1.6")).isInstanceOf(HttpClientErrorException.BadRequest.class);
	}

	private ResponseEntity<String> exchangeWithVersion(String version) {
		String url = "http://localhost:" + this.port;
		RequestEntity<Void> requestEntity = RequestEntity.get(url).header("X-API-Version", version).build();
		return getRestTemplate().exchange(requestEntity, String.class);
	}


	@EnableWebFlux
	private static class WebConfig implements WebFluxConfigurer {

		@Override
		public void configureApiVersioning(ApiVersionConfigurer configurer) {
			configurer.useRequestHeader("X-API-Version").addSupportedVersions("1", "1.1", "1.3", "1.6");
		}
	}


	@RestController
	private static class TestController {

		@GetMapping
		String noVersion() {
			return getBody("none");
		}

		@GetMapping(version = "1.2+")
		String version1_2() {
			return getBody("1.2");
		}

		@GetMapping(version = "1.5")
		String version1_5() {
			return getBody("1.5");
		}

		private static String getBody(String version) {
			return version;
		}
	}

}
