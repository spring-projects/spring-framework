/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.web.reactive.handler;

import java.net.URISyntaxException;

import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.springframework.web.reactive.HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE;

/**
 * Unit tests for {@link SimpleUrlHandlerMapping}.
 *
 * @author Rossen Stoyanchev
 */
public class SimpleUrlHandlerMappingTests {

	@Test
	public void handlerMappingJavaConfig() throws Exception {
		AnnotationConfigApplicationContext wac = new AnnotationConfigApplicationContext();
		wac.register(WebConfig.class);
		wac.refresh();

		HandlerMapping handlerMapping = (HandlerMapping) wac.getBean("handlerMapping");
		Object mainController = wac.getBean("mainController");
		Object otherController = wac.getBean("otherController");

		testUrl("/welcome.html", mainController, handlerMapping, "/welcome.html");
		testUrl("/welcome.x", otherController, handlerMapping, "welcome.x");
		testUrl("/welcome/", otherController, handlerMapping, "welcome");
		testUrl("/show.html", mainController, handlerMapping, "/show.html");
		testUrl("/bookseats.html", mainController, handlerMapping, "/bookseats.html");
	}

	@Test
	public void handlerMappingXmlConfig() throws Exception {
		ClassPathXmlApplicationContext wac = new ClassPathXmlApplicationContext("map.xml", getClass());
		wac.refresh();

		HandlerMapping handlerMapping = wac.getBean("mapping", HandlerMapping.class);
		Object mainController = wac.getBean("mainController");

		testUrl("/pathmatchingTest.html", mainController, handlerMapping, "pathmatchingTest.html");
		testUrl("welcome.html", null, handlerMapping, null);
		testUrl("/pathmatchingAA.html", mainController, handlerMapping, "pathmatchingAA.html");
		testUrl("/pathmatchingA.html", null, handlerMapping, null);
		testUrl("/administrator/pathmatching.html", mainController, handlerMapping, "pathmatching.html");
		testUrl("/administrator/test/pathmatching.html", mainController, handlerMapping, "test/pathmatching.html");
		testUrl("/administratort/pathmatching.html", null, handlerMapping, null);
		testUrl("/administrator/another/bla.xml", mainController, handlerMapping, "/administrator/another/bla.xml");
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
		testUrl("/show123.html", mainController, handlerMapping, "/show123.html");
		testUrl("/show1.html", mainController, handlerMapping, "show1.html");
		testUrl("/reallyGood-test-is-this.jpeg", mainController, handlerMapping, "reallyGood-test-is-this.jpeg");
		testUrl("/reallyGood-tst-is-this.jpeg", null, handlerMapping, null);
		testUrl("/testing/test.jpeg", mainController, handlerMapping, "testing/test.jpeg");
		testUrl("/testing/test.jpg", null, handlerMapping, null);
		testUrl("/anotherTest", mainController, handlerMapping, "anotherTest");
		testUrl("/stillAnotherTest", null, handlerMapping, null);
		testUrl("outofpattern*ye", null, handlerMapping, null);
		testUrl("/test%26t%20est/path%26m%20atching.html", null, handlerMapping, null);

	}

	private void testUrl(String url, Object bean, HandlerMapping handlerMapping, String pathWithinMapping)
			throws URISyntaxException {

		ServerWebExchange exchange = createExchange(url);
		Object actual = handlerMapping.getHandler(exchange).block();
		if (bean != null) {
			assertNotNull(actual);
			assertSame(bean, actual);
			//noinspection OptionalGetWithoutIsPresent
			assertEquals(pathWithinMapping, exchange.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).get());
		}
		else {
			assertNull(actual);
		}
	}

	private ServerWebExchange createExchange(String path) throws URISyntaxException {
		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, path);
		WebSessionManager sessionManager = new MockWebSessionManager();
		return new DefaultServerWebExchange(request, new MockServerHttpResponse(), sessionManager);
	}


	@Configuration
	static class WebConfig {
	
		@Bean @SuppressWarnings("unused")
		public SimpleUrlHandlerMapping handlerMapping() {
			SimpleUrlHandlerMapping hm = new SimpleUrlHandlerMapping();
			hm.setUseTrailingSlashMatch(true);
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
