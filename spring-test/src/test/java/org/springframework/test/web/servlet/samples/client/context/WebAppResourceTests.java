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

package org.springframework.test.web.servlet.samples.client.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.context.WebAppResourceTests}.
 *
 * @author Rossen Stoyanchev
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration("src/test/resources/META-INF/web-resources")
@ContextHierarchy({
	@ContextConfiguration("../../context/root-context.xml"),
	@ContextConfiguration("../../context/servlet-context.xml")
})
public class WebAppResourceTests {

	@Autowired
	private WebApplicationContext wac;

	private WebTestClient testClient;


	@BeforeEach
	public void setup() {
		this.testClient = MockMvcWebTestClient.bindToApplicationContext(this.wac).build();
	}

	// TilesConfigurer: resources under "/WEB-INF/**/tiles.xml"

	@Test
	public void tilesDefinitions() {
		testClient.get().uri("/")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals("Forwarded-Url", "/WEB-INF/layouts/standardLayout.jsp");
	}

	// Resources served via <mvc:resources/>

	@Test
	public void resourceRequest() {
		testClient.get().uri("/resources/Spring.js")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType("application/javascript")
				.expectBody(String.class).value(containsString("Spring={};"));
	}

	// Forwarded to the "default" servlet via <mvc:default-servlet-handler/>

	@Test
	public void resourcesViaDefaultServlet() throws Exception {
		EntityExchangeResult<Void> result = testClient.get().uri("/unknown/resource")
				.exchange()
				.expectStatus().isOk()
				.expectBody().isEmpty();

		MockMvcWebTestClient.resultActionsFor(result)
				.andExpect(handler().handlerType(DefaultServletHttpRequestHandler.class))
				.andExpect(forwardedUrl("default"));
	}

}
