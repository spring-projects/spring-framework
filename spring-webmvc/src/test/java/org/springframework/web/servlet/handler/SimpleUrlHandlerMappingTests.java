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

package org.springframework.web.servlet.handler;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockServletContext;
import org.springframework.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class SimpleUrlHandlerMappingTests {

	@Test
	@SuppressWarnings("resource")
	public void handlerBeanNotFound() {
		MockServletContext sc = new MockServletContext("");
		XmlWebApplicationContext root = new XmlWebApplicationContext();
		root.setServletContext(sc);
		root.setConfigLocations("/org/springframework/web/servlet/handler/map1.xml");
		root.refresh();
		XmlWebApplicationContext wac = new XmlWebApplicationContext();
		wac.setParent(root);
		wac.setServletContext(sc);
		wac.setNamespace("map2err");
		wac.setConfigLocations("/org/springframework/web/servlet/handler/map2err.xml");
		assertThatExceptionOfType(FatalBeanException.class).isThrownBy(
				wac::refresh)
			.withCauseInstanceOf(NoSuchBeanDefinitionException.class)
			.satisfies(ex -> assertThat(((NoSuchBeanDefinitionException) ex.getCause()).getBeanName()).isEqualTo("mainControlle"));
	}

	@Test
	public void urlMappingWithUrlMap() throws Exception {
		checkMappings("urlMapping");
	}

	@Test
	public void urlMappingWithProps() throws Exception {
		checkMappings("urlMappingWithProps");
	}

	@Test
	public void testNewlineInRequest() throws Exception {
		Object controller = new Object();
		SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping(
			Collections.singletonMap("/*/baz", controller));
		handlerMapping.setUrlDecode(false);
		handlerMapping.setApplicationContext(new StaticApplicationContext());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo%0a%0dbar/baz");

		HandlerExecutionChain hec = handlerMapping.getHandler(request);
		assertThat(hec).isNotNull();
		assertThat(hec.getHandler()).isSameAs(controller);
	}

	@SuppressWarnings("resource")
	private void checkMappings(String beanName) throws Exception {
		MockServletContext sc = new MockServletContext("");
		XmlWebApplicationContext wac = new XmlWebApplicationContext();
		wac.setServletContext(sc);
		wac.setConfigLocations("/org/springframework/web/servlet/handler/map2.xml");
		wac.refresh();
		Object bean = wac.getBean("mainController");
		Object otherBean = wac.getBean("otherController");
		Object defaultBean = wac.getBean("starController");
		HandlerMapping hm = (HandlerMapping) wac.getBean(beanName);

		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/welcome.html");
		HandlerExecutionChain hec = getHandler(hm, req);
		assertThat(hec != null && hec.getHandler() == bean).as("Handler is correct bean").isTrue();
		assertThat(req.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).isEqualTo("/welcome.html");
		assertThat(req.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE)).isEqualTo(bean);

		req = new MockHttpServletRequest("GET", "/welcome.x");
		hec = getHandler(hm, req);
		assertThat(hec != null && hec.getHandler() == otherBean).as("Handler is correct bean").isTrue();
		assertThat(req.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).isEqualTo("welcome.x");
		assertThat(req.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE)).isEqualTo(otherBean);

		req = new MockHttpServletRequest("GET", "/welcome/");
		hec = getHandler(hm, req);
		assertThat(hec != null && hec.getHandler() == otherBean).as("Handler is correct bean").isTrue();
		assertThat(req.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).isEqualTo("welcome");

		req = new MockHttpServletRequest("GET", "/");
		req.setServletPath("/welcome.html");
		hec = getHandler(hm, req);
		assertThat(hec != null && hec.getHandler() == bean).as("Handler is correct bean").isTrue();

		req = new MockHttpServletRequest("GET", "/welcome.html");
		req.setContextPath("/app");
		hec = getHandler(hm, req);
		assertThat(hec != null && hec.getHandler() == bean).as("Handler is correct bean").isTrue();

		req = new MockHttpServletRequest("GET", "/show.html");
		hec = getHandler(hm, req);
		assertThat(hec != null && hec.getHandler() == bean).as("Handler is correct bean").isTrue();

		req = new MockHttpServletRequest("GET", "/bookseats.html");
		hec = getHandler(hm, req);
		assertThat(hec != null && hec.getHandler() == bean).as("Handler is correct bean").isTrue();

		req = new MockHttpServletRequest("GET", "/original-welcome.html");
		req.setAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE, "/welcome.html");
		hec = getHandler(hm, req);
		assertThat(hec != null && hec.getHandler() == bean).as("Handler is correct bean").isTrue();

		req = new MockHttpServletRequest("GET", "/original-show.html");
		req.setAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE, "/show.html");
		hec = getHandler(hm, req);
		assertThat(hec != null && hec.getHandler() == bean).as("Handler is correct bean").isTrue();

		req = new MockHttpServletRequest("GET", "/original-bookseats.html");
		req.setAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE, "/bookseats.html");
		hec = getHandler(hm, req);
		assertThat(hec != null && hec.getHandler() == bean).as("Handler is correct bean").isTrue();

		req = new MockHttpServletRequest("GET", "/");
		hec = getHandler(hm, req);
		assertThat(hec != null && hec.getHandler() == bean).as("Handler is correct bean").isTrue();
		assertThat(req.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).isEqualTo("/");

		req = new MockHttpServletRequest("GET", "/somePath");
		hec = getHandler(hm, req);
		assertThat(hec != null && hec.getHandler() == defaultBean).as("Handler is correct bean").isTrue();
		assertThat(req.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).isEqualTo("/somePath");
	}

	private HandlerExecutionChain getHandler(HandlerMapping hm, MockHttpServletRequest req) throws Exception {
		HandlerExecutionChain hec = hm.getHandler(req);
		HandlerInterceptor[] interceptors = hec.getInterceptors();
		if (interceptors != null) {
			for (HandlerInterceptor interceptor : interceptors) {
				interceptor.preHandle(req, null, hec.getHandler());
			}
		}
		return hec;
	}

}
