/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.context.support;

import java.io.IOException;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;

import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletConfig;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.0
 */
class HttpRequestHandlerTests {

	@Test
	void testHttpRequestHandlerServletPassThrough() throws Exception {
		MockServletContext servletContext = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.getBeanFactory().registerSingleton("myHandler", (HttpRequestHandler) (req, res) -> {
			assertThat(req).isSameAs(request);
			assertThat(res).isSameAs(response);
			String exception = request.getParameter("exception");
			if ("ServletException".equals(exception)) {
				throw new ServletException("test");
			}
			if ("IOException".equals(exception)) {
				throw new IOException("test");
			}
			res.getWriter().write("myResponse");
		});
		wac.setServletContext(servletContext);
		wac.refresh();
		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

		Servlet servlet = new HttpRequestHandlerServlet();
		servlet.init(new MockServletConfig(servletContext, "myHandler"));

		servlet.service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("myResponse");

		request.setParameter("exception", "ServletException");
		assertThatExceptionOfType(ServletException.class)
			.isThrownBy(() -> servlet.service(request, response))
			.withMessage("test");

		request.setParameter("exception", "IOException");
		assertThatIOException()
			.isThrownBy(() -> servlet.service(request, response))
			.withMessage("test");
	}

}
