/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.resource;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.test.MockFilterChain;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import static org.junit.Assert.*;


/**
 *
 * @author Rossen Stoyanchev
 */
public class ResourceUrlFilterTests {

	private MockFilterChain filterChain;

	private TestServlet servlet;


	@Test
	public void rootServletMapping() throws Exception {

		initFilterChain(WebConfig.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.setRequestURI("/myapp/index.html");
		request.setContextPath("/myapp");
		request.setServletPath("/index.html");
		this.filterChain.doFilter(request, new MockHttpServletResponse());

		String actual = this.servlet.response.encodeURL("/myapp/resources/foo.css");
		assertEquals("/myapp/resources/foo-e36d2e05253c6c7085a91522ce43a0b4.css", actual);
	}

	@Test
	public void prefixServletMapping() throws Exception {

		initFilterChain(WebConfig.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.setRequestURI("/myapp/myservlet/index.html");
		request.setContextPath("/myapp");
		request.setServletPath("/myservlet");
		this.filterChain.doFilter(request, new MockHttpServletResponse());

		String actual = this.servlet.response.encodeURL("/myapp/myservlet/resources/foo.css");
		assertEquals("/myapp/myservlet/resources/foo-e36d2e05253c6c7085a91522ce43a0b4.css", actual);
	}

	@Test
	public void extensionServletMapping() throws Exception {

		initFilterChain(WebConfig.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.setRequestURI("/myapp/index.html");
		request.setContextPath("/myapp");
		request.setServletPath("/index.html");
		this.filterChain.doFilter(request, new MockHttpServletResponse());

		String actual = this.servlet.response.encodeURL("/myapp/resources/foo.css");
		assertEquals("/myapp/resources/foo-e36d2e05253c6c7085a91522ce43a0b4.css", actual);
	}

	private void initFilterChain(Class<?> configClass) throws ServletException {

		MockServletContext servletContext = new MockServletContext();

		AnnotationConfigWebApplicationContext cxt = new AnnotationConfigWebApplicationContext();
		cxt.setServletContext(servletContext);
		cxt.register(configClass);
		cxt.refresh();

		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, cxt);

		ResourceUrlFilter filter = new ResourceUrlFilter();
		filter.setServletContext(servletContext);
		filter.initFilterBean();

		this.servlet = new TestServlet();
		this.filterChain = new MockFilterChain(servlet, filter);
	}


	@Configuration
	static class WebConfig extends WebMvcConfigurationSupport {


		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {

			List<ResourceResolver> resourceResolvers = new ArrayList<>();
			resourceResolvers.add(new FingerprintResourceResolver());
			resourceResolvers.add(new PathResourceResolver());

			registry.addResourceHandler("/resources/**")
				.addResourceLocations("classpath:org/springframework/web/servlet/resource/test/")
				.setResourceResolvers(resourceResolvers);
		}
	}

	private static class TestServlet extends HttpServlet {

		private HttpServletResponse response;

		@Override
		protected void doGet(HttpServletRequest request, HttpServletResponse response) {
			this.response = response;
		}
	}

}
