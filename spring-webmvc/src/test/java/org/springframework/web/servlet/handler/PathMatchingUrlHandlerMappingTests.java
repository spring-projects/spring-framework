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

package org.springframework.web.servlet.handler;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockServletContext;
import org.springframework.web.util.ServletRequestPathUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alef Arendsen
 * @author Juergen Hoeller
 */
public class PathMatchingUrlHandlerMappingTests {

	@SuppressWarnings("unused")
	static Stream<?> pathPatternsArguments() {
		String location = "/org/springframework/web/servlet/handler/map3.xml";
		WebApplicationContext wac = initConfig(location);

		SimpleUrlHandlerMapping mapping1 = wac.getBean("urlMapping1", SimpleUrlHandlerMapping.class);
		assertThat(mapping1.getPathPatternHandlerMap()).isNotEmpty();

		SimpleUrlHandlerMapping mapping2 = wac.getBean("urlMapping2", SimpleUrlHandlerMapping.class);
		assertThat(mapping2.getPathPatternHandlerMap()).isEmpty();

		return Stream.of(Arguments.of(mapping1, wac), Arguments.of(mapping2, wac));
	}

	private static WebApplicationContext initConfig(String... configLocations) {
		MockServletContext sc = new MockServletContext("");
		ConfigurableWebApplicationContext context = new XmlWebApplicationContext();
		context.setServletContext(sc);
		context.setConfigLocations(configLocations);
		context.refresh();
		return context;
	}


	@PathPatternsParameterizedTest
	void requestsWithHandlers(HandlerMapping mapping, WebApplicationContext wac) throws Exception {
		Object bean = wac.getBean("mainController");

		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/welcome.html");
		HandlerExecutionChain hec = getHandler(mapping, wac, req);
		assertThat(hec.getHandler()).isSameAs(bean);

		req = new MockHttpServletRequest("GET", "/show.html");
		hec = getHandler(mapping, wac, req);
		assertThat(hec.getHandler()).isSameAs(bean);

		req = new MockHttpServletRequest("GET", "/bookseats.html");
		hec = getHandler(mapping, wac, req);
		assertThat(hec.getHandler()).isSameAs(bean);
	}

	@PathPatternsParameterizedTest
	void actualPathMatching(SimpleUrlHandlerMapping mapping, WebApplicationContext wac) throws Exception {
		// there a couple of mappings defined with which we can test the
		// path matching, let's do that...

		Object bean = wac.getBean("mainController");
		Object defaultBean = wac.getBean("starController");

		// testing some normal behavior
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/pathmatchingTest.html");
		HandlerExecutionChain chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(bean);
		assertThat(request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE))
				.isEqualTo("/pathmatchingTest.html");

		// no match, no forward slash included
		request = new MockHttpServletRequest("GET", "welcome.html");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(defaultBean);
		assertThat(request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE))
				.isEqualTo("welcome.html");

		// testing some ????? behavior
		request = new MockHttpServletRequest("GET", "/pathmatchingAA.html");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(bean);
		assertThat(request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE))
				.isEqualTo("pathmatchingAA.html");

		// testing some ????? behavior
		request = new MockHttpServletRequest("GET", "/pathmatchingA.html");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(defaultBean);
		assertThat(request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE))
				.isEqualTo("/pathmatchingA.html");

		// testing some ????? behavior
		request = new MockHttpServletRequest("GET", "/administrator/pathmatching.html");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(bean);

		// testing simple /**/behavior
		request = new MockHttpServletRequest("GET", "/administrator/test/pathmatching.html");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(bean);

		// this should not match because of the administratorT
		request = new MockHttpServletRequest("GET", "/administratort/pathmatching.html");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(defaultBean);

		// this should match because of *.jsp
		request = new MockHttpServletRequest("GET", "/bla.jsp");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(bean);

		// should match because exact pattern is there
		request = new MockHttpServletRequest("GET", "/administrator/another/bla.xml");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(bean);

		// should not match, because there's not .gif extension in there
		request = new MockHttpServletRequest("GET", "/administrator/another/bla.gif");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(defaultBean);

		// should match because there testlast* in there
		request = new MockHttpServletRequest("GET", "/administrator/test/testlastbit");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(bean);

		// but this not, because it's testlast and not testla
		request = new MockHttpServletRequest("GET", "/administrator/test/testla");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(defaultBean);

		if (mapping.getPatternParser() != null) {
			request = new MockHttpServletRequest("GET", "/administrator/testing/longer/bla");
			chain = getHandler(mapping, wac, request);
			assertThat(chain.getHandler()).isSameAs(bean);

			request = new MockHttpServletRequest("GET", "/administrator/testing/longer/test.jsp");
			chain = getHandler(mapping, wac, request);
			assertThat(chain.getHandler()).isSameAs(bean);
		}

		request = new MockHttpServletRequest("GET", "/administrator/testing/longer2/notmatching/notmatching");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(defaultBean);

		request = new MockHttpServletRequest("GET", "/shortpattern/testing/toolong");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(defaultBean);

		request = new MockHttpServletRequest("GET", "/XXpathXXmatching.html");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(bean);

		request = new MockHttpServletRequest("GET", "/pathXXmatching.html");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(bean);

		request = new MockHttpServletRequest("GET", "/XpathXXmatching.html");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(defaultBean);

		request = new MockHttpServletRequest("GET", "/XXpathmatching.html");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(defaultBean);

		request = new MockHttpServletRequest("GET", "/show12.html");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(bean);

		request = new MockHttpServletRequest("GET", "/show123.html");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(bean);

		request = new MockHttpServletRequest("GET", "/show1.html");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(bean);

		request = new MockHttpServletRequest("GET", "/reallyGood-test-is-this.jpeg");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(bean);

		request = new MockHttpServletRequest("GET", "/reallyGood-tst-is-this.jpeg");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(defaultBean);

		request = new MockHttpServletRequest("GET", "/testing/test.jpeg");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(bean);

		request = new MockHttpServletRequest("GET", "/testing/test.jpg");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(defaultBean);

		request = new MockHttpServletRequest("GET", "/anotherTest");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(bean);

		request = new MockHttpServletRequest("GET", "/stillAnotherTest");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(defaultBean);

		// there outofpattern*yeah in the pattern, so this should fail
		request = new MockHttpServletRequest("GET", "/outofpattern*ye");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(defaultBean);

		request = new MockHttpServletRequest("GET", "/test't%20est/path'm%20atching.html");
		chain = getHandler(mapping, wac, request);
		assertThat(chain.getHandler()).isSameAs(defaultBean);

		request = new MockHttpServletRequest("GET", "/test%26t%20est/path%26m%20atching.html");
		chain = getHandler(mapping, wac, request);
		if (!mapping.getPathPatternHandlerMap().isEmpty()) {
			assertThat(chain.getHandler())
					.as("PathPattern always matches to encoded paths.")
					.isSameAs(bean);
		}
		else {
			assertThat(chain.getHandler())
					.as("PathMatcher should not match encoded pattern with urlDecode=true")
					.isSameAs(defaultBean);
		}
	}

	@PathPatternsParameterizedTest
	void defaultMapping(HandlerMapping mapping, WebApplicationContext wac) throws Exception {
		Object bean = wac.getBean("starController");
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/goggog.html");
		HandlerExecutionChain hec = getHandler(mapping, wac, req);
		assertThat(hec.getHandler()).isSameAs(bean);
	}

	@PathPatternsParameterizedTest
	void mappingExposedInRequest(HandlerMapping mapping, WebApplicationContext wac) throws Exception {
		Object bean = wac.getBean("mainController");
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/show.html");
		HandlerExecutionChain hec = getHandler(mapping, wac, req);
		assertThat(hec.getHandler()).isSameAs(bean);
		assertThat(req.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE))
				.as("Mapping not exposed").isEqualTo("show.html");
	}

	private HandlerExecutionChain getHandler(
			HandlerMapping mapping, WebApplicationContext wac, MockHttpServletRequest request)
			throws Exception {

		// At runtime this is done by the DispatcherServlet
		if (((AbstractHandlerMapping) mapping).getPatternParser() != null) {
			ServletRequestPathUtils.parseAndCache(request);
		}

		HandlerExecutionChain chain = mapping.getHandler(request);
		for (HandlerInterceptor interceptor : chain.getInterceptorList()) {
			interceptor.preHandle(request, null, chain.getHandler());
		}
		return chain;
	}

}
