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

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockServletContext;
import org.springframework.web.util.UrlPathHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class BeanNameUrlHandlerMappingTests {

	private ConfigurableWebApplicationContext wac;


	@BeforeEach
	public void setUp() throws Exception {
		MockServletContext sc = new MockServletContext("");
		wac = new XmlWebApplicationContext();
		wac.setServletContext(sc);
		wac.setConfigLocations("/org/springframework/web/servlet/handler/map1.xml");
		wac.refresh();
	}

	@Test
	public void requestsWithoutHandlers() throws Exception {
		HandlerMapping hm = (HandlerMapping) wac.getBean("handlerMapping");

		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/myapp/mypath/nonsense.html");
		req.setContextPath("/myapp");
		Object h = hm.getHandler(req);
		assertThat(h).as("Handler is null").isNull();

		req = new MockHttpServletRequest("GET", "/foo/bar/baz.html");
		h = hm.getHandler(req);
		assertThat(h).as("Handler is null").isNull();
	}

	@Test
	public void requestsWithSubPaths() throws Exception {
		HandlerMapping hm = (HandlerMapping) wac.getBean("handlerMapping");
		doTestRequestsWithSubPaths(hm);
	}

	@Test
	public void requestsWithSubPathsInParentContext() throws Exception {
		BeanNameUrlHandlerMapping hm = new BeanNameUrlHandlerMapping();
		hm.setDetectHandlersInAncestorContexts(true);
		hm.setApplicationContext(new StaticApplicationContext(wac));
		doTestRequestsWithSubPaths(hm);
	}

	private void doTestRequestsWithSubPaths(HandlerMapping hm) throws Exception {
		Object bean = wac.getBean("godCtrl");

		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mypath/welcome.html");
		HandlerExecutionChain hec = hm.getHandler(req);
		assertThat(hec != null && hec.getHandler() == bean).as("Handler is correct bean").isTrue();

		req = new MockHttpServletRequest("GET", "/myapp/mypath/welcome.html");
		req.setContextPath("/myapp");
		hec = hm.getHandler(req);
		assertThat(hec != null && hec.getHandler() == bean).as("Handler is correct bean").isTrue();

		req = new MockHttpServletRequest("GET", "/myapp/mypath/welcome.html");
		req.setContextPath("/myapp");
		req.setServletPath("/mypath/welcome.html");
		hec = hm.getHandler(req);
		assertThat(hec != null && hec.getHandler() == bean).as("Handler is correct bean").isTrue();

		req = new MockHttpServletRequest("GET", "/myapp/myservlet/mypath/welcome.html");
		req.setContextPath("/myapp");
		req.setServletPath("/myservlet");
		hec = hm.getHandler(req);
		assertThat(hec != null && hec.getHandler() == bean).as("Handler is correct bean").isTrue();

		req = new MockHttpServletRequest("GET", "/myapp/myapp/mypath/welcome.html");
		req.setContextPath("/myapp");
		req.setServletPath("/myapp");
		hec = hm.getHandler(req);
		assertThat(hec != null && hec.getHandler() == bean).as("Handler is correct bean").isTrue();

		req = new MockHttpServletRequest("GET", "/mypath/show.html");
		hec = hm.getHandler(req);
		assertThat(hec != null && hec.getHandler() == bean).as("Handler is correct bean").isTrue();

		req = new MockHttpServletRequest("GET", "/mypath/bookseats.html");
		hec = hm.getHandler(req);
		assertThat(hec != null && hec.getHandler() == bean).as("Handler is correct bean").isTrue();
	}

	@Test
	public void requestsWithFullPaths() throws Exception {

		UrlPathHelper pathHelper = new UrlPathHelper();
		pathHelper.setAlwaysUseFullPath(true);

		BeanNameUrlHandlerMapping hm = new BeanNameUrlHandlerMapping();
		hm.setPatternParser(null);  // the test targets AntPathPatcher-specific feature
		hm.setUrlPathHelper(pathHelper);
		hm.setApplicationContext(wac);
		Object bean = wac.getBean("godCtrl");

		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mypath/welcome.html");
		HandlerExecutionChain hec = hm.getHandler(req);
		assertThat(hec != null && hec.getHandler() == bean).as("Handler is correct bean").isTrue();

		req = new MockHttpServletRequest("GET", "/myapp/mypath/welcome.html");
		req.setContextPath("/myapp");
		hec = hm.getHandler(req);
		assertThat(hec != null && hec.getHandler() == bean).as("Handler is correct bean").isTrue();

		req = new MockHttpServletRequest("GET", "/mypath/welcome.html");
		req.setContextPath("");
		req.setServletPath("/mypath");
		hec = hm.getHandler(req);
		assertThat(hec != null && hec.getHandler() == bean).as("Handler is correct bean").isTrue();

		req = new MockHttpServletRequest("GET", "/Myapp/mypath/welcome.html");
		req.setContextPath("/myapp");
		req.setServletPath("/mypath");
		hec = hm.getHandler(req);
		assertThat(hec != null && hec.getHandler() == bean).as("Handler is correct bean").isTrue();

		req = new MockHttpServletRequest("GET", "/");
		hec = hm.getHandler(req);
		assertThat(hec != null && hec.getHandler() == bean).as("Handler is correct bean").isTrue();
	}

	@Test
	public void asteriskMatches() throws Exception {
		HandlerMapping hm = (HandlerMapping) wac.getBean("handlerMapping");
		Object bean = wac.getBean("godCtrl");

		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mypath/test.html");
		HandlerExecutionChain hec = hm.getHandler(req);
		assertThat(hec != null && hec.getHandler() == bean).as("Handler is correct bean").isTrue();

		req = new MockHttpServletRequest("GET", "/mypath/testarossa");
		hec = hm.getHandler(req);
		assertThat(hec != null && hec.getHandler() == bean).as("Handler is correct bean").isTrue();

		req = new MockHttpServletRequest("GET", "/mypath/tes");
		hec = hm.getHandler(req);
		assertThat(hec).as("Handler is correct bean").isNull();
	}

	@Test
	public void overlappingMappings() throws Exception {
		BeanNameUrlHandlerMapping hm = (BeanNameUrlHandlerMapping) wac.getBean("handlerMapping");
		Object anotherHandler = new Object();
		hm.registerHandler("/mypath/testaross*", anotherHandler);
		Object bean = wac.getBean("godCtrl");

		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mypath/test.html");
		HandlerExecutionChain hec = hm.getHandler(req);
		assertThat(hec != null && hec.getHandler() == bean).as("Handler is correct bean").isTrue();

		req = new MockHttpServletRequest("GET", "/mypath/testarossa");
		hec = hm.getHandler(req);
		assertThat(hec != null && hec.getHandler() == anotherHandler).as("Handler is correct bean").isTrue();

		req = new MockHttpServletRequest("GET", "/mypath/tes");
		hec = hm.getHandler(req);
		assertThat(hec).as("Handler is correct bean").isNull();
	}

	@Test
	public void doubleMappings() throws ServletException {
		BeanNameUrlHandlerMapping hm = (BeanNameUrlHandlerMapping) wac.getBean("handlerMapping");
		assertThatIllegalStateException().isThrownBy(() ->
				hm.registerHandler("/mypath/welcome.html", new Object()));
	}

}
