/*
 * Copyright 2002-2020 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.stereotype.Controller;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcConfigurerAdapter;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;

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
		this.client.get().uri("/")
				.header("Foo", "a=b")
				.exchange()
				.expectBody(String.class).isEqualTo("Foo");
	}

	@Test
	public void barHeader() {
		this.client.get().uri("/")
				.header("Bar", "a=b")
				.exchange()
				.expectBody(String.class).isEqualTo("Bar");
	}

	private static TestMockMvcConfigurer defaultSetup() {
		return new TestMockMvcConfigurer();
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
		public RequestPostProcessor beforeMockMvcCreated(ConfigurableMockMvcBuilder<?> builder,
				WebApplicationContext context) {
			return request -> {
				request.setUserPrincipal(mock(Principal.class));
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
