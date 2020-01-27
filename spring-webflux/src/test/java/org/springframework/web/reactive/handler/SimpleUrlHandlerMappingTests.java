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

package org.springframework.web.reactive.handler;

import java.net.URI;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.reactive.HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE;

/**
 * Unit tests for {@link SimpleUrlHandlerMapping}.
 *
 * @author Rossen Stoyanchev
 */
public class SimpleUrlHandlerMappingTests {

	@Test
	@SuppressWarnings("resource")
	public void handlerMappingJavaConfig() throws Exception {
		AnnotationConfigApplicationContext wac = new AnnotationConfigApplicationContext();
		wac.register(WebConfig.class);
		wac.refresh();

		HandlerMapping handlerMapping = (HandlerMapping) wac.getBean("handlerMapping");
		Object mainController = wac.getBean("mainController");
		Object otherController = wac.getBean("otherController");

		testUrl("/welcome.html", mainController, handlerMapping, "");
		testUrl("/welcome.x", otherController, handlerMapping, "welcome.x");
		testUrl("/welcome/", otherController, handlerMapping, "welcome");
		testUrl("/show.html", mainController, handlerMapping, "");
		testUrl("/bookseats.html", mainController, handlerMapping, "");
	}

	@Test
	@SuppressWarnings("resource")
	public void handlerMappingXmlConfig() throws Exception {
		ClassPathXmlApplicationContext wac = new ClassPathXmlApplicationContext("map.xml", getClass());
		wac.refresh();

		HandlerMapping handlerMapping = wac.getBean("mapping", HandlerMapping.class);
		Object mainController = wac.getBean("mainController");

		testUrl("/pathmatchingTest.html", mainController, handlerMapping, "pathmatchingTest.html");
		testUrl("welcome.html", null, handlerMapping, null);
		testUrl("/pathmatchingAA.html", mainController, handlerMapping, "pathmatchingAA.html");
		testUrl("/pathmatchingA.html", null, handlerMapping, null);
		testUrl("/administrator/pathmatching.html", mainController, handlerMapping, "");
		testUrl("/administrator/test/pathmatching.html", mainController, handlerMapping, "test/pathmatching.html");
		testUrl("/administratort/pathmatching.html", null, handlerMapping, null);
		testUrl("/administrator/another/bla.xml", mainController, handlerMapping, "");
		testUrl("/administrator/another/bla.gif", null, handlerMapping, null);
		testUrl("/administrator/test/testlastbit", mainController, handlerMapping, "test/testlastbit");
		testUrl("/administrator/test/testla", null, handlerMapping, null);
		testUrl("/administrator/testing/longer/bla", mainController, handlerMapping, "bla");
		testUrl("/administrator/testing/longer2/notmatching/notmatching", null, handlerMapping, null);
		testUrl("/shortpattern/testing/toolong", null, handlerMapping, null);
		testUrl("/XXpathXXmatching.html", mainController, handlerMapping, "XXpathXXmatching.html");
		testUrl("/pathXXmatching.html", mainController, handlerMapping, "pathXXmatching.html");
		testUrl("/XpathXXmatching.html", null, handlerMapping, null);
		testUrl("/XXpathmatching.html", null, handlerMapping, null);
		testUrl("/show12.html", mainController, handlerMapping, "show12.html");
		testUrl("/show123.html", mainController, handlerMapping, "");
		testUrl("/show1.html", mainController, handlerMapping, "show1.html");
		testUrl("/reallyGood-test-is-this.jpeg", mainController, handlerMapping, "reallyGood-test-is-this.jpeg");
		testUrl("/reallyGood-tst-is-this.jpeg", null, handlerMapping, null);
		testUrl("/testing/test.jpeg", mainController, handlerMapping, "testing/test.jpeg");
		testUrl("/testing/test.jpg", null, handlerMapping, null);
		testUrl("/anotherTest", mainController, handlerMapping, "anotherTest");
		testUrl("/stillAnotherTest", null, handlerMapping, null);
		testUrl("outofpattern*ye", null, handlerMapping, null);
	}

	private void testUrl(String url, Object bean, HandlerMapping handlerMapping, String pathWithinMapping) {
		MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.GET, URI.create(url)).build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		Object actual = handlerMapping.getHandler(exchange).block();
		if (bean != null) {
			assertThat(actual).isNotNull();
			assertThat(actual).isSameAs(bean);
			//noinspection OptionalGetWithoutIsPresent
			PathContainer path = exchange.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
			assertThat(path).isNotNull();
			assertThat(path.value()).isEqualTo(pathWithinMapping);
		}
		else {
			assertThat(actual).isNull();
		}
	}


	@Configuration
	static class WebConfig {

		@Bean @SuppressWarnings("unused")
		public SimpleUrlHandlerMapping handlerMapping() {
			SimpleUrlHandlerMapping hm = new SimpleUrlHandlerMapping();
			hm.registerHandler("/welcome*", otherController());
			hm.registerHandler("/welcome.html", mainController());
			hm.registerHandler("/show.html", mainController());
			hm.registerHandler("/bookseats.html", mainController());
			return hm;
		}

		@Bean
		public Object mainController() {
			return new Object();
		}

		@Bean
		public Object otherController() {
			return new Object();
		}
	}

}
