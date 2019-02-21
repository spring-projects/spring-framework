/*
 * Copyright 2002-2018 the original author or authors.
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

import org.junit.Test;
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

import static org.junit.Assert.*;

/**
 * {@code @RequestMapping} integration focusing on controller method parameters.
 * Also see:
 * <ul>
 * <li>{@link RequestMappingDataBindingIntegrationTests}
 * <li>{@link RequestMappingMessageConversionIntegrationTests}
 * </ul>
 * @author Rossen Stoyanchev
 */
public class ControllerInputIntegrationTests extends AbstractRequestMappingIntegrationTests {

	@Override
	protected ApplicationContext initApplicationContext() {
		AnnotationConfigApplicationContext wac = new AnnotationConfigApplicationContext();
		wac.register(WebConfig.class, TestRestController.class);
		wac.refresh();
		return wac;
	}


	@Test
	public void handleWithParam() throws Exception {
		String expected = "Hello George!";
		assertEquals(expected, performGet("/param?name=George", new HttpHeaders(), String.class).getBody());
	}

	@Test  // SPR-15140
	public void handleWithEncodedParam() throws Exception {
		String expected = "Hello  + \u00e0!";
		assertEquals(expected, performGet("/param?name=%20%2B+%C3%A0", new HttpHeaders(), String.class).getBody());
	}

	@Test
	public void matrixVariable() throws Exception {
		String expected = "p=11, q2=22, q4=44";
		String url = "/first;p=11/second;q=22/third-fourth;q=44";
		assertEquals(expected, performGet(url, new HttpHeaders(), String.class).getBody());
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
