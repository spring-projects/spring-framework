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

package org.springframework.web.servlet.handler;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
import static org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE;
import static org.springframework.web.servlet.HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE;

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
		assertThatExceptionOfType(FatalBeanException.class)
				.isThrownBy(wac::refresh)
				.withCauseInstanceOf(NoSuchBeanDefinitionException.class)
				.satisfies(ex -> {
					NoSuchBeanDefinitionException cause = (NoSuchBeanDefinitionException) ex.getCause();
					assertThat(cause.getBeanName()).isEqualTo("mainControlle");
				});
	}

	@Test
	public void testNewlineInRequest() throws Exception {
		Object controller = new Object();
		SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping(Collections.singletonMap("/*/baz", controller));
		mapping.setUrlDecode(false);
		mapping.setApplicationContext(new StaticApplicationContext());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo%0a%0dbar/baz");

		HandlerExecutionChain hec = mapping.getHandler(request);
		assertThat(hec).isNotNull();
		assertThat(hec.getHandler()).isSameAs(controller);
	}

	@ParameterizedTest
	@ValueSource(strings = {"urlMapping", "urlMappingWithProps", "urlMappingWithPathPatterns"})
	void checkMappings(String beanName) throws Exception {
		MockServletContext sc = new MockServletContext("");
		XmlWebApplicationContext wac = new XmlWebApplicationContext();
		wac.setServletContext(sc);
		wac.setConfigLocations("/org/springframework/web/servlet/handler/map2.xml");
		wac.refresh();
		Object bean = wac.getBean("mainController");
		Object otherBean = wac.getBean("otherController");
		Object defaultBean = wac.getBean("starController");
		HandlerMapping hm = (HandlerMapping) wac.getBean(beanName);
		wac.close();

		boolean usePathPatterns = (((AbstractHandlerMapping) hm).getPatternParser() != null);
		MockHttpServletRequest request = PathPatternsTestUtils.initRequest("GET", "/welcome.html", usePathPatterns);
		HandlerExecutionChain chain = getHandler(hm, request);
		assertThat(chain.getHandler()).isSameAs(bean);
		assertThat(request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).isEqualTo("/welcome.html");
		assertThat(request.getAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE)).isEqualTo(bean);

		request = PathPatternsTestUtils.initRequest("GET", "/welcome.x", usePathPatterns);
		chain = getHandler(hm, request);
		assertThat(chain.getHandler()).isSameAs(otherBean);
		assertThat(request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).isEqualTo("welcome.x");
		assertThat(request.getAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE)).isEqualTo(otherBean);

		request = PathPatternsTestUtils.initRequest("GET", "/welcome/", usePathPatterns);
		chain = getHandler(hm, request);
		assertThat(chain.getHandler()).isSameAs(otherBean);
		assertThat(request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).isEqualTo("welcome");

		request = PathPatternsTestUtils.initRequest("GET", "/", usePathPatterns);
		request.setServletPath("/welcome.html");
		chain = getHandler(hm, request);
		assertThat(chain.getHandler()).isSameAs(bean);

		request = PathPatternsTestUtils.initRequest("GET", "/app", "/welcome.html", usePathPatterns);
		chain = getHandler(hm, request);
		assertThat(chain.getHandler()).isSameAs(bean);

		request = PathPatternsTestUtils.initRequest("GET", "/show.html", usePathPatterns);
		chain = getHandler(hm, request);
		assertThat(chain.getHandler()).isSameAs(bean);

		request = PathPatternsTestUtils.initRequest("GET", "/bookseats.html", usePathPatterns);
		chain = getHandler(hm, request);
		assertThat(chain.getHandler()).isSameAs(bean);

		request = PathPatternsTestUtils.initRequest("GET", null, "/original-welcome.html", usePathPatterns,
				req -> req.setAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE, "/welcome.html"));
		chain = getHandler(hm, request);
		assertThat(chain.getHandler()).isSameAs(bean);

		request = PathPatternsTestUtils.initRequest("GET", null, "/original-show.html", usePathPatterns,
				req -> req.setAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE, "/show.html"));
		chain = getHandler(hm, request);
		assertThat(chain.getHandler()).isSameAs(bean);

		request = PathPatternsTestUtils.initRequest("GET", null, "/original-bookseats.html", usePathPatterns,
				req -> req.setAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE, "/bookseats.html"));
		chain = getHandler(hm, request);
		assertThat(chain.getHandler()).isSameAs(bean);

		request = PathPatternsTestUtils.initRequest("GET", "/", usePathPatterns);
		chain = getHandler(hm, request);
		assertThat(chain.getHandler()).isSameAs(bean);
		assertThat(request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).isEqualTo("/");

		request = PathPatternsTestUtils.initRequest("GET", "/somePath", usePathPatterns);
		chain = getHandler(hm, request);
		assertThat(chain.getHandler() == defaultBean).as("Handler is correct bean").isTrue();
		assertThat(request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).isEqualTo("/somePath");
	}

	private HandlerExecutionChain getHandler(HandlerMapping mapping, MockHttpServletRequest request) throws Exception {
		HandlerExecutionChain chain = mapping.getHandler(request);
		for (HandlerInterceptor interceptor : chain.getInterceptorList()) {
			interceptor.preHandle(request, null, chain.getHandler());
		}
		return chain;
	}

}
