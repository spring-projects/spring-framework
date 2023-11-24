/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.web.servlet.samples.client.standalone;

import java.security.Principal;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClientConfigurer;
import org.springframework.test.web.servlet.client.MockMvcHttpConnector;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcConfigurerAdapter;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.standalone.FrameworkExtensionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class FrameworkExtensionTests {

	private final WebTestClient client =
			MockMvcWebTestClient.bindToController(new SampleController())
					.apply(defaultSetup())
					.build();


	@Test
	public void fooHeader() {
		this.client.mutateWith(headers().foo("a=b"))
				.get().uri("/")
				.exchange()
				.expectBody(String.class).isEqualTo("Foo");
	}

	@Test
	public void barHeader() {
		this.client.mutateWith(headers().bar("a=b"))
				.get().uri("/")
				.exchange()
				.expectBody(String.class).isEqualTo("Bar");
	}

	private static TestMockMvcConfigurer defaultSetup() {
		return new TestMockMvcConfigurer();
	}

	private static TestWebTestClientConfigurer headers() {
		return new TestWebTestClientConfigurer();
	}


	/**
	 * Test WebTestClientConfigurer that re-creates the MockMvcHttpConnector
	 * with a {@code TestRequestPostProcessor}.
	 */
	private static class TestWebTestClientConfigurer implements WebTestClientConfigurer {

		private final TestRequestPostProcessor requestPostProcessor = new TestRequestPostProcessor();

		public TestWebTestClientConfigurer foo(String value) {
			this.requestPostProcessor.foo(value);
			return this;
		}

		public TestWebTestClientConfigurer bar(String value) {
			this.requestPostProcessor.bar(value);
			return this;
		}

		@Override
		public void afterConfigurerAdded(
				WebTestClient.Builder builder, WebHttpHandlerBuilder httpHandlerBuilder,
				ClientHttpConnector connector) {

			if (connector instanceof MockMvcHttpConnector mockMvcConnector) {
				builder.clientConnector(mockMvcConnector.with(List.of(this.requestPostProcessor)));
			}
		}
	}


	/**
	 * Test {@code RequestPostProcessor} for custom headers.
	 */
	private static class TestRequestPostProcessor implements RequestPostProcessor {

		private final HttpHeaders headers = new HttpHeaders();


		public TestRequestPostProcessor foo(String value) {
			this.headers.add("Foo", value);
			return this;
		}

		public TestRequestPostProcessor bar(String value) {
			this.headers.add("Bar", value);
			return this;
		}

		@Override
		public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
			for (String headerName : this.headers.keySet()) {
				request.addHeader(headerName, this.headers.get(headerName));
			}
			return request;
		}
	}


	/**
	 * Test {@code MockMvcConfigurer}.
	 */
	private static class TestMockMvcConfigurer extends MockMvcConfigurerAdapter {

		@Override
		public void afterConfigurerAdded(ConfigurableMockMvcBuilder<?> builder) {
			builder.alwaysExpect(status().isOk());
		}

		@Override
		public RequestPostProcessor beforeMockMvcCreated(
				ConfigurableMockMvcBuilder<?> builder, WebApplicationContext context) {

			return request -> {
				request.setUserPrincipal(mock());
				return request;
			};
		}
	}


	@Controller
	@RequestMapping("/")
	private static class SampleController {

		@RequestMapping(headers = "Foo")
		@ResponseBody
		public String handleFoo(Principal principal) {
			Assert.notNull(principal, "Principal must not be null");
			return "Foo";
		}

		@RequestMapping(headers = "Bar")
		@ResponseBody
		public String handleBar(Principal principal) {
			Assert.notNull(principal, "Principal must not be null");
			return "Bar";
		}
	}

}
